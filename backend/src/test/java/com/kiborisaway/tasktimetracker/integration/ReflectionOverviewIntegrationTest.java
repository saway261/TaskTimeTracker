package com.kiborisaway.tasktimetracker.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.security.AuthenticatedUser;
import com.kiborisaway.tasktimetracker.support.AuthenticatedUserTestFactory;
import com.kiborisaway.tasktimetracker.support.TestPublicIds;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(ReflectionOverviewIntegrationTest.QueryCountConfiguration.class)
class ReflectionOverviewIntegrationTest {

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
  void 一覧取得成功_直下とグループ配下を表示順に返し未完了タスクを除外すること() throws Exception {
    queryCounter.reset();

    mockMvc.perform(MockMvcRequestBuilders.get("/projects/" + TestPublicIds.project(3) + "/reflections")
            .with(authenticatedUser(2, "user-b@example.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.projectId").value(TestPublicIds.project(3)))
        .andExpect(jsonPath("$.projectTitle").value("プロジェクトX"))
        .andExpect(jsonPath("$.tasks.length()").value(2))
        .andExpect(jsonPath("$.tasks[0].id").value(TestPublicIds.task(7)))
        .andExpect(jsonPath("$.tasks[0].reflection").isEmpty())
        .andExpect(jsonPath("$.tasks[1].id").value(TestPublicIds.task(6)))
        .andExpect(jsonPath("$.tasks[1].gapRateCached").value(50.0))
        .andExpect(jsonPath("$.tasks[1].reflection.cause")
            .value("着手前の調査が不足していた"))
        .andExpect(jsonPath("$.tasks[1].reflection.causeCategories").isEmpty())
        .andExpect(jsonPath("$.taskGroups.length()").value(2))
        .andExpect(jsonPath("$.taskGroups[0].id").value(TestPublicIds.taskGroup(5)))
        .andExpect(jsonPath("$.taskGroups[0].tasks.length()").value(1))
        .andExpect(jsonPath("$.taskGroups[0].tasks[0].id").value(TestPublicIds.task(12)))
        .andExpect(jsonPath("$.taskGroups[1].id").value(TestPublicIds.taskGroup(4)))
        .andExpect(jsonPath("$.taskGroups[1].tasks.length()").value(2))
        .andExpect(jsonPath("$.taskGroups[1].tasks[0].id").value(TestPublicIds.task(10)))
        .andExpect(jsonPath("$.taskGroups[1].tasks[0].reflection").isEmpty())
        .andExpect(jsonPath("$.taskGroups[1].tasks[1].id").value(TestPublicIds.task(9)))
        .andExpect(jsonPath("$.taskGroups[1].tasks[1].reflection.nextAction").isEmpty())
        .andExpect(jsonPath("$.taskGroups[1].tasks[1].reflection.causeCategories").isEmpty());

    assertThat(queryCounter.getCount()).isEqualTo(4);
  }

  @Test
  void 一覧取得成功_複数の原因カテゴリを持つ振り返りはcauseCategoriesに表示順で含むこと() throws Exception {
    Integer taskBreakdownId = jdbcTemplate.queryForObject(
        "SELECT id FROM reflection_cause_categories WHERE code = 'TASK_BREAKDOWN'", Integer.class);
    Integer otherId = jdbcTemplate.queryForObject(
        "SELECT id FROM reflection_cause_categories WHERE code = 'OTHER'", Integer.class);
    Integer reflectionId = jdbcTemplate.queryForObject(
        "SELECT id FROM reflections WHERE task_id = 6", Integer.class);
    jdbcTemplate.update(
        "INSERT INTO reflection_cause_category_links(reflection_id, cause_category_id) "
            + "VALUES (?, ?)",
        reflectionId, otherId);
    jdbcTemplate.update(
        "INSERT INTO reflection_cause_category_links(reflection_id, cause_category_id) "
            + "VALUES (?, ?)",
        reflectionId, taskBreakdownId);
    queryCounter.reset();

    mockMvc.perform(MockMvcRequestBuilders.get("/projects/" + TestPublicIds.project(3) + "/reflections")
            .with(authenticatedUser(2, "user-b@example.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tasks[1].id").value(TestPublicIds.task(6)))
        .andExpect(jsonPath("$.tasks[1].reflection.causeCategories.length()").value(2))
        .andExpect(jsonPath("$.tasks[1].reflection.causeCategories[0].code")
            .value("TASK_BREAKDOWN"))
        .andExpect(jsonPath("$.tasks[1].reflection.causeCategories[1].code").value("OTHER"));

    assertThat(queryCounter.getCount()).isEqualTo(4);
  }

  @Test
  void 一覧取得成功_完了タスクを含まないグループを返さないこと() throws Exception {
    queryCounter.reset();

    mockMvc.perform(MockMvcRequestBuilders.get("/projects/" + TestPublicIds.project(1) + "/reflections")
            .with(authenticatedUser(1, "user-a@example.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tasks").isEmpty())
        .andExpect(jsonPath("$.taskGroups.length()").value(1))
        .andExpect(jsonPath("$.taskGroups[0].id").value(TestPublicIds.taskGroup(2)))
        .andExpect(jsonPath("$.taskGroups[0].tasks.length()").value(1))
        .andExpect(jsonPath("$.taskGroups[0].tasks[0].id").value(TestPublicIds.task(3)));

    assertThat(queryCounter.getCount()).isEqualTo(4);
  }

  @Test
  void 一覧取得性能_タスク件数を増やしても発行クエリ数が4件のままであること() throws Exception {
    for (int index = 0; index < 10; index++) {
      jdbcTemplate.update("""
          INSERT INTO tasks(
            project_id, title, description, estimated_minutes, created_at, finished_at,
            actual_minutes_cached, gap_minutes_cached, gap_rate_cached)
          VALUES (3, ?, 'クエリ数確認用', 30, NOW(), NOW(), 30, 0, 0.0)
          """, "追加直下" + index);
      jdbcTemplate.update("""
          INSERT INTO tasks(
            task_group_id, title, description, estimated_minutes, created_at, finished_at,
            actual_minutes_cached, gap_minutes_cached, gap_rate_cached)
          VALUES (4, ?, 'クエリ数確認用', 30, NOW(), NOW(), 30, 0, 0.0)
          """, "追加配下" + index);
    }
    queryCounter.reset();

    mockMvc.perform(MockMvcRequestBuilders.get("/projects/" + TestPublicIds.project(3) + "/reflections")
            .with(authenticatedUser(2, "user-b@example.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tasks.length()").value(12))
        .andExpect(jsonPath("$.taskGroups.length()").value(2))
        .andExpect(jsonPath("$.taskGroups[1].tasks.length()").value(12));

    assertThat(queryCounter.getCount()).isEqualTo(4);
  }

  private static RequestPostProcessor authenticatedUser(int userId, String email) {
    AuthenticatedUser principal = AuthenticatedUserTestFactory.create(userId, email, false);
    return authentication(UsernamePasswordAuthenticationToken.authenticated(
        principal, principal.getPassword(), principal.getAuthorities()));
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
