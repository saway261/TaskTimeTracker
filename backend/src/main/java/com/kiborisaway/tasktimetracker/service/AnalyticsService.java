package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.config.AnalyticsThresholdProperties;
import com.kiborisaway.tasktimetracker.data.dto.analytics.AccuracySummaryResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.AccuracyTrendPointResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.AnalyticsPeriod;
import com.kiborisaway.tasktimetracker.data.dto.analytics.AnalyticsQueryCondition;
import com.kiborisaway.tasktimetracker.data.dto.analytics.DiagnosisResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.EstimationAccuracyResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.ExcludedTaskCountResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.MetricAvailabilityResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.OutcomeBreakdownResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.ReflectionTimelineItemResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.ReflectionTimelineQueryCondition;
import com.kiborisaway.tasktimetracker.data.dto.analytics.ReflectionTimelineResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.ScatterPointResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.SizeBucketResponse;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionCauseCategorySummaryResponse;
import com.kiborisaway.tasktimetracker.exception.AnalyticsQueryInvalidException;
import com.kiborisaway.tasktimetracker.exception.ReflectionCauseCategoryInvalidException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.AnalyticsRecentVarianceRow;
import com.kiborisaway.tasktimetracker.repository.AnalyticsRepository;
import com.kiborisaway.tasktimetracker.repository.AnalyticsSummaryRow;
import com.kiborisaway.tasktimetracker.repository.ExcludedCountRow;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import com.kiborisaway.tasktimetracker.repository.ReflectionCauseCategoryLinkRow;
import com.kiborisaway.tasktimetracker.repository.ReflectionCauseCategoryRepository;
import com.kiborisaway.tasktimetracker.repository.ReflectionTimelineRow;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

  private static final int MIN_TASKS_FOR_SUMMARY = 5;
  private static final int MIN_TASKS_FOR_DIAGNOSIS = 10;
  private static final int MIN_TASKS_FOR_TREND = 20;
  private static final double VARIANCE_THRESHOLD_PERCENT = 25.0;

  // 「直近の傾向」は、直近10件と前10件のばらつき（MdAPE）の差がこの範囲内なら「横ばい」とする（要件§4.2）。
  private static final double RECENT_TREND_STABLE_MARGIN = 3.0;

  private static final String GOOD_TITLE = "精度良好";
  private static final String GOOD_MESSAGE = "見積もりは信頼できる水準です。現在の見積もり方を維持しましょう。";
  private static final String UNSTABLE_TITLE = "平均は合うが個別に振れる";
  private static final String UNSTABLE_MESSAGE =
      "タスクごとの当たり外れが大きい傾向があります。着手前に作業を分解し、不明点を洗い出してみましょう。";
  private static final String BIASED_LATE_TITLE = "一貫して長くかかっている（最も直しやすい状態）";
  private static final String BIASED_EARLY_TITLE = "一貫して余裕を持たせすぎている";
  private static final String UNSTABLE_BIASED_LATE_TITLE = "見積もりの前提が定まっていない";
  private static final String UNSTABLE_BIASED_LATE_MESSAGE =
      "まず作業セッションの記録と振り返りを確実に行い、タスクを1時間以内の単位まで分割することから始めましょう。";
  private static final String UNSTABLE_BIASED_EARLY_TITLE = "短く終わる回とそうでない回が混在している";
  private static final String UNSTABLE_BIASED_EARLY_MESSAGE =
      "短縮側の振り返りを読み返し、「余裕の持たせすぎ」なのか「範囲を落として終えた」のかを切り分けてみましょう。";

  private AnalyticsRepository analyticsRepository;
  private AnalyticsThresholdProperties thresholdProperties;
  private ProjectRepository projectRepository;
  private ReflectionCauseCategoryRepository causeCategoryRepository;

  @Autowired
  public AnalyticsService(
      AnalyticsRepository analyticsRepository,
      AnalyticsThresholdProperties thresholdProperties,
      ProjectRepository projectRepository,
      ReflectionCauseCategoryRepository causeCategoryRepository) {
    this.analyticsRepository = analyticsRepository;
    this.thresholdProperties = thresholdProperties;
    this.projectRepository = projectRepository;
    this.causeCategoryRepository = causeCategoryRepository;
  }

  /**
   * 見積もり精度の集計（サマリー・診断）を取得します。散布図・サイズ帯別・精度推移は空で返します
   * （それぞれB6・B7で埋めます）。
   *
   * @param userId    認証ユーザーID
   * @param condition 絞り込み条件（projectId / from / to）
   * @return 見積もり精度集計
   */
  @Transactional(readOnly = true)
  public EstimationAccuracyResponse getEstimationAccuracy(
      int userId, AnalyticsQueryCondition condition) {
    requireProjectOwnership(condition.getProjectId(), userId);
    requireValidPeriod(condition);

    double threshold = thresholdProperties.getOnTimePercent();
    AnalyticsSummaryRow summaryRow = analyticsRepository.findSummary(userId, condition, threshold);
    // 分析対象0件のとき、recent_variance・previous_varianceの両列がNULLになり、MyBatisは
    // 「全列NULLの行」を空行とみなしてnullを返す（returnInstanceForEmptyRow）。0件しか
    // ないマスタ数値クエリ（analyzedCountなど非NULL列を含む）では起きないため、ここだけの対応。
    AnalyticsRecentVarianceRow varianceRow =
        analyticsRepository.findRecentVariance(userId, condition);
    if (varianceRow == null) {
      varianceRow = new AnalyticsRecentVarianceRow(null, null);
    }
    ExcludedCountRow excludedRow = analyticsRepository.findExcludedCounts(userId, condition);

    int analyzedCount = summaryRow.getAnalyzedCount();
    ExcludedTaskCountResponse excluded = buildExcluded(excludedRow, analyzedCount);
    AccuracySummaryResponse summary = buildSummary(summaryRow, varianceRow, analyzedCount);
    DiagnosisResponse diagnosis = analyzedCount >= MIN_TASKS_FOR_DIAGNOSIS
        ? buildDiagnosis(summaryRow, threshold)
        : null;

    return new EstimationAccuracyResponse(
        threshold,
        analyzedCount,
        excluded,
        summary,
        diagnosis,
        List.<ScatterPointResponse>of(),
        false,
        List.<SizeBucketResponse>of(),
        List.<AccuracyTrendPointResponse>of(),
        new MetricAvailabilityResponse(false, MIN_TASKS_FOR_TREND, analyzedCount));
  }

  /**
   * 振り返り済みの完了タスクを完了日時降順でページング取得します。除外ルール（§2-1）は適用しません
   * （振り返り済みの完了タスクは、実績記録漏れであっても内省の対象として残す。要件§0-2-1）。
   *
   * @param userId    認証ユーザーID
   * @param condition 絞り込み条件（projectId / from / to / causeCategory / outcome / page / size）
   * @return 振り返りタイムライン（本ページの一覧・ページング情報）
   */
  @Transactional(readOnly = true)
  public ReflectionTimelineResponse getReflectionTimeline(
      int userId, ReflectionTimelineQueryCondition condition) {
    requireProjectOwnership(condition.getProjectId(), userId);
    requireValidPeriod(condition);
    if (condition.getCauseCategory() != null
        && causeCategoryRepository.findActiveByCode(condition.getCauseCategory()) == null) {
      throw new ReflectionCauseCategoryInvalidException(
          "causeCategory", "指定した原因カテゴリは選択できません");
    }

    double threshold = thresholdProperties.getOnTimePercent();
    List<ReflectionTimelineRow> rows =
        analyticsRepository.findReflectionTimelineItems(userId, condition, threshold);
    int totalCount = analyticsRepository.countReflectionTimeline(userId, condition, threshold);
    List<ReflectionCauseCategoryLinkRow> categoryRows =
        analyticsRepository.findReflectionTimelineCategories(userId, condition, threshold);

    Map<Integer, List<ReflectionCauseCategorySummaryResponse>> categoriesByReflectionId =
        categoryRows.stream()
            .collect(Collectors.groupingBy(
                ReflectionCauseCategoryLinkRow::getReflectionId,
                LinkedHashMap::new,
                Collectors.mapping(
                    row -> new ReflectionCauseCategorySummaryResponse(
                        row.getCauseCategoryCode(), row.getCauseCategoryLabel()),
                    Collectors.toList())));

    List<ReflectionTimelineItemResponse> items = rows.stream()
        .map(row -> toTimelineItem(row, categoriesByReflectionId))
        .toList();

    boolean hasNext = (long) (condition.getPage() + 1) * condition.getSize() < totalCount;
    return new ReflectionTimelineResponse(
        items, condition.getPage(), condition.getSize(), totalCount, hasNext);
  }

  private ReflectionTimelineItemResponse toTimelineItem(
      ReflectionTimelineRow row,
      Map<Integer, List<ReflectionCauseCategorySummaryResponse>> categoriesByReflectionId) {
    return new ReflectionTimelineItemResponse(
        row.getTaskId(),
        row.getTaskTitle(),
        row.getProjectId(),
        row.getProjectTitle(),
        row.getFinishedAt(),
        row.getEstimatedMinutes(),
        row.getActualMinutes(),
        row.getGapMinutes(),
        row.getGapRate(),
        row.getOutcome(),
        categoriesByReflectionId.getOrDefault(row.getReflectionId(), List.of()),
        row.getCause(),
        row.getNextAction());
  }

  private void requireProjectOwnership(Integer projectId, int userId) {
    if (projectId != null && !projectRepository.existsByIdAndUserId(projectId, userId)) {
      throw new TargetNotFoundException(
          "projectId", "指定したIDのプロジェクトは見つかりませんでした");
    }
  }

  private void requireValidPeriod(AnalyticsPeriod condition) {
    // コントローラ側の @ValidAnalyticsPeriod が効いていれば通常は到達しないが、
    // バックエンドが業務ルールの唯一の保証主体であるため省略しない（要件§6）。
    if (condition.getFrom() != null && condition.getTo() != null
        && condition.getFrom().isAfter(condition.getTo())) {
      throw new AnalyticsQueryInvalidException("to", "fromはto以前の日時を指定してください");
    }
  }

  private ExcludedTaskCountResponse buildExcluded(ExcludedCountRow row, int analyzedCount) {
    int total = row.getFinishedCount() - analyzedCount;
    return new ExcludedTaskCountResponse(total, row.getMissingGapRate(), row.getMissingActualMinutes());
  }

  private AccuracySummaryResponse buildSummary(
      AnalyticsSummaryRow summaryRow, AnalyticsRecentVarianceRow varianceRow, int analyzedCount) {
    OutcomeBreakdownResponse outcomeBreakdown = new OutcomeBreakdownResponse(
        summaryRow.getLateCount(), summaryRow.getOnTimeCount(), summaryRow.getEarlyCount());
    boolean available = analyzedCount >= MIN_TASKS_FOR_SUMMARY;
    MetricAvailabilityResponse availability =
        new MetricAvailabilityResponse(available, MIN_TASKS_FOR_SUMMARY, analyzedCount);

    if (!available) {
      return new AccuracySummaryResponse(
          availability, outcomeBreakdown, null, null, null, null, null, null, null, null);
    }

    double onTimeRate = (summaryRow.getOnTimeCount() * 100.0) / analyzedCount;
    Double recentVariance = varianceRow.getRecentVariance();
    Double previousVariance = varianceRow.getPreviousVariance();

    return new AccuracySummaryResponse(
        availability,
        outcomeBreakdown,
        onTimeRate,
        summaryRow.getFactorMedian(),
        summaryRow.getFactorP25(),
        summaryRow.getFactorP75(),
        summaryRow.getVariancePercent(),
        resolveRecentTrend(recentVariance, previousVariance),
        recentVariance,
        previousVariance);
  }

  private String resolveRecentTrend(Double recentVariance, Double previousVariance) {
    if (recentVariance == null || previousVariance == null) {
      return null;
    }
    double diff = recentVariance - previousVariance;
    if (diff <= -RECENT_TREND_STABLE_MARGIN) {
      return "IMPROVED";
    }
    if (diff >= RECENT_TREND_STABLE_MARGIN) {
      return "WORSENED";
    }
    return "STABLE";
  }

  private DiagnosisResponse buildDiagnosis(AnalyticsSummaryRow summaryRow, double threshold) {
    double factorMedian = summaryRow.getFactorMedian();
    double variancePercent = summaryRow.getVariancePercent();
    boolean biasSmall = Math.abs(factorMedian - 1.0) <= threshold / 100.0;
    boolean varianceSmall = variancePercent <= VARIANCE_THRESHOLD_PERCENT;

    if (biasSmall && varianceSmall) {
      return new DiagnosisResponse("GOOD", "NONE", GOOD_TITLE, GOOD_MESSAGE);
    }
    if (biasSmall) {
      return new DiagnosisResponse("UNSTABLE", "NONE", UNSTABLE_TITLE, UNSTABLE_MESSAGE);
    }
    if (factorMedian > 1.0) {
      String message = varianceSmall
          ? formatFactorMessage(
              "ずれ方が安定しているため、見積もりに代表係数（%s倍）を掛けるだけで精度が大きく改善します。",
              factorMedian)
          : UNSTABLE_BIASED_LATE_MESSAGE;
      String title = varianceSmall ? BIASED_LATE_TITLE : UNSTABLE_BIASED_LATE_TITLE;
      String code = varianceSmall ? "BIASED_LATE" : "UNSTABLE_BIASED";
      return new DiagnosisResponse(code, "LATE", title, message);
    }
    String message = varianceSmall
        ? formatFactorMessage(
            "見積もりを代表係数（%s倍）の分だけ短くできます。空けすぎた時間を他の予定に回せる余地があります。",
            factorMedian)
        : UNSTABLE_BIASED_EARLY_MESSAGE;
    String title = varianceSmall ? BIASED_EARLY_TITLE : UNSTABLE_BIASED_EARLY_TITLE;
    String code = varianceSmall ? "BIASED_EARLY" : "UNSTABLE_BIASED";
    return new DiagnosisResponse(code, "EARLY", title, message);
  }

  private String formatFactorMessage(String template, double factorMedian) {
    return template.formatted("%.1f".formatted(factorMedian));
  }
}
