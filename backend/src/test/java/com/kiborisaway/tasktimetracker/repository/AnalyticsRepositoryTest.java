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
  void タグ絞り込み_tagId指定時は指定タグが付与されたタスクのみ集計されタグなしなら未指定で含まれること() {
    int projectId = insertProject(USER_ID, "タグ絞り込み検証");
    int tagA = insertTag(USER_ID, "タグA");
    int tagB = insertTag(USER_ID, "タグB");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    int taskWithA1 = insertFinishedTaskForTimeline(projectId, base, 100, 5.0);
    int taskWithA2 = insertFinishedTaskForTimeline(projectId, base.plusDays(1), 100, -5.0);
    int taskWithB = insertFinishedTaskForTimeline(projectId, base.plusDays(2), 100, 10.0);
    insertFinishedTaskForTimeline(projectId, base.plusDays(3), 100, 0.0); // タグなし
    linkTag(taskWithA1, tagA);
    linkTag(taskWithA2, tagA);
    linkTag(taskWithB, tagB);

    AnalyticsQueryCondition conditionA = conditionFor(projectId);
    conditionA.setTagId(tagA);
    AnalyticsQueryCondition conditionB = conditionFor(projectId);
    conditionB.setTagId(tagB);
    AnalyticsQueryCondition conditionAll = conditionFor(projectId);

    assertThat(sut.findSummary(USER_ID, conditionA, THRESHOLD).getAnalyzedCount()).isEqualTo(2);
    assertThat(sut.findSummary(USER_ID, conditionB, THRESHOLD).getAnalyzedCount()).isEqualTo(1);
    // tagId未指定ならタグなしタスクも含めて4件全て対象になる
    assertThat(sut.findSummary(USER_ID, conditionAll, THRESHOLD).getAnalyzedCount()).isEqualTo(4);
  }

  @Test
  void タグ絞り込み_複数タグが付いたタスクでも二重に数えられないこと() {
    int projectId = insertProject(USER_ID, "タグ重複防止検証");
    int tagA = insertTag(USER_ID, "重複タグA");
    int tagB = insertTag(USER_ID, "重複タグB");
    int taskId = insertFinishedTaskForTimeline(
        projectId, LocalDateTime.of(2026, 1, 1, 0, 0), 100, 5.0);
    linkTag(taskId, tagA);
    linkTag(taskId, tagB);

    AnalyticsQueryCondition condition = conditionFor(projectId);
    condition.setTagId(tagA);

    assertThat(sut.findSummary(USER_ID, condition, THRESHOLD).getAnalyzedCount()).isEqualTo(1);
  }

  @Test
  void AND絞り込み_tagIdとprojectIdと期間を同時指定すると絞り込みがANDで効くこと() {
    int projectA = insertProject(USER_ID, "AND検証A");
    int projectB = insertProject(USER_ID, "AND検証B");
    int tag = insertTag(USER_ID, "AND検証タグ");
    LocalDateTime inRange = LocalDateTime.of(2026, 3, 1, 0, 0);
    LocalDateTime outOfRange = LocalDateTime.of(2026, 6, 1, 0, 0);

    int matchAll = insertFinishedTaskForTimeline(projectA, inRange, 100, 5.0);
    int wrongProject = insertFinishedTaskForTimeline(projectB, inRange, 100, 5.0);
    int wrongPeriod = insertFinishedTaskForTimeline(projectA, outOfRange, 100, 5.0);
    insertFinishedTaskForTimeline(projectA, inRange, 100, 5.0); // タグなし
    linkTag(matchAll, tag);
    linkTag(wrongProject, tag);
    linkTag(wrongPeriod, tag);

    AnalyticsQueryCondition condition = conditionFor(projectA);
    condition.setTagId(tag);
    condition.setFrom(LocalDateTime.of(2026, 2, 1, 0, 0));
    condition.setTo(LocalDateTime.of(2026, 4, 1, 0, 0));

    assertThat(sut.findSummary(USER_ID, condition, THRESHOLD).getAnalyzedCount()).isEqualTo(1);
  }

  @Test
  void findExcludedCounts_tagId指定時も指定タグのタスクのみ集計されること() {
    int projectId = insertProject(USER_ID, "除外件数タグ絞り込み検証");
    int tag = insertTag(USER_ID, "除外検証タグ");
    int taggedMissingActual = insertFinishedTaskForTimeline(
        projectId, LocalDateTime.of(2026, 1, 1, 0, 0), 0, -100.0);
    insertFinishedTaskForTimeline(
        projectId, LocalDateTime.of(2026, 1, 2, 0, 0), 0, -100.0); // タグなし
    linkTag(taggedMissingActual, tag);

    AnalyticsQueryCondition condition = conditionFor(projectId);
    condition.setTagId(tag);

    ExcludedCountRow actual = sut.findExcludedCounts(USER_ID, condition);

    assertThat(actual.getFinishedCount()).isEqualTo(1);
    assertThat(actual.getMissingActualMinutes()).isEqualTo(1);
  }

  @Test
  void findProjectBreakdown_プロジェクト別件数が件数降順プロジェクト名昇順で返り合計が分析対象件数と一致すること() {
    int projectA = insertProject(USER_ID, "分布検証A");
    int projectB = insertProject(USER_ID, "分布検証B");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    insertFinishedTaskForTimeline(projectA, base, 100, 5.0);
    insertFinishedTaskForTimeline(projectA, base.plusDays(1), 100, 5.0);
    insertFinishedTaskForTimeline(projectA, base.plusDays(2), 100, 5.0);
    insertFinishedTaskForTimeline(projectB, base, 100, 5.0);

    // 自分の他フィクスチャの影響を受けないよう、両プロジェクトに絞り込んだ集計と個別集計を突き合わせる
    AnalyticsQueryCondition conditionA = conditionFor(projectA);
    AnalyticsQueryCondition conditionB = conditionFor(projectB);
    int countA = sut.findSummary(USER_ID, conditionA, THRESHOLD).getAnalyzedCount();
    int countB = sut.findSummary(USER_ID, conditionB, THRESHOLD).getAnalyzedCount();

    List<ProjectBreakdownRow> actual = sut.findProjectBreakdown(USER_ID, new AnalyticsQueryCondition());

    ProjectBreakdownRow rowA = actual.stream()
        .filter(row -> row.getProjectId() == projectA).findFirst().orElseThrow();
    ProjectBreakdownRow rowB = actual.stream()
        .filter(row -> row.getProjectId() == projectB).findFirst().orElseThrow();
    assertThat(rowA.getCount()).isEqualTo(countA);
    assertThat(rowA.getProjectTitle()).isEqualTo("分布検証A");
    assertThat(rowB.getCount()).isEqualTo(countB);
    // 分布の合計は分析対象件数の総数と一致する（タスクは必ず1プロジェクトに属するため）
    int totalBreakdown = actual.stream().mapToInt(ProjectBreakdownRow::getCount).sum();
    int totalAnalyzed = sut.findSummary(USER_ID, new AnalyticsQueryCondition(), THRESHOLD)
        .getAnalyzedCount();
    assertThat(totalBreakdown).isEqualTo(totalAnalyzed);
  }

  @Test
  void findProjectBreakdown_同数の場合はプロジェクト名の昇順で並ぶこと() {
    int projectZ = insertProject(USER_ID, "並び順検証Z");
    int projectA = insertProject(USER_ID, "並び順検証A");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    insertFinishedTaskForTimeline(projectZ, base, 100, 5.0);
    insertFinishedTaskForTimeline(projectA, base, 100, 5.0);

    AnalyticsQueryCondition condition = new AnalyticsQueryCondition();
    List<ProjectBreakdownRow> actual = sut.findProjectBreakdown(USER_ID, condition);

    // count=1のプロジェクトが複数あってもZ・Aの相対順序はプロジェクト名昇順になる
    // （streamのfilterはDBが返した順序を保持する）。
    List<ProjectBreakdownRow> ordered = actual.stream()
        .filter(row -> row.getProjectId() == projectZ || row.getProjectId() == projectA)
        .toList();
    assertThat(ordered).extracting(ProjectBreakdownRow::getProjectTitle)
        .containsExactly("並び順検証A", "並び順検証Z");
  }

  @Test
  void findProjectBreakdown_tagId指定時は指定タグのタスクのみ集計されること() {
    int projectA = insertProject(USER_ID, "分布タグ検証A");
    int projectB = insertProject(USER_ID, "分布タグ検証B");
    int tag = insertTag(USER_ID, "分布タグ");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    int taggedInA = insertFinishedTaskForTimeline(projectA, base, 100, 5.0);
    insertFinishedTaskForTimeline(projectA, base.plusDays(1), 100, 5.0); // タグなし
    int taggedInB = insertFinishedTaskForTimeline(projectB, base, 100, 5.0);
    linkTag(taggedInA, tag);
    linkTag(taggedInB, tag);

    AnalyticsQueryCondition condition = new AnalyticsQueryCondition();
    condition.setTagId(tag);
    List<ProjectBreakdownRow> actual = sut.findProjectBreakdown(USER_ID, condition);

    List<ProjectBreakdownRow> relevant = actual.stream()
        .filter(row -> row.getProjectId() == projectA || row.getProjectId() == projectB)
        .toList();
    assertThat(relevant).extracting(ProjectBreakdownRow::getCount).containsOnly(1);
    assertThat(relevant).hasSize(2);
  }

  @Test
  void findProjectBreakdown_分析対象0件のときは空配列でエラーにならないこと() {
    int projectId = insertProject(USER_ID, "分布0件検証");
    int tag = insertTag(USER_ID, "誰も付与していないタグ");

    AnalyticsQueryCondition condition = conditionFor(projectId);
    condition.setTagId(tag);

    List<ProjectBreakdownRow> actual = sut.findProjectBreakdown(USER_ID, condition);

    assertThat(actual).isEmpty();
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
  void findScatterPoints_tagId指定時は指定タグのタスクのみ返ること() {
    int projectId = insertProject(USER_ID, "散布図タグ絞り込み検証");
    int tag = insertTag(USER_ID, "散布図タグ");
    int tagged = insertFinishedTaskForTimeline(
        projectId, LocalDateTime.of(2026, 1, 1, 0, 0), 90, 50.0);
    insertFinishedTaskForTimeline(
        projectId, LocalDateTime.of(2026, 1, 2, 0, 0), 90, 50.0); // タグなし
    linkTag(tagged, tag);

    AnalyticsQueryCondition condition = conditionFor(projectId);
    condition.setTagId(tag);

    List<AnalyticsScatterPointRow> actual = sut.findScatterPoints(USER_ID, condition, THRESHOLD, 500);

    assertThat(actual).extracting(AnalyticsScatterPointRow::getTaskId).containsExactly(tagged);
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
  void findSizeBuckets_tagId指定時は指定タグのタスクのみ集計されること() {
    int projectId = insertProject(USER_ID, "サイズ帯タグ絞り込み検証");
    int tag = insertTag(USER_ID, "サイズ帯タグ");
    // insertFinishedTaskForTimelineのestimated_minutesは固定60分 → M60帯
    int tagged = insertFinishedTaskForTimeline(
        projectId, LocalDateTime.of(2026, 1, 1, 0, 0), 60, 0.0);
    insertFinishedTaskForTimeline(
        projectId, LocalDateTime.of(2026, 1, 2, 0, 0), 60, 0.0); // タグなし
    linkTag(tagged, tag);

    AnalyticsQueryCondition condition = conditionFor(projectId);
    condition.setTagId(tag);

    List<AnalyticsSizeBucketRow> actual = sut.findSizeBuckets(USER_ID, condition, THRESHOLD);

    assertThat(actual).hasSize(1);
    assertThat(actual.get(0).getBucketCode()).isEqualTo("M60");
    assertThat(actual.get(0).getTaskCount()).isEqualTo(1);
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
  void タイムライン_tagId指定時は指定タグの振り返りのみ一覧件数カテゴリのすべてで一致すること() {
    int projectId = insertProject(USER_ID, "タイムラインタグ絞り込み検証");
    int tag = insertTag(USER_ID, "タイムラインタグ");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    int tagged = insertFinishedTaskForTimeline(projectId, base, 100, 5.0);
    int untagged = insertFinishedTaskForTimeline(projectId, base.plusDays(1), 100, 5.0);
    int taggedReflectionId = insertReflection(tagged, "タグあり", null);
    insertReflection(untagged, "タグなし", null);
    linkCategory(taggedReflectionId, "TASK_BREAKDOWN");
    linkTag(tagged, tag);

    ReflectionTimelineQueryCondition condition = timelineConditionFor(projectId);
    condition.setTagId(tag);

    List<ReflectionTimelineRow> items =
        sut.findReflectionTimelineItems(USER_ID, condition, THRESHOLD);
    int totalCount = sut.countReflectionTimeline(USER_ID, condition, THRESHOLD);
    List<ReflectionCauseCategoryLinkRow> categories =
        sut.findReflectionTimelineCategories(USER_ID, condition, THRESHOLD);

    // findReflectionTimelineItems・countReflectionTimeline・findReflectionTimelineCategoriesの
    // 3クエリすべてにタグ述語が正しく入っていることを1テストで確認する（10本目の漏れがないことの確認）。
    assertThat(items).extracting(ReflectionTimelineRow::getTaskId).containsExactly(tagged);
    assertThat(totalCount).isEqualTo(1);
    assertThat(categories)
        .extracting(ReflectionCauseCategoryLinkRow::getCauseCategoryCode)
        .containsExactly("TASK_BREAKDOWN");
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
    linkCategory(reflection2, "CONDITION");

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
        .containsExactly(tuple(reflection2, "CONDITION"));
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

  @Test
  void タイムラインタグ取得_ページに含まれるタスクのタグのみ名前昇順で返ること() {
    int projectId = insertProject(USER_ID, "タイムラインタグ取得検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    int taskWithTags = insertFinishedTaskForTimeline(projectId, base, 100, 5.0);
    int taskWithoutTags = insertFinishedTaskForTimeline(projectId, base.minusDays(1), 100, 5.0);
    insertReflection(taskWithTags, "タグあり", null);
    insertReflection(taskWithoutTags, "タグなし", null);
    int tagZ = insertTag(USER_ID, "ゼータタグ");
    int tagA = insertTag(USER_ID, "アルファタグ");
    linkTag(taskWithTags, tagZ);
    linkTag(taskWithTags, tagA);

    ReflectionTimelineQueryCondition condition = timelineConditionFor(projectId);

    List<TaskTagRow> actual = sut.findReflectionTimelineTags(USER_ID, condition, THRESHOLD);

    assertThat(actual)
        .extracting(TaskTagRow::getTaskId, TaskTagRow::getTagId)
        .containsExactly(tuple(taskWithTags, tagA), tuple(taskWithTags, tagZ));
  }

  @Test
  void タイムラインタグ取得_アーカイブ済みタグも含めて返すこと() {
    int projectId = insertProject(USER_ID, "タイムラインアーカイブ済みタグ検証");
    int taskId = insertFinishedTaskForTimeline(
        projectId, LocalDateTime.of(2026, 1, 1, 0, 0), 100, 5.0);
    insertReflection(taskId, "アーカイブ済みタグ付き", null);
    int archivedTag = insertTag(USER_ID, "旧タグ");
    linkTag(taskId, archivedTag);
    jdbcTemplate.update("UPDATE tags SET is_archived = TRUE WHERE id = ?", archivedTag);

    List<TaskTagRow> actual = sut.findReflectionTimelineTags(
        USER_ID, timelineConditionFor(projectId), THRESHOLD);

    assertThat(actual).extracting(TaskTagRow::getTagId).containsExactly(archivedTag);
  }

  @Test
  void タイムラインタグ取得_2ページ目には1ページ目のタグが含まれないこと() {
    int projectId = insertProject(USER_ID, "タイムラインタグページ境界検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    int task1 = insertFinishedTaskForTimeline(projectId, base, 100, 5.0);
    int task2 = insertFinishedTaskForTimeline(projectId, base.plusDays(1), 100, 5.0);
    insertReflection(task1, "1件目", null);
    insertReflection(task2, "2件目", null);
    int tag1 = insertTag(USER_ID, "タグ1件目");
    int tag2 = insertTag(USER_ID, "タグ2件目");
    linkTag(task1, tag1);
    linkTag(task2, tag2);

    ReflectionTimelineQueryCondition page1 = timelineConditionFor(projectId);
    page1.setSize(1);
    page1.setPage(1); // 完了日時降順の2件目 = task1

    List<TaskTagRow> actual = sut.findReflectionTimelineTags(USER_ID, page1, THRESHOLD);

    assertThat(actual).extracting(TaskTagRow::getTagId).containsOnly(tag1);
  }

  @Test
  void findAccuracyTrend_窓幅ごとの移動中央値が手計算した期待値と一致すること() {
    int projectId = insertProject(USER_ID, "精度推移検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    double[] gapRates = {10, -10, 20, -20, 30};
    for (int i = 0; i < gapRates.length; i++) {
      insertFinishedTask(projectId, null, base.plusDays(i), 100, gapRates[i]);
    }

    List<AnalyticsTrendRow> actual = sut.findAccuracyTrend(USER_ID, conditionFor(projectId), 3);

    // 窓幅3のため、rn=1,2は窓が埋まらず現れず、rn=3,4,5の3点のみ返る。
    assertThat(actual).extracting(AnalyticsTrendRow::getSequence).containsExactly(3, 4, 5);

    AnalyticsTrendRow first = actual.get(0); // rn=3: rn1-3 → factor 1.10,0.90,1.20 / ape 10,10,20
    assertThat(first.getFactorMedian()).isCloseTo(1.10, within(1e-9));
    assertThat(first.getVariancePercent()).isCloseTo(10.0, within(1e-9));
    assertThat(first.getFinishedAt()).isEqualTo(base.plusDays(2));
    assertThat(first.getWindowFrom()).isEqualTo(base);

    AnalyticsTrendRow second = actual.get(1); // rn=4: rn2-4 → factor 0.90,1.20,0.80 / ape 10,20,20
    assertThat(second.getFactorMedian()).isCloseTo(0.90, within(1e-9));
    assertThat(second.getVariancePercent()).isCloseTo(20.0, within(1e-9));
    assertThat(second.getFinishedAt()).isEqualTo(base.plusDays(3));
    assertThat(second.getWindowFrom()).isEqualTo(base.plusDays(1));

    AnalyticsTrendRow third = actual.get(2); // rn=5: rn3-5 → factor 1.20,0.80,1.30 / ape 20,20,30
    assertThat(third.getFactorMedian()).isCloseTo(1.20, within(1e-9));
    assertThat(third.getVariancePercent()).isCloseTo(20.0, within(1e-9));
    assertThat(third.getFinishedAt()).isEqualTo(base.plusDays(4));
    assertThat(third.getWindowFrom()).isEqualTo(base.plusDays(2));
  }

  @Test
  void findAccuracyTrend_窓が埋まらない件数の場合は空で返ること() {
    int projectId = insertProject(USER_ID, "精度推移件数不足検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    insertFinishedTask(projectId, null, base, 100, 10.0);
    insertFinishedTask(projectId, null, base.plusDays(1), 100, -10.0);

    List<AnalyticsTrendRow> actual = sut.findAccuracyTrend(USER_ID, conditionFor(projectId), 3);

    assertThat(actual).isEmpty();
  }

  @Test
  void findAccuracyTrend_tagId指定時は指定タグのタスクのみが移動窓の対象になること() {
    int projectId = insertProject(USER_ID, "推移タグ絞り込み検証");
    int tag = insertTag(USER_ID, "推移タグ");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    // タグなしタスクを10件（絞り込みなしなら窓幅10が埋まる件数）
    for (int i = 0; i < 10; i++) {
      insertFinishedTaskForTimeline(projectId, base.plusDays(i), 100, 5.0);
    }
    // タグ付きタスクは3件のみ（絞り込むと窓幅10が埋まらない）
    for (int i = 10; i < 13; i++) {
      int taskId = insertFinishedTaskForTimeline(projectId, base.plusDays(i), 100, 5.0);
      linkTag(taskId, tag);
    }

    AnalyticsQueryCondition unfiltered = conditionFor(projectId);
    AnalyticsQueryCondition tagged = conditionFor(projectId);
    tagged.setTagId(tag);

    assertThat(sut.findAccuracyTrend(USER_ID, unfiltered, 10)).isNotEmpty();
    assertThat(sut.findAccuracyTrend(USER_ID, tagged, 10)).isEmpty();
  }

  @Test
  void findGapCauses_タスク判定区分とカテゴリごとに集計されカテゴリ方向は判定に影響しないこと() {
    int projectId = insertProject(USER_ID, "原因カテゴリ集計検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    int taskA = insertFinishedTaskForTimeline(projectId, base, 100, 30.0);
    int taskB = insertFinishedTaskForTimeline(projectId, base.plusDays(1), 100, 50.0);
    int taskC = insertFinishedTaskForTimeline(projectId, base.plusDays(2), 100, 10.0);
    int taskD = insertFinishedTaskForTimeline(projectId, base.plusDays(3), 100, -40.0);
    int taskE = insertFinishedTaskForTimeline(projectId, base.plusDays(4), 100, 5.0);
    int taskF = insertFinishedTaskForTimeline(projectId, base.plusDays(5), 100, -60.0);
    linkCategory(insertReflection(taskA, "原因A", null), "TASK_BREAKDOWN");
    linkCategory(insertReflection(taskB, "原因B", null), "TASK_BREAKDOWN");
    linkCategory(insertReflection(taskC, "原因C", null), "TASK_BREAKDOWN");
    linkCategory(insertReflection(taskD, "原因D", null), "UNCLEAR_GOAL");
    insertReflection(taskE, "原因E", null); // カテゴリなし＝未分類
    int reflectionF = insertReflection(taskF, "原因F", null);
    linkCategory(reflectionF, "TASK_BREAKDOWN"); // カテゴリ方向はOVERだが、タスク判定はEARLY
    linkCategory(reflectionF, "OTHER");

    List<GapCauseRow> actual = sut.findGapCauses(USER_ID, conditionFor(projectId), 10.0);

    GapCauseRow lateTaskBreakdown = rowFor(actual, "LATE", "TASK_BREAKDOWN");
    assertThat(lateTaskBreakdown.getTaskCount()).isEqualTo(2); // A, B
    assertThat(lateTaskBreakdown.getGapRateMedian()).isCloseTo(40.0, within(1e-9));

    GapCauseRow onTimeTaskBreakdown = rowFor(actual, "ON_TIME", "TASK_BREAKDOWN");
    assertThat(onTimeTaskBreakdown.getTaskCount()).isEqualTo(1); // C（境界値+10%）
    assertThat(onTimeTaskBreakdown.getGapRateMedian()).isCloseTo(10.0, within(1e-9));

    GapCauseRow earlyTaskBreakdown = rowFor(actual, "EARLY", "TASK_BREAKDOWN");
    assertThat(earlyTaskBreakdown.getTaskCount()).isEqualTo(1); // F
    assertThat(earlyTaskBreakdown.getGapRateMedian()).isCloseTo(-60.0, within(1e-9));

    GapCauseRow earlyUnclearGoal = rowFor(actual, "EARLY", "UNCLEAR_GOAL");
    assertThat(earlyUnclearGoal.getTaskCount()).isEqualTo(1); // D
    assertThat(earlyUnclearGoal.getGapRateMedian()).isCloseTo(-40.0, within(1e-9));

    GapCauseRow earlyOther = rowFor(actual, "EARLY", "OTHER");
    assertThat(earlyOther.getTaskCount()).isEqualTo(1); // F
    assertThat(earlyOther.getGapRateMedian()).isCloseTo(-60.0, within(1e-9));

    GapCauseRow unclassified = actual.stream()
        .filter(row -> "ON_TIME".equals(row.getOutcome()) && row.getCauseCategoryCode() == null)
        .findFirst()
        .orElseThrow();
    assertThat(unclassified.getTaskCount()).isEqualTo(1); // E
    assertThat(unclassified.getGapRateMedian()).isCloseTo(5.0, within(1e-9));

    // 延べ件数の合計（4+1+1+1=7）は分析対象タスク数（6件）を上回りうる（Fが2カテゴリに重複計上されるため）。
    int totalLinkCount = actual.stream().mapToInt(GapCauseRow::getTaskCount).sum();
    assertThat(totalLinkCount).isEqualTo(7);
  }

  @Test
  void findGapCauses_振り返り未入力のタスクは集計に含まれないこと() {
    int projectId = insertProject(USER_ID, "原因カテゴリ振り返り未入力検証");
    insertFinishedTaskForTimeline(projectId, LocalDateTime.of(2026, 1, 1, 0, 0), 100, 10.0);

    List<GapCauseRow> actual = sut.findGapCauses(USER_ID, conditionFor(projectId), 10.0);

    assertThat(actual).isEmpty();
  }

  @Test
  void findGapCauses_tagId指定時は指定タグの振り返りのみ集計されること() {
    int projectId = insertProject(USER_ID, "原因カテゴリタグ絞り込み検証");
    int tag = insertTag(USER_ID, "原因カテゴリタグ");
    int tagged = insertFinishedTaskForTimeline(
        projectId, LocalDateTime.of(2026, 1, 1, 0, 0), 100, 5.0);
    int untagged = insertFinishedTaskForTimeline(
        projectId, LocalDateTime.of(2026, 1, 2, 0, 0), 100, 5.0);
    insertReflection(tagged, "タグあり", null);
    insertReflection(untagged, "タグなし", null);
    linkTag(tagged, tag);

    AnalyticsQueryCondition condition = conditionFor(projectId);
    condition.setTagId(tag);

    List<GapCauseRow> actual = sut.findGapCauses(USER_ID, condition, 10.0);

    int totalTaskCount = actual.stream().mapToInt(GapCauseRow::getTaskCount).sum();
    assertThat(totalTaskCount).isEqualTo(1);
  }

  private static GapCauseRow rowFor(
      List<GapCauseRow> rows, String outcome, String causeCategoryCode) {
    return rows.stream()
        .filter(row -> outcome.equals(row.getOutcome())
            && causeCategoryCode.equals(row.getCauseCategoryCode()))
        .findFirst()
        .orElseThrow();
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

  private int insertTag(int userId, String name) {
    jdbcTemplate.update(
        "INSERT INTO tags(user_id, name, name_normalized, is_archived, created_at) "
            + "VALUES (?, ?, ?, FALSE, NOW())",
        userId, name, name.toLowerCase());
    return jdbcTemplate.queryForObject(
        "SELECT id FROM tags WHERE user_id = ? AND name = ?", Integer.class, userId, name);
  }

  private void linkTag(int taskId, int tagId) {
    jdbcTemplate.update("INSERT INTO task_tags(task_id, tag_id) VALUES (?, ?)", taskId, tagId);
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
