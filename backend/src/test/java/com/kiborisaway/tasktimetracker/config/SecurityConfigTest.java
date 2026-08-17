package com.kiborisaway.tasktimetracker.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest(properties = {
    "app.cors.allowed-origins=http://localhost:5173,https://frontend.example.com",
    "server.servlet.session.cookie.secure=true"
})
@AutoConfigureMockMvc
class SecurityConfigTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private UserRepository userRepository;

  @Test
  void パスワードエンコード_BCryptプレフィックスとstrength12を使用すること() {
    String encoded = passwordEncoder.encode("123456789012");

    assertThat(encoded).startsWith("{bcrypt}$2a$12$");
    assertThat(passwordEncoder.matches("123456789012", encoded)).isTrue();
  }

  @Test
  void 未認証アクセス_保護APIへ401のJSONを返すこと() throws Exception {
    mockMvc.perform(get("/projects"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value("401 UNAUTHORIZED"))
        .andExpect(jsonPath("$.message").value("authentication required"))
        .andExpect(jsonPath("$.errors").isEmpty());
  }

  @Test
  void CSRF不正_公開POSTにも403のJSONを返すこと() throws Exception {
    mockMvc.perform(post("/auth/login"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value("403 FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("forbidden"))
        .andExpect(jsonPath("$.errors").isEmpty());
  }

  @Test
  void 公開POST_正しいCSRFトークンがあれば認可を通過すること() throws Exception {
    mockMvc.perform(post("/auth/login").with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void CORSプリフライト_許可オリジンへcredential付きヘッダーを返すこと() throws Exception {
    mockMvc.perform(options("/projects")
            .header(HttpHeaders.ORIGIN, "http://localhost:5173")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
            "http://localhost:5173"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
  }

  @Test
  void CORSプリフライト_未許可オリジンを拒否すること() throws Exception {
    mockMvc.perform(options("/projects")
            .header(HttpHeaders.ORIGIN, "https://untrusted.example.com")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void CORSプリフライト_許可するメソッドとヘッダーを限定すること() throws Exception {
    mockMvc.perform(options("/projects")
            .header(HttpHeaders.ORIGIN, "https://frontend.example.com")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                "Content-Type, X-CSRF-TOKEN"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
            "https://frontend.example.com"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
            org.hamcrest.Matchers.containsString("POST")))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
            org.hamcrest.Matchers.allOf(
                org.hamcrest.Matchers.containsString("Content-Type"),
                org.hamcrest.Matchers.containsString("X-CSRF-TOKEN"))));
  }

  @Test
  void セキュリティヘッダー_HTTPS応答へ既定ヘッダーとキャッシュ禁止を付与すること() throws Exception {
    mockMvc.perform(get("/auth/csrf").secure(true))
        .andExpect(status().isOk())
        .andExpect(header().string("Strict-Transport-Security",
            org.hamcrest.Matchers.containsString("max-age=")))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL,
            org.hamcrest.Matchers.containsString("no-store")));
  }

  @Test
  void 未確認ユーザー_業務APIで403のJSONを返すこと() throws Exception {
    Cookie session = register("unverified-user@example.com", "register-passphrase");

    mockMvc.perform(get("/projects").cookie(session))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("email verification required"));
  }

  @Test
  void 両制限同時_パスワード変更を優先すること() throws Exception {
    Cookie session = registerWithTemporaryPassword(
        "both-restricted@example.com", "temporary-passphrase");

    mockMvc.perform(get("/projects").cookie(session))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("password change required"));
  }

  @Test
  void 変更必須ユーザー_確認メール再送URLは許可されること() throws Exception {
    Cookie session = registerWithTemporaryPassword(
        "resend-restricted@example.com", "temporary-passphrase");

    mockMvc.perform(post("/auth/email-verifications/resend").cookie(session).with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  void 変更必須ユーザー_メールアドレス変更要求URLは許可されること() throws Exception {
    Cookie session = registerWithTemporaryPassword(
        "change-restricted@example.com", "temporary-passphrase");

    mockMvc.perform(put("/auth/email")
            .cookie(session)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"newEmail\":\"other@example.com\",\"currentPassword\":\"wrong\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("current password is incorrect"));
  }

  @Test
  void メール確認実行URL_認証なしで到達できること() throws Exception {
    mockMvc.perform(post("/auth/email-verifications")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"invalid\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void メール変更確定URL_認証なしで到達できること() throws Exception {
    mockMvc.perform(post("/auth/email-changes")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"invalid\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 本番Cookie_安全な属性と30日の有効期間を設定しDomainを指定しないこと() throws Exception {
    mockMvc.perform(get("/auth/csrf").secure(true))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.SET_COOKIE,
            org.hamcrest.Matchers.allOf(
                org.hamcrest.Matchers.containsString("JSESSIONID="),
                org.hamcrest.Matchers.containsString("Path=/api"),
                org.hamcrest.Matchers.containsString("Max-Age=2592000"),
                org.hamcrest.Matchers.containsString("Secure"),
                org.hamcrest.Matchers.containsString("HttpOnly"),
                org.hamcrest.Matchers.containsString("SameSite=Lax"),
                org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Domain=")))));
  }

  private Cookie register(String email, String password) throws Exception {
    MvcResult result = mockMvc.perform(post("/auth/register")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
        .andExpect(status().isCreated())
        .andReturn();
    return result.getResponse().getCookie("JSESSIONID");
  }

  private Cookie registerWithTemporaryPassword(String email, String temporaryPassword)
      throws Exception {
    register(email, "original-passphrase");
    AppUser user = userRepository.findByEmail(email);
    userRepository.updateTemporaryPassword(
        user.getId(), passwordEncoder.encode(temporaryPassword),
        LocalDateTime.now().plusHours(72), LocalDateTime.now());

    MvcResult login = mockMvc.perform(post("/auth/login")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + temporaryPassword + "\"}"))
        .andExpect(status().isOk())
        .andReturn();
    return login.getResponse().getCookie("JSESSIONID");
  }
}
