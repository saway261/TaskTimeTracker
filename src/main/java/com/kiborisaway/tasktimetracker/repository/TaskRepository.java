package com.kiborisaway.tasktimetracker.repository;

import com.kiborisaway.tasktimetracker.data.Task;
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
   * タスクグループ内のタスクの全件検索を行います。
   *
   * @return タスク一覧
   */
  @Select("SELECT * FROM tasks WHERE task_group_id=#{taskGroupId}")
  List<Task> findAllInTaskGroup(int taskGroupId);

  /**
   * 完了フラグを指定してタスクグループ内のタスクの全件検索を行います。
   *
   * @return タスク一覧
   */
  @Select("""
        SELECT * FROM tasks
        WHERE task_group_id=#{taskGroupId}
        AND (finished_at IS NOT NULL) = #{isFinished}
      """)
  List<Task> findAllInTaskGroupByCondition(int taskGroupId, boolean isFinished);

  /**
   * プロジェクト内のタスクの全件検索を行います。
   *
   * @return タスク一覧
   */
  @Select("""
      SELECT * FROM tasks
      WHERE project_id=#{projectId}
      OR task_group_id IN (
        SELECT id FROM task_groups
        WHERE project_id=#{projectId}
        )
      """)
  List<Task> findAllInProject(int projectId);

  /**
   * 完了フラグを指定してプロジェクト内のタスクを検索します。
   *
   * @param isFinished 完了フラグ
   * @return 指定した完了状態のタスク一覧
   */
  @Select("""
      SELECT * FROM tasks
      WHERE (
          project_id=#{projectId}
          OR task_group_id IN (
            SELECT id FROM task_groups
            WHERE project_id=#{projectId}
          )
        )
        AND (finished_at IS NOT NULL) = #{isFinished}
      """)
  List<Task> findAllInProjectByCondition(int projectId, boolean isFinished);

  /**
   * IDによるタスクの単一検索を行います
   *
   * @param id タスクのID
   * @return タスク
   */
  @Select("SELECT * FROM tasks WHERE id=#{id}")
  Task findById(int id);

  /**
   * IDによるタスクの存在チェックを行います。
   *
   * @param id タスクのID
   * @return 存在すればtrue, 存在しなければfalse
   */
  @Select("""
      SELECT EXISTS(
        SELECT 1
        FROM tasks
        WHERE id = #{id}
      )
      """)
  boolean existsById(int id);

  /**
   * 指定したタスクが完了状態かチェックします。
   *
   * @param id タスクのID
   * @return 完了状態ならtrue, 未完了または存在しなければfalse
   */
  @Select("""
      SELECT EXISTS(
        SELECT 1
        FROM tasks
        WHERE id = #{id}
          AND finished_at IS NOT NULL
      )
      """)
  boolean isFinished(int id);

  /**
   * タスクの新規追加を行います。 完了フラグは新規追加時にはfalseとなります。
   *
   * @param task タスク
   */
  @Insert("""
      INSERT INTO tasks(project_id, task_group_id, title, description, estimated_minutes, created_at)
      VALUES(#{projectId}, #{taskGroupId}, #{title}, #{description}, #{estimatedMinutes}, NOW())
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  void insert(Task task);

  /**
   * タスクの名前と説明を変更できます。
   *
   * @return 更新を実行した件数
   */
  @Update("""
      UPDATE tasks
      SET title=#{title}, description=#{description}
      WHERE id=#{id}
      """)
  int updateProperty(Task task);

  /**
   * タスクの所属を指定したタスクグループ配下へ変更します。
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
      """)
  int updateTaskGroup(int id, int taskGroupId);

  /**
   * タスクの所属を指定したプロジェクト直下へ変更します。 ただし同一プロジェクト配下のタスクグループからプロジェクト直下に映すことを想定します。
   * プロジェクト間の移動はできないようサービス層で制御します。
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
      """)
  int updateProject(int id, int projectId);

  /**
   * タスクの見積もり作業時間を変更します。 紐づくWorkSessionが存在する場合は更新できないようにサービス層で制御する想定です。
   *
   * @return 更新を実行した件数
   */
  @Update("""
      UPDATE tasks
      SET estimated_minutes = #{estimatedMinutes}
      WHERE id = #{id}
      """)
  int updateEstimateMinutes(int id, int estimatedMinutes);

  /**
   * タスクの完了状態を更新します。完了にする場合は実績時間と差分をキャッシュし、未完了に戻す場合は完了日時とキャッシュを削除します。
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
      """)
  int updateFinished(int id, boolean isFinished);

  /**
   * IDを指定してタスクを削除します。
   *
   * @param id タスクのID
   * @return 削除を実行した件数
   */
  @Delete("DELETE FROM tasks WHERE id = #{id}")
  int deleteById(int id);

}
