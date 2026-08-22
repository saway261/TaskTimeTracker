package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.config.AnalyticsThresholdProperties;
import com.kiborisaway.tasktimetracker.data.dto.analytics.AccuracySummaryResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.AccuracyTrendPointResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.AnalyticsPeriod;
import com.kiborisaway.tasktimetracker.data.dto.analytics.AnalyticsQueryCondition;
import com.kiborisaway.tasktimetracker.data.dto.analytics.DiagnosisResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.EstimationAccuracyResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.ExcludedTaskCountResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.GapCauseAggregateResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.GapCauseGroupResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.GapCauseItemResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.MetricAvailabilityResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.OutcomeBreakdownResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.ProjectBreakdownItemResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.ReflectionTimelineItemResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.ReflectionTimelineQueryCondition;
import com.kiborisaway.tasktimetracker.data.dto.analytics.ReflectionTimelineResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.ScatterPointResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.SizeBucketResponse;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionCauseCategorySummaryResponse;
import com.kiborisaway.tasktimetracker.data.dto.tag.TagSummaryResponse;
import com.kiborisaway.tasktimetracker.data.entity.Tag;
import com.kiborisaway.tasktimetracker.exception.AnalyticsQueryInvalidException;
import com.kiborisaway.tasktimetracker.exception.ReflectionCauseCategoryInvalidException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.AnalyticsRecentVarianceRow;
import com.kiborisaway.tasktimetracker.repository.AnalyticsRepository;
import com.kiborisaway.tasktimetracker.repository.AnalyticsScatterPointRow;
import com.kiborisaway.tasktimetracker.repository.AnalyticsSizeBucketRow;
import com.kiborisaway.tasktimetracker.repository.AnalyticsSummaryRow;
import com.kiborisaway.tasktimetracker.repository.AnalyticsTrendRow;
import com.kiborisaway.tasktimetracker.repository.ExcludedCountRow;
import com.kiborisaway.tasktimetracker.repository.GapCauseRow;
import com.kiborisaway.tasktimetracker.repository.ProjectBreakdownRow;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import com.kiborisaway.tasktimetracker.repository.ReflectionCauseCategoryLinkRow;
import com.kiborisaway.tasktimetracker.repository.ReflectionCauseCategoryRepository;
import com.kiborisaway.tasktimetracker.repository.ReflectionTimelineRow;
import com.kiborisaway.tasktimetracker.repository.TagRepository;
import com.kiborisaway.tasktimetracker.repository.TaskTagRow;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
  private static final int MIN_TASKS_PER_SIZE_BUCKET = 3;
  private static final int MIN_TASKS_PER_CAUSE_CATEGORY = 3;
  private static final double VARIANCE_THRESHOLD_PERCENT = 25.0;
  private static final int SCATTER_MAX_POINTS = 500;
  // サマリーの「直近の傾向」（直近10件と前10件の比較）と窓幅を必ず一致させる（要件§4.2）。
  private static final int TREND_WINDOW_SIZE = 10;

  // 帯の境界は15/30/60/120分（§12-11で確定）。表示順は小さい帯から大きい帯へ固定する。
  private static final List<String> SIZE_BUCKET_CODES =
      List.of("M15", "M30", "M60", "M120", "OVER120");
  private static final Map<String, String> SIZE_BUCKET_LABELS = Map.of(
      "M15", "〜15分",
      "M30", "16〜30分",
      "M60", "31〜60分",
      "M120", "61〜120分",
      "OVER120", "121分〜");

  // 原因カテゴリのグループ表示順は超過側→短縮側→共通で固定する（要件§4.7）。
  private static final List<String> GAP_CAUSE_DIRECTIONS = List.of("OVER", "UNDER", "BOTH");
  private static final Map<String, String> GAP_CAUSE_DIRECTION_LABELS = Map.of(
      "OVER", "超過側",
      "UNDER", "短縮側",
      "BOTH", "共通");
  private static final String UNCLASSIFIED_LABEL = "未分類";

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
  private TagRepository tagRepository;

  @Autowired
  public AnalyticsService(
      AnalyticsRepository analyticsRepository,
      AnalyticsThresholdProperties thresholdProperties,
      ProjectRepository projectRepository,
      ReflectionCauseCategoryRepository causeCategoryRepository,
      TagRepository tagRepository) {
    this.analyticsRepository = analyticsRepository;
    this.thresholdProperties = thresholdProperties;
    this.projectRepository = projectRepository;
    this.causeCategoryRepository = causeCategoryRepository;
    this.tagRepository = tagRepository;
  }

  /**
   * 見積もり精度の集計（サマリー・診断・散布図・サイズ帯別・精度推移）を取得します。
   *
   * @param userId    認証ユーザーID
   * @param condition 絞り込み条件（projectId / from / to）
   * @return 見積もり精度集計
   */
  @Transactional(readOnly = true)
  public EstimationAccuracyResponse getEstimationAccuracy(
      int userId, AnalyticsQueryCondition condition) {
    requireProjectOwnership(condition.getProjectId(), userId);
    requireTagOwnership(condition.getTagId(), userId);
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
    List<AnalyticsScatterPointRow> scatterRows =
        analyticsRepository.findScatterPoints(userId, condition, threshold, SCATTER_MAX_POINTS);
    List<AnalyticsSizeBucketRow> sizeBucketRows =
        analyticsRepository.findSizeBuckets(userId, condition, threshold);
    // フィルタ条件・分析対象件数によらず常に返す。タグ絞り込み時の交絡確認に用いる（タグ要件§1.4/§5.3）。
    List<ProjectBreakdownRow> projectBreakdownRows =
        analyticsRepository.findProjectBreakdown(userId, condition);

    int analyzedCount = summaryRow.getAnalyzedCount();
    ExcludedTaskCountResponse excluded = buildExcluded(excludedRow, analyzedCount);
    AccuracySummaryResponse summary = buildSummary(summaryRow, varianceRow, analyzedCount);
    DiagnosisResponse diagnosis = analyzedCount >= MIN_TASKS_FOR_DIAGNOSIS
        ? buildDiagnosis(summaryRow, threshold)
        : null;
    boolean scatterTruncated = scatterRows.size() > SCATTER_MAX_POINTS;
    List<ScatterPointResponse> scatter = buildScatter(scatterRows, SCATTER_MAX_POINTS);
    List<SizeBucketResponse> sizeBuckets = buildSizeBuckets(sizeBucketRows);

    // 窓（TREND_WINDOW_SIZE件）が埋まらない場合は0件しか返らないため、無駄なクエリを避けるために
    // 分析対象件数の時点でスキップする。
    List<AccuracyTrendPointResponse> trend = analyzedCount >= MIN_TASKS_FOR_TREND
        ? buildTrend(analyticsRepository.findAccuracyTrend(userId, condition, TREND_WINDOW_SIZE))
        : List.of();
    MetricAvailabilityResponse trendAvailability = new MetricAvailabilityResponse(
        analyzedCount >= MIN_TASKS_FOR_TREND, MIN_TASKS_FOR_TREND, analyzedCount);

    return new EstimationAccuracyResponse(
        threshold,
        analyzedCount,
        excluded,
        summary,
        diagnosis,
        scatter,
        scatterTruncated,
        sizeBuckets,
        trend,
        trendAvailability,
        buildProjectBreakdown(projectBreakdownRows));
  }

  /**
   * 原因カテゴリ別の集計（超過側・短縮側・共通の3グループ）を取得します。
   * 除外ルール（§2-1）を適用した分析対象タスクのうち、振り返り済みのものを対象とします。
   *
   * @param userId    認証ユーザーID
   * @param condition 絞り込み条件（projectId / from / to）
   * @return 原因カテゴリ別集計
   */
  @Transactional(readOnly = true)
  public GapCauseAggregateResponse getGapCauses(int userId, AnalyticsQueryCondition condition) {
    requireProjectOwnership(condition.getProjectId(), userId);
    requireTagOwnership(condition.getTagId(), userId);
    requireValidPeriod(condition);

    double threshold = thresholdProperties.getOnTimePercent();
    // analyzedTaskCountはestimation-accuracyと同じ抽出条件・同じクエリで算出し、二重に定義しない。
    int analyzedTaskCount =
        analyticsRepository.findSummary(userId, condition, threshold).getAnalyzedCount();
    List<GapCauseRow> rows = analyticsRepository.findGapCauses(userId, condition);

    GapCauseRow unclassifiedRow = rows.stream()
        .filter(row -> row.getDirection() == null)
        .findFirst()
        .orElse(null);
    Map<String, List<GapCauseRow>> classifiedRowsByDirection = rows.stream()
        .filter(row -> row.getDirection() != null)
        .collect(Collectors.groupingBy(GapCauseRow::getDirection));

    List<GapCauseGroupResponse> groups = GAP_CAUSE_DIRECTIONS.stream()
        .map(direction -> buildGapCauseGroup(
            direction,
            classifiedRowsByDirection.getOrDefault(direction, List.of()),
            "BOTH".equals(direction) ? unclassifiedRow : null,
            analyzedTaskCount))
        .toList();
    int totalLinkCount = groups.stream().mapToInt(GapCauseGroupResponse::getTotalCount).sum();

    return new GapCauseAggregateResponse(analyzedTaskCount, totalLinkCount, groups);
  }

  /**
   * 散布図データを組み立てます。DBからは完了日時降順で最大 {@code limit + 1} 件受け取り、
   * 上限を超える場合は最新 {@code limit} 件へ切り詰めたうえで、描画順を安定させるため昇順へ戻します。
   */
  private List<ScatterPointResponse> buildScatter(List<AnalyticsScatterPointRow> rows, int limit) {
    List<AnalyticsScatterPointRow> trimmed = rows.size() > limit ? rows.subList(0, limit) : rows;
    // 新しいクエリは書かず、取得済みのtaskId群に対してB2で作成済みのfindTagsInTasksをそのまま使う
    // （タグ機能実装計画フェーズB4 §2）。
    Map<Integer, List<TagSummaryResponse>> tagsByTaskId = tagsByTaskId(
        trimmed.stream().map(AnalyticsScatterPointRow::getTaskId).toList());
    List<ScatterPointResponse> result = new ArrayList<>(trimmed.stream()
        .map(row -> new ScatterPointResponse(
            row.getTaskId(),
            row.getTaskTitle(),
            row.getEstimatedMinutes(),
            row.getActualMinutes(),
            row.getGapRate(),
            row.getOutcome(),
            tagsByTaskId.getOrDefault(row.getTaskId(), List.of())))
        .toList());
    Collections.reverse(result);
    return result;
  }

  /**
   * タスクIDの一覧から、タスクごとに付与されたタグ（名前昇順・アーカイブ済みを含む）へのマップを
   * 一括取得します。空リストで問い合わせるとMyBatisの{@code IN ()}が構文エラーになるため、
   * 呼び出し前に空チェックを行います（既存のmemoRepository.findAllInTasksと同じ扱い）。
   */
  private Map<Integer, List<TagSummaryResponse>> tagsByTaskId(List<Integer> taskIds) {
    if (taskIds.isEmpty()) {
      return Map.of();
    }
    return tagRepository.findTagsInTasks(taskIds).stream()
        .collect(Collectors.groupingBy(
            TaskTagRow::getTaskId,
            Collectors.mapping(
                row -> new TagSummaryResponse(row.getTagId(), row.getName()),
                Collectors.toList())));
  }

  private List<ProjectBreakdownItemResponse> buildProjectBreakdown(List<ProjectBreakdownRow> rows) {
    return rows.stream()
        .map(row -> new ProjectBreakdownItemResponse(
            row.getProjectId(), row.getProjectTitle(), row.getCount()))
        .toList();
  }

  /**
   * タスクサイズ帯別の集計を組み立てます。該当0件の帯はSQLの結果に現れないため、
   * 全帯（{@code SIZE_BUCKET_CODES}）を固定順で埋めてから返します。
   */
  private List<SizeBucketResponse> buildSizeBuckets(List<AnalyticsSizeBucketRow> rows) {
    Map<String, AnalyticsSizeBucketRow> rowsByBucketCode = rows.stream()
        .collect(Collectors.toMap(AnalyticsSizeBucketRow::getBucketCode, row -> row));

    return SIZE_BUCKET_CODES.stream()
        .map(bucketCode -> {
          AnalyticsSizeBucketRow row = rowsByBucketCode.get(bucketCode);
          String label = SIZE_BUCKET_LABELS.get(bucketCode);
          if (row == null) {
            return new SizeBucketResponse(bucketCode, label, 0, null, null);
          }
          int taskCount = row.getTaskCount();
          boolean available = taskCount >= MIN_TASKS_PER_SIZE_BUCKET;
          Double factorMedian = available ? row.getFactorMedian() : null;
          Double onTimeRate = available ? (row.getOnTimeCount() * 100.0) / taskCount : null;
          return new SizeBucketResponse(bucketCode, label, taskCount, factorMedian, onTimeRate);
        })
        .toList();
  }

  /**
   * 精度推移（移動中央値）を組み立てます。SQL側で既に完了順の連番（{@code sequence}）でソート済みです。
   */
  private List<AccuracyTrendPointResponse> buildTrend(List<AnalyticsTrendRow> rows) {
    return rows.stream()
        .map(row -> new AccuracyTrendPointResponse(
            row.getSequence(),
            row.getFinishedAt(),
            row.getWindowFrom(),
            row.getFactorMedian(),
            row.getVariancePercent()))
        .toList();
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
    requireTagOwnership(condition.getTagId(), userId);
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
    List<TaskTagRow> tagRows =
        analyticsRepository.findReflectionTimelineTags(userId, condition, threshold);

    Map<Integer, List<ReflectionCauseCategorySummaryResponse>> categoriesByReflectionId =
        categoryRows.stream()
            .collect(Collectors.groupingBy(
                ReflectionCauseCategoryLinkRow::getReflectionId,
                LinkedHashMap::new,
                Collectors.mapping(
                    row -> new ReflectionCauseCategorySummaryResponse(
                        row.getCauseCategoryCode(), row.getCauseCategoryLabel()),
                    Collectors.toList())));
    // タグはタスク単位で付与されるため、原因カテゴリ（reflectionId単位）とは異なりtaskIdでグループ化する。
    Map<Integer, List<TagSummaryResponse>> tagsByTaskId = tagRows.stream()
        .collect(Collectors.groupingBy(
            TaskTagRow::getTaskId,
            LinkedHashMap::new,
            Collectors.mapping(
                row -> new TagSummaryResponse(row.getTagId(), row.getName()),
                Collectors.toList())));

    List<ReflectionTimelineItemResponse> items = rows.stream()
        .map(row -> toTimelineItem(row, categoriesByReflectionId, tagsByTaskId))
        .toList();

    boolean hasNext = (long) (condition.getPage() + 1) * condition.getSize() < totalCount;
    return new ReflectionTimelineResponse(
        items, condition.getPage(), condition.getSize(), totalCount, hasNext);
  }

  private ReflectionTimelineItemResponse toTimelineItem(
      ReflectionTimelineRow row,
      Map<Integer, List<ReflectionCauseCategorySummaryResponse>> categoriesByReflectionId,
      Map<Integer, List<TagSummaryResponse>> tagsByTaskId) {
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
        row.getNextAction(),
        tagsByTaskId.getOrDefault(row.getTaskId(), List.of()));
  }

  private void requireProjectOwnership(Integer projectId, int userId) {
    if (projectId != null && !projectRepository.existsByIdAndUserId(projectId, userId)) {
      throw new TargetNotFoundException(
          "projectId", "指定したIDのプロジェクトは見つかりませんでした");
    }
  }

  /**
   * 絞り込み対象タグの所有権と、アーカイブ状態を検証します。存在しない、または他ユーザーのタグは
   * 404（projectIdと同じ扱い）、アーカイブ済みのタグは400とします。アーカイブは「このタグでの分析を
   * やめる」という宣言であり、存在は隠さないが宣言に反して集計しません（タグ要件§3.5 / §8.3）。
   */
  private void requireTagOwnership(Integer tagId, int userId) {
    if (tagId == null) {
      return;
    }
    Tag tag = tagRepository.findByIdAndUserId(tagId, userId);
    if (tag == null) {
      throw new TargetNotFoundException("tagId", "指定したIDのタグは見つかりませんでした");
    }
    if (Boolean.TRUE.equals(tag.getIsArchived())) {
      throw new AnalyticsQueryInvalidException(
          "tagId", "アーカイブ済みのタグは分析の絞り込みに指定できません");
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

  /**
   * 方向グループ（超過側・短縮側・共通）を組み立てます。件数降順で並べたうえで、
   * 未分類（{@code unclassifiedRow}）は常に末尾に追加します（要件§4.7）。
   */
  private GapCauseGroupResponse buildGapCauseGroup(
      String direction, List<GapCauseRow> rows, GapCauseRow unclassifiedRow, int analyzedTaskCount) {
    List<GapCauseItemResponse> items = new ArrayList<>(rows.stream()
        .sorted(Comparator.comparing(GapCauseRow::getTaskCount, Comparator.reverseOrder())
            .thenComparing(GapCauseRow::getDisplayOrder))
        .map(row -> toGapCauseItem(row, analyzedTaskCount))
        .toList());
    if (unclassifiedRow != null) {
      items.add(toGapCauseItem(unclassifiedRow, analyzedTaskCount));
    }
    int totalCount = items.stream().mapToInt(GapCauseItemResponse::getTaskCount).sum();
    double sharePercent = analyzedTaskCount == 0 ? 0.0 : (totalCount * 100.0) / analyzedTaskCount;
    return new GapCauseGroupResponse(
        direction, GAP_CAUSE_DIRECTION_LABELS.get(direction), totalCount, sharePercent, items);
  }

  private GapCauseItemResponse toGapCauseItem(GapCauseRow row, int analyzedTaskCount) {
    int taskCount = row.getTaskCount();
    boolean available = taskCount >= MIN_TASKS_PER_CAUSE_CATEGORY;
    Double gapRateMedian = available ? row.getGapRateMedian() : null;
    double sharePercent = analyzedTaskCount == 0 ? 0.0 : (taskCount * 100.0) / analyzedTaskCount;
    String code = row.getCauseCategoryCode();
    String label = code == null ? UNCLASSIFIED_LABEL : row.getCauseCategoryLabel();
    return new GapCauseItemResponse(code, label, taskCount, sharePercent, gapRateMedian);
  }
}
