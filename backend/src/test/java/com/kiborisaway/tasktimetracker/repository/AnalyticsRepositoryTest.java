package com.kiborisaway.tasktimetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

import com.kiborisaway.tasktimetracker.data.dto.analytics.AnalyticsQueryCondition;
import com.kiborisaway.tasktimetracker.data.dto.analytics.ReflectionOutcomeFilter;
import com.kiborisaway.tasktimetracker.data.dto.analytics.ReflectionTimelineQueryCondition;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

@MybatisTest
class AnalyticsRepositoryTest {

  private static final int USER_ID = 1;
  private static final int OTHER_USER_ID = 2;
  private static final double THRESHOLD = 10.0;
  // 20件フィクスチャの gapRate（昇順）。挿入順に finished_at を進め、末尾ほど新しい完了とする。
  private static final double[] GAP_RATES = {
      -50, -40, -30, -20, -15, -12, -8, -5, 0, 2, 5, 8, 10, 15, 20, 25, 30, 40, 50, 60
  };

  @Autowired
  private AnalyticsRepository sut;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void SQL方言検証_PERCENTILE_CONTとCTEとROW_NUMBERとNULL許容パラメータがH2で動作すること() {
    AnalyticsQueryCondition condition = new AnalyticsQueryCondition();

    AnalyticsSummaryRow summary = sut.findSummary(OTHER_USER_ID, condition, THRESHOLD);
    AnalyticsRecentVarianceRow variance = sut.findRecentVariance(OTHER_USER_ID, condition);
    ExcludedCountRow excluded = sut.findExcludedCounts(OTHER_USER_ID, condition);

    assertThat(summary).isNotNull();
    assertThat(summary.getAnalyzedCount()).isGreaterThanOrEqualTo(0);
    assertThat(variance).isNotNull();
    assertThat(excluded).isNotNull();
  }

  @Test
  void findSummary_件数と統計量が手計算した期待値と一致すること() {
    int projectId = insertProject(USER_ID, "分析20件検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    for (int i = 0; i < GAP_RATES.length; i++) {
      insertFinishedTask(projectId, null, base.plusDays(i), 100, GAP_RATES[i]);
    }

    AnalyticsSummaryRow actual = sut.findSummary(USER_ID, conditionFor(projectId), THRESHOLD);

    assertThat(actual.getAnalyzedCount()).isEqualTo(20);
    assertThat(actual.getLateCount()).isEqualTo(7);
    assertThat(actual.getEarlyCount()).isEqualTo(6);
    assertThat(actual.getOnTimeCount()).isEqualTo(7);
    assertThat(actual.getFactorMedian()).isCloseTo(1.035, within(1e-9));
    assertThat(actual.getFactorP25()).isCloseTo(0.8725, within(1e-9));
    assertThat(actual.getFactorP75()).isCloseTo(1.2125, within(1e-9));
    assertThat(actual.getVariancePercent()).isCloseTo(17.5, within(1e-9));
  }

  @Test
  void findSummary_分析対象0件の場合は件数0で統計値がnullになること() {
    int projectId = insertProject(USER_ID, "分析0件検証");

    AnalyticsSummaryRow actual = sut.findSummary(USER_ID, conditionFor(projectId), THRESHOLD);

    assertThat(actual.getAnalyzedCount()).isZero();
    assertThat(actual.getLateCount()).isZero();
    assertThat(actual.getEarlyCount()).isZero();
    assertThat(actual.getOnTimeCount()).isZero();
    assertThat(actual.getFactorMedian()).isNull();
    assertThat(actual.getFactorP25()).isNull();
    assertThat(actual.getFactorP75()).isNull();
    assertThat(actual.getVariancePercent()).isNull();
  }

  @Test
  void findRecentVariance_直近10件とその前10件が手計算した期待値と一致すること() {
    int projectId = insertProject(USER_ID, "直近傾向検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    for (int i = 0; i < GAP_RATES.length; i++) {
      insertFinishedTask(projectId, null, base.plusDays(i), 100, GAP_RATES[i]);
    }

    AnalyticsRecentVarianceRow actual =
        sut.findRecentVariance(USER_ID, conditionFor(projectId));

    assertThat(actual.getRecentVariance()).isCloseTo(22.5, within(1e-9));
    assertThat(actual.getPreviousVariance()).isCloseTo(13.5, within(1e-9));
  }

