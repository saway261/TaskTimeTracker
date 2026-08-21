package com.kiborisaway.tasktimetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.kiborisaway.tasktimetracker.data.entity.Tag;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@MybatisTest
class TagRepositoryTest {

  private static final int USER_A = 1;
  private static final int USER_B = 2;

  @Autowired
  private TagRepository sut;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void 一覧検索_付与タスク数の降順_同数なら名前昇順で返ること() {
    int taskA = existingTaskIdForUser(USER_A);
    int taskB = anotherExistingTaskIdForUser(USER_A);

    int fewUsed = insertTag(USER_A, "たまに使う", "たまに使う");
    int oftenUsed = insertTag(USER_A, "よく使う", "よく使う");
    int unused = insertTag(USER_A, "未使用", "未使用");
    linkTask(taskA, fewUsed);
    linkTask(taskA, oftenUsed);
    linkTask(taskB, oftenUsed);

    List<TagRow> actual = sut.findAllByUserId(USER_A, false);

    assertThat(actual)
        .extracting(TagRow::getId, TagRow::getAssignedTaskCount)
        .containsExactly(
            tuple(oftenUsed, 2),
            tuple(fewUsed, 1),
            tuple(unused, 0));
  }

  @Test
  void 一覧検索_同じ付与タスク数なら名前の昇順で並ぶこと() {
    int c = insertTag(USER_A, "charlie", "charlie");
    int a = insertTag(USER_A, "alpha", "alpha");
    int b = insertTag(USER_A, "bravo", "bravo");

    List<TagRow> actual = sut.findAllByUserId(USER_A, false);

    assertThat(actual).extracting(TagRow::getId).containsExactly(a, b, c);
  }

  @Test
  void 一覧検索_includeArchivedがfalseならアーカイブ済みタグを含まないこと() {
    insertTag(USER_A, "アクティブ", "アクティブ");
    insertArchivedTag(USER_A, "アーカイブ済み", "アーカイブ済み");

    List<TagRow> actual = sut.findAllByUserId(USER_A, false);

    assertThat(actual).extracting(TagRow::getName).containsExactly("アクティブ");
  }

  @Test
  void 一覧検索_includeArchivedがtrueならアーカイブ済みタグも含むこと() {
    insertTag(USER_A, "アクティブ", "アクティブ");
    insertArchivedTag(USER_A, "アーカイブ済み", "アーカイブ済み");

    List<TagRow> actual = sut.findAllByUserId(USER_A, true);

    assertThat(actual).extracting(TagRow::getName)
        .containsExactlyInAnyOrder("アクティブ", "アーカイブ済み");
  }

  @Test
  void 一覧検索_他ユーザーのタグを含まないこと() {
    insertTag(USER_A, "ユーザーAのタグ", "ユーザーaのタグ");
    insertTag(USER_B, "ユーザーBのタグ", "ユーザーbのタグ");

    List<TagRow> actual = sut.findAllByUserId(USER_A, true);

    assertThat(actual).extracting(TagRow::getName).containsExactly("ユーザーAのタグ");
  }

  @Test
  void ID検索成功_IDと所有者が一致するタグを取得できること() {
    int tagId = insertTag(USER_A, "調査", "調査");

    Tag actual = sut.findByIdAndUserId(tagId, USER_A);

    assertThat(actual.getName()).isEqualTo("調査");
    assertThat(actual.getIsArchived()).isFalse();
  }

  @Test
  void ID検索失敗_存在しないIDを指定するとnullを返すこと() {
    assertThat(sut.findByIdAndUserId(99999, USER_A)).isNull();
  }

  @Test
  void ID検索失敗_所有者が一致しない場合はnullを返すこと() {
    int tagId = insertTag(USER_A, "調査", "調査");

    assertThat(sut.findByIdAndUserId(tagId, USER_B)).isNull();
  }

  @Test
  void 正規化名検索成功_一致するタグを取得できること() {
    insertTag(USER_A, "ＡＰＩ", "api");

    Tag actual = sut.findByUserIdAndNameNormalized(USER_A, "api");

    assertThat(actual.getName()).isEqualTo("ＡＰＩ");
  }

  @Test
  void 正規化名検索失敗_一致しない場合はnullを返すこと() {
    insertTag(USER_A, "調査", "調査");

    assertThat(sut.findByUserIdAndNameNormalized(USER_A, "せってい")).isNull();
  }

