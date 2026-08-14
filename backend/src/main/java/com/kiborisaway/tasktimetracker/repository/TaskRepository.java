package com.kiborisaway.tasktimetracker.repository;

import com.kiborisaway.tasktimetracker.data.entity.Task;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TaskRepository {

  /**
   * タスクグループ内のタスクの全件検索を行います。所有者が一致するプロジェクトのみを対象とします。
   *
   * @return タスク一覧
   */
  @Select("""
      SELECT t.* FROM tasks t
      JOIN task_groups tg ON tg.id = t.task_group_id
      JOIN projects p ON p.id = tg.project_id
      WHERE t.task_group_id=#{taskGroupId} AND p.user_id=#{userId}
      """)
  List<Task> findAllInTaskGroup(int taskGroupId, int userId);

  /**
   * 完了フラグを指定してタスクグループ内のタスクの全件検索を行います。所有者が一致するプロジェクトのみを対象とします。
   *
   * @return タスク一覧
   */
  @Select("""
      SELECT t.* FROM tasks t
      JOIN task_groups tg ON tg.id = t.task_group_id
      JOIN projects p ON p.id = tg.project_id
      WHERE t.task_group_id=#{taskGroupId}
        AND (t.finished_at IS NOT NULL) = #{isFinished}
        AND p.user_id=#{userId}
      """)
  List<Task> findAllInTaskGroupByCondition(int taskGroupId, boolean isFinished, int userId);

  /**
   * プロジェクト内のタスクの全件検索を行います。所有者が一致するプロジェクトのみを対象とします。
   *
   * @return タスク一覧
   */
  @Select("""
      SELECT t.* FROM tasks t
      LEFT JOIN task_groups tg ON tg.id = t.task_group_id
      JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
      WHERE (
          t.project_id=#{projectId}
          OR t.task_group_id IN (
            SELECT id FROM task_groups
            WHERE project_id=#{projectId}
          )
        )
        AND p.user_id=#{userId}
      """)
  List<Task> findAllInProject(int projectId, int userId);

  /**
   * 完了フラグを指定してプロジェクト内のタスクを検索します。所有者が一致するプロジェクトのみを対象とします。
   *
   * @param isFinished 完了フラグ
   * @return 指定した完了状態のタスク一覧
   */
  @Select("""
      SELECT t.* FROM tasks t
      LEFT JOIN task_groups tg ON tg.id = t.task_group_id
      JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
      WHERE (
          t.project_id=#{projectId}
          OR t.task_group_id IN (
            SELECT id FROM task_groups
            WHERE project_id=#{projectId}
          )
        )
        AND (t.finished_at IS NOT NULL) = #{isFinished}
        AND p.user_id=#{userId}
      """)
  List<Task> findAllInProjectByCondition(int projectId, boolean isFinished, int userId);

  /**
   * IDによるタスクの単一検索を行います。所有者が一致するプロジェクトのみを対象とします。
   *
   * @param id タスクのID
   * @return タスク
   */
  @Select("""
      SELECT t.* FROM tasks t
      LEFT JOIN task_groups tg ON tg.id = t.task_group_id
      JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
      WHERE t.id=#{id} AND p.user_id=#{userId}
      """)
  Task findById(int id, int userId);

  /**
   * IDと所有者によるタスクの存在チェックを行います。
   *
   * @param id タスクのID
   * @return 存在すればtrue, 存在しなければfalse
   */
  @Select("""
      SELECT EXISTS(
        SELECT 1
        FROM tasks t
        LEFT JOIN task_groups tg ON tg.id = t.task_group_id
        JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
        WHERE t.id = #{id} AND p.user_id = #{userId}
      )
      """)
  boolean existsByIdAndUserId(int id, int userId);

  /**
   * 指定したタスクが完了状態かチェックします。所有者が一致しない場合は常にfalseを返します。
   *
   * @param id タスクのID
   * @return 完了状態ならtrue, 未完了・存在しない・所有者が一致しなければfalse
   */
  @Select("""
      SELECT EXISTS(
        SELECT 1
        FROM tasks t
        LEFT JOIN task_groups tg ON tg.id = t.task_group_id
        JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
        WHERE t.id = #{id}
          AND t.finished_at IS NOT NULL
          AND p.user_id = #{userId}
      )
      """)
  boolean isFinished(int id, int userId);

  /**
   * タスクの新規追加を行います。 完了フラグは新規追加時にはfalseとなります。
   * 親プロジェクト・タスクグループの所有権はService層で事前に確認済みであることが前提です。
   *
   * @param task タスク
   */
  @Insert("""
      INSERT INTO tasks(project_id, task_group_id, title, description, estimated_minutes, created_at)
      VALUES(#{projectId}, #{taskGroupId}, #{title}, #{description}, #{estimatedMinutes}, NOW())
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  void insert(Task task);

  /**
   * タスクの名前と説明を変更できます。所有者が一致しない場合は更新されません。
   *
   * @return 更新を実行した件数
   */
  @Update("""
      UPDATE tasks
      SET title=#{task.title}, description=#{task.description}
      WHERE id=#{task.id}
        AND EXISTS (
          SELECT 1 FROM tasks t
          LEFT JOIN task_groups tg ON tg.id = t.task_group_id
          JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
          WHERE t.id = tasks.id AND p.user_id = #{userId}
        )
      """)
  int updateProperty(Task task, int userId);

  /**
   * タスクの所属を指定したタスクグループ配下へ変更します。所有者が一致しない場合は更新されません。
   *
   * @param id          タスクのID
   * @param taskGroupId 移動先タスクグループのID
   * @return 更新を実行した件数
   */
  @Update("""
      UPDATE tasks
      SET project_id = NULL,
          task_group_id = #{taskGroupId}
      WHERE id = #{id}
        AND EXISTS (
          SELECT 1 FROM tasks t
          LEFT JOIN task_groups tg ON tg.id = t.task_group_id
          JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
          WHERE t.id = tasks.id AND p.user_id = #{userId}
        )
      """)
  int updateTaskGroup(int id, int taskGroupId, int userId);

  /**
   * タスクの所属を指定したプロジェクト直下へ変更します。 ただし同一プロジェクト配下のタスクグループからプロジェクト直下に映すことを想定します。
   * プロジェクト間の移動はできないようサービス層で制御します。所有者が一致しない場合は更新されません。
   *
   * @param id        タスクのID
   * @param projectId 移動先プロジェクトのID
   * @return 更新を実行した件数
   */
  @Update("""
      UPDATE tasks
      SET project_id = #{projectId},
          task_group_id = NULL
      WHERE id = #{id}
        AND EXISTS (
          SELECT 1 FROM tasks t
          LEFT JOIN task_groups tg ON tg.id = t.task_group_id
          JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
          WHERE t.id = tasks.id AND p.user_id = #{userId}
        )
      """)
  int updateProject(int id, int projectId, int userId);

  /**
   * タスクの見積もり作業時間を変更します。 紐づくWorkSessionが存在する場合は更新できないようにサービス層で制御する想定です。
   * 所有者が一致しない場合は更新されません。
   *
   * @return 更新を実行した件数
   */
  @Update("""
      UPDATE tasks
      SET estimated_minutes = #{estimatedMinutes}
      WHERE id = #{id}
        AND EXISTS (
          SELECT 1 FROM tasks t
          LEFT JOIN task_groups tg ON tg.id = t.task_group_id
          JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
          WHERE t.id = tasks.id AND p.user_id = #{userId}
        )
      """)
  int updateEstimateMinutes(int id, int estimatedMinutes, int userId);

  /**
   * タスクの完了状態を更新します。完了にする場合は実績時間と差分をキャッシュし、未完了に戻す場合は完了日時とキャッシュを削除します。
   * 所有者が一致しない場合は更新されません。
   *
   * @param id         タスクのID
   * @param isFinished 完了状態
   * @return 更新を実行した件数
   */
  @Update("""
      UPDATE tasks
      SET finished_at = CASE
              WHEN #{isFinished} THEN NOW()
              ELSE NULL
          END,
          actual_minutes_cached = CASE
              WHEN #{isFinished} THEN (
                  SELECT COALESCE(SUM(minutes), 0)
                  FROM work_sessions
                  WHERE task_id = #{id}
              )
              ELSE NULL
          END,
          gap_minutes_cached = CASE
              WHEN #{isFinished} THEN (
                  (
                      SELECT COALESCE(SUM(minutes), 0)
                      FROM work_sessions
                      WHERE task_id = #{id}
                  ) - estimated_minutes
              )
              ELSE NULL
          END,
          gap_rate_cached = CASE
              WHEN NOT #{isFinished} THEN NULL
              WHEN estimated_minutes IS NULL OR estimated_minutes = 0 THEN NULL
              ELSE (
                  (
                      SELECT COALESCE(SUM(minutes), 0)
                      FROM work_sessions
                      WHERE task_id = #{id}
                  ) - estimated_minutes
              ) * 100.0 / estimated_minutes
          END
      WHERE id = #{id}
        AND EXISTS (
          SELECT 1 FROM tasks t
          LEFT JOIN task_groups tg ON tg.id = t.task_group_id
          JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
          WHERE t.id = tasks.id AND p.user_id = #{userId}
        )
      """)
  int updateFinished(int id, boolean isFinished, int userId);

  /**
   * IDを指定してタスクを削除します。所有者が一致しない場合は削除されません。
   *
   * @param id タスクのID
   * @return 削除を実行した件数
   */
  @Delete("""
      DELETE FROM tasks
      WHERE id = #{id}
        AND EXISTS (
          SELECT 1 FROM tasks t
          LEFT JOIN task_groups tg ON tg.id = t.task_group_id
          JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
          WHERE t.id = tasks.id AND p.user_id = #{userId}
        )
      """)
  int deleteById(int id, int userId);

}
