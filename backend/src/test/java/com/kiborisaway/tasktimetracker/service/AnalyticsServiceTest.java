package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.config.AnalyticsThresholdProperties;
import com.kiborisaway.tasktimetracker.data.dto.analytics.AnalyticsQueryCondition;
import com.kiborisaway.tasktimetracker.data.dto.analytics.EstimationAccuracyResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.GapCauseAggregateResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.GapCauseGroupResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.GapCauseItemResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.ReflectionOutcomeFilter;
import com.kiborisaway.tasktimetracker.data.dto.analytics.ReflectionTimelineQueryCondition;
import com.kiborisaway.tasktimetracker.data.dto.analytics.ReflectionTimelineResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.ScatterPointResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.SizeBucketResponse;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionCauseCategorySummaryResponse;
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
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import com.kiborisaway.tasktimetracker.repository.ReflectionCauseCategoryLinkRow;
import com.kiborisaway.tasktimetracker.repository.ReflectionCauseCategoryRepository;
import com.kiborisaway.tasktimetracker.repository.ReflectionTimelineRow;
import com.kiborisaway.tasktimetracker.repository.TagRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

  @Mock
  private ReflectionCauseCategoryRepository causeCategoryRepository;

  @Mock
  private TagRepository tagRepository;

  private final AnalyticsThresholdProperties thresholdProperties =
      new AnalyticsThresholdProperties(10.0);

  private AnalyticsService service() {
    return new AnalyticsService(
        analyticsRepository, thresholdProperties, projectRepository, causeCategoryRepository,
        tagRepository);
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
  void 取得失敗_指定したタグが認証ユーザーのものでない場合は404用例外を投げること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    condition.setTagId(5);
    when(tagRepository.findByIdAndUserId(5, USER_ID)).thenReturn(null);

    assertThatThrownBy(() -> sut.getEstimationAccuracy(USER_ID, condition))
        .isInstanceOfSatisfying(TargetNotFoundException.class,
            ex -> assertThat(ex.getField()).isEqualTo("tagId"));
    verify(analyticsRepository, never()).findSummary(
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.anyDouble());
  }

  @Test
  void 取得失敗_アーカイブ済みのタグを指定した場合は400用例外を投げること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    condition.setTagId(5);
    when(tagRepository.findByIdAndUserId(5, USER_ID))
        .thenReturn(new Tag(5, USER_ID, "旧タグ", "旧タグ", true, null));

    assertThatThrownBy(() -> sut.getEstimationAccuracy(USER_ID, condition))
        .isInstanceOfSatisfying(AnalyticsQueryInvalidException.class,
            ex -> assertThat(ex.getField()).isEqualTo("tagId"));
    verify(analyticsRepository, never()).findSummary(
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.anyDouble());
  }

  @Test
  void 取得成功_アクティブなタグを指定した場合は集計処理を呼び出すこと() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    condition.setTagId(5);
    when(tagRepository.findByIdAndUserId(5, USER_ID))
        .thenReturn(new Tag(5, USER_ID, "調査", "調査", false, null));
    stub(condition,
        summaryRow(0, 0, 0, 0, null, null, null, null),
        varianceRow(null, null),
        excludedRow(0, 0, 0));

    sut.getEstimationAccuracy(USER_ID, condition);

    verify(analyticsRepository).findSummary(USER_ID, condition, 10.0);
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
    // 該当0件の帯も全5帯が固定順で埋まって返る（虫食いにしない。要件§4.6）。
    assertThat(actual.getSizeBuckets())
        .extracting(SizeBucketResponse::getBucketCode)
        .containsExactly("M15", "M30", "M60", "M120", "OVER120");
    assertThat(actual.getSizeBuckets())
        .allSatisfy(bucket -> {
          assertThat(bucket.getTaskCount()).isZero();
          assertThat(bucket.getFactorMedian()).isNull();
          assertThat(bucket.getOnTimeRate()).isNull();
        });
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
  void 散布図_上限を超える場合は最新N件に切り詰めて昇順で返しtruncatedになること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    // findScatterPointsはfinished_at DESCで返す想定。taskId=500が最新（先頭）、taskId=0が最古。
    List<AnalyticsScatterPointRow> rows = new ArrayList<>();
    for (int i = 0; i <= 500; i++) {
      int taskId = 500 - i;
      rows.add(new AnalyticsScatterPointRow(taskId, "タスク" + taskId, 60, 60, 0.0, "ON_TIME"));
    }
    stub(condition, summaryRow(501, 0, 0, 501, 1.0, 1.0, 1.0, 0.0),
        varianceRow(null, null), excludedRow(501, 0, 0));
    when(analyticsRepository.findScatterPoints(USER_ID, condition, 10.0, 500)).thenReturn(rows);
    when(analyticsRepository.findSizeBuckets(USER_ID, condition, 10.0)).thenReturn(List.of());

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    assertThat(actual.isScatterTruncated()).isTrue();
    assertThat(actual.getScatter()).hasSize(500);
    // 最古の1件（taskId=0）が切り詰めで落ち、残り500件が昇順で返る。
    assertThat(actual.getScatter().get(0).getTaskId()).isEqualTo(1);
    assertThat(actual.getScatter().get(499).getTaskId()).isEqualTo(500);
  }

  @Test
  void 散布図_上限以下の場合はtruncatedにならず完了日時昇順で返ること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    List<AnalyticsScatterPointRow> rows = List.of(
        new AnalyticsScatterPointRow(2, "新しい方", 60, 60, 0.0, "ON_TIME"),
        new AnalyticsScatterPointRow(1, "古い方", 60, 90, 50.0, "LATE"));
    stub(condition, summaryRow(2, 1, 0, 1, 1.25, 1.0, 1.5, 25.0),
        varianceRow(null, null), excludedRow(2, 0, 0));
    when(analyticsRepository.findScatterPoints(USER_ID, condition, 10.0, 500)).thenReturn(rows);
    when(analyticsRepository.findSizeBuckets(USER_ID, condition, 10.0)).thenReturn(List.of());

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    assertThat(actual.isScatterTruncated()).isFalse();
    assertThat(actual.getScatter())
        .extracting(ScatterPointResponse::getTaskId)
        .containsExactly(1, 2);
  }

  @Test
  void サイズ帯_3件未満の帯は件数のみ返り統計値がnullになること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    stub(condition, summaryRow(2, 0, 0, 2, 1.0, 1.0, 1.0, 0.0),
        varianceRow(null, null), excludedRow(2, 0, 0));
    when(analyticsRepository.findScatterPoints(USER_ID, condition, 10.0, 500)).thenReturn(List.of());
    when(analyticsRepository.findSizeBuckets(USER_ID, condition, 10.0))
        .thenReturn(List.of(new AnalyticsSizeBucketRow("M15", 2, 1.2, 2)));

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    SizeBucketResponse m15 = bucket(actual, "M15");
    assertThat(m15.getTaskCount()).isEqualTo(2);
    assertThat(m15.getFactorMedian()).isNull();
    assertThat(m15.getOnTimeRate()).isNull();
  }

  @Test
  void サイズ帯_3件以上の帯は代表係数とオンタイム率が算出されること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    stub(condition, summaryRow(4, 1, 0, 3, 1.0, 1.0, 1.0, 0.0),
        varianceRow(null, null), excludedRow(4, 0, 0));
    when(analyticsRepository.findScatterPoints(USER_ID, condition, 10.0, 500)).thenReturn(List.of());
    when(analyticsRepository.findSizeBuckets(USER_ID, condition, 10.0))
        .thenReturn(List.of(new AnalyticsSizeBucketRow("M30", 4, 1.25, 3)));

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    SizeBucketResponse m30 = bucket(actual, "M30");
    assertThat(m30.getTaskCount()).isEqualTo(4);
    assertThat(m30.getFactorMedian()).isEqualTo(1.25);
    assertThat(m30.getOnTimeRate()).isEqualTo(75.0);
  }

  @Test
  void サイズ帯_境界値ちょうどが下側の帯に含まれること() {
    // SQL側のCASE式（estimated_minutes <= 境界値）の境界値自体をここでは検証できないため、
    // リポジトリ層のテスト（AnalyticsRepositoryTest）で確認する。ここではJava側の
    // 全帯埋めロジックが5帯すべてを固定順で返すことのみ確認する。
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    stub(condition, summaryRow(0, 0, 0, 0, null, null, null, null),
        varianceRow(null, null), excludedRow(0, 0, 0));
    when(analyticsRepository.findScatterPoints(USER_ID, condition, 10.0, 500)).thenReturn(List.of());
    when(analyticsRepository.findSizeBuckets(USER_ID, condition, 10.0)).thenReturn(List.of());

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    assertThat(actual.getSizeBuckets())
        .extracting(SizeBucketResponse::getLabel)
        .containsExactly("〜15分", "16〜30分", "31〜60分", "61〜120分", "121分〜");
  }

  private static SizeBucketResponse bucket(EstimationAccuracyResponse response, String bucketCode) {
    return response.getSizeBuckets().stream()
        .filter(b -> b.getBucketCode().equals(bucketCode))
        .findFirst()
        .orElseThrow();
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

  @Test
  void 精度推移_分析対象19件では推移クエリを呼ばず空配列になること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    stub(condition, summaryRow(19, 5, 5, 9, 1.2, 1.0, 1.4, 20.0),
        varianceRow(null, null), excludedRow(19, 0, 0));

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    assertThat(actual.getTrend()).isEmpty();
    assertThat(actual.getTrendAvailability().isAvailable()).isFalse();
    verify(analyticsRepository, never()).findAccuracyTrend(
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.anyInt());
  }

  @Test
  void 精度推移_分析対象20件以上では推移データを窓幅10で取得し変換して返ること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    stub(condition, summaryRow(20, 5, 5, 10, 1.2, 1.0, 1.4, 20.0),
        varianceRow(null, null), excludedRow(20, 0, 0));
    LocalDateTime windowFrom = LocalDateTime.of(2026, 8, 1, 10, 0);
    LocalDateTime finishedAt = LocalDateTime.of(2026, 8, 10, 10, 0);
    when(analyticsRepository.findAccuracyTrend(USER_ID, condition, 10))
        .thenReturn(List.of(new AnalyticsTrendRow(10, finishedAt, windowFrom, 1.5, 22.0)));

    EstimationAccuracyResponse actual = sut.getEstimationAccuracy(USER_ID, condition);

    assertThat(actual.getTrendAvailability().isAvailable()).isTrue();
    assertThat(actual.getTrend()).hasSize(1);
    assertThat(actual.getTrend().get(0).getSequence()).isEqualTo(10);
    assertThat(actual.getTrend().get(0).getFinishedAt()).isEqualTo(finishedAt);
    assertThat(actual.getTrend().get(0).getWindowFrom()).isEqualTo(windowFrom);
    assertThat(actual.getTrend().get(0).getFactorMedian()).isEqualTo(1.5);
    assertThat(actual.getTrend().get(0).getVariancePercent()).isEqualTo(22.0);
  }

  @Test
  void 原因カテゴリ集計_取得失敗_指定したプロジェクトが認証ユーザーのものでない場合は404用例外を投げること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(3, null, null);
    when(projectRepository.existsByIdAndUserId(3, USER_ID)).thenReturn(false);

    assertThatThrownBy(() -> sut.getGapCauses(USER_ID, condition))
        .isInstanceOf(TargetNotFoundException.class);
    verify(analyticsRepository, never()).findGapCauses(ArgumentMatchers.anyInt(), ArgumentMatchers.any());
  }

  @Test
  void 原因カテゴリ集計_取得失敗_fromがtoより後の場合は400用例外を投げること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(
        null,
        LocalDateTime.of(2026, 2, 1, 0, 0),
        LocalDateTime.of(2026, 1, 1, 0, 0));

    assertThatThrownBy(() -> sut.getGapCauses(USER_ID, condition))
        .isInstanceOf(AnalyticsQueryInvalidException.class);
    verify(analyticsRepository, never()).findGapCauses(ArgumentMatchers.anyInt(), ArgumentMatchers.any());
  }

  @Test
  void 原因カテゴリ集計_取得失敗_指定したタグが認証ユーザーのものでない場合は404用例外を投げること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    condition.setTagId(5);
    when(tagRepository.findByIdAndUserId(5, USER_ID)).thenReturn(null);

    assertThatThrownBy(() -> sut.getGapCauses(USER_ID, condition))
        .isInstanceOf(TargetNotFoundException.class);
    verify(analyticsRepository, never()).findGapCauses(ArgumentMatchers.anyInt(), ArgumentMatchers.any());
  }

  @Test
  void 原因カテゴリ集計_取得失敗_アーカイブ済みのタグを指定した場合は400用例外を投げること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    condition.setTagId(5);
    when(tagRepository.findByIdAndUserId(5, USER_ID))
        .thenReturn(new Tag(5, USER_ID, "旧タグ", "旧タグ", true, null));

    assertThatThrownBy(() -> sut.getGapCauses(USER_ID, condition))
        .isInstanceOf(AnalyticsQueryInvalidException.class);
    verify(analyticsRepository, never()).findGapCauses(ArgumentMatchers.anyInt(), ArgumentMatchers.any());
  }

  @Test
  void 原因カテゴリ集計_件数降順で並び未分類が共通グループの末尾に来ること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    when(analyticsRepository.findSummary(USER_ID, condition, 10.0))
        .thenReturn(summaryRow(10, 3, 2, 5, 1.2, 1.0, 1.4, 20.0));
    when(analyticsRepository.findGapCauses(USER_ID, condition)).thenReturn(List.of(
        new GapCauseRow("OVER", "TASK_BREAKDOWN", "作業の洗い出しが足りなかった", 1, 3, 40.0),
        new GapCauseRow("OVER", "INTERRUPTION", "割り込みが発生した", 2, 5, 35.0),
        new GapCauseRow(null, null, null, null, 2, null)));

    GapCauseAggregateResponse actual = sut.getGapCauses(USER_ID, condition);

    GapCauseGroupResponse overGroup = group(actual, "OVER");
    assertThat(overGroup.getItems())
        .extracting(GapCauseItemResponse::getCauseCategoryCode)
        .containsExactly("INTERRUPTION", "TASK_BREAKDOWN");
    assertThat(overGroup.getTotalCount()).isEqualTo(8);

    GapCauseGroupResponse bothGroup = group(actual, "BOTH");
    assertThat(bothGroup.getItems())
        .extracting(GapCauseItemResponse::getCauseCategoryLabel)
        .containsExactly("未分類");
    assertThat(bothGroup.getTotalCount()).isEqualTo(2);

    GapCauseGroupResponse underGroup = group(actual, "UNDER");
    assertThat(underGroup.getItems()).isEmpty();
    assertThat(underGroup.getTotalCount()).isZero();
  }

  @Test
  void 原因カテゴリ集計_3件未満のカテゴリは代表誤差率がnullになること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    when(analyticsRepository.findSummary(USER_ID, condition, 10.0))
        .thenReturn(summaryRow(10, 3, 2, 5, 1.2, 1.0, 1.4, 20.0));
    when(analyticsRepository.findGapCauses(USER_ID, condition)).thenReturn(List.of(
        new GapCauseRow("UNDER", "OVER_ESTIMATION", "余裕を持たせすぎた", 1, 2, 15.0)));

    GapCauseAggregateResponse actual = sut.getGapCauses(USER_ID, condition);

    GapCauseItemResponse item = group(actual, "UNDER").getItems().get(0);
    assertThat(item.getTaskCount()).isEqualTo(2);
    assertThat(item.getGapRateMedian()).isNull();
  }

  @Test
  void 原因カテゴリ集計_延べ件数の合計は分析対象件数を上回りうること() {
    AnalyticsService sut = service();
    AnalyticsQueryCondition condition = condition(null, null, null);
    when(analyticsRepository.findSummary(USER_ID, condition, 10.0))
        .thenReturn(summaryRow(5, 2, 1, 2, 1.2, 1.0, 1.4, 20.0));
    when(analyticsRepository.findGapCauses(USER_ID, condition)).thenReturn(List.of(
        new GapCauseRow("OVER", "TASK_BREAKDOWN", "作業の洗い出しが足りなかった", 1, 4, 40.0),
        new GapCauseRow("UNDER", "OVER_ESTIMATION", "余裕を持たせすぎた", 1, 3, 15.0)));

    GapCauseAggregateResponse actual = sut.getGapCauses(USER_ID, condition);

    assertThat(actual.getAnalyzedTaskCount()).isEqualTo(5);
    assertThat(actual.getTotalLinkCount()).isEqualTo(7); // 4 + 3 > analyzedTaskCount(5)
    assertThat(group(actual, "OVER").getSharePercent()).isEqualTo(80.0); // 4/5*100
  }

  private static GapCauseGroupResponse group(GapCauseAggregateResponse response, String direction) {
    return response.getGroups().stream()
        .filter(g -> g.getDirection().equals(direction))
        .findFirst()
        .orElseThrow();
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

  @Test
  void タイムライン取得失敗_指定したプロジェクトが認証ユーザーのものでない場合は404用例外を投げること() {
    AnalyticsService sut = service();
    ReflectionTimelineQueryCondition condition = timelineCondition(3, null, 0, 20);
    when(projectRepository.existsByIdAndUserId(3, USER_ID)).thenReturn(false);

    assertThatThrownBy(() -> sut.getReflectionTimeline(USER_ID, condition))
        .isInstanceOf(TargetNotFoundException.class);
    verify(analyticsRepository, never()).findReflectionTimelineItems(
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.anyDouble());
  }

  @Test
  void タイムライン取得失敗_fromがtoより後の場合は400用例外を投げること() {
    AnalyticsService sut = service();
    ReflectionTimelineQueryCondition condition = timelineCondition(null, null, 0, 20);
    condition.setFrom(LocalDateTime.of(2026, 2, 1, 0, 0));
    condition.setTo(LocalDateTime.of(2026, 1, 1, 0, 0));

    assertThatThrownBy(() -> sut.getReflectionTimeline(USER_ID, condition))
        .isInstanceOf(AnalyticsQueryInvalidException.class);
    verify(analyticsRepository, never()).findReflectionTimelineItems(
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.anyDouble());
  }

  @Test
  void タイムライン取得失敗_原因カテゴリが無効な場合は400用例外を投げること() {
    AnalyticsService sut = service();
    ReflectionTimelineQueryCondition condition = timelineCondition(null, "UNKNOWN", 0, 20);
    when(causeCategoryRepository.findActiveByCode("UNKNOWN")).thenReturn(null);

    assertThatThrownBy(() -> sut.getReflectionTimeline(USER_ID, condition))
        .isInstanceOfSatisfying(ReflectionCauseCategoryInvalidException.class,
            ex -> assertThat(ex.getField()).isEqualTo("causeCategory"));
    verify(analyticsRepository, never()).findReflectionTimelineItems(
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.anyDouble());
  }

  @Test
  void タイムライン取得失敗_指定したタグが認証ユーザーのものでない場合は404用例外を投げること() {
    AnalyticsService sut = service();
    ReflectionTimelineQueryCondition condition = timelineCondition(null, null, 0, 20);
    condition.setTagId(5);
    when(tagRepository.findByIdAndUserId(5, USER_ID)).thenReturn(null);

    assertThatThrownBy(() -> sut.getReflectionTimeline(USER_ID, condition))
        .isInstanceOf(TargetNotFoundException.class);
    verify(analyticsRepository, never()).findReflectionTimelineItems(
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.anyDouble());
  }

  @Test
  void タイムライン取得失敗_アーカイブ済みのタグを指定した場合は400用例外を投げること() {
    AnalyticsService sut = service();
    ReflectionTimelineQueryCondition condition = timelineCondition(null, null, 0, 20);
    condition.setTagId(5);
    when(tagRepository.findByIdAndUserId(5, USER_ID))
        .thenReturn(new Tag(5, USER_ID, "旧タグ", "旧タグ", true, null));

    assertThatThrownBy(() -> sut.getReflectionTimeline(USER_ID, condition))
        .isInstanceOf(AnalyticsQueryInvalidException.class);
    verify(analyticsRepository, never()).findReflectionTimelineItems(
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.anyDouble());
  }

  @Test
  void タイムライン取得成功_原因カテゴリが表示順でまとめられ持たない振り返りは空配列になること() {
    AnalyticsService sut = service();
    ReflectionTimelineQueryCondition condition = timelineCondition(null, null, 0, 20);
    LocalDateTime finishedAt = LocalDateTime.of(2026, 8, 10, 10, 0);
    ReflectionTimelineRow withCategories = new ReflectionTimelineRow(
        1, "タスクA", 1, "プロジェクトA", finishedAt, 60, 90, 30, 50.0, "LATE", 10, "原因A", null);
    ReflectionTimelineRow withoutCategories = new ReflectionTimelineRow(
        2, "タスクB", 1, "プロジェクトA", finishedAt.minusDays(1), 60, 60, 0, 0.0, "ON_TIME", 11, null,
        "次のアクション");
    when(analyticsRepository.findReflectionTimelineItems(USER_ID, condition, 10.0))
        .thenReturn(List.of(withCategories, withoutCategories));
    when(analyticsRepository.countReflectionTimeline(USER_ID, condition, 10.0)).thenReturn(2);
    when(analyticsRepository.findReflectionTimelineCategories(USER_ID, condition, 10.0))
        .thenReturn(List.of(
            new ReflectionCauseCategoryLinkRow(10, "OTHER", "その他"),
            new ReflectionCauseCategoryLinkRow(10, "TASK_BREAKDOWN", "作業の洗い出しが足りなかった")));

    ReflectionTimelineResponse actual = sut.getReflectionTimeline(USER_ID, condition);

    assertThat(actual.getItems()).hasSize(2);
    assertThat(actual.getItems().get(0).getCauseCategories())
        .extracting(ReflectionCauseCategorySummaryResponse::getCode)
        .containsExactly("OTHER", "TASK_BREAKDOWN");
    assertThat(actual.getItems().get(1).getCauseCategories()).isEmpty();
    assertThat(actual.getItems().get(1).getCause()).isNull();
  }

  @Test
  void タイムライン取得成功_次のページがある場合hasNextがtrueになること() {
    AnalyticsService sut = service();
    ReflectionTimelineQueryCondition condition = timelineCondition(null, null, 0, 20);
    when(analyticsRepository.findReflectionTimelineItems(USER_ID, condition, 10.0))
        .thenReturn(List.of());
    when(analyticsRepository.countReflectionTimeline(USER_ID, condition, 10.0)).thenReturn(25);
    when(analyticsRepository.findReflectionTimelineCategories(USER_ID, condition, 10.0))
        .thenReturn(List.of());

    ReflectionTimelineResponse actual = sut.getReflectionTimeline(USER_ID, condition);

    assertThat(actual.getTotalCount()).isEqualTo(25);
    assertThat(actual.isHasNext()).isTrue();
  }

  @Test
  void タイムライン取得成功_最終ページではhasNextがfalseになること() {
    AnalyticsService sut = service();
    ReflectionTimelineQueryCondition condition = timelineCondition(null, null, 1, 20);
    when(analyticsRepository.findReflectionTimelineItems(USER_ID, condition, 10.0))
        .thenReturn(List.of());
    when(analyticsRepository.countReflectionTimeline(USER_ID, condition, 10.0)).thenReturn(25);
    when(analyticsRepository.findReflectionTimelineCategories(USER_ID, condition, 10.0))
        .thenReturn(List.of());

    ReflectionTimelineResponse actual = sut.getReflectionTimeline(USER_ID, condition);

    assertThat(actual.isHasNext()).isFalse();
  }

  private static ReflectionTimelineQueryCondition timelineCondition(
      Integer projectId, String causeCategory, int page, int size) {
    ReflectionTimelineQueryCondition condition = new ReflectionTimelineQueryCondition();
    condition.setProjectId(projectId);
    condition.setCauseCategory(causeCategory);
    condition.setOutcome(ReflectionOutcomeFilter.ALL);
    condition.setPage(page);
    condition.setSize(size);
    return condition;
  }
}
