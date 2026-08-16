package com.kiborisaway.tasktimetracker.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class EmailVerificationIntegrationTest {

  private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^&\"\\s]+)");

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private MailDeliveryClient mailDeliveryClient;

  @Test
  void 確認完了_未確認ユーザーの業務API制限が解除されセッションが失効すること() throws Exception {
    String email = "verify-flow@example.com";
    Cookie registrationSession = register(email, "register-passphrase");

    mockMvc.perform(get("/projects").cookie(registrationSession))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("email verification required"));

    String rawToken = capturedRawToken();

    mockMvc.perform(post("/auth/email-verifications")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + rawToken + "\"}"))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/auth/me").cookie(registrationSession))
        .andExpect(status().isUnauthorized());

    Cookie newSession = login(email, "register-passphrase");
    mockMvc.perform(get("/auth/me").cookie(newSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emailVerified").value(true));
    mockMvc.perform(get("/projects").cookie(newSession))
        .andExpect(status().isOk());
  }

  @Test
  void 確認冪等性_確認済みユーザーのトークン再提示も204を返すこと() throws Exception {
    String email = "verify-idempotent@example.com";
    register(email, "register-passphrase");
    String rawToken = capturedRawToken();

    mockMvc.perform(post("/auth/email-verifications")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + rawToken + "\"}"))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/auth/email-verifications")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + rawToken + "\"}"))
        .andExpect(status().isNoContent());
  }

  @Test
  void 確認失敗_不正なトークンは400を返すこと() throws Exception {
    mockMvc.perform(post("/auth/email-verifications")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"invalid-token\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("email verification request is invalid or expired"));
  }

  @Test
  void 再送成功_新しいトークンで確認メールを送信すること() throws Exception {
    String email = "verify-resend@example.com";
    Cookie session = register(email, "register-passphrase");

    mockMvc.perform(post("/auth/email-verifications/resend").cookie(session).with(csrf()))
        .andExpect(status().isNoContent());

    verify(mailDeliveryClient, times(2)).send(any());
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
    MvcResult result = mockMvc.perform(post("/auth/login")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
        .andExpect(status().isOk())
        .andReturn();
    return result.getResponse().getCookie("JSESSIONID");
  }

  private String capturedRawToken() {
    ArgumentCaptor<MailMessage> captor = ArgumentCaptor.forClass(MailMessage.class);
    verify(mailDeliveryClient, atLeastOnce()).send(captor.capture());
    MailMessage message = captor.getAllValues().get(captor.getAllValues().size() - 1);
    Matcher matcher = TOKEN_PATTERN.matcher(message.htmlBody());
    assertThat(matcher.find()).isTrue();
    return matcher.group(1);
  }
}
