package com.kiborisaway.tasktimetracker.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * BE14で追加したレート制限のうち、ユーザー登録以外の代表的なものを検証します
 * （登録は{@link RegistrationRateLimitIntegrationTest}を参照）。
 *
 * <p>いずれも準備段階でIPベースの登録レート制限（このクラスではテストのデフォルト値のまま高いため
 * 消費しても問題ない）を1回だけ使う程度に留め、対象のレート制限だけを低い値へ上書きしています。
 */
@SpringBootTest(properties = {
    "app.security.rate-limit.password-reset-request-email.maximum-attempts=2",
    "app.security.rate-limit.email-change-confirm.maximum-failures=3",
    "app.security.rate-limit.login.maximum-failures=3"
})
@AutoConfigureMockMvc
class RateLimitIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void パスワードリセット要求_同一メールで上限到達で429を返すこと() throws Exception {
    String email = "rate-limit-reset-request@example.com";
    requestPasswordReset(email);
    requestPasswordReset(email);

    mockMvc.perform(post("/auth/password-reset-requests")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\"}"))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.message").value("too many requests"));
  }

  @Test
  void メールアドレス変更確定_不正トークンの繰り返しで上限到達で429を返すこと() throws Exception {
    confirmEmailChangeExpectingBadRequest();
    confirmEmailChangeExpectingBadRequest();

    mockMvc.perform(post("/auth/email-changes")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"invalid-token\"}"))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.message").value("too many requests"));
  }

  @Test
  void メールアドレス変更要求_現在パスワード誤りの繰り返しでログイン用レート制限により429を返すこと()
      throws Exception {
    String email = "rate-limit-email-change-wrong-password@example.com";
    Cookie session = register(email, "register-passphrase");

    requestEmailChangeWithWrongPasswordExpectingBadRequest(session);
    requestEmailChangeWithWrongPasswordExpectingBadRequest(session);

    mockMvc.perform(put("/auth/email")
            .cookie(session)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"newEmail\":\"other@example.com\",\"currentPassword\":\"wrong-password\"}"))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.message").value("too many requests"));
  }

  private void requestPasswordReset(String email) throws Exception {
    mockMvc.perform(post("/auth/password-reset-requests")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\"}"))
        .andExpect(status().isAccepted());
  }

  private void confirmEmailChangeExpectingBadRequest() throws Exception {
    mockMvc.perform(post("/auth/email-changes")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"invalid-token\"}"))
        .andExpect(status().isBadRequest());
  }

  private void requestEmailChangeWithWrongPasswordExpectingBadRequest(Cookie session)
      throws Exception {
    mockMvc.perform(put("/auth/email")
            .cookie(session)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"newEmail\":\"other@example.com\",\"currentPassword\":\"wrong-password\"}"))
        .andExpect(status().isBadRequest());
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
}
