package com.kiborisaway.tasktimetracker.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * ユーザー登録のレート制限（送信元IP、仕様書14章）だけを検証する専用クラスです。
 *
 * <p>他のレート制限テストと同居させると、それらのテストが準備のために呼ぶ{@code register()}が
 * 同じIPベースのカウンタを消費してしまい不安定になるため、独立したSpringコンテキストで実施します。
 */
@SpringBootTest(properties = "app.security.rate-limit.registration.maximum-attempts=2")
@AutoConfigureMockMvc
class RegistrationRateLimitIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void 登録_上限到達で429を返すこと() throws Exception {
    register("rate-limit-register-1@example.com");
    register("rate-limit-register-2@example.com");

    mockMvc.perform(post("/auth/register")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"email\":\"rate-limit-register-3@example.com\",\"password\":\""
                    + "register-passphrase\"}"))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.message").value("too many requests"));
  }

  private void register(String email) throws Exception {
    mockMvc.perform(post("/auth/register")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"register-passphrase\"}"))
        .andExpect(status().isCreated());
  }
}
