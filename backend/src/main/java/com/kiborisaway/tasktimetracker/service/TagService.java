package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.dto.tag.TagCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.tag.TagResponse;
import com.kiborisaway.tasktimetracker.data.dto.tag.TagUpdateRequest;
import com.kiborisaway.tasktimetracker.data.entity.Tag;
import com.kiborisaway.tasktimetracker.exception.TagLimitExceededException;
import com.kiborisaway.tasktimetracker.exception.TagNameDuplicateException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.TagRepository;
import com.kiborisaway.tasktimetracker.repository.TagRow;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {

  /**
   * アクティブなタグ（アーカイブ済みを除く）の保有上限。
   * 目的はタグ設計そのものへの歯止めであり、上限に近づくと1タグあたりの母数が確保できなくなる
   * （docs/tag-requirements.md §3.4）。
   */
  private static final int MAX_ACTIVE_TAGS = 50;

  private TagRepository repository;

  @Autowired
  public TagService(TagRepository repository) {
    this.repository = repository;
  }

  /**
   * タグの新規作成結果。既存タグを再利用した場合はcreatedがfalseになる。
   * コントローラはこのフラグでステータスコード（201/200）を出し分ける。
   *
   * @param tag     作成または再利用されたタグ
   * @param created 新規作成であればtrue、既存タグを再利用したのであればfalse
   */
  public record CreateResult(TagResponse tag, boolean created) {

  }

  /**
   * 認証ユーザーが保有するタグを一覧検索します。
   *
   * @param userId          認証ユーザーのID
   * @param includeArchived trueの場合アーカイブ済みタグを含める
   * @return 付与タスク数つきのタグ一覧。付与タスク数の降順、同数の場合は名前の昇順
   */
  public List<TagResponse> findAll(int userId, boolean includeArchived) {
    return repository.findAllByUserId(userId, includeArchived).stream()
        .map(TagService::toResponse)
        .toList();
  }

  /**
   * タグを新規作成します。正規化後の名前が既存タグと一致する場合は、新規作成せず既存タグを返します
   * （docs/tag-requirements.md §3.3）。上限判定は既存タグの再利用判定より後に行うため、
   * 保有上限に達していても既存タグの再利用は妨げられません。
   *
   * @param userId  認証ユーザーのID
   * @param request タグ新規登録リクエスト
   * @return 作成結果（作成されたタグと、新規作成か再利用かを示すフラグ）
   */
  @Transactional
  public CreateResult create(int userId, TagCreateRequest request) {
    String name = request.getName().trim();
    String nameNormalized = normalize(name);

    Tag existing = repository.findByUserIdAndNameNormalized(userId, nameNormalized);
    if (existing != null) {
      return new CreateResult(toResponse(existing, repository.countAssignedTasks(existing.getId())),
          false);
    }

    if (repository.countActiveByUserId(userId) >= MAX_ACTIVE_TAGS) {
      throw new TagLimitExceededException("tagLimit",
          "保有できるタグの上限（" + MAX_ACTIVE_TAGS + "件）に達しています");
    }

    Tag tag = new Tag();
    tag.setUserId(userId);
    tag.setName(name);
    tag.setNameNormalized(nameNormalized);
    repository.insert(tag);
    // useGeneratedKeysはidのみを反映し、DB側のデフォルト値（is_archived）は反映しないため、
    // ここで明示的に補う。INSERT文自体は常にFALSEを書き込む（TagRepository#insert）。
    tag.setIsArchived(false);

    return new CreateResult(toResponse(tag, 0), true);
  }

  /**
   * タグ名を変更します。正規化後の名前が自分自身以外の既存タグと一致する場合は更新できません。
   *
   * @param userId  認証ユーザーのID
   * @param tagId   タグID
   * @param request タグ名変更リクエスト
   * @return 更新後のタグ
   */
  @Transactional
  public TagResponse update(int userId, int tagId, TagUpdateRequest request) {
    String name = request.getName().trim();
    String nameNormalized = normalize(name);

    Tag duplicate = repository.findByUserIdAndNameNormalized(userId, nameNormalized);
    if (duplicate != null && !duplicate.getId().equals(tagId)) {
      throw new TagNameDuplicateException("name", "同じ名前のタグが既に存在します");
    }

    int updated = repository.updateName(tagId, userId, name, nameNormalized);
    if (updated == 0) {
      throw new TargetNotFoundException("tag.id", "更新対象のタグが見つかりませんでした");
    }

    Tag tag = findTagById(userId, tagId);
    return toResponse(tag, repository.countAssignedTasks(tag.getId()));
  }

  /**
   * タグのアーカイブ状態を更新します。アーカイブ済みタグを解除する場合、保有上限に達していると
   * 更新できません（アクティブなタグが1件増えるため。docs/tag-requirements.md §3.4）。
   * 既にアクティブなタグへ isArchived=false を送る、または既にアーカイブ済みのタグへ isArchived=true を
   * 送るなど状態が変わらない要求は上限に関係なく成功します。
   *
   * @param userId     認証ユーザーのID
   * @param tagId      タグID
   * @param isArchived アーカイブ状態
   * @return 更新後のタグ
   */
  @Transactional
  public TagResponse updateArchived(int userId, int tagId, boolean isArchived) {
    Tag tag = findTagById(userId, tagId);

    boolean willIncreaseActiveCount = Boolean.TRUE.equals(tag.getIsArchived()) && !isArchived;
    if (willIncreaseActiveCount && repository.countActiveByUserId(userId) >= MAX_ACTIVE_TAGS) {
      throw new TagLimitExceededException("tagLimit",
          "保有できるタグの上限（" + MAX_ACTIVE_TAGS + "件）に達しています");
    }

    repository.updateArchived(tagId, userId, isArchived);

    Tag updated = findTagById(userId, tagId);
    return toResponse(updated, repository.countAssignedTasks(updated.getId()));
  }

  private Tag findTagById(int userId, int tagId) {
    Tag tag = repository.findByIdAndUserId(tagId, userId);
    if (tag == null) {
      throw new TargetNotFoundException("tag.id", "指定したIDのタグは見つかりませんでした");
    }
    return tag;
  }

  /**
   * タグ名の同一性判定に用いる正規化を行います。大文字・小文字、全角・半角英数字、半角・全角カナの
   * 違いを吸収します（Unicode NFKC ＋ 小文字化）。表示に用いる名前には適用しません
   * （docs/tag-implementation-plan.md §0-1-1-A）。
   *
   * @param trimmedName トリム済みのタグ名
   * @return 正規化後の値
   */
  private String normalize(String trimmedName) {
    return Normalizer.normalize(trimmedName, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
  }

  private static TagResponse toResponse(TagRow row) {
    return new TagResponse(row.getId(), row.getName(), row.getIsArchived(),
        row.getAssignedTaskCount());
  }

  private static TagResponse toResponse(Tag tag, int assignedTaskCount) {
    return new TagResponse(tag.getId(), tag.getName(), tag.getIsArchived(), assignedTaskCount);
  }
}
