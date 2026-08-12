package com.kiborisaway.tasktimetracker.repository;

import com.kiborisaway.tasktimetracker.data.entity.ProjectItemOrder;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProjectItemOrderRepository {

  /**
   * プロジェクト直下の並び順一覧をposition昇順で取得します。
   *
   * @param projectId プロジェクトID
   * @return 並び順一覧
   */
  @Select("SELECT * FROM project_item_orders WHERE project_id = #{projectId} ORDER BY position ASC")
  List<ProjectItemOrder> findAllInProjectOrdered(int projectId);

  /**
   * タスクをプロジェクト直下の末尾に追加します。
   *
   * @param projectId プロジェクトID
   * @param taskId    タスクID
   */
  @Insert("""
      INSERT INTO project_item_orders(project_id, task_id, task_group_id, position)
      VALUES(#{projectId}, #{taskId}, NULL,
        COALESCE((SELECT MAX(position) + 1 FROM project_item_orders WHERE project_id = #{projectId}), 0))
      """)
  void insertAppendForTask(int projectId, int taskId);

  /**
   * タスクグループをプロジェクト直下の末尾に追加します。
   *
   * @param projectId   プロジェクトID
   * @param taskGroupId タスクグループID
   */
  @Insert("""
      INSERT INTO project_item_orders(project_id, task_id, task_group_id, position)
      VALUES(#{projectId}, NULL, #{taskGroupId},
        COALESCE((SELECT MAX(position) + 1 FROM project_item_orders WHERE project_id = #{projectId}), 0))
      """)
  void insertAppendForTaskGroup(int projectId, int taskGroupId);

  /**
   * タスクの並び順を更新します。
   *
   * @param taskId   タスクID
   * @param position 新しい並び順
   * @return 更新を実行した件数
   */
  @Update("UPDATE project_item_orders SET position = #{position} WHERE task_id = #{taskId}")
  int updatePositionByTaskId(int taskId, int position);

  /**
   * タスクグループの並び順を更新します。
   *
   * @param taskGroupId タスクグループID
   * @param position    新しい並び順
   * @return 更新を実行した件数
   */
  @Update("UPDATE project_item_orders SET position = #{position} WHERE task_group_id = #{taskGroupId}")
  int updatePositionByTaskGroupId(int taskGroupId, int position);

  /**
   * タスクの並び順レコードを削除します。プロジェクト直下に存在しない場合は何も削除しません。
   *
   * @param taskId タスクID
   * @return 削除を実行した件数
   */
  @Delete("DELETE FROM project_item_orders WHERE task_id = #{taskId}")
  int deleteByTaskId(int taskId);

  /**
   * タスクグループの並び順レコードを削除します。
   *
   * @param taskGroupId タスクグループID
   * @return 削除を実行した件数
   */
  @Delete("DELETE FROM project_item_orders WHERE task_group_id = #{taskGroupId}")
  int deleteByTaskGroupId(int taskGroupId);

}
