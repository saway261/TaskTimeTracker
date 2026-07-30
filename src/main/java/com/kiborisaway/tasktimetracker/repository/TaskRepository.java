package com.kiborisaway.tasktimetracker.repository;

import com.kiborisaway.tasktimetracker.data.Task;
import java.util.List;
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
   * タスクの見積もり作業時間を変更します。 紐づくWorkSessionが存在する場合は更新できないようにサービス層で制御する想定です。
   *
   * @return 更新を実行した件数
   */
  @Update("""
      UPDATE tasks
      SET estimated_minutes = #{estimatedMinutes}
      WHERE id = #{id}
      """)
  int updateEstimateMinutes(Task task);

  @Update("""
      UPDATE tasks
      SET finished_at = NOW(),
          actual_minutes_cached = (
              SELECT COALESCE(SUM(minutes), 0)
              FROM work_sessions
              WHERE task_id = #{id}
          ),
          gap_minutes_cached = (
              SELECT COALESCE(SUM(minutes), 0)
              FROM work_sessions
              WHERE task_id = #{id}
          ) - estimated_minutes,
          gap_rate_cached = CASE
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
  int setFinished(int id);

}
