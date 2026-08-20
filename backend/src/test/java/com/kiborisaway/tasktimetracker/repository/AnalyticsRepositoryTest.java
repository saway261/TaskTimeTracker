package com.kiborisaway.tasktimetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.kiborisaway.tasktimetracker.data.dto.analytics.AnalyticsQueryCondition;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

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

  private static AnalyticsQueryCondition conditionFor(int projectId) {
    AnalyticsQueryCondition condition = new AnalyticsQueryCondition();
    condition.setProjectId(projectId);
    return condition;
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
}
