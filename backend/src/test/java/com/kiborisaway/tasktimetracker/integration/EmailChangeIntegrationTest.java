package com.kiborisaway.tasktimetracker.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
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
class EmailChangeIntegrationTest {

  private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^&\"\\s]+)");

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private MailDeliveryClient mailDeliveryClient;

  @Test
  void 変更完了_確認済みユーザーは新旧アドレス両方へメールを送り新アドレスでログインできること()
      throws Exception {
    String oldEmail = "change-old@example.com";
    String newEmail = "change-new@example.com";
    String password = "register-passphrase";
    Cookie registrationSession = register(oldEmail, password);
    String verifyToken = capturedRawToken();
    mockMvc.perform(post("/auth/email-verifications")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + verifyToken + "\"}"))
        .andExpect(status().isNoContent());
    Cookie session = login(oldEmail, password);

    mockMvc.perform(put("/auth/email")
            .cookie(session)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"newEmail\":\"" + newEmail + "\",\"currentPassword\":\"" + password + "\"}"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.pendingEmail").value(newEmail));

    verify(mailDeliveryClient, times(3)).send(any()); // 確認1通 + 変更確定1通 + 変更通知1通
    String confirmToken = capturedRawToken();

    mockMvc.perform(post("/auth/email-changes")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + confirmToken + "\"}"))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/auth/me").cookie(session))
        .andExpect(status().isUnauthorized());
    loginExpectingStatus(oldEmail, password, 401);
    Cookie newSession = login(newEmail, password);
    mockMvc.perform(get("/auth/me").cookie(newSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(newEmail));
  }

  @Test
  void 変更要求失敗_現在パスワードが不一致なら400を返すこと() throws Exception {
    String email = "change-wrong-password@example.com";
    Cookie session = register(email, "register-passphrase");

    mockMvc.perform(put("/auth/email")
            .cookie(session)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"newEmail\":\"other@example.com\",\"currentPassword\":\"wrong-password\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("current password is incorrect"));
  }

  @Test
  void 変更要求失敗_他ユーザーが使用中のアドレスなら409を返すこと() throws Exception {
    String email = "change-conflict@example.com";
    String password = "register-passphrase";
    register(email, password);
    Cookie session = login(email, password);

    mockMvc.perform(put("/auth/email")
            .cookie(session)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"newEmail\":\"user-a@example.com\",\"currentPassword\":\"" + password + "\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("email is unavailable"));
  }

  @Test
  void 変更要求成功_未確認ユーザーでも要求でき制限状態でも許可されること() throws Exception {
    String email = "change-unverified@example.com";
    String password = "register-passphrase";
    Cookie session = register(email, password);

    mockMvc.perform(put("/auth/email")
            .cookie(session)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"newEmail\":\"change-unverified-new@example.com\",\"currentPassword\":\""
                    + password + "\"}"))
        .andExpect(status().isAccepted());
  }

  @Test
  void 変更確定失敗_不正なトークンは400を返すこと() throws Exception {
    mockMvc.perform(post("/auth/email-changes")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"invalid-token\"}"))
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
