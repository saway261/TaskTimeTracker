package com.kiborisaway.tasktimetracker.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.infrastructure.MailDeliveryClient;
import com.kiborisaway.tasktimetracker.infrastructure.MailMessage;
import jakarta.servlet.http.Cookie;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordResetIntegrationTest {

  private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^&\"\\s]+)");

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private MailDeliveryClient mailDeliveryClient;

  @Test
  void リセット完了_旧パスワードでログインできず新パスワードでログインでき全セッションが失効すること()
      throws Exception {
    String email = "reset-flow@example.com";
    String oldPassword = "register-passphrase";
    String newPassword = "new-reset-passphrase";
    Cookie registrationSession = register(email, oldPassword);

    mockMvc.perform(post("/auth/password-reset-requests")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\"}"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.message").value(
            "If the email address is registered, a password reset email will be sent."));

    String rawToken = capturedRawToken();

    mockMvc.perform(post("/auth/password-resets")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"token\":\"" + rawToken + "\",\"newPassword\":\"" + newPassword + "\"}"))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/auth/me").cookie(registrationSession))
        .andExpect(status().isUnauthorized());
    loginExpectingStatus(email, oldPassword, 401);
    Cookie newSession = login(email, newPassword);
    mockMvc.perform(get("/auth/me").cookie(newSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emailVerified").value(true));
  }

  @Test
  void リセット要求_未登録メールでも202を返しメールを送信しないこと() throws Exception {
    mockMvc.perform(post("/auth/password-reset-requests")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"unregistered-reset@example.com\"}"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.message").value(
            "If the email address is registered, a password reset email will be sent."));

    verify(mailDeliveryClient, never()).send(any());
  }

  @Test
  void リセット確定失敗_不正なトークンは400を返すこと() throws Exception {
    mockMvc.perform(post("/auth/password-resets")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"invalid-token\",\"newPassword\":\"whatever-password\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("password reset request is invalid or expired"));
  }

  @Test
  void リセット完了_保留中のメールアドレス変更要求が無効化されること() throws Exception {
    String email = "reset-cancels-change@example.com";
    String password = "register-passphrase";
    Cookie session = register(email, password);

    mockMvc.perform(put("/auth/email")
            .cookie(session)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"newEmail\":\"reset-cancels-change-new@example.com\",\"currentPassword\":\""
                    + password + "\"}"))
        .andExpect(status().isAccepted());
    String emailChangeToken = capturedRawToken();

    mockMvc.perform(post("/auth/password-reset-requests")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\"}"))
        .andExpect(status().isAccepted());
    String resetToken = capturedRawToken();
    mockMvc.perform(post("/auth/password-resets")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"token\":\"" + resetToken + "\",\"newPassword\":\"reset-new-passphrase\"}"))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/auth/email-changes")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + emailChangeToken + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("email change request is invalid or expired"));
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

  private Cookie login(String email, String password) throws Exception {
    return loginExpectingStatus(email, password, 200).getResponse().getCookie("JSESSIONID");
  }

  private MvcResult loginExpectingStatus(String email, String password, int statusCode)
      throws Exception {
    return mockMvc.perform(post("/auth/login")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
        .andExpect(status().is(statusCode))
        .andReturn();
  }

  private String capturedRawToken() {
    ArgumentCaptor<MailMessage> captor = ArgumentCaptor.forClass(MailMessage.class);
    verify(mailDeliveryClient, atLeastOnce()).send(captor.capture());
    for (int i = captor.getAllValues().size() - 1; i >= 0; i--) {
      MailMessage message = captor.getAllValues().get(i);
      Matcher matcher = TOKEN_PATTERN.matcher(message.htmlBody());
      if (matcher.find()) {
        return matcher.group(1);
      }
    }
    throw new AssertionError("no mail with a token URL was captured");
  }
}
