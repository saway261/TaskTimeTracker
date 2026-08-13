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
   * タスクグループ直下の並び順一覧をposition昇順で取得します。
   *
   * @param taskGroupId タスクグループID
   * @return 並び順一覧
   */
  @Select("SELECT * FROM task_group_item_orders WHERE task_group_id = #{taskGroupId} ORDER BY position ASC")
  List<TaskGroupItemOrder> findAllInTaskGroupOrdered(int taskGroupId);

  /**
   * タスクをタスクグループ直下の末尾に追加します。
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
   * タスクの並び順を更新します。
   *
   * @param taskId   タスクID
   * @param position 新しい並び順
   * @return 更新を実行した件数
   */
  @Update("UPDATE task_group_item_orders SET position = #{position} WHERE task_id = #{taskId}")
  int updatePositionByTaskId(int taskId, int position);

  /**
   * タスクの並び順レコードを削除します。タスクグループ直下に存在しない場合は何も削除しません。
   *
   * @param taskId タスクID
   * @return 削除を実行した件数
   */
  @Delete("DELETE FROM task_group_item_orders WHERE task_id = #{taskId}")
  int deleteByTaskId(int taskId);

  /**
   * タスクグループ直下の並び順レコードを全件削除します。
   *
   * @param taskGroupId タスクグループID
   * @return 削除を実行した件数
   */
  @Delete("DELETE FROM task_group_item_orders WHERE task_group_id = #{taskGroupId}")
  int deleteByTaskGroupId(int taskGroupId);

}
