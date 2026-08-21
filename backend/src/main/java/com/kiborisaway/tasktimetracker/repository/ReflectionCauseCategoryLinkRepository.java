package com.kiborisaway.tasktimetracker.repository;

import com.kiborisaway.tasktimetracker.data.entity.ReflectionCauseCategory;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReflectionCauseCategoryLinkRepository {

  /**
   * 振り返りと原因カテゴリのリンクを1件登録します。呼び出し前に振り返りの存在と
   * カテゴリの有効性が検証済みであることが前提です。件数の上限（3件）は呼び出し側で保証します。
   *
   * @param reflectionId  振り返りID
   * @param causeCategoryId 原因カテゴリID
   */
  @Insert("""
      INSERT INTO reflection_cause_category_links(reflection_id, cause_category_id)
      VALUES(#{reflectionId}, #{causeCategoryId})
      """)
  void insert(@Param("reflectionId") int reflectionId, @Param("causeCategoryId") int causeCategoryId);

  /**
   * 振り返りIDに紐づく原因カテゴリのリンクをすべて削除します。更新時の全置換に使います。
   *
   * @param reflectionId 振り返りID
   * @return 削除件数
   */
  @Delete("DELETE FROM reflection_cause_category_links WHERE reflection_id = #{reflectionId}")
  int deleteByReflectionId(int reflectionId);

  /**
   * 振り返りIDに紐づく原因カテゴリを表示順で取得します。
   *
   * @param reflectionId 振り返りID
   * @return 表示順に並んだ原因カテゴリ一覧。リンクがなければ空リスト
   */
  @Select("""
      SELECT rcc.* FROM reflection_cause_category_links rcl
      JOIN reflection_cause_categories rcc ON rcc.id = rcl.cause_category_id
      WHERE rcl.reflection_id = #{reflectionId}
      ORDER BY rcc.display_order, rcc.id
      """)
  List<ReflectionCauseCategory> findCategoriesByReflectionId(int reflectionId);

  /**
   * プロジェクト内（直下・タスクグループ配下の両方）の振り返りに紐づく原因カテゴリを
   * 一括取得します。1タスク一覧クエリと結合すると行が重複するため、独立した1クエリとして呼び出します。
   * 呼び出し前にプロジェクトの所有権が検証済みであることが前提です。
   *
   * @param projectId プロジェクトID
   * @return 振り返りIDごとの原因カテゴリ行。表示順に並ぶ
   */
  @Select("""
      SELECT
        rcl.reflection_id,
        rcc.code AS cause_category_code,
        rcc.label AS cause_category_label
      FROM reflection_cause_category_links rcl
      JOIN reflection_cause_categories rcc ON rcc.id = rcl.cause_category_id
      JOIN reflections r ON r.id = rcl.reflection_id
      JOIN tasks t ON t.id = r.task_id
      LEFT JOIN task_groups tg ON tg.id = t.task_group_id
      WHERE COALESCE(t.project_id, tg.project_id) = #{projectId}
      ORDER BY rcl.reflection_id, rcc.display_order, rcc.id
      """)
  List<ReflectionCauseCategoryLinkRow> findCategoriesInProject(int projectId);
}
