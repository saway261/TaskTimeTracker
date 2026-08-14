package com.kiborisaway.tasktimetracker.repository;

import com.kiborisaway.tasktimetracker.data.entity.Memo;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * メモの親（プロジェクト・タスクグループ・タスク）は事前に呼び出し元Serviceで所有権検証済みのIDのみを渡す前提のため、
 * 親IDによる一覧検索系メソッドは所有者条件を持たない（6-1の内部カスケード規則）。
 * ただしIDを直接指定するupdate・deleteは所有者条件を必須とする。
 */
@Mapper
public interface MemoRepository {


  @Select("SELECT * FROM memos WHERE project_id = #{projectId}")
  List<Memo> findAllInProject(int projectId);

  @Select("SELECT * FROM memos WHERE task_group_id = #{taskGroupId}")
  List<Memo> findAllInTaskGroup(int taskGroupId);

  @Select("SELECT * FROM memos WHERE task_id = #{taskId}")
  List<Memo> findAllInTask(int taskId);

  @Select("""
      <script>
      SELECT *
      FROM memos
      WHERE project_id IN
      <foreach collection="projectIds" item="id" open="(" separator="," close=")">
        #{id}
      </foreach>
      ORDER BY project_id, id
      </script>
      """)
  List<Memo> findAllInProjects(@Param("projectIds") List<Integer> projectIds);

  @Select("""
      <script>
      SELECT *
      FROM memos
      WHERE task_group_id IN
      <foreach collection="taskGroupIds" item="id" open="(" separator="," close=")">
        #{id}
      </foreach>
      ORDER BY task_group_id, id
      </script>
      """)
  List<Memo> findAllInTaskGroups(@Param("taskGroupIds") List<Integer> taskGroupIds);

  @Select("""
      <script>
      SELECT *
      FROM memos
      WHERE task_id IN
      <foreach collection="taskIds" item="id" open="(" separator="," close=")">
        #{id}
      </foreach>
      ORDER BY task_id, id
      </script>
      """)
  List<Memo> findAllInTasks(@Param("taskIds") List<Integer> taskIds);

  @Select("SELECT * FROM memos WHERE id = #{id}")
  Memo findById(int id);

  @Insert("""
      INSERT INTO memos(project_id, task_group_id, task_id, comment)
      VALUES(#{projectId},#{taskGroupId},#{taskId},#{comment})
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  void insert(Memo memo);

  /**
   * メモのコメントを更新します。メモの親（プロジェクト・タスクグループ・タスクのいずれか）をたどり、
   * 所有者が一致しない場合は更新されません。
   *
   * @return 更新を実行した件数
   */
  @Update("""
      UPDATE memos
      SET comment = #{memo.comment}
      WHERE id = #{memo.id}
        AND EXISTS (
          SELECT 1
          FROM memos m
          LEFT JOIN projects p1 ON p1.id = m.project_id
          LEFT JOIN task_groups tg ON tg.id = m.task_group_id
          LEFT JOIN projects p2 ON p2.id = tg.project_id
          LEFT JOIN tasks t ON t.id = m.task_id
          LEFT JOIN task_groups ttg ON ttg.id = t.task_group_id
          LEFT JOIN projects p3 ON p3.id = COALESCE(t.project_id, ttg.project_id)
          WHERE m.id = memos.id
            AND (p1.user_id = #{userId} OR p2.user_id = #{userId} OR p3.user_id = #{userId})
        )
      """)
  int update(@Param("memo") Memo memo, @Param("userId") int userId);

  /**
   * IDを指定してメモを削除します。メモの親をたどり、所有者が一致しない場合は削除されません。
   *
   * @param id メモのID
   * @return 削除を実行した件数
   */
  @Delete("""
      DELETE FROM memos
      WHERE id = #{id}
        AND EXISTS (
          SELECT 1
          FROM memos m
          LEFT JOIN projects p1 ON p1.id = m.project_id
          LEFT JOIN task_groups tg ON tg.id = m.task_group_id
          LEFT JOIN projects p2 ON p2.id = tg.project_id
          LEFT JOIN tasks t ON t.id = m.task_id
          LEFT JOIN task_groups ttg ON ttg.id = t.task_group_id
          LEFT JOIN projects p3 ON p3.id = COALESCE(t.project_id, ttg.project_id)
          WHERE m.id = memos.id
            AND (p1.user_id = #{userId} OR p2.user_id = #{userId} OR p3.user_id = #{userId})
        )
      """)
  int delete(int id, int userId);

  /**
   * タスクIDを指定してタスク配下のメモを削除します。 呼び出し前にタスクの所有権が検証済みであることが前提です。
   *
   * @param taskId タスクのID
   * @return 削除を実行した件数
   */
  @Delete("DELETE FROM memos WHERE task_id = #{taskId}")
  int deleteAllInTask(int taskId);
}