  @Test
  void 正規化名検索失敗_他ユーザーのタグは一致しないこと() {
    insertTag(USER_B, "調査", "調査");

    assertThat(sut.findByUserIdAndNameNormalized(USER_A, "調査")).isNull();
  }

  @Test
  void アクティブ件数取得_アーカイブ済みを除いた件数を返すこと() {
    insertTag(USER_A, "アクティブ1", "アクティブ1");
    insertTag(USER_A, "アクティブ2", "アクティブ2");
    insertArchivedTag(USER_A, "アーカイブ済み", "アーカイブ済み");
    insertTag(USER_B, "他ユーザー", "他ユーザー");

    assertThat(sut.countActiveByUserId(USER_A)).isEqualTo(2);
  }

  @Test
  void 登録成功_タグを登録でき採番されたIDが設定されアーカイブ状態は常にfalseで登録されること() {
    Tag tag = new Tag();
    tag.setUserId(USER_A);
    tag.setName("設定");
    tag.setNameNormalized("設定");

    sut.insert(tag);

    assertThat(tag.getId()).isNotNull();
    Tag registered = sut.findByIdAndUserId(tag.getId(), USER_A);
    assertThat(registered.getName()).isEqualTo("設定");
    assertThat(registered.getNameNormalized()).isEqualTo("設定");
    assertThat(registered.getIsArchived()).isFalse();
    assertThat(registered.getCreatedAt()).isNotNull();
  }

  @Test
  void 登録失敗_同一ユーザー内で正規化後の名前が重複する場合は一意制約違反が発生すること() {
    insertTag(USER_A, "API", "api");

    Tag duplicate = new Tag();
    duplicate.setUserId(USER_A);
    duplicate.setName("ＡＰＩ");
    duplicate.setNameNormalized("api");

    assertThatThrownBy(() -> sut.insert(duplicate))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 登録成功_別ユーザーであれば同じ正規化名を登録できること() {
    insertTag(USER_A, "API", "api");

    Tag sameNormalizedName = new Tag();
    sameNormalizedName.setUserId(USER_B);
    sameNormalizedName.setName("API");
    sameNormalizedName.setNameNormalized("api");

    sut.insert(sameNormalizedName);

    assertThat(sameNormalizedName.getId()).isNotNull();
  }

  @Test
  void 登録成功_NFKCで文字数が増える入力でも桁あふれにならないこと() {
    // ㍿（1文字）はNFKCで「株式会社」（4文字）に展開される。name(20文字以内)に含めても、
    // name_normalized(VARCHAR(60))に収まり桁あふれで失敗しないことを確認する
    // （docs/tag-implementation-plan.md §0-1-1-A）。
    String name = "㍿" + "あ".repeat(19); // 20文字
    String nameNormalized =
        java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFKC)
            .toLowerCase(java.util.Locale.ROOT);

    Tag tag = new Tag();
    tag.setUserId(USER_A);
    tag.setName(name);
    tag.setNameNormalized(nameNormalized);

    sut.insert(tag);

    assertThat(tag.getId()).isNotNull();
    Tag registered = sut.findByIdAndUserId(tag.getId(), USER_A);
    assertThat(registered.getName()).isEqualTo(name);
    assertThat(registered.getNameNormalized()).isEqualTo("株式会社" + "あ".repeat(19));
  }

