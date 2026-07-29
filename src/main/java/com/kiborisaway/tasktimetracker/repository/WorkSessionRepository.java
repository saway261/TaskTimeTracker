package com.kiborisaway.tasktimetracker.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WorkSessionRepository {

  /**
   * タスクIDに紐づく作業セッションの存在チェックを行います
   *
   * @param taskId タスクのID
   * @return 存在すればtrue, 存在しなければfalse
   */
  @Select("""
      SELECT EXISTS(
        SELECT 1
        FROM work_sessions
        WHERE task_id = #{taskId}
      )
      """)
  boolean existsByTaskId(int taskId);

}
