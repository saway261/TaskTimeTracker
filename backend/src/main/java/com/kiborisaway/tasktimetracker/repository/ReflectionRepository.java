package com.kiborisaway.tasktimetracker.repository;

import com.kiborisaway.tasktimetracker.data.entity.Reflection;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ReflectionRepository {

  /**
   * タスクIDに紐づく振り返りを取得します。呼び出し前にタスクの所有権が検証済みであることが前提です。
   *
   * @param taskId タスクID
   * @return 振り返り。存在しない場合はnull
   */
  @Select("SELECT * FROM reflections WHERE task_id = #{taskId}")
  Reflection findByTaskId(int taskId);

  /**
   * タスクIDに紐づく振り返りが存在するかを返します。呼び出し前にタスクの所有権が検証済みであることが前提です。
   *
   * @param taskId タスクID
   * @return 存在する場合はtrue
   */
  @Select("SELECT EXISTS(SELECT 1 FROM reflections WHERE task_id = #{taskId})")
  boolean existsByTaskId(int taskId);

  /**
   * 振り返りを登録します。呼び出し前にタスクの所有権と完了状態が検証済みであることが前提です。
   *
   * @param reflection 登録する振り返り
   */
  @Insert("""
      INSERT INTO reflections(task_id, cause, next_action, cause_category_id, created_at, updated_at)
      VALUES(#{taskId}, #{cause}, #{nextAction}, #{causeCategoryId}, NOW(), NOW())
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  void insert(Reflection reflection);

  /**
   * タスクIDに紐づく振り返りを更新します。呼び出し前にタスクの所有権と完了状態が検証済みであることが前提です。
   *
   * @param reflection 更新する振り返り
   * @return 更新件数
   */
  @Update("""
      UPDATE reflections
      SET cause = #{cause},
          next_action = #{nextAction},
          cause_category_id = #{causeCategoryId},
          updated_at = NOW()
      WHERE task_id = #{taskId}
      """)
  int updateByTaskId(Reflection reflection);

  /**
   * タスクIDに紐づく振り返りを削除します。呼び出し前にタスクの所有権が検証済みであることが前提です。
   *
   * @param taskId タスクID
   * @return 削除件数
   */
  @Delete("DELETE FROM reflections WHERE task_id = #{taskId}")
  int deleteByTaskId(int taskId);

  /**
   * プロジェクト直下にある完了タスクを、振り返りを含めて表示順に取得します。
   * 呼び出し前にプロジェクトの所有権が検証済みであることが前提です。
   *
   * @param projectId プロジェクトID
   * @return プロジェクト直下の完了タスク行
   */
  @Select("""
      SELECT
        t.id AS task_id,
        t.title AS task_title,
        t.finished_at,
        t.actual_minutes_cached,
        t.gap_minutes_cached,
        t.gap_rate_cached,
        r.id AS reflection_id,
        r.cause,
        r.next_action,
        r.cause_category_id,
        rcc.code AS cause_category_code,
        rcc.label AS cause_category_label,
        r.created_at AS reflection_created_at,
        r.updated_at AS reflection_updated_at
      FROM tasks t
      LEFT JOIN project_item_orders pio
        ON pio.project_id = t.project_id
        AND pio.task_id = t.id
      LEFT JOIN reflections r ON r.task_id = t.id
      LEFT JOIN reflection_cause_categories rcc ON rcc.id = r.cause_category_id
      WHERE t.project_id = #{projectId}
        AND t.finished_at IS NOT NULL
      ORDER BY COALESCE(pio.position, 2147483647), t.id
      """)
  List<ReflectionTaskRow> findDirectTasksInProject(int projectId);

  /**
   * タスクグループ配下にある完了タスクを、グループ情報と振り返りを含めて表示順に取得します。
   * 呼び出し前にプロジェクトの所有権が検証済みであることが前提です。
   *
   * @param projectId プロジェクトID
   * @return タスクグループ配下の完了タスク行
   */
  @Select("""
      SELECT
        t.id AS task_id,
        t.title AS task_title,
        t.finished_at,
        t.actual_minutes_cached,
        t.gap_minutes_cached,
        t.gap_rate_cached,
        tg.id AS task_group_id,
        tg.title AS task_group_title,
        r.id AS reflection_id,
        r.cause,
        r.next_action,
        r.cause_category_id,
        rcc.code AS cause_category_code,
        rcc.label AS cause_category_label,
        r.created_at AS reflection_created_at,
        r.updated_at AS reflection_updated_at
      FROM tasks t
      JOIN task_groups tg ON tg.id = t.task_group_id
      LEFT JOIN project_item_orders pio
        ON pio.project_id = tg.project_id
        AND pio.task_group_id = tg.id
      LEFT JOIN task_group_item_orders tgio
        ON tgio.task_group_id = tg.id
        AND tgio.task_id = t.id
      LEFT JOIN reflections r ON r.task_id = t.id
      LEFT JOIN reflection_cause_categories rcc ON rcc.id = r.cause_category_id
      WHERE tg.project_id = #{projectId}
        AND t.finished_at IS NOT NULL
      ORDER BY
        COALESCE(pio.position, 2147483647),
        tg.id,
        COALESCE(tgio.position, 2147483647),
        t.id
      """)
  List<ReflectionTaskRow> findGroupedTasksInProject(int projectId);
}
