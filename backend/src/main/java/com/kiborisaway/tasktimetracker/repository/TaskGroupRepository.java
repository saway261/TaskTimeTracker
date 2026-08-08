package com.kiborisaway.tasktimetracker.repository;

import com.kiborisaway.tasktimetracker.data.entity.TaskGroup;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TaskGroupRepository {

  /**
   * プロジェクト内タスクグループの全件検索を行います。
   *
   * @return プロジェクト内のタスクグループ一覧
   */
  @Select("SELECT * FROM task_groups WHERE project_id=#{projectId}")
  List<TaskGroup> findAllInProject(int projectId);

  /**
   * プロジェクト内で完了フラグを指定してタスクグループを検索します。
   *
   * @param isFinished 完了フラグ
   * @return プロジェクト内の指定した完了状態のタスクグループ一覧
   */
  @Select("SELECT * FROM task_groups WHERE project_id=#{projectId} AND is_finished=#{isFinished}")
  List<TaskGroup> findAllInProjectByIsFinished(int projectId, boolean isFinished);

  /**
   * IDによるタスクグループの単一検索を行います
   *
   * @param id タスクグループのID
   * @return タスクグループ
   */
  @Select("SELECT * FROM task_groups WHERE id=#{id}")
  TaskGroup findById(int id);

  /**
   * IDによるタスクグループの存在チェックを行います
   *
   * @param id タスクグループのID
   * @return 存在すればtrue, 存在しなければfalse
   */
  @Select("""
      SELECT EXISTS(
        SELECT 1
        FROM task_groups
        WHERE id = #{id}
      )
      """)
  boolean existsById(int id);

  /**
   * タスクグループの新規追加を行います。 完了フラグは新規追加時にはfalseとなります。
   *
   * @param taskGroup タスクグループ
   */
  @Insert("INSERT INTO task_groups(project_id, title, description, is_finished)"
      + " VALUES(#{projectId}, #{title}, #{description}, false)")
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  void insert(TaskGroup taskGroup);

  /**
   * タスクグループのの更新を行います。タスクグループ名と説明と完了フラグを変更できます。 未変更の項目はDBに既存の値のままフロントエンドから返される想定で、全体更新します。
   *
   * @param taskGroup タスクグループ
   * @return 更新を実行した件数
   */
  @Update(
      "UPDATE task_groups SET title=#{title}, description=#{description}, is_finished=#{isFinished} "
          + "WHERE id=#{id}")
  int update(TaskGroup taskGroup);

}
