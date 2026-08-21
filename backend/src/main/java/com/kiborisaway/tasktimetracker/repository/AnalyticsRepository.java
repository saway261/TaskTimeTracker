package com.kiborisaway.tasktimetracker.repository;

import com.kiborisaway.tasktimetracker.data.dto.analytics.AnalyticsQueryCondition;
import com.kiborisaway.tasktimetracker.data.dto.analytics.ReflectionTimelineQueryCondition;
import java.util.List;
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
          AND (CAST(#{condition.from, jdbcType=TIMESTAMP} AS TIMESTAMP) IS NULL
               OR t.finished_at >= #{condition.from, jdbcType=TIMESTAMP})
          AND (CAST(#{condition.to, jdbcType=TIMESTAMP} AS TIMESTAMP) IS NULL
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
          AND (CAST(#{condition.from, jdbcType=TIMESTAMP} AS TIMESTAMP) IS NULL
               OR t.finished_at >= #{condition.from, jdbcType=TIMESTAMP})
          AND (CAST(#{condition.to, jdbcType=TIMESTAMP} AS TIMESTAMP) IS NULL
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
        AND (CAST(#{condition.from, jdbcType=TIMESTAMP} AS TIMESTAMP) IS NULL
             OR t.finished_at >= #{condition.from, jdbcType=TIMESTAMP})
        AND (CAST(#{condition.to, jdbcType=TIMESTAMP} AS TIMESTAMP) IS NULL
             OR t.finished_at <  #{condition.to, jdbcType=TIMESTAMP})
      """)
  ExcludedCountRow findExcludedCounts(
      @Param("userId") int userId,
      @Param("condition") AnalyticsQueryCondition condition);

  /**
   * 散布図用に、分析対象タスクを完了日時が新しい順に最大 {@code limit + 1} 件取得します。
   * {@code limit + 1} 件取れた場合は表示上限を超えていることを示すため、呼び出し側で
   * {@code limit} 件に切り詰めたうえで昇順へ並べ替えてください（返却順は降順のまま渡します）。
   *
   * @param userId    認証ユーザーID
   * @param condition 絞り込み条件（projectId / from / to）
   * @param threshold 判定区分しきい値（百分率）
   * @param limit     表示上限件数（{@code SCATTER_MAX_POINTS}）
   * @return 完了日時降順の散布図データ行。最大 {@code limit + 1} 件
   */
  @Select("""
      SELECT
        t.id AS task_id,
        t.title AS task_title,
        t.estimated_minutes,
        t.actual_minutes_cached AS actual_minutes,
        t.gap_rate_cached AS gap_rate,
        CASE
          WHEN t.gap_rate_cached >  #{threshold} THEN 'LATE'
          WHEN t.gap_rate_cached < -#{threshold} THEN 'EARLY'
          ELSE 'ON_TIME'
        END AS outcome
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
        AND (CAST(#{condition.from, jdbcType=TIMESTAMP} AS TIMESTAMP) IS NULL
             OR t.finished_at >= #{condition.from, jdbcType=TIMESTAMP})
        AND (CAST(#{condition.to, jdbcType=TIMESTAMP} AS TIMESTAMP) IS NULL
             OR t.finished_at <  #{condition.to, jdbcType=TIMESTAMP})
      ORDER BY t.finished_at DESC, t.id DESC
      LIMIT #{limit} + 1
      """)
  List<AnalyticsScatterPointRow> findScatterPoints(
      @Param("userId") int userId,
      @Param("condition") AnalyticsQueryCondition condition,
      @Param("threshold") double threshold,
      @Param("limit") int limit);

  /**
   * タスクサイズ帯（見積時間）ごとに件数・代表係数の中央値・オンタイム件数を1クエリで取得します。
   * 該当タスクが0件の帯はこの結果に現れないため、呼び出し側で全帯を埋めてください。
   *
   * @param userId    認証ユーザーID
   * @param condition 絞り込み条件（projectId / from / to）
   * @param threshold 判定区分しきい値（百分率）
   * @return 帯ごとの集計行。該当0件の帯は含まれない
   */
  @Select("""
      WITH target AS (
        SELECT
          t.gap_rate_cached / 100.0 + 1 AS factor,
          t.gap_rate_cached             AS gap_rate,
          CASE
            WHEN t.estimated_minutes <= 15  THEN 'M15'
            WHEN t.estimated_minutes <= 30  THEN 'M30'
            WHEN t.estimated_minutes <= 60  THEN 'M60'
            WHEN t.estimated_minutes <= 120 THEN 'M120'
            ELSE 'OVER120'
          END AS bucket_code
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
          AND (CAST(#{condition.from, jdbcType=TIMESTAMP} AS TIMESTAMP) IS NULL
               OR t.finished_at >= #{condition.from, jdbcType=TIMESTAMP})
          AND (CAST(#{condition.to, jdbcType=TIMESTAMP} AS TIMESTAMP) IS NULL
               OR t.finished_at <  #{condition.to, jdbcType=TIMESTAMP})
      )
      SELECT
        bucket_code,
        COUNT(*) AS task_count,
        PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY factor) AS factor_median,
        COALESCE(SUM(CASE WHEN ABS(gap_rate) <= #{threshold} THEN 1 ELSE 0 END), 0) AS on_time_count
      FROM target
      GROUP BY bucket_code
      """)
  List<AnalyticsSizeBucketRow> findSizeBuckets(
      @Param("userId") int userId,
      @Param("condition") AnalyticsQueryCondition condition,
      @Param("threshold") double threshold);

  /**
   * 完了順に窓幅 {@code windowSize} の移動中央値（代表係数・誤差率ばらつき）を1クエリで取得します。
   * 窓が埋まらない先頭 {@code windowSize - 1} 件は点を作らないため、分析対象が
   * {@code windowSize} 件未満の場合は空リストを返します。
   *
   * @param userId     認証ユーザーID
   * @param condition  絞り込み条件（projectId / from / to）
   * @param windowSize 移動窓の幅（{@code TREND_WINDOW_SIZE}）
   * @return 完了順（連番昇順）の移動中央値行
   */
  @Select("""
      WITH target AS (
        SELECT
          t.id,
          t.finished_at,
          t.gap_rate_cached / 100.0 + 1 AS factor,
          ABS(t.gap_rate_cached)        AS ape,
          ROW_NUMBER() OVER (ORDER BY t.finished_at, t.id) AS rn
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
          AND (CAST(#{condition.from, jdbcType=TIMESTAMP} AS TIMESTAMP) IS NULL
               OR t.finished_at >= #{condition.from, jdbcType=TIMESTAMP})
          AND (CAST(#{condition.to, jdbcType=TIMESTAMP} AS TIMESTAMP) IS NULL
               OR t.finished_at <  #{condition.to, jdbcType=TIMESTAMP})
      )
      SELECT
        a.rn                                                  AS sequence,
        a.finished_at                                         AS finished_at,
        MIN(b.finished_at)                                    AS window_from,
        PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY b.factor) AS factor_median,
        PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY b.ape)    AS variance_percent
      FROM target a
      JOIN target b ON b.rn BETWEEN a.rn - (#{windowSize} - 1) AND a.rn
      WHERE a.rn >= #{windowSize}
      GROUP BY a.rn, a.finished_at
      ORDER BY a.rn
      """)
  List<AnalyticsTrendRow> findAccuracyTrend(
      @Param("userId") int userId,
      @Param("condition") AnalyticsQueryCondition condition,
      @Param("windowSize") int windowSize);

  /**
   * 原因カテゴリ別（方向・コード単位）に、延べ件数と代表誤差率（中央値）を1クエリで取得します。
   * 原因カテゴリの中間テーブルは {@code LEFT JOIN} するため、リンクを持たない振り返り（未分類）も
   * {@code direction} / {@code causeCategoryCode} / {@code causeCategoryLabel} がnullの行として含まれます。
   * 1タスクが複数カテゴリを持つ場合、この結合は意図的に行を増やします（延べ件数のため。要件§4.7）。
   *
   * @param userId    認証ユーザーID
   * @param condition 絞り込み条件（projectId / from / to）
   * @return カテゴリ（未分類を含む）ごとの延べ件数と代表誤差率
   */
  @Select("""
      SELECT
        rcc.direction     AS direction,
        rcc.code          AS cause_category_code,
        rcc.label         AS cause_category_label,
        rcc.display_order AS display_order,
        COUNT(*)          AS task_count,
        PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY t.gap_rate_cached) AS gap_rate_median
      FROM tasks t
      LEFT JOIN task_groups tg ON tg.id = t.task_group_id
      JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
      JOIN reflections r ON r.task_id = t.id
      LEFT JOIN reflection_cause_category_links rcl ON rcl.reflection_id = r.id
      LEFT JOIN reflection_cause_categories rcc ON rcc.id = rcl.cause_category_id
      WHERE p.user_id = #{userId}
        AND t.finished_at IS NOT NULL
        AND t.gap_rate_cached IS NOT NULL
        AND t.actual_minutes_cached IS NOT NULL
        AND t.actual_minutes_cached <> 0
        AND (#{condition.projectId, jdbcType=INTEGER} IS NULL
             OR COALESCE(t.project_id, tg.project_id) = #{condition.projectId, jdbcType=INTEGER})
        AND (CAST(#{condition.from, jdbcType=TIMESTAMP} AS TIMESTAMP) IS NULL
             OR t.finished_at >= #{condition.from, jdbcType=TIMESTAMP})
        AND (CAST(#{condition.to, jdbcType=TIMESTAMP} AS TIMESTAMP) IS NULL
             OR t.finished_at <  #{condition.to, jdbcType=TIMESTAMP})
      GROUP BY rcc.direction, rcc.code, rcc.label, rcc.display_order
      """)
  List<GapCauseRow> findGapCauses(
      @Param("userId") int userId,
      @Param("condition") AnalyticsQueryCondition condition);

  /**
   * 振り返り済みの完了タスクを完了日時降順でページング取得します。除外ルール（§2-1）は適用しません
   * （要件§0-2-1のとおり、振り返り済みの完了タスクはすべてタイムラインの対象）。
   * 呼び出し前に {@code condition.projectId} の所有権と {@code condition.causeCategory} の
   * 有効性が検証済みであることが前提です。
   *
   * @param userId    認証ユーザーID
   * @param condition 絞り込み条件（projectId / from / to / causeCategory / outcome / page / size）
   * @param threshold 判定区分しきい値（百分率）
   * @return 本ページの振り返りタイムライン行
   */
  @Select("""
      WITH base AS (
        SELECT
          t.id AS task_id,
          t.title AS task_title,
          COALESCE(t.project_id, tg.project_id) AS project_id,
          p.title AS project_title,
          t.finished_at,
          t.estimated_minutes,
          t.actual_minutes_cached AS actual_minutes,
          t.gap_minutes_cached AS gap_minutes,
          t.gap_rate_cached AS gap_rate,
          CASE
            WHEN t.gap_rate_cached IS NULL THEN NULL
            WHEN t.gap_rate_cached >  #{threshold} THEN 'LATE'
            WHEN t.gap_rate_cached < -#{threshold} THEN 'EARLY'
            ELSE 'ON_TIME'
          END AS outcome,
          r.id AS reflection_id,
          r.cause,
          r.next_action
        FROM tasks t
        LEFT JOIN task_groups tg ON tg.id = t.task_group_id
        JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
        JOIN reflections r ON r.task_id = t.id
        WHERE p.user_id = #{userId}
          AND t.finished_at IS NOT NULL
          AND (#{condition.projectId, jdbcType=INTEGER} IS NULL
               OR COALESCE(t.project_id, tg.project_id) = #{condition.projectId, jdbcType=INTEGER})
          AND (CAST(#{condition.from, jdbcType=TIMESTAMP} AS TIMESTAMP) IS NULL
               OR t.finished_at >= #{condition.from, jdbcType=TIMESTAMP})
          AND (CAST(#{condition.to, jdbcType=TIMESTAMP} AS TIMESTAMP) IS NULL
               OR t.finished_at <  #{condition.to, jdbcType=TIMESTAMP})
          AND (#{condition.causeCategory, jdbcType=VARCHAR} IS NULL OR EXISTS (
                SELECT 1 FROM reflection_cause_category_links rcl
                JOIN reflection_cause_categories rcc ON rcc.id = rcl.cause_category_id
                WHERE rcl.reflection_id = r.id
                  AND rcc.code = #{condition.causeCategory, jdbcType=VARCHAR}))
      )
      SELECT * FROM base
      WHERE (#{condition.outcome} = 'ALL' OR outcome = #{condition.outcome})
      ORDER BY finished_at DESC, task_id DESC
      LIMIT #{condition.size} OFFSET (#{condition.page} * #{condition.size})
      """)
  List<ReflectionTimelineRow> findReflectionTimelineItems(
      @Param("userId") int userId,
      @Param("condition") ReflectionTimelineQueryCondition condition,
      @Param("threshold") double threshold);

  /**
   * {@link #findReflectionTimelineItems} と同じ絞り込み条件に一致する総件数を取得します。
   * ページングを除いた抽出条件は完全に同一である必要があります。
   *
   * @param userId    認証ユーザーID
   * @param condition 絞り込み条件（projectId / from / to / causeCategory / outcome）
   * @param threshold 判定区分しきい値（百分率）
   * @return 総件数
   */
  @Select("""
      WITH base AS (
        SELECT
          t.gap_rate_cached AS gap_rate,
          CASE
            WHEN t.gap_rate_cached IS NULL THEN NULL
            WHEN t.gap_rate_cached >  #{threshold} THEN 'LATE'
            WHEN t.gap_rate_cached < -#{threshold} THEN 'EARLY'
            ELSE 'ON_TIME'
          END AS outcome,
          r.id AS reflection_id
        FROM tasks t
        LEFT JOIN task_groups tg ON tg.id = t.task_group_id
        JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
        JOIN reflections r ON r.task_id = t.id
        WHERE p.user_id = #{userId}
          AND t.finished_at IS NOT NULL
          AND (#{condition.projectId, jdbcType=INTEGER} IS NULL
               OR COALESCE(t.project_id, tg.project_id) = #{condition.projectId, jdbcType=INTEGER})
          AND (CAST(#{condition.from, jdbcType=TIMESTAMP} AS TIMESTAMP) IS NULL
               OR t.finished_at >= #{condition.from, jdbcType=TIMESTAMP})
          AND (CAST(#{condition.to, jdbcType=TIMESTAMP} AS TIMESTAMP) IS NULL
               OR t.finished_at <  #{condition.to, jdbcType=TIMESTAMP})
          AND (#{condition.causeCategory, jdbcType=VARCHAR} IS NULL OR EXISTS (
                SELECT 1 FROM reflection_cause_category_links rcl
                JOIN reflection_cause_categories rcc ON rcc.id = rcl.cause_category_id
                WHERE rcl.reflection_id = r.id
                  AND rcc.code = #{condition.causeCategory, jdbcType=VARCHAR}))
      )
      SELECT COUNT(*) FROM base
      WHERE (#{condition.outcome} = 'ALL' OR outcome = #{condition.outcome})
      """)
  int countReflectionTimeline(
      @Param("userId") int userId,
      @Param("condition") ReflectionTimelineQueryCondition condition,
      @Param("threshold") double threshold);

  /**
   * {@link #findReflectionTimelineItems} と同一ページの原因カテゴリを一括取得します。
   * 一覧クエリへリンクを結合すると1件が最大3行に増えページングが壊れるため、独立した1クエリとします
   * （要件§0-6-4と同じ理由）。内側の副問い合わせは {@link #findReflectionTimelineItems} と
   * 完全に同じ絞り込み・並び順・ページングでなければなりません。
   *
   * @param userId    認証ユーザーID
   * @param condition 絞り込み条件（projectId / from / to / causeCategory / outcome / page / size）
   * @param threshold 判定区分しきい値（百分率）
   * @return 本ページに含まれる振り返りの原因カテゴリ行
   */
  @Select("""
      WITH base AS (
        SELECT
          t.id AS task_id,
          t.finished_at,
          t.gap_rate_cached AS gap_rate,
          CASE
            WHEN t.gap_rate_cached IS NULL THEN NULL
            WHEN t.gap_rate_cached >  #{threshold} THEN 'LATE'
            WHEN t.gap_rate_cached < -#{threshold} THEN 'EARLY'
            ELSE 'ON_TIME'
          END AS outcome,
          r.id AS reflection_id
        FROM tasks t
        LEFT JOIN task_groups tg ON tg.id = t.task_group_id
        JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
        JOIN reflections r ON r.task_id = t.id
        WHERE p.user_id = #{userId}
          AND t.finished_at IS NOT NULL
          AND (#{condition.projectId, jdbcType=INTEGER} IS NULL
               OR COALESCE(t.project_id, tg.project_id) = #{condition.projectId, jdbcType=INTEGER})
          AND (CAST(#{condition.from, jdbcType=TIMESTAMP} AS TIMESTAMP) IS NULL
               OR t.finished_at >= #{condition.from, jdbcType=TIMESTAMP})
          AND (CAST(#{condition.to, jdbcType=TIMESTAMP} AS TIMESTAMP) IS NULL
               OR t.finished_at <  #{condition.to, jdbcType=TIMESTAMP})
          AND (#{condition.causeCategory, jdbcType=VARCHAR} IS NULL OR EXISTS (
                SELECT 1 FROM reflection_cause_category_links rcl
                JOIN reflection_cause_categories rcc ON rcc.id = rcl.cause_category_id
                WHERE rcl.reflection_id = r.id
                  AND rcc.code = #{condition.causeCategory, jdbcType=VARCHAR}))
      )
      SELECT
        rcl.reflection_id,
        rcc.code  AS cause_category_code,
        rcc.label AS cause_category_label
      FROM (
        SELECT reflection_id, finished_at, task_id FROM base
        WHERE (#{condition.outcome} = 'ALL' OR outcome = #{condition.outcome})
        ORDER BY finished_at DESC, task_id DESC
        LIMIT #{condition.size} OFFSET (#{condition.page} * #{condition.size})
      ) page
      JOIN reflection_cause_category_links rcl ON rcl.reflection_id = page.reflection_id
      JOIN reflection_cause_categories rcc ON rcc.id = rcl.cause_category_id
      ORDER BY rcl.reflection_id, rcc.display_order, rcc.id
      """)
  List<ReflectionCauseCategoryLinkRow> findReflectionTimelineCategories(
      @Param("userId") int userId,
      @Param("condition") ReflectionTimelineQueryCondition condition,
      @Param("threshold") double threshold);
}
