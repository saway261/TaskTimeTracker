package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.config.AnalyticsThresholdProperties;
import com.kiborisaway.tasktimetracker.data.dto.analytics.AnalyticsQueryCondition;
import com.kiborisaway.tasktimetracker.data.dto.analytics.EstimationAccuracyResponse;
import com.kiborisaway.tasktimetracker.exception.AnalyticsQueryInvalidException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.AnalyticsRecentVarianceRow;
import com.kiborisaway.tasktimetracker.repository.AnalyticsRepository;
import com.kiborisaway.tasktimetracker.repository.AnalyticsSummaryRow;
import com.kiborisaway.tasktimetracker.repository.ExcludedCountRow;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

  private static final int USER_ID = 1;

  @Mock
  private AnalyticsRepository analyticsRepository;

  @Mock
  private ProjectRepository projectRepository;

  private final AnalyticsThresholdProperties thresholdProperties =
      new AnalyticsThresholdProperties(10.0);

  private AnalyticsService service() {
    return new AnalyticsService(analyticsRepository, thresholdProperties, projectRepository);
  }

  @Test
  void 取得失敗_指定したプロジェクトが認証ユーザーのものでない場合は404用例外を投げること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(3, null, null);
    when(projectRepository.existsByIdAndUserId(3, USER_ID)).thenReturn(false);

    assertThatThrownBy(() -> sut.getEstimationAccuracy(USER_ID, condition))
        .isInstanceOf(TargetNotFoundException.class);
    verify(analyticsRepository, never()).findSummary(
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.anyDouble());
  }

  @Test
  void 取得失敗_fromがtoより後の場合は400用例外を投げること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(
        null,
        LocalDateTime.of(2026, 2, 1, 0, 0),
        LocalDateTime.of(2026, 1, 1, 0, 0));

    assertThatThrownBy(() -> sut.getEstimationAccuracy(USER_ID, condition))
        .isInstanceOf(AnalyticsQueryInvalidException.class);
    verify(analyticsRepository, never()).findSummary(
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.anyDouble());
  }

  @Test
  void 取得成功_分析対象0件でもエラーにならず各指標がnullになること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    stub(condition, summaryRow(0, 0, 0, 0, null, null, null, null),
        varianceRow(null, null), excludedRow(0, 0, 0));

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    assertThat(actual.getAnalyzedTaskCount()).isZero();
    assertThat(actual.getSummary().getAvailability().isAvailable()).isFalse();
    assertThat(actual.getSummary().getOnTimeRate()).isNull();
    assertThat(actual.getSummary().getOutcomeBreakdown().getLateCount()).isZero();
    assertThat(actual.getDiagnosis()).isNull();
    assertThat(actual.getScatter()).isEmpty();
    assertThat(actual.getSizeBuckets()).isEmpty();
    assertThat(actual.getTrend()).isEmpty();
    assertThat(actual.getTrendAvailability().isAvailable()).isFalse();
    assertThat(actual.getTrendAvailability().getRequiredCount()).isEqualTo(20);
  }

  @Test
  void 取得成功_findRecentVarianceがnullを返してもエラーにならないこと() {
    // 分析対象0件のときrecent_variance・previous_variance列が両方NULLになり、MyBatisが
    // 空行とみなして戻り値自体をnullにする（AnalyticsRepositoryのJavaDoc参照）。
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    when(analyticsRepository.findSummary(USER_ID, condition, 10.0))
        .thenReturn(summaryRow(0, 0, 0, 0, null, null, null, null));
    when(analyticsRepository.findRecentVariance(USER_ID, condition)).thenReturn(null);
    when(analyticsRepository.findExcludedCounts(USER_ID, condition))
        .thenReturn(excludedRow(0, 0, 0));

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    assertThat(actual.getSummary().getOnTimeRate()).isNull();
    assertThat(actual.getSummary().getRecentTrend()).isNull();
  }

  @Test
  void 取得成功_分析対象4件では統計値がnullで内訳件数のみ返ること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    stub(condition, summaryRow(4, 2, 1, 1, 1.5, 1.2, 1.8, 20.0),
        varianceRow(null, null), excludedRow(4, 0, 0));

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    assertThat(actual.getSummary().getAvailability())
        .satisfies(a -> {
          assertThat(a.isAvailable()).isFalse();
          assertThat(a.getRequiredCount()).isEqualTo(5);
          assertThat(a.getCurrentCount()).isEqualTo(4);
        });
    assertThat(actual.getSummary().getOnTimeRate()).isNull();
    assertThat(actual.getSummary().getFactorMedian()).isNull();
    assertThat(actual.getSummary().getOutcomeBreakdown().getLateCount()).isEqualTo(2);
    assertThat(actual.getSummary().getOutcomeBreakdown().getEarlyCount()).isEqualTo(1);
    assertThat(actual.getSummary().getOutcomeBreakdown().getOnTimeCount()).isEqualTo(1);
    assertThat(actual.getDiagnosis()).isNull();
  }

  @Test
  void 取得成功_分析対象5件以上で代表係数などの統計値が返ること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    stub(condition, summaryRow(5, 2, 1, 2, 1.4, 1.1, 1.7, 18.0),
        varianceRow(null, null), excludedRow(5, 0, 0));

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    assertThat(actual.getSummary().getAvailability().isAvailable()).isTrue();
    assertThat(actual.getSummary().getOnTimeRate()).isEqualTo(40.0); // 2/5 * 100
    assertThat(actual.getSummary().getFactorMedian()).isEqualTo(1.4);
    assertThat(actual.getSummary().getFactorP25()).isEqualTo(1.1);
    assertThat(actual.getSummary().getFactorP75()).isEqualTo(1.7);
    assertThat(actual.getSummary().getVariancePercent()).isEqualTo(18.0);
  }

  @Test
  void 取得成功_分析対象9件では診断がnullになること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    stub(condition, summaryRow(9, 3, 2, 4, 1.1, 1.0, 1.2, 10.0),
        varianceRow(null, null), excludedRow(9, 0, 0));

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    assertThat(actual.getDiagnosis()).isNull();
  }

  @Test
  void 診断_偏り小ばらつき小はGOODになること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    stub(condition, summaryRow(10, 3, 2, 5, 1.05, 1.0, 1.1, 20.0),
        varianceRow(null, null), excludedRow(10, 0, 0));

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    assertThat(actual.getDiagnosis().getCode()).isEqualTo("GOOD");
    assertThat(actual.getDiagnosis().getBiasDirection()).isEqualTo("NONE");
  }

  @Test
  void 診断_偏り小ばらつき大はUNSTABLEになること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    stub(condition, summaryRow(10, 3, 2, 5, 1.05, 1.0, 1.1, 30.0),
        varianceRow(null, null), excludedRow(10, 0, 0));

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    assertThat(actual.getDiagnosis().getCode()).isEqualTo("UNSTABLE");
  }

  @Test
  void 診断_偏り大超過側ばらつき小はBIASED_LATEになり代表係数を文中に含むこと() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    stub(condition, summaryRow(10, 8, 0, 2, 1.6, 1.4, 1.8, 20.0),
        varianceRow(null, null), excludedRow(10, 0, 0));

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    assertThat(actual.getDiagnosis().getCode()).isEqualTo("BIASED_LATE");
    assertThat(actual.getDiagnosis().getBiasDirection()).isEqualTo("LATE");
    assertThat(actual.getDiagnosis().getMessage()).contains("1.6");
  }

  @Test
  void 診断_代表係数が1未満のときBIASED_EARLYになり短縮側専用の文言が返ること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    stub(condition, summaryRow(10, 0, 8, 2, 0.7, 0.6, 0.8, 20.0),
        varianceRow(null, null), excludedRow(10, 0, 0));

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    assertThat(actual.getDiagnosis().getCode()).isEqualTo("BIASED_EARLY");
    assertThat(actual.getDiagnosis().getBiasDirection()).isEqualTo("EARLY");
    assertThat(actual.getDiagnosis().getMessage()).contains("0.7");
  }

  @Test
  void 診断_偏り大超過側ばらつき大はUNSTABLE_BIASEDでLATEになること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    stub(condition, summaryRow(10, 8, 0, 2, 1.6, 1.2, 2.0, 30.0),
        varianceRow(null, null), excludedRow(10, 0, 0));

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    assertThat(actual.getDiagnosis().getCode()).isEqualTo("UNSTABLE_BIASED");
    assertThat(actual.getDiagnosis().getBiasDirection()).isEqualTo("LATE");
  }

  @Test
  void 診断_偏り大短縮側ばらつき大はUNSTABLE_BIASEDでEARLYになること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    stub(condition, summaryRow(10, 0, 8, 2, 0.6, 0.3, 0.9, 30.0),
        varianceRow(null, null), excludedRow(10, 0, 0));

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    assertThat(actual.getDiagnosis().getCode()).isEqualTo("UNSTABLE_BIASED");
    assertThat(actual.getDiagnosis().getBiasDirection()).isEqualTo("EARLY");
  }

  @Test
  void 直近の傾向_差が3ポイント以下ならIMPROVEDになること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    stub(condition, summaryRow(20, 5, 5, 10, 1.2, 1.0, 1.4, 20.0),
        varianceRow(20.0, 25.0), excludedRow(20, 0, 0));

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    assertThat(actual.getSummary().getRecentTrend()).isEqualTo("IMPROVED");
  }

  @Test
  void 直近の傾向_差が3ポイント以上ならWORSENEDになること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    stub(condition, summaryRow(20, 5, 5, 10, 1.2, 1.0, 1.4, 20.0),
        varianceRow(25.0, 20.0), excludedRow(20, 0, 0));

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    assertThat(actual.getSummary().getRecentTrend()).isEqualTo("WORSENED");
  }

  @Test
  void 直近の傾向_差が3ポイント未満ならSTABLEになること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    stub(condition, summaryRow(20, 5, 5, 10, 1.2, 1.0, 1.4, 20.0),
        varianceRow(21.0, 20.0), excludedRow(20, 0, 0));

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    assertThat(actual.getSummary().getRecentTrend()).isEqualTo("STABLE");
  }

  @Test
  void 直近の傾向_いずれかがnullならnullになること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    stub(condition, summaryRow(15, 5, 5, 5, 1.2, 1.0, 1.4, 20.0),
        varianceRow(20.0, null), excludedRow(15, 0, 0));

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    assertThat(actual.getSummary().getRecentTrend()).isNull();
    assertThat(actual.getSummary().getRecentVariancePercent()).isEqualTo(20.0);
    assertThat(actual.getSummary().getPreviousVariancePercent()).isNull();
  }

  @Test
  void 除外件数_合計は完了件数と分析対象件数の差になること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    stub(condition, summaryRow(7, 2, 2, 3, 1.2, 1.0, 1.4, 20.0),
        varianceRow(null, null), excludedRow(10, 2, 1));

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    assertThat(actual.getExcluded().getTotal()).isEqualTo(3); // 10 - 7
    assertThat(actual.getExcluded().getMissingGapRate()).isEqualTo(2);
    assertThat(actual.getExcluded().getMissingActualMinutes()).isEqualTo(1);
  }

  private void stub(
      AnalyticsQueryCondition condition,
      AnalyticsSummaryRow summaryRow,
      AnalyticsRecentVarianceRow varianceRow,
      ExcludedCountRow excludedRow) {
    when(analyticsRepository.findSummary(USER_ID, condition, 10.0)).thenReturn(summaryRow);
    when(analyticsRepository.findRecentVariance(USER_ID, condition)).thenReturn(varianceRow);
    when(analyticsRepository.findExcludedCounts(USER_ID, condition)).thenReturn(excludedRow);
  }

  private static AnalyticsQueryCondition condition(
      Integer projectId, LocalDateTime from, LocalDateTime to) {
    AnalyticsQueryCondition condition = new AnalyticsQueryCondition();
    condition.setProjectId(projectId);
    condition.setFrom(from);
    condition.setTo(to);
    return condition;
  }

  private static AnalyticsSummaryRow summaryRow(
      int analyzedCount, int lateCount, int earlyCount, int onTimeCount,
      Double factorMedian, Double factorP25, Double factorP75, Double variancePercent) {
    return new AnalyticsSummaryRow(
        analyzedCount, lateCount, earlyCount, onTimeCount,
        factorMedian, factorP25, factorP75, variancePercent);
  }

  private static AnalyticsRecentVarianceRow varianceRow(Double recent, Double previous) {
    return new AnalyticsRecentVarianceRow(recent, previous);
  }

  private static ExcludedCountRow excludedRow(
      int finishedCount, int missingGapRate, int missingActualMinutes) {
    return new ExcludedCountRow(missingGapRate, missingActualMinutes, finishedCount);
  }
}
