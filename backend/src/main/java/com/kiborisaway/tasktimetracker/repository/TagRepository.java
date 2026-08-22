package com.kiborisaway.tasktimetracker.repository;

import com.kiborisaway.tasktimetracker.data.entity.Tag;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TagRepository {

  /**
   * 認証ユーザーが保有するタグを、付与タスク数つきで一覧検索します。
   * 付与タスク数の降順、同数の場合は名前の昇順で返します。
   *
   * @param userId          認証ユーザーのID
   * @param includeArchived trueの場合アーカイブ済みタグを含める
   * @return タグ一覧（付与タスク数つき）
   */
  @Select("""
      SELECT
        t.id AS id,
        t.name AS name,
        t.is_archived AS is_archived,
        COUNT(tt.task_id) AS assigned_task_count
      FROM tags t
      LEFT JOIN task_tags tt ON tt.tag_id = t.id
      WHERE t.user_id = #{userId}
        AND (#{includeArchived} = TRUE OR t.is_archived = FALSE)
      GROUP BY t.id, t.name, t.is_archived
      ORDER BY COUNT(tt.task_id) DESC, t.name ASC
      """)
  List<TagRow> findAllByUserId(int userId, boolean includeArchived);

  /**
   * IDと所有者による単一検索を行います。
   *
   * @param tagId  タグID
   * @param userId 認証ユーザーのID
   * @return タグ。存在しない、または他ユーザーのものの場合はnull
   */
  @Select("SELECT * FROM tags WHERE id = #{tagId} AND user_id = #{userId}")
  Tag findByIdAndUserId(int tagId, int userId);

  /**
   * 正規化後の名前が一致する既存タグを検索します。新規作成時の再利用判定に用います。
   *
   * @param userId         認証ユーザーのID
   * @param nameNormalized 正規化済みのタグ名
   * @return 一致するタグ。存在しない場合はnull
   */
  @Select("SELECT * FROM tags WHERE user_id = #{userId} AND name_normalized = #{nameNormalized}")
  Tag findByUserIdAndNameNormalized(int userId, String nameNormalized);

  /**
   * 認証ユーザーが保有するアクティブなタグ（アーカイブ済みを除く）の件数を取得します。
   * 保有上限の検証に用います。
   *
   * @param userId 認証ユーザーのID
   * @return アクティブなタグの件数
   */
  @Select("SELECT COUNT(*) FROM tags WHERE user_id = #{userId} AND is_archived = FALSE")
  int countActiveByUserId(int userId);

  /**
   * タグの新規登録を行います。アーカイブ状態は新規登録時には常にfalseとなります。
   *
   * @param tag 登録するタグ（userId・name・nameNormalizedを設定した状態で渡す）
   */
  @Insert("""
      INSERT INTO tags(user_id, name, name_normalized, is_archived, created_at)
      VALUES(#{userId}, #{name}, #{nameNormalized}, FALSE, NOW())
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  void insert(Tag tag);

  /**
   * タグ名を変更します。所有者が一致しない場合は更新されません。
   *
   * @param tagId          タグID
   * @param userId         認証ユーザーのID
   * @param name           新しいタグ名
   * @param nameNormalized 新しいタグ名の正規化後の値
   * @return 更新を実行した件数
   */
  @Update("""
      UPDATE tags SET name = #{name}, name_normalized = #{nameNormalized}
      WHERE id = #{tagId} AND user_id = #{userId}
      """)
  int updateName(int tagId, int userId, String name, String nameNormalized);

  /**
   * タグのアーカイブ状態を更新します。所有者が一致しない場合は更新されません。
   *
   * @param tagId      タグID
   * @param userId     認証ユーザーのID
   * @param isArchived アーカイブ状態
   * @return 更新を実行した件数
   */
  @Update("UPDATE tags SET is_archived = #{isArchived} WHERE id = #{tagId} AND user_id = #{userId}")
  int updateArchived(int tagId, int userId, boolean isArchived);

  /**
   * タグに付与されているタスクの件数（未完了を含む）を取得します。
   *
   * @param tagId タグID
   * @return 付与タスク数
   */
  @Select("SELECT COUNT(*) FROM task_tags WHERE tag_id = #{tagId}")
  int countAssignedTasks(int tagId);

  /**
   * タスクへタグを付与します。呼び出し前にタスクの所有権とタグの有効性が検証済みであることが前提です。
   *
   * @param taskId タスクID
   * @param tagId  タグID
   */
  @Insert("INSERT INTO task_tags(task_id, tag_id) VALUES(#{taskId}, #{tagId})")
  void insertLink(int taskId, int tagId);

  /**
   * タスクに付与されているタグのリンクをすべて削除します。全置換の前処理・タスク削除時に使います。
   *
   * @param taskId タスクID
   * @return 削除件数
   */
  @Delete("DELETE FROM task_tags WHERE task_id = #{taskId}")
  int deleteLinksByTaskId(int taskId);

  /**
   * タスクに付与されているタグを名前の昇順で取得します。
   *
   * @param taskId タスクID
   * @return タグの要約一覧
   */
  @Select("""
      SELECT t.id AS id, t.name AS name
      FROM task_tags tt
      JOIN tags t ON t.id = tt.tag_id
      WHERE tt.task_id = #{taskId}
      ORDER BY t.name
      """)
  List<TagSummaryRow> findTagsByTaskId(int taskId);

  /**
   * 指定したタグIDのうち、認証ユーザーが所有するものの件数を取得します。
   * リクエストに含まれる全タグIDが有効（存在し、かつ自分のもの）かどうかを1クエリで検証するために使います。
   *
   * @param userId 認証ユーザーのID
   * @param tagIds 検証するタグID一覧
   * @return 所有するタグの件数
   */
  @Select("""
      <script>
      SELECT COUNT(*) FROM tags
      WHERE user_id = #{userId}
        AND id IN
        <foreach collection="tagIds" item="id" open="(" separator="," close=")">
          #{id}
        </foreach>
      </script>
      """)
  int countOwnedByIds(@Param("userId") int userId, @Param("tagIds") List<Integer> tagIds);

  /**
   * 複数タスクに付与されているタグを一括取得します。タスク件数に比例したクエリの発行を避けるために使います。
   *
   * @param taskIds タスクID一覧
   * @return タスクIDごとのタグ付与行。タスクID・名前の昇順で並ぶ
   */
  @Select("""
      <script>
      SELECT tt.task_id AS task_id, t.id AS tag_id, t.name AS name
      FROM task_tags tt
      JOIN tags t ON t.id = tt.tag_id
      WHERE tt.task_id IN
      <foreach collection="taskIds" item="id" open="(" separator="," close=")">
        #{id}
      </foreach>
      ORDER BY tt.task_id, t.name
      </script>
      """)
  List<TaskTagRow> findTagsInTasks(@Param("taskIds") List<Integer> taskIds);
}
