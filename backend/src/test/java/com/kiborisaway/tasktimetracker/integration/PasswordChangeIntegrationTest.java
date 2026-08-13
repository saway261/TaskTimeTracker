package com.kiborisaway.tasktimetracker.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordChangeIntegrationTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private PasswordEncoder passwordEncoder;

  @Test
  void パスワード変更成功_全セッションを失効し新パスワードだけで再ログインできること() throws Exception {
    String email = "password-change@example.com";
    String currentPassword = "current-passphrase";
    String newPassword = "new-passphrase-123";
    Cookie firstSessionCookie = register(email, currentPassword);
    Cookie secondSessionCookie = login(email, currentPassword);

    mockMvc.perform(put("/auth/password")
            .cookie(firstSessionCookie)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"currentPassword":"current-passphrase","newPassword":"new-passphrase-123"}
                """))
        .andExpect(status().isNoContent())
        .andExpect(header().string("Set-Cookie",
            org.hamcrest.Matchers.allOf(
                org.hamcrest.Matchers.containsString("JSESSIONID="),
                org.hamcrest.Matchers.containsString("Path=/api"),
                org.hamcrest.Matchers.containsString("Max-Age=0"))));

    AppUser updated = userRepository.findByEmail(email);
    assertThat(passwordEncoder.matches(newPassword, updated.getPasswordHash())).isTrue();
    assertThat(updated.getPasswordChangeRequired()).isFalse();
    assertThat(updated.getTemporaryPasswordExpiresAt()).isNull();
    mockMvc.perform(get("/auth/me").cookie(secondSessionCookie))
        .andExpect(status().isUnauthorized());
    loginExpectingStatus(email, currentPassword, 401);
    loginExpectingStatus(email, newPassword, 200);
  }

  @Test
  void 変更必須ユーザー_業務APIを拒否し認証関連APIを許可すること() throws Exception {
    String email = "temporary-password@example.com";
    String originalPassword = "original-passphrase";
    String temporaryPassword = "temporary-passphrase";
    register(email, originalPassword);
    AppUser user = userRepository.findByEmail(email);
    userRepository.updateTemporaryPassword(
        user.getId(),
        passwordEncoder.encode(temporaryPassword),
        LocalDateTime.now().plusHours(72),
        LocalDateTime.now());
    Cookie sessionCookie = login(email, temporaryPassword);

    mockMvc.perform(get("/projects").cookie(sessionCookie))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("password change required"));
    mockMvc.perform(get("/auth/me").cookie(sessionCookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.passwordChangeRequired").value(true));
    mockMvc.perform(post("/auth/register")
            .cookie(sessionCookie)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"another@example.com","password":"another-passphrase"}
                """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("password change required"));
  }

  @Test
  void パスワード変更失敗_現在パスワード不一致なら400を返すこと() throws Exception {
    String email = "wrong-current@example.com";
    Cookie sessionCookie = register(email, "current-passphrase");

    mockMvc.perform(put("/auth/password")
            .cookie(sessionCookie)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"currentPassword":"wrong-passphrase","newPassword":"new-passphrase-123"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("current password is incorrect"));
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
    MvcResult result = loginExpectingStatus(email, password, 200);
    return result.getResponse().getCookie("JSESSIONID");
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
}