  @Test
  void findRecentVariance_分析対象0件の場合は戻り値自体がnullになること() {
    // recent_variance・previous_varianceの両列がNULLになり、MyBatisが空行とみなしてnullを返す
    // （returnInstanceForEmptyRow）。呼び出し側（AnalyticsService）でのnull許容が必要になる。
    int projectId = insertProject(USER_ID, "件数不足検証");

    AnalyticsRecentVarianceRow actual =
        sut.findRecentVariance(USER_ID, conditionFor(projectId));

    assertThat(actual).isNull();
  }

  @Test
  void findRecentVariance_11件未満の場合は直近のみ算出されその前はnullになること() {
    // rn<=10の窓は1件でもあれば算出される。previousVariance（rn 11-20）はrn=11が存在する
    // 11件以上でなければ算出されないため、10件ちょうどで両者の境界を確認する。
    int projectId = insertProject(USER_ID, "10件検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    for (int i = 0; i < 10; i++) {
      insertFinishedTask(projectId, null, base.plusDays(i), 100, 10.0);
    }

    AnalyticsRecentVarianceRow actual =
        sut.findRecentVariance(USER_ID, conditionFor(projectId));

    assertThat(actual.getRecentVariance()).isNotNull();
    assertThat(actual.getPreviousVariance()).isNull();
  }

  @Test
  void findRecentVariance_11件以上の場合はその前の分も算出されること() {
    int projectId = insertProject(USER_ID, "11件検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    for (int i = 0; i < 11; i++) {
      insertFinishedTask(projectId, null, base.plusDays(i), 100, 10.0);
    }

    AnalyticsRecentVarianceRow actual =
        sut.findRecentVariance(USER_ID, conditionFor(projectId));

    assertThat(actual.getRecentVariance()).isNotNull();
    assertThat(actual.getPreviousVariance()).isNotNull();
  }

