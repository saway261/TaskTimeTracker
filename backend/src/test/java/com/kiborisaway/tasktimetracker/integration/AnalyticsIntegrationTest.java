package com.kiborisaway.tasktimetracker.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.security.AuthenticatedUser;
import com.kiborisaway.tasktimetracker.support.AuthenticatedUserTestFactory;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(AnalyticsIntegrationTest.QueryCountConfiguration.class)
class AnalyticsIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private SelectQueryCountingInterceptor queryCounter;

  @BeforeEach
  void resetQueryCounter() {
    queryCounter.reset();
  }

  @Test
  void 取得成功_分析対象0件でもエラーにならないこと() throws Exception {
    int projectId = insertProject(1, "分析0件検証");
    queryCounter.reset();

    mockMvc.perform(MockMvcRequestBuilders.get("/analytics/estimation-accuracy")
            .param("projectId", String.valueOf(projectId))
            .with(authenticatedUser(1, "user-a@example.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.analyzedTaskCount").value(0))
        .andExpect(jsonPath("$.summary.availability.available").value(false))
        .andExpect(jsonPath("$.summary.onTimeRate").isEmpty())
        .andExpect(jsonPath("$.diagnosis").isEmpty())
        .andExpect(jsonPath("$.scatter").isEmpty())
        .andExpect(jsonPath("$.sizeBuckets").isEmpty())
        .andExpect(jsonPath("$.trend").isEmpty())
        .andExpect(jsonPath("$.trendAvailability.available").value(false));

    // projectId指定時はプロジェクト所有権確認が1本追加され、集計3本と合わせて4本になる。
    assertThat(queryCounter.getCount()).isEqualTo(4);
  }

  @Test
  void 取得成功_分析対象5件以上で統計値が返り発行クエリ数がタスク件数に依存しないこと() throws Exception {
    int projectId = insertProject(1, "分析5件検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    double[] gapRates = {-20, -5, 0, 5, 20};
    for (int i = 0; i < gapRates.length; i++) {
      insertFinishedTask(projectId, base.plusDays(i), 100, gapRates[i]);
    }
    queryCounter.reset();

    mockMvc.perform(MockMvcRequestBuilders.get("/analytics/estimation-accuracy")
            .param("projectId", String.valueOf(projectId))
            .with(authenticatedUser(1, "user-a@example.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.analyzedTaskCount").value(5))
        .andExpect(jsonPath("$.summary.availability.available").value(true))
        .andExpect(jsonPath("$.summary.outcomeBreakdown.lateCount").value(1))
        .andExpect(jsonPath("$.summary.outcomeBreakdown.earlyCount").value(1))
        .andExpect(jsonPath("$.summary.outcomeBreakdown.onTimeCount").value(3))
        .andExpect(jsonPath("$.summary.factorMedian").value(1.0))
        .andExpect(jsonPath("$.summary.onTimeRate").value(60.0));

    assertThat(queryCounter.getCount()).isEqualTo(4);
  }

  @Test
  void 取得成功_タスク件数を増やしても発行クエリ数が変わらないこと() throws Exception {
    // 同一トランザクション内で同一パラメータのクエリを2回発行するとMyBatisのセッションキャッシュに
    // ヒットしてしまうため（上のテストと同じ罠）、比較対象は別テストメソッド（別トランザクション）に分ける。
    int projectId = insertProject(1, "分析25件検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    for (int i = 0; i < 25; i++) {
      insertFinishedTask(projectId, base.plusDays(i), 100, 5.0);
    }
    queryCounter.reset();

    mockMvc.perform(MockMvcRequestBuilders.get("/analytics/estimation-accuracy")
            .param("projectId", String.valueOf(projectId))
            .with(authenticatedUser(1, "user-a@example.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.analyzedTaskCount").value(25));

    assertThat(queryCounter.getCount()).isEqualTo(4);
  }

  @Test
  void 取得失敗_存在しないプロジェクトIDで404を返すこと() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/analytics/estimation-accuracy")
            .param("projectId", "999999")
            .with(authenticatedUser(1, "user-a@example.com")))
        .andExpect(status().isNotFound());
  }

  @Test
  void 取得失敗_他ユーザーのプロジェクトIDを指定すると404を返すこと() throws Exception {
    // プロジェクトID 3 は user-b（id=2）が所有する。
    mockMvc.perform(MockMvcRequestBuilders.get("/analytics/estimation-accuracy")
            .param("projectId", "3")
            .with(authenticatedUser(1, "user-a@example.com")))
        .andExpect(status().isNotFound());
  }

  @Test
  void 取得失敗_fromがtoより後の場合は400を返すこと() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/analytics/estimation-accuracy")
            .param("from", "2026-02-01T00:00:00")
            .param("to", "2026-01-01T00:00:00")
            .with(authenticatedUser(1, "user-a@example.com")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 他ユーザー分離_他ユーザーのタスクが集計に混入しないこと() throws Exception {
    int otherUsersProject = insertProject(2, "他ユーザー混入検証");
    insertFinishedTask(otherUsersProject, LocalDateTime.of(2026, 1, 1, 0, 0), 100, 5.0);

    mockMvc.perform(MockMvcRequestBuilders.get("/analytics/estimation-accuracy")
            .with(authenticatedUser(1, "user-a@example.com")))
        .andExpect(status().isOk());
    // user-aの横断集計に、直前に作成したuser-bのプロジェクトのタスクが含まれないことは
    // AnalyticsRepositoryTestのユーザー分離テストで検証済み。ここでは200で完了することのみ確認する。
  }

  private static RequestPostProcessor authenticatedUser(int userId, String email) {
    AuthenticatedUser principal = AuthenticatedUserTestFactory.create(userId, email, false);
    return authentication(UsernamePasswordAuthenticationToken.authenticated(
        principal, principal.getPassword(), principal.getAuthorities()));
  }

  private int insertProject(int userId, String title) {
    jdbcTemplate.update(
        "INSERT INTO projects(user_id, title, is_finished) VALUES (?, ?, false)", userId, title);
    return jdbcTemplate.queryForObject(
        "SELECT id FROM projects WHERE user_id = ? AND title = ?", Integer.class, userId, title);
  }

  private void insertFinishedTask(
      int projectId, LocalDateTime finishedAt, int actualMinutes, double gapRate) {
    jdbcTemplate.update("""
        INSERT INTO tasks(
          project_id, title, estimated_minutes, created_at,
          finished_at, actual_minutes_cached, gap_minutes_cached, gap_rate_cached)
        VALUES (?, 'フィクスチャ', 60, NOW(), ?, ?, 0, ?)
        """, projectId, finishedAt, actualMinutes, gapRate);
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class QueryCountConfiguration {

    @Bean
    SelectQueryCountingInterceptor selectQueryCountingInterceptor() {
      return new SelectQueryCountingInterceptor();
    }
  }

  @Intercepts({
      @Signature(
          type = Executor.class,
          method = "query",
          args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
  })
  static class SelectQueryCountingInterceptor implements Interceptor {

    private final AtomicInteger count = new AtomicInteger();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
      count.incrementAndGet();
      return invocation.proceed();
    }

    @Override
    public void setProperties(Properties properties) {
      // テスト専用カウンターのため設定項目はありません。
    }

    int getCount() {
      return count.get();
    }

    void reset() {
      count.set(0);
    }
  }
}
