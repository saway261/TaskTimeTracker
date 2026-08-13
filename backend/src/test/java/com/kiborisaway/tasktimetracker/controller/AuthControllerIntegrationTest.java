package com.kiborisaway.tasktimetracker.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

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
        .andReturn();

    AppUser saved = userRepository.findByEmail("new-user@example.com");
    assertThat(saved).isNotNull();
    assertThat(saved.getPasswordHash()).startsWith("{bcrypt}$2a$12$");
    assertThat(saved.getPasswordHash()).doesNotContain("register-passphrase");
    assertThat(passwordEncoder.matches("register-passphrase", saved.getPasswordHash())).isTrue();

    mockMvc.perform(get("/auth/me").session((MockHttpSession) result.getRequest().getSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(saved.getId()))
        .andExpect(jsonPath("$.email").value("new-user@example.com"));
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
    MockHttpSession session = new MockHttpSession();
    String sessionIdBeforeLogin = session.getId();
    MvcResult login = mockMvc.perform(post("/auth/login")
            .session(session)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":" USER-A@EXAMPLE.COM ","password":"TestPasswordA123!"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.email").value("user-a@example.com"))
        .andReturn();

    session = (MockHttpSession) login.getRequest().getSession();
    assertThat(session.getId()).isNotEqualTo(sessionIdBeforeLogin);
    mockMvc.perform(get("/auth/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
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
    MockHttpSession session = (MockHttpSession) login.getRequest().getSession();

    mockMvc.perform(post("/auth/logout").session(session).with(csrf()))
        .andExpect(status().isNoContent())
        .andExpect(cookie().maxAge("JSESSIONID", 0));

    assertThat(session.isInvalid()).isTrue();
  }
}
