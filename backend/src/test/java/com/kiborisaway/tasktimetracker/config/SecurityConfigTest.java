package com.kiborisaway.tasktimetracker.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private PasswordEncoder passwordEncoder;

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
        .andExpect(status().isNotFound());
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
}