  @Test
  void 登録失敗_nameが空白のみの場合は例外が発生すること() {
    Tag tag = new Tag();
    tag.setUserId(USER_A);
    tag.setName(" ");
    tag.setNameNormalized("dummy");

    assertThatThrownBy(() -> sut.insert(tag))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 登録失敗_nameNormalizedがトリムまたは小文字化されていない場合は例外が発生すること() {
    Tag tag = new Tag();
    tag.setUserId(USER_A);
    tag.setName("Bug");
    tag.setNameNormalized("Bug"); // 呼び出し側が正規化を忘れたケースを模擬（大文字を含む）

    assertThatThrownBy(() -> sut.insert(tag))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 登録失敗_存在しないuserIdを指定すると外部キー制約違反が発生すること() {
    Tag tag = new Tag();
    tag.setUserId(99999);
    tag.setName("調査");
    tag.setNameNormalized("調査");

    assertThatThrownBy(() -> sut.insert(tag))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void リネーム成功_名前と正規化名を更新できること() {
    int tagId = insertTag(USER_A, "調査", "調査");

    int updated = sut.updateName(tagId, USER_A, "リサーチ", "リサーチ");

    assertThat(updated).isEqualTo(1);
    Tag actual = sut.findByIdAndUserId(tagId, USER_A);
    assertThat(actual.getName()).isEqualTo("リサーチ");
    assertThat(actual.getNameNormalized()).isEqualTo("リサーチ");
  }

  @Test
  void リネーム成功_アーカイブ済みタグもリネームできること() {
    int tagId = insertArchivedTag(USER_A, "調査", "調査");

    int updated = sut.updateName(tagId, USER_A, "リサーチ", "リサーチ");

    assertThat(updated).isEqualTo(1);
    Tag actual = sut.findByIdAndUserId(tagId, USER_A);
    assertThat(actual.getName()).isEqualTo("リサーチ");
    assertThat(actual.getIsArchived()).isTrue();
  }

  @Test
  void リネーム失敗_存在しないIDの場合は更新されず0件となること() {
    int updated = sut.updateName(99999, USER_A, "リサーチ", "リサーチ");

    assertThat(updated).isZero();
  }

  @Test
  void リネーム失敗_所有者が一致しない場合は更新されず0件となること() {
    int tagId = insertTag(USER_A, "調査", "調査");

    int updated = sut.updateName(tagId, USER_B, "リサーチ", "リサーチ");

    assertThat(updated).isZero();
    assertThat(sut.findByIdAndUserId(tagId, USER_A).getName()).isEqualTo("調査");
  }

  @Test
  void アーカイブ状態更新成功_trueに更新できること() {
    int tagId = insertTag(USER_A, "調査", "調査");

    int updated = sut.updateArchived(tagId, USER_A, true);

    assertThat(updated).isEqualTo(1);
    assertThat(sut.findByIdAndUserId(tagId, USER_A).getIsArchived()).isTrue();
  }

  @Test
  void アーカイブ状態更新成功_falseに更新できること() {
    int tagId = insertArchivedTag(USER_A, "調査", "調査");

    int updated = sut.updateArchived(tagId, USER_A, false);

    assertThat(updated).isEqualTo(1);
    assertThat(sut.findByIdAndUserId(tagId, USER_A).getIsArchived()).isFalse();
  }

  @Test
  void アーカイブ状態更新失敗_所有者が一致しない場合は更新されず0件となること() {
    int tagId = insertTag(USER_A, "調査", "調査");

    int updated = sut.updateArchived(tagId, USER_B, true);

    assertThat(updated).isZero();
    assertThat(sut.findByIdAndUserId(tagId, USER_A).getIsArchived()).isFalse();
  }

  @Test
  void 付与タスク数取得_リンクされたタスクの件数を返すこと() {
    int tagId = insertTag(USER_A, "調査", "調査");
    int taskA = existingTaskIdForUser(USER_A);
    int taskB = anotherExistingTaskIdForUser(USER_A);
    linkTask(taskA, tagId);
    linkTask(taskB, tagId);

    assertThat(sut.countAssignedTasks(tagId)).isEqualTo(2);
  }

  @Test
  void 付与タスク数取得_リンクがなければ0を返すこと() {
    int tagId = insertTag(USER_A, "未使用", "未使用");

    assertThat(sut.countAssignedTasks(tagId)).isZero();
  }

  private int insertTag(int userId, String name, String nameNormalized) {
    Tag tag = new Tag();
    tag.setUserId(userId);
    tag.setName(name);
    tag.setNameNormalized(nameNormalized);
    sut.insert(tag);
    return tag.getId();
  }

  private int insertArchivedTag(int userId, String name, String nameNormalized) {
    int tagId = insertTag(userId, name, nameNormalized);
    sut.updateArchived(tagId, userId, true);
    return tagId;
  }

  private void linkTask(int taskId, int tagId) {
    jdbcTemplate.update(
        "INSERT INTO task_tags(task_id, tag_id) VALUES (?, ?)", taskId, tagId);
  }

  private int existingTaskIdForUser(int userId) {
    // data.sqlのフィクスチャに合わせる: id=1のタスクはプロジェクト1（ユーザーA）配下
    return userId == USER_A ? 1 : 8;
  }

  private int anotherExistingTaskIdForUser(int userId) {
    return userId == USER_A ? 2 : 9;
  }
}
