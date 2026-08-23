package com.kiborisaway.tasktimetracker.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.security.rate-limit.login.maximum-failures=2")
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private SessionRepository<? extends Session> sessionRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void CSRF取得_トークンとヘッダー名を返すこと() throws Exception {
    mockMvc.perform(get("/auth/csrf"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"));
  }

  @Test
  void 登録成功_メールを正規化してBCryptハッシュを保存し認証済みになること() throws Exception {
    MvcResult result = mockMvc.perform(post("/auth/register")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":" New-User@Example.com ","password":"register-passphrase"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("new-user@example.com"))
        .andExpect(jsonPath("$.passwordChangeRequired").value(false))
        .andExpect(jsonPath("$.emailVerified").value(false))
        .andReturn();

    AppUser saved = userRepository.findByEmail("new-user@example.com");
    assertThat(saved).isNotNull();
    assertThat(saved.getPasswordHash()).startsWith("{bcrypt}$2a$12$");
    assertThat(saved.getPasswordHash()).doesNotContain("register-passphrase");
    assertThat(passwordEncoder.matches("register-passphrase", saved.getPasswordHash())).isTrue();

    mockMvc.perform(get("/auth/me").cookie(result.getResponse().getCookie("JSESSIONID")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(saved.getId()))
        .andExpect(jsonPath("$.email").value("new-user@example.com"))
        .andExpect(jsonPath("$.emailVerified").value(false));
  }

  @Test
  void 登録成功_確認トークンが発行されること() throws Exception {
    mockMvc.perform(post("/auth/register")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"verification-target@example.com","password":"register-passphrase"}
                """))
        .andExpect(status().isCreated());

    AppUser saved = userRepository.findByEmail("verification-target@example.com");
    Integer tokenCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM email_verification_tokens WHERE user_id = ?",
        Integer.class, saved.getId());
    assertThat(tokenCount).isEqualTo(1);
  }

  @Test
  void 登録失敗_重複メールなら409を返すこと() throws Exception {
    mockMvc.perform(post("/auth/register")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":" USER-A@EXAMPLE.COM ","password":"register-passphrase"}
                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("email is unavailable"));
  }

  @Test
  void 登録失敗_パスワードが72バイトを超えるなら400を返すこと() throws Exception {
    String password = "あ".repeat(25);

    mockMvc.perform(post("/auth/register")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"new-user@example.com\",\"password\":\"" + password + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("payload validation error"));
  }

  @Test
  void ログイン成功_セッションへ認証を保存してmeを取得できること() throws Exception {
    MvcResult csrfResult = mockMvc.perform(get("/auth/csrf"))
        .andExpect(status().isOk())
        .andReturn();
    Cookie anonymousCookie = csrfResult.getResponse().getCookie("JSESSIONID");
    MvcResult login = mockMvc.perform(post("/auth/login")
            .cookie(anonymousCookie)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":" USER-A@EXAMPLE.COM ","password":"TestPasswordA123!"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.email").value("user-a@example.com"))
        .andExpect(jsonPath("$.emailVerified").value(true))
        .andReturn();

    Cookie authenticatedCookie = login.getResponse().getCookie("JSESSIONID");
    assertThat(authenticatedCookie.getValue()).isNotEqualTo(anonymousCookie.getValue());
    String sessionId = new String(
        Base64.getDecoder().decode(authenticatedCookie.getValue()),
        StandardCharsets.UTF_8);
    Session authenticatedSession = sessionRepository.findById(sessionId);
    assertThat(authenticatedSession).isNotNull();
    assertThat(authenticatedSession.getMaxInactiveInterval())
        .isEqualTo(java.time.Duration.ofDays(30));
    Object authenticatedAt = authenticatedSession.getAttribute(
        com.kiborisaway.tasktimetracker.security.AbsoluteSessionTimeoutFilter
            .AUTHENTICATED_AT_SESSION_ATTRIBUTE);
    assertThat(authenticatedAt).isInstanceOf(Instant.class);
    mockMvc.perform(get("/auth/me").cookie(authenticatedCookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void me取得_同一セッション内でDBが更新されると最新値が返ること() throws Exception {
    Cookie authenticatedCookie = loginAsUserA();

    mockMvc.perform(get("/auth/me").cookie(authenticatedCookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emailVerified").value(true));

    // Principal（セッション内キャッシュ）ではなくDBを読み直すことの検証（要件 §5.1）。
    // userRepositoryの更新系メソッドを経由するのは、jdbcTemplateによる素のSQL更新だと
    // 同一トランザクション内でMyBatisのローカルキャッシュ（SqlSession単位）に阻まれ、
    // 次のfindByIdが古い結果を返してしまうため。
    userRepository.updateEmailVerified(1, null, java.time.LocalDateTime.now());

    mockMvc.perform(get("/auth/me").cookie(authenticatedCookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emailVerified").value(false));
  }

  private Cookie loginAsUserA() throws Exception {
    MvcResult csrfResult = mockMvc.perform(get("/auth/csrf"))
        .andExpect(status().isOk())
        .andReturn();
    Cookie anonymousCookie = csrfResult.getResponse().getCookie("JSESSIONID");
    MvcResult login = mockMvc.perform(post("/auth/login")
            .cookie(anonymousCookie)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"user-a@example.com","password":"TestPasswordA123!"}
                """))
        .andExpect(status().isOk())
        .andReturn();
    return login.getResponse().getCookie("JSESSIONID");
  }

  @Test
  void ログイン失敗_誤パスワードなら共通の401本文を返すこと() throws Exception {
    mockMvc.perform(post("/auth/login")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"user-a@example.com","password":"wrong-password"}
                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("email or password is incorrect"))
        .andExpect(jsonPath("$.errors").isEmpty());
  }

  @Test
  void ログイン失敗_未登録メールでも誤パスワードと同じ401本文を返すこと() throws Exception {
    mockMvc.perform(post("/auth/login")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"missing@example.com","password":"wrong-password"}
                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("email or password is incorrect"))
        .andExpect(jsonPath("$.errors").isEmpty());
  }

  @Test
  void ログイン失敗_上限へ到達すると429を返すこと() throws Exception {
    String body = """
        {"email":"rate-limit@example.com","password":"wrong-password"}
        """;
    mockMvc.perform(post("/auth/login")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(post("/auth/login")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.message").value("too many failed attempts"));
  }

  @Test
  void セッションCookie_永続期間と安全な属性を設定すること() throws Exception {
    mockMvc.perform(get("/auth/csrf"))
        .andExpect(status().isOk())
        .andExpect(cookie().httpOnly("JSESSIONID", true))
        .andExpect(cookie().path("JSESSIONID", "/api"))
        .andExpect(cookie().maxAge("JSESSIONID", 30 * 24 * 60 * 60))
        .andExpect(header().string(HttpHeaders.SET_COOKIE,
            org.hamcrest.Matchers.containsString("SameSite=Lax")));
  }

  @Test
  void ログアウト失敗_未認証なら401を返すこと() throws Exception {
    mockMvc.perform(post("/auth/logout").with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("authentication required"));
  }

  @Test
  void ログアウト成功_セッションとCookieを無効化すること() throws Exception {
    MvcResult login = mockMvc.perform(post("/auth/login")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"user-a@example.com","password":"TestPasswordA123!"}
                """))
        .andExpect(status().isOk())
        .andReturn();
    Cookie sessionCookie = login.getResponse().getCookie("JSESSIONID");

    mockMvc.perform(post("/auth/logout").cookie(sessionCookie).with(csrf()))
        .andExpect(status().isNoContent())
        .andExpect(cookie().maxAge("JSESSIONID", 0));
    mockMvc.perform(get("/auth/me").cookie(sessionCookie))
        .andExpect(status().isUnauthorized());
  }
}
