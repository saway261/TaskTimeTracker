package com.kiborisaway.tasktimetracker.repository;

import com.kiborisaway.tasktimetracker.data.dto.analytics.AnalyticsQueryCondition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AnalyticsRepository {

  /**
   * 分析対象タスクの件数・判定区分内訳・見積もり係数の中央値と四分位・誤差率ばらつき（MdAPE）を1クエリで取得します。
   * 呼び出し前に {@code condition.projectId} の所有権が検証済みであることが前提です。
   *
   * @param userId    認証ユーザーID
   * @param condition 絞り込み条件（projectId / from / to）
   * @param threshold 判定区分しきい値（百分率）
   * @return サマリー集計結果
   */
  @Select("""
      WITH target AS (
        SELECT
          t.gap_rate_cached / 100.0 + 1 AS factor,
          ABS(t.gap_rate_cached)        AS ape,
          t.gap_rate_cached             AS gap_rate
        FROM tasks t
        LEFT JOIN task_groups tg ON tg.id = t.task_group_id
        JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
        WHERE p.user_id = #{userId}
          AND t.finished_at IS NOT NULL
          AND t.gap_rate_cached IS NOT NULL
          AND t.actual_minutes_cached IS NOT NULL
          AND t.actual_minutes_cached <> 0
          AND (#{condition.projectId, jdbcType=INTEGER} IS NULL
               OR COALESCE(t.project_id, tg.project_id) = #{condition.projectId, jdbcType=INTEGER})
          AND (#{condition.from, jdbcType=TIMESTAMP} IS NULL
               OR t.finished_at >= #{condition.from, jdbcType=TIMESTAMP})
          AND (#{condition.to, jdbcType=TIMESTAMP} IS NULL
               OR t.finished_at <  #{condition.to, jdbcType=TIMESTAMP})
      )
      SELECT
        COUNT(*)                                                                AS analyzed_count,
        COALESCE(SUM(CASE WHEN gap_rate >  #{threshold} THEN 1 ELSE 0 END), 0)  AS late_count,
        COALESCE(SUM(CASE WHEN gap_rate < -#{threshold} THEN 1 ELSE 0 END), 0)  AS early_count,
        COALESCE(SUM(CASE WHEN ABS(gap_rate) <= #{threshold} THEN 1 ELSE 0 END), 0) AS on_time_count,
        PERCENTILE_CONT(0.5)  WITHIN GROUP (ORDER BY factor) AS factor_median,
        PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY factor) AS factor_p25,
        PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY factor) AS factor_p75,
        PERCENTILE_CONT(0.5)  WITHIN GROUP (ORDER BY ape)    AS variance_percent
      FROM target
      """)
  AnalyticsSummaryRow findSummary(
      @Param("userId") int userId,
      @Param("condition") AnalyticsQueryCondition condition,
      @Param("threshold") double threshold);

  /**
   * 完了日時が新しい順に直近10件と、その前の10件それぞれの誤差率ばらつき（MdAPE）を1クエリで取得します。
   * 分析対象が11件未満の場合はpreviousVarianceがnullになります。
   * <b>分析対象が0件の場合、両列がNULLになりMyBatisが空行とみなすため、戻り値自体がnullになります。</b>
   * 呼び出し側でnullを許容してください。
   *
   * @param userId    認証ユーザーID
   * @param condition 絞り込み条件（projectId / from / to）
   * @return 直近・その前のばらつき。分析対象0件の場合はnull
   */
  @Select("""
      WITH target AS (
        SELECT
          ABS(t.gap_rate_cached) AS ape,
          ROW_NUMBER() OVER (ORDER BY t.finished_at DESC, t.id DESC) AS rn
        FROM tasks t
        LEFT JOIN task_groups tg ON tg.id = t.task_group_id
        JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
        WHERE p.user_id = #{userId}
          AND t.finished_at IS NOT NULL
          AND t.gap_rate_cached IS NOT NULL
          AND t.actual_minutes_cached IS NOT NULL
          AND t.actual_minutes_cached <> 0
          AND (#{condition.projectId, jdbcType=INTEGER} IS NULL
               OR COALESCE(t.project_id, tg.project_id) = #{condition.projectId, jdbcType=INTEGER})
          AND (#{condition.from, jdbcType=TIMESTAMP} IS NULL
               OR t.finished_at >= #{condition.from, jdbcType=TIMESTAMP})
          AND (#{condition.to, jdbcType=TIMESTAMP} IS NULL
               OR t.finished_at <  #{condition.to, jdbcType=TIMESTAMP})
      )
      SELECT
        (SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY ape)
           FROM target WHERE rn <= 10)             AS recent_variance,
        (SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY ape)
           FROM target WHERE rn BETWEEN 11 AND 20)  AS previous_variance
      """)
  AnalyticsRecentVarianceRow findRecentVariance(
      @Param("userId") int userId,
      @Param("condition") AnalyticsQueryCondition condition);

  /**
   * 絞り込み条件に一致する完了タスクのうち、除外理由別の件数と完了タスクの総数を1クエリで取得します。
   * 分析対象件数（{@link #findSummary}のanalyzedCount）との差分が除外件数の合計になります。
   *
   * @param userId    認証ユーザーID
   * @param condition 絞り込み条件（projectId / from / to）
   * @return 除外理由別の件数と完了タスク総数
   */
  @Select("""
      SELECT
        COALESCE(SUM(CASE WHEN t.gap_rate_cached IS NULL THEN 1 ELSE 0 END), 0) AS missing_gap_rate,
        COALESCE(SUM(CASE WHEN t.actual_minutes_cached IS NULL
                   OR t.actual_minutes_cached = 0 THEN 1 ELSE 0 END), 0) AS missing_actual_minutes,
        COUNT(*) AS finished_count
      FROM tasks t
      LEFT JOIN task_groups tg ON tg.id = t.task_group_id
      JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
      WHERE p.user_id = #{userId}
        AND t.finished_at IS NOT NULL
        AND (#{condition.projectId, jdbcType=INTEGER} IS NULL
             OR COALESCE(t.project_id, tg.project_id) = #{condition.projectId, jdbcType=INTEGER})
        AND (#{condition.from, jdbcType=TIMESTAMP} IS NULL
             OR t.finished_at >= #{condition.from, jdbcType=TIMESTAMP})
        AND (#{condition.to, jdbcType=TIMESTAMP} IS NULL
             OR t.finished_at <  #{condition.to, jdbcType=TIMESTAMP})
      """)
  ExcludedCountRow findExcludedCounts(
      @Param("userId") int userId,
      @Param("condition") AnalyticsQueryCondition condition);
}
