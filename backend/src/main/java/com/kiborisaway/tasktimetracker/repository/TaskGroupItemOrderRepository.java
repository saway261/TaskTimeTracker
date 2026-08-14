package com.kiborisaway.tasktimetracker.repository;

import com.kiborisaway.tasktimetracker.data.entity.TaskGroupItemOrder;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TaskGroupItemOrderRepository {

  /**
   * タスクグループ直下の並び順一覧をposition昇順で取得します。所有者が一致するプロジェクトのみを対象とします。
   *
   * @param taskGroupId タスクグループID
   * @return 並び順一覧
   */
  @Select("""
      SELECT tgio.* FROM task_group_item_orders tgio
      JOIN task_groups tg ON tg.id = tgio.task_group_id
      JOIN projects p ON p.id = tg.project_id
      WHERE tgio.task_group_id = #{taskGroupId} AND p.user_id = #{userId}
      ORDER BY tgio.position ASC
      """)
  List<TaskGroupItemOrder> findAllInTaskGroupOrdered(int taskGroupId, int userId);

  /**
   * タスクをタスクグループ直下の末尾に追加します。 呼び出し前にタスクグループの所有権が検証済みであることが前提です。
   *
   * @param taskGroupId タスクグループID
   * @param taskId      タスクID
   */
  @Insert("""
      INSERT INTO task_group_item_orders(task_group_id, task_id, position)
      VALUES(#{taskGroupId}, #{taskId},
        COALESCE((SELECT MAX(position) + 1 FROM task_group_item_orders WHERE task_group_id = #{taskGroupId}), 0))
      """)
  void insertAppendForTask(int taskGroupId, int taskId);

  /**
   * タスクの並び順を更新します。所有者が一致しない場合は更新されません。
   *
   * @param taskId   タスクID
   * @param position 新しい並び順
   * @return 更新を実行した件数
   */
  @Update("""
      UPDATE task_group_item_orders
      SET position = #{position}
      WHERE task_id = #{taskId}
        AND task_group_id IN (
          SELECT tg.id FROM task_groups tg
          JOIN projects p ON p.id = tg.project_id
          WHERE p.user_id = #{userId}
        )
      """)
  int updatePositionByTaskId(int taskId, int position, int userId);

  /**
   * タスクの並び順レコードを削除します。タスクグループ直下に存在しない場合は何も削除しません。
   * 呼び出し前にタスクの所有権が検証済みであることが前提です。
   *
   * @param taskId タスクID
   * @return 削除を実行した件数
   */
  @Delete("DELETE FROM task_group_item_orders WHERE task_id = #{taskId}")
  int deleteByTaskId(int taskId);

  /**
   * タスクグループ直下の並び順レコードを全件削除します。 呼び出し前にタスクグループの所有権が検証済みであることが前提です。
   *
   * @param taskGroupId タスクグループID
   * @return 削除を実行した件数
   */
  @Delete("DELETE FROM task_group_item_orders WHERE task_group_id = #{taskGroupId}")
  int deleteByTaskGroupId(int taskGroupId);

}