  @Test
  void findExcludedCounts_除外理由別の件数と完了件数を取得できること() {
    int projectId = insertProject(USER_ID, "除外件数検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    insertFinishedTask(projectId, null, base, 100, 20.0); // 分析対象
    insertFinishedTask(projectId, null, base.plusDays(1), 100, 30.0); // 分析対象
    insertFinishedTask(projectId, null, base.plusDays(2), 0, 0.0); // 実績未記録で除外
    insertFinishedTask(projectId, null, base.plusDays(3), 0, 0.0); // 実績未記録で除外
    insertFinishedTask(projectId, null, base.plusDays(4), 100, null); // 誤差率算出不可で除外

    ExcludedCountRow actual = sut.findExcludedCounts(USER_ID, conditionFor(projectId));

    assertThat(actual.getFinishedCount()).isEqualTo(5);
    assertThat(actual.getMissingActualMinutes()).isEqualTo(2);
    assertThat(actual.getMissingGapRate()).isEqualTo(1);
  }

  @Test
  void プロジェクト絞り込み_projectId指定時は指定プロジェクトのみ集計されること() {
    // MyBatisのセッションキャッシュは同一パラメータのSELECTを再実行しないため、
    // JdbcTemplate経由の素のCOUNTで事前件数を取る（挿入後にsutへ同一条件で再問い合わせしても
    // キャッシュされた古い結果が返ってしまうのを避けるため）。
    int baselineCount = countAnalyzedTasks(USER_ID);

    int projectA = insertProject(USER_ID, "絞り込みA");
    int projectB = insertProject(USER_ID, "絞り込みB");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    insertFinishedTask(projectA, null, base, 100, 5.0);
    insertFinishedTask(projectA, null, base.plusDays(1), 100, -5.0);
    insertFinishedTask(projectA, null, base.plusDays(2), 100, 15.0);
    insertFinishedTask(projectB, null, base, 100, 100.0);
    insertFinishedTask(projectB, null, base.plusDays(1), 100, -100.0);

    AnalyticsSummaryRow onlyA = sut.findSummary(USER_ID, conditionFor(projectA), THRESHOLD);
    AnalyticsSummaryRow onlyB = sut.findSummary(USER_ID, conditionFor(projectB), THRESHOLD);
    AnalyticsSummaryRow both = sut.findSummary(USER_ID, new AnalyticsQueryCondition(), THRESHOLD);

    assertThat(onlyA.getAnalyzedCount()).isEqualTo(3);
    assertThat(onlyB.getAnalyzedCount()).isEqualTo(2);
    assertThat(both.getAnalyzedCount())
        .isEqualTo(baselineCount + onlyA.getAnalyzedCount() + onlyB.getAnalyzedCount());
  }

  @Test
  void 期間絞り込み_fromとtoで完了日時が絞り込まれること() {
    int projectId = insertProject(USER_ID, "期間絞り込み");
    LocalDateTime early = LocalDateTime.of(2026, 1, 1, 0, 0);
    LocalDateTime late = LocalDateTime.of(2026, 6, 1, 0, 0);
    insertFinishedTask(projectId, null, early, 100, 5.0);
    insertFinishedTask(projectId, null, late, 100, 5.0);

    AnalyticsQueryCondition fromOnly = conditionFor(projectId);
    fromOnly.setFrom(LocalDateTime.of(2026, 3, 1, 0, 0));
    AnalyticsQueryCondition toOnly = conditionFor(projectId);
    toOnly.setTo(LocalDateTime.of(2026, 3, 1, 0, 0));

    assertThat(sut.findSummary(USER_ID, fromOnly, THRESHOLD).getAnalyzedCount()).isEqualTo(1);
    assertThat(sut.findSummary(USER_ID, toOnly, THRESHOLD).getAnalyzedCount()).isEqualTo(1);
  }

  @Test
  void ユーザー分離_他ユーザーのタスクは集計に混入しないこと() {
    // findSummaryをここで先に呼ぶとMyBatisのセッションキャッシュに載り、挿入後の再問い合わせが
    // 古い結果を返してしまう（プロジェクト絞り込みテストと同じ理由）。事前件数はJdbcTemplateで取る。
    int baselineCount = countAnalyzedTasks(USER_ID);

    int otherUsersProject = insertProject(OTHER_USER_ID, "他ユーザーの検証");
    insertFinishedTask(otherUsersProject, null, LocalDateTime.of(2026, 1, 1, 0, 0), 100, 5.0);

    AnalyticsSummaryRow actual =
        sut.findSummary(USER_ID, new AnalyticsQueryCondition(), THRESHOLD);

    assertThat(actual.getAnalyzedCount()).isEqualTo(baselineCount);
  }

  @Test
  void findScatterPoints_完了日時降順で上限プラス1件まで取得できること() {
    int projectId = insertProject(USER_ID, "散布図検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    for (int i = 0; i < 5; i++) {
      insertFinishedTaskWithEstimate(projectId, base.plusDays(i), 60, 90, 50.0);
    }

    List<AnalyticsScatterPointRow> actual =
        sut.findScatterPoints(USER_ID, conditionFor(projectId), THRESHOLD, 3);

    // limit=3に対しlimit+1=4件までしか返らない（5件挿入しても4件で頭打ち）。
    assertThat(actual).hasSize(4);
    assertThat(actual.get(0).getOutcome()).isEqualTo("LATE");
    assertThat(actual.get(0).getGapRate()).isEqualTo(50.0);
  }

  @Test
  void findScatterPoints_上限以下の場合はすべて取得できること() {
    int projectId = insertProject(USER_ID, "散布図上限未満検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    insertFinishedTaskWithEstimate(projectId, base, 60, 60, 0.0);
    insertFinishedTaskWithEstimate(projectId, base.plusDays(1), 60, 30, -50.0);

    List<AnalyticsScatterPointRow> actual =
        sut.findScatterPoints(USER_ID, conditionFor(projectId), THRESHOLD, 500);

    assertThat(actual).hasSize(2);
    assertThat(actual)
        .extracting(AnalyticsScatterPointRow::getOutcome)
        .containsExactly("EARLY", "ON_TIME"); // finished_at DESCなので新しい順
  }

  @Test
  void findSizeBuckets_境界値ちょうどが下側の帯に分類され代表係数が手計算した期待値と一致すること() {
    int projectId = insertProject(USER_ID, "サイズ帯境界値検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    // M15: 15分ちょうど。M30: 16分と30分。境界のみ確認できればよいため各1〜2件で十分。
    insertFinishedTaskWithEstimate(projectId, base, 15, 100, 0.0);
    insertFinishedTaskWithEstimate(projectId, base.plusDays(1), 16, 100, 0.0);
    insertFinishedTaskWithEstimate(projectId, base.plusDays(2), 30, 100, 20.0);
    insertFinishedTaskWithEstimate(projectId, base.plusDays(3), 121, 100, 0.0);

    List<AnalyticsSizeBucketRow> actual =
        sut.findSizeBuckets(USER_ID, conditionFor(projectId), THRESHOLD);

    assertThat(actual)
        .extracting(AnalyticsSizeBucketRow::getBucketCode, AnalyticsSizeBucketRow::getTaskCount)
        .containsExactlyInAnyOrder(
            tuple("M15", 1), tuple("M30", 2), tuple("OVER120", 1));
    AnalyticsSizeBucketRow m30 = actual.stream()
        .filter(row -> row.getBucketCode().equals("M30"))
        .findFirst()
        .orElseThrow();
    // factor: 1.0（gapRate=0）と1.2（gapRate=20）の中央値 = 1.1
    assertThat(m30.getFactorMedian()).isCloseTo(1.1, within(1e-9));
    assertThat(m30.getOnTimeCount()).isEqualTo(1); // gapRate=20は超過、0のみオンタイム
  }

  @Test
  void findSizeBuckets_該当0件の帯はレスポンスに現れないこと() {
    int projectId = insertProject(USER_ID, "サイズ帯0件検証");
    insertFinishedTaskWithEstimate(
        projectId, LocalDateTime.of(2026, 1, 1, 0, 0), 15, 100, 0.0);

    List<AnalyticsSizeBucketRow> actual =
        sut.findSizeBuckets(USER_ID, conditionFor(projectId), THRESHOLD);

    assertThat(actual).extracting(AnalyticsSizeBucketRow::getBucketCode).containsExactly("M15");
  }

  @Test
  void タイムライン_完了日時の降順で振り返り未入力のタスクを含まずに返ること() {
    int projectId = insertProject(USER_ID, "タイムライン順序検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    int task1 = insertFinishedTaskForTimeline(projectId, base, 100, 5.0);
    int task2 = insertFinishedTaskForTimeline(projectId, base.plusDays(1), 100, 5.0);
    insertFinishedTaskForTimeline(projectId, base.plusDays(2), 100, 5.0); // 振り返り未入力
    insertReflection(task1, "原因1", null);
    insertReflection(task2, "原因2", null);

    List<ReflectionTimelineRow> actual =
        sut.findReflectionTimelineItems(USER_ID, timelineConditionFor(projectId), THRESHOLD);

    assertThat(actual)
        .extracting(ReflectionTimelineRow::getTaskId)
        .containsExactly(task2, task1);
  }

  @Test
  void タイムライン_実績未記録タスクも含まれること() {
    int projectId = insertProject(USER_ID, "タイムライン実績未記録検証");
    int taskId = insertFinishedTaskForTimeline(
        projectId, LocalDateTime.of(2026, 1, 1, 0, 0), 0, -100.0);
    insertReflection(taskId, "実績を記録し忘れた", null);

    List<ReflectionTimelineRow> actual =
        sut.findReflectionTimelineItems(USER_ID, timelineConditionFor(projectId), THRESHOLD);

    assertThat(actual).extracting(ReflectionTimelineRow::getTaskId).containsExactly(taskId);
  }

  @Test
  void タイムライン_outcomeALLでは誤差率算出不可の行もoutcomeNullで含まれること() {
    int projectId = insertProject(USER_ID, "タイムラインoutcomeNull検証");
    int taskId = insertFinishedTaskForTimeline(
        projectId, LocalDateTime.of(2026, 1, 1, 0, 0), 100, null);
    insertReflection(taskId, "誤差率が算出できない", null);

    ReflectionTimelineQueryCondition condition = timelineConditionFor(projectId);
    condition.setOutcome(ReflectionOutcomeFilter.ALL);
    List<ReflectionTimelineRow> actual =
        sut.findReflectionTimelineItems(USER_ID, condition, THRESHOLD);

    assertThat(actual).hasSize(1);
    assertThat(actual.get(0).getOutcome()).isNull();
  }

  @Test
  void タイムライン_outcome指定時は誤差率算出不可の行が除外されること() {
    int projectId = insertProject(USER_ID, "タイムラインoutcome絞り込み検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    int nullGapTask = insertFinishedTaskForTimeline(projectId, base, 100, null);
    int lateTask = insertFinishedTaskForTimeline(projectId, base.plusDays(1), 100, 50.0);
    insertReflection(nullGapTask, "算出不可", null);
    insertReflection(lateTask, "超過", null);

    ReflectionTimelineQueryCondition condition = timelineConditionFor(projectId);
    condition.setOutcome(ReflectionOutcomeFilter.LATE);
    List<ReflectionTimelineRow> actual =
        sut.findReflectionTimelineItems(USER_ID, condition, THRESHOLD);

    assertThat(actual).extracting(ReflectionTimelineRow::getTaskId).containsExactly(lateTask);
  }

  @Test
  void タイムライン_causeがNULLの振り返りも返ること() {
    int projectId = insertProject(USER_ID, "タイムラインcauseNULL検証");
    int taskId = insertFinishedTaskForTimeline(
        projectId, LocalDateTime.of(2026, 1, 1, 0, 0), 100, 5.0);
    insertReflection(taskId, null, "次のアクションだけ書いた");

    List<ReflectionTimelineRow> actual =
        sut.findReflectionTimelineItems(USER_ID, timelineConditionFor(projectId), THRESHOLD);

    assertThat(actual.get(0).getCause()).isNull();
    assertThat(actual.get(0).getNextAction()).isEqualTo("次のアクションだけ書いた");
  }

  @Test
  void タイムライン_ページングが正しく総件数とhasNext相当の情報が整合すること() {
    int projectId = insertProject(USER_ID, "タイムラインページング検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    for (int i = 0; i < 5; i++) {
      int taskId = insertFinishedTaskForTimeline(projectId, base.plusDays(i), 100, 5.0);
      insertReflection(taskId, "原因" + i, null);
    }

    ReflectionTimelineQueryCondition page0 = timelineConditionFor(projectId);
    page0.setSize(2);
    page0.setPage(0);
    ReflectionTimelineQueryCondition page1 = timelineConditionFor(projectId);
    page1.setSize(2);
    page1.setPage(1);
    ReflectionTimelineQueryCondition page2 = timelineConditionFor(projectId);
    page2.setSize(2);
    page2.setPage(2);

    assertThat(sut.findReflectionTimelineItems(USER_ID, page0, THRESHOLD)).hasSize(2);
    assertThat(sut.findReflectionTimelineItems(USER_ID, page1, THRESHOLD)).hasSize(2);
    assertThat(sut.findReflectionTimelineItems(USER_ID, page2, THRESHOLD)).hasSize(1);
    assertThat(sut.countReflectionTimeline(USER_ID, page0, THRESHOLD)).isEqualTo(5);
  }

  @Test
  void タイムライン_causeCategory絞り込みが指定カテゴリを含む振り返りのみ返すこと() {
    int projectId = insertProject(USER_ID, "タイムラインカテゴリ絞り込み検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    int matching = insertFinishedTaskForTimeline(projectId, base, 100, 5.0);
    int other = insertFinishedTaskForTimeline(projectId, base.plusDays(1), 100, 5.0);
    int matchingReflectionId = insertReflection(matching, "一致する", null);
    insertReflection(other, "一致しない", null);
    linkCategory(matchingReflectionId, "TASK_BREAKDOWN");

    ReflectionTimelineQueryCondition condition = timelineConditionFor(projectId);
    condition.setCauseCategory("TASK_BREAKDOWN");
    List<ReflectionTimelineRow> actual =
        sut.findReflectionTimelineItems(USER_ID, condition, THRESHOLD);

    assertThat(actual).extracting(ReflectionTimelineRow::getTaskId).containsExactly(matching);
  }

  @Test
  void タイムライン_複数カテゴリを持つ振り返りがcauseCategory絞り込みでも1件のみ返ること() {
    int projectId = insertProject(USER_ID, "タイムライン複数カテゴリ検証");
    int taskId = insertFinishedTaskForTimeline(
        projectId, LocalDateTime.of(2026, 1, 1, 0, 0), 100, 5.0);
    int reflectionId = insertReflection(taskId, "複数カテゴリ", null);
    linkCategory(reflectionId, "TASK_BREAKDOWN");
    linkCategory(reflectionId, "OTHER");

    ReflectionTimelineQueryCondition condition = timelineConditionFor(projectId);
    condition.setCauseCategory("OTHER");
    List<ReflectionTimelineRow> actual =
        sut.findReflectionTimelineItems(USER_ID, condition, THRESHOLD);

    assertThat(actual).hasSize(1);
  }

  @Test
  void タイムラインカテゴリ取得_ページに含まれる振り返りの原因カテゴリのみ表示順で返ること() {
    int projectId = insertProject(USER_ID, "タイムラインカテゴリ取得検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    int task1 = insertFinishedTaskForTimeline(projectId, base, 100, 5.0);
    int task2 = insertFinishedTaskForTimeline(projectId, base.plusDays(1), 100, 5.0);
    int task3 = insertFinishedTaskForTimeline(projectId, base.plusDays(2), 100, 5.0);
    int reflection1 = insertReflection(task1, "1件目", null);
    int reflection2 = insertReflection(task2, "2件目", null);
    insertReflection(task3, "3件目", null);
    linkCategory(reflection1, "OTHER");
    linkCategory(reflection1, "TASK_BREAKDOWN");
    linkCategory(reflection2, "FATIGUE");

    // 完了日時降順のため task3, task2 が1ページ目（size=2）に入り、task1は2ページ目になる。
    ReflectionTimelineQueryCondition page0 = timelineConditionFor(projectId);
    page0.setSize(2);
    page0.setPage(0);

    List<ReflectionCauseCategoryLinkRow> actual =
        sut.findReflectionTimelineCategories(USER_ID, page0, THRESHOLD);

    assertThat(actual)
        .extracting(
            ReflectionCauseCategoryLinkRow::getReflectionId,
            ReflectionCauseCategoryLinkRow::getCauseCategoryCode)
        .containsExactly(tuple(reflection2, "FATIGUE"));
  }

  @Test
  void タイムラインカテゴリ取得_2ページ目には1ページ目のカテゴリが含まれないこと() {
    int projectId = insertProject(USER_ID, "タイムラインカテゴリページ境界検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    int task1 = insertFinishedTaskForTimeline(projectId, base, 100, 5.0);
    int task2 = insertFinishedTaskForTimeline(projectId, base.plusDays(1), 100, 5.0);
    int reflection1 = insertReflection(task1, "1件目", null);
    int reflection2 = insertReflection(task2, "2件目", null);
    linkCategory(reflection1, "OTHER");
    linkCategory(reflection2, "TASK_BREAKDOWN");

    ReflectionTimelineQueryCondition page1 = timelineConditionFor(projectId);
    page1.setSize(1);
    page1.setPage(1); // 完了日時降順の2件目 = task1

    List<ReflectionCauseCategoryLinkRow> actual =
        sut.findReflectionTimelineCategories(USER_ID, page1, THRESHOLD);

    assertThat(actual)
        .extracting(ReflectionCauseCategoryLinkRow::getReflectionId)
        .containsOnly(reflection1);
  }

  private static AnalyticsQueryCondition conditionFor(int projectId) {
    AnalyticsQueryCondition condition = new AnalyticsQueryCondition();
    condition.setProjectId(projectId);
    return condition;
  }

  private static ReflectionTimelineQueryCondition timelineConditionFor(int projectId) {
    ReflectionTimelineQueryCondition condition = new ReflectionTimelineQueryCondition();
    condition.setProjectId(projectId);
    return condition;
  }

  private int insertFinishedTaskForTimeline(
      int projectId, LocalDateTime finishedAt, int actualMinutes, Double gapRate) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement("""
          INSERT INTO tasks(
            project_id, title, estimated_minutes, created_at,
            finished_at, actual_minutes_cached, gap_minutes_cached, gap_rate_cached)
          VALUES (?, 'フィクスチャ', 60, NOW(), ?, ?, 0, ?)
          """, Statement.RETURN_GENERATED_KEYS);
      ps.setInt(1, projectId);
      ps.setObject(2, finishedAt);
      ps.setInt(3, actualMinutes);
      if (gapRate != null) {
        ps.setDouble(4, gapRate);
      } else {
        ps.setNull(4, Types.DOUBLE);
      }
      return ps;
    }, keyHolder);
    return keyHolder.getKey().intValue();
  }

  private int insertReflection(int taskId, String cause, String nextAction) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement(
          "INSERT INTO reflections(task_id, cause, next_action, created_at, updated_at) "
              + "VALUES (?, ?, ?, NOW(), NOW())",
          Statement.RETURN_GENERATED_KEYS);
      ps.setInt(1, taskId);
      if (cause != null) {
        ps.setString(2, cause);
      } else {
        ps.setNull(2, Types.VARCHAR);
      }
      if (nextAction != null) {
        ps.setString(3, nextAction);
      } else {
        ps.setNull(3, Types.VARCHAR);
      }
      return ps;
    }, keyHolder);
    return keyHolder.getKey().intValue();
  }

  private void linkCategory(int reflectionId, String categoryCode) {
    int categoryId = jdbcTemplate.queryForObject(
        "SELECT id FROM reflection_cause_categories WHERE code = ?", Integer.class, categoryCode);
    jdbcTemplate.update(
        "INSERT INTO reflection_cause_category_links(reflection_id, cause_category_id) "
            + "VALUES (?, ?)",
        reflectionId, categoryId);
  }

  private int countAnalyzedTasks(int userId) {
    return jdbcTemplate.queryForObject("""
        SELECT COUNT(*) FROM tasks t
        LEFT JOIN task_groups tg ON tg.id = t.task_group_id
        JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
        WHERE p.user_id = ?
          AND t.finished_at IS NOT NULL
          AND t.gap_rate_cached IS NOT NULL
          AND t.actual_minutes_cached IS NOT NULL
          AND t.actual_minutes_cached <> 0
        """, Integer.class, userId);
  }

  private int insertProject(int userId, String title) {
    jdbcTemplate.update(
        "INSERT INTO projects(user_id, title, is_finished) VALUES (?, ?, false)", userId, title);
    return jdbcTemplate.queryForObject(
        "SELECT id FROM projects WHERE user_id = ? AND title = ?", Integer.class, userId, title);
  }

  private void insertFinishedTask(
      Integer projectId, Integer taskGroupId, LocalDateTime finishedAt, Integer actualMinutes,
      Double gapRate) {
    jdbcTemplate.update("""
        INSERT INTO tasks(
          project_id, task_group_id, title, estimated_minutes, created_at,
          finished_at, actual_minutes_cached, gap_minutes_cached, gap_rate_cached)
        VALUES (?, ?, 'フィクスチャ', 60, NOW(), ?, ?, 0, ?)
        """, projectId, taskGroupId, finishedAt, actualMinutes, gapRate);
  }

  private void insertFinishedTaskWithEstimate(
      int projectId, LocalDateTime finishedAt, int estimatedMinutes, int actualMinutes,
      double gapRate) {
    jdbcTemplate.update("""
        INSERT INTO tasks(
          project_id, title, estimated_minutes, created_at,
          finished_at, actual_minutes_cached, gap_minutes_cached, gap_rate_cached)
        VALUES (?, 'フィクスチャ', ?, NOW(), ?, ?, 0, ?)
        """, projectId, estimatedMinutes, finishedAt, actualMinutes, gapRate);
  }
}
