package com.kiborisaway.tasktimetracker.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.security.AuthenticatedUser;
import com.kiborisaway.tasktimetracker.support.AuthenticatedUserTestFactory;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
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
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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

  @Test
  void タイムライン取得成功_完了日時降順で返り発行クエリ数が3であること() throws Exception {
    int projectId = insertProject(1, "タイムライン基本検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    int task1 = insertFinishedTaskForTimeline(projectId, base, 100, 5.0);
    int task2 = insertFinishedTaskForTimeline(projectId, base.plusDays(1), 100, 5.0);
    insertReflection(task1, "1件目", null);
    insertReflection(task2, "2件目", null);
    queryCounter.reset();

    mockMvc.perform(MockMvcRequestBuilders.get("/analytics/reflections")
            .with(authenticatedUser(1, "user-a@example.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalCount").value(2))
        .andExpect(jsonPath("$.items[0].taskId").value(task2))
        .andExpect(jsonPath("$.items[1].taskId").value(task1))
        .andExpect(jsonPath("$.hasNext").value(false));

    // projectId・causeCategoryとも未指定なので所有権確認・カテゴリ検証のクエリは発生せず3本のまま。
    assertThat(queryCounter.getCount()).isEqualTo(3);
  }

  @Test
  void タイムライン取得成功_projectId指定時は発行クエリ数が4であること() throws Exception {
    int projectId = insertProject(1, "タイムラインprojectId検証");
    int task = insertFinishedTaskForTimeline(
        projectId, LocalDateTime.of(2026, 1, 1, 0, 0), 100, 5.0);
    insertReflection(task, "原因", null);
    queryCounter.reset();

    mockMvc.perform(MockMvcRequestBuilders.get("/analytics/reflections")
            .param("projectId", String.valueOf(projectId))
            .with(authenticatedUser(1, "user-a@example.com")))
        .andExpect(status().isOk());

    assertThat(queryCounter.getCount()).isEqualTo(4);
  }

  @Test
  void タイムライン取得成功_causeCategory指定時は発行クエリ数が4であること() throws Exception {
    int projectId = insertProject(1, "タイムラインカテゴリ検証");
    int task = insertFinishedTaskForTimeline(
        projectId, LocalDateTime.of(2026, 1, 1, 0, 0), 100, 5.0);
    int reflectionId = insertReflection(task, "原因", null);
    linkCategory(reflectionId, "TASK_BREAKDOWN");
    queryCounter.reset();

    mockMvc.perform(MockMvcRequestBuilders.get("/analytics/reflections")
            .param("causeCategory", "TASK_BREAKDOWN")
            .with(authenticatedUser(1, "user-a@example.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].causeCategories[0].code").value("TASK_BREAKDOWN"));

    // 原因カテゴリの有効性確認クエリが1本加わり、タイムライン集計3本と合わせて4本になる。
    assertThat(queryCounter.getCount()).isEqualTo(4);
  }

  @Test
  void タイムライン取得成功_ページングが正しくカテゴリがページ間で入れ替わらないこと() throws Exception {
    int projectId = insertProject(1, "タイムラインページング検証");
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
    int task1 = insertFinishedTaskForTimeline(projectId, base, 100, 5.0);
    int task2 = insertFinishedTaskForTimeline(projectId, base.plusDays(1), 100, 5.0);
    int task3 = insertFinishedTaskForTimeline(projectId, base.plusDays(2), 100, 5.0);
    int reflection1 = insertReflection(task1, "1件目", null);
    int reflection2 = insertReflection(task2, "2件目", null);
    insertReflection(task3, "3件目", null);
    linkCategory(reflection1, "OTHER");
    linkCategory(reflection2, "FATIGUE");

    // 完了日時降順: task3, task2, task1。size=2のページ0にはtask3・task2が入る。
    mockMvc.perform(MockMvcRequestBuilders.get("/analytics/reflections")
            .param("projectId", String.valueOf(projectId))
            .param("size", "2")
            .param("page", "0")
            .with(authenticatedUser(1, "user-a@example.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[0].taskId").value(task3))
        .andExpect(jsonPath("$.items[0].causeCategories").isEmpty())
        .andExpect(jsonPath("$.items[1].taskId").value(task2))
        .andExpect(jsonPath("$.items[1].causeCategories[0].code").value("FATIGUE"))
        .andExpect(jsonPath("$.hasNext").value(true));

    mockMvc.perform(MockMvcRequestBuilders.get("/analytics/reflections")
            .param("projectId", String.valueOf(projectId))
            .param("size", "2")
            .param("page", "1")
            .with(authenticatedUser(1, "user-a@example.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].taskId").value(task1))
        .andExpect(jsonPath("$.items[0].causeCategories[0].code").value("OTHER"))
        .andExpect(jsonPath("$.hasNext").value(false));
  }

  @Test
  void タイムライン取得成功_実績未記録タスクとcauseNULLの振り返りも含まれること() throws Exception {
    int projectId = insertProject(1, "タイムライン実績未記録検証");
    int task = insertFinishedTaskForTimeline(
        projectId, LocalDateTime.of(2026, 1, 1, 0, 0), 0, -100.0);
    insertReflection(task, null, "次のアクションだけ書いた");

    mockMvc.perform(MockMvcRequestBuilders.get("/analytics/reflections")
            .param("projectId", String.valueOf(projectId))
            .with(authenticatedUser(1, "user-a@example.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].actualMinutes").value(0))
        .andExpect(jsonPath("$.items[0].cause").isEmpty())
        .andExpect(jsonPath("$.items[0].nextAction").value("次のアクションだけ書いた"));
  }

  @Test
  void タイムライン取得失敗_未知の原因カテゴリで400を返すこと() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/analytics/reflections")
            .param("causeCategory", "UNKNOWN_CODE")
            .with(authenticatedUser(1, "user-a@example.com")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void タイムライン取得失敗_存在しないプロジェクトIDで404を返すこと() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/analytics/reflections")
            .param("projectId", "999999")
            .with(authenticatedUser(1, "user-a@example.com")))
        .andExpect(status().isNotFound());
  }

  @Test
  void タイムライン他ユーザー分離_他ユーザーの振り返りが含まれないこと() throws Exception {
    int otherUsersProject = insertProject(2, "タイムライン他ユーザー検証");
    int task = insertFinishedTaskForTimeline(
        otherUsersProject, LocalDateTime.of(2026, 1, 1, 0, 0), 100, 5.0);
    insertReflection(task, "他ユーザーの振り返り", null);

    mockMvc.perform(MockMvcRequestBuilders.get("/analytics/reflections")
            .with(authenticatedUser(1, "user-a@example.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[*].taskId").value(not(hasItem(task))));
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
