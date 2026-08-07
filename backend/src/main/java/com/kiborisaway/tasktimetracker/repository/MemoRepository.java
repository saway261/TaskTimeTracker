package com.kiborisaway.tasktimetracker.repository;

import com.kiborisaway.tasktimetracker.data.entity.Memo;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MemoRepository {


  @Select("SELECT * FROM memos WHERE project_id = #{projectId}")
  List<Memo> findAllInProject(int projectId);

  @Select("SELECT * FROM memos WHERE task_group_id = #{taskGroupId}")
  List<Memo> findAllInTaskGroup(int taskGroupId);

  @Select("SELECT * FROM memos WHERE task_id = #{taskId}")
  List<Memo> findAllInTask(int taskId);

  @Insert("""
      INSERT INTO memos(project_id, task_group_id, task_id, comment)
      VALUES(#{projectId},#{taskGroupId},#{taskId},#{comment})
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  void insert(Memo memo);

  @Update("UPDATE memos SET comment = #{comment} WHERE id = #{id}")
  int update(Memo memo);

  /**
   * IDを指定してメモを削除します。
   *
   * @param id メモのID
   * @return 削除を実行した件数
   */
  @Delete("DELETE FROM memos WHERE id = #{id}")
  int delete(int id);

  /**
   * タスクIDを指定してタスク配下のメモを削除します。
   *
   * @param taskId タスクのID
   * @return 削除を実行した件数
   */
  @Delete("DELETE FROM memos WHERE task_id = #{taskId}")
  int deleteAllInTask(int taskId);
}
