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
   * プロジェクト内タスクグループの全件検索を行います。所有者が一致するプロジェクトのみを対象とします。
   *
   * @return プロジェクト内のタスクグループ一覧
   */
  @Select("""
      SELECT tg.* FROM task_groups tg
      JOIN projects p ON p.id = tg.project_id
      WHERE tg.project_id=#{projectId} AND p.user_id=#{userId}
      """)
  List<TaskGroup> findAllInProject(int projectId, int userId);

  /**
   * プロジェクト内で完了フラグを指定してタスクグループを検索します。所有者が一致するプロジェクトのみを対象とします。
   *
   * @param isFinished 完了フラグ
   * @return プロジェクト内の指定した完了状態のタスクグループ一覧
   */
  @Select("""
      SELECT tg.* FROM task_groups tg
      JOIN projects p ON p.id = tg.project_id
      WHERE tg.project_id=#{projectId} AND tg.is_finished=#{isFinished} AND p.user_id=#{userId}
      """)
  List<TaskGroup> findAllInProjectByIsFinished(int projectId, boolean isFinished, int userId);

  /**
   * IDによるタスクグループの単一検索を行います。所有者が一致するプロジェクトのみを対象とします。
   *
   * @param id タスクグループのID
   * @return タスクグループ
   */
  @Select("""
      SELECT tg.* FROM task_groups tg
      JOIN projects p ON p.id = tg.project_id
      WHERE tg.id=#{id} AND p.user_id=#{userId}
      """)
  TaskGroup findById(int id, int userId);

  /**
   * IDと所有者によるタスクグループの存在チェックを行います
   *
   * @param id タスクグループのID
   * @return 存在すればtrue, 存在しなければfalse
   */
  @Select("""
      SELECT EXISTS(
        SELECT 1
        FROM task_groups tg
        JOIN projects p ON p.id = tg.project_id
        WHERE tg.id = #{id} AND p.user_id = #{userId}
      )
      """)
  boolean existsByIdAndUserId(int id, int userId);

  /**
   * タスクグループの新規追加を行います。 完了フラグは新規追加時にはfalseとなります。
   * 親プロジェクトの所有権はService層で事前に確認済みであることが前提です。
   *
   * @param taskGroup タスクグループ
   */
  @Insert("INSERT INTO task_groups(project_id, title, description, is_finished)"
      + " VALUES(#{projectId}, #{title}, #{description}, false)")
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  void insert(TaskGroup taskGroup);

  /**
   * タスクグループのの更新を行います。タスクグループ名と説明と完了フラグを変更できます。 未変更の項目はDBに既存の値のままフロントエンドから返される想定で、全体更新します。
   * 所有者が一致しない場合は更新されません。
   *
   * @param taskGroup タスクグループ
   * @return 更新を実行した件数
   */
  @Update("""
      UPDATE task_groups
      SET title=#{taskGroup.title}, description=#{taskGroup.description}, is_finished=#{taskGroup.isFinished}
      WHERE id=#{taskGroup.id}
        AND project_id IN (SELECT id FROM projects WHERE user_id=#{userId})
      """)
  int update(TaskGroup taskGroup, int userId);

}
