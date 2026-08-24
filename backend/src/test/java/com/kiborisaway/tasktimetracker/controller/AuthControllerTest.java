package com.kiborisaway.tasktimetracker.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorDetailsBuilder;
import com.kiborisaway.tasktimetracker.security.EmailSendRateLimiter;
import com.kiborisaway.tasktimetracker.security.JsonAuthenticationEntryPoint;
import com.kiborisaway.tasktimetracker.security.LoginRateLimiter;
import com.kiborisaway.tasktimetracker.service.PasswordChangeService;
import com.kiborisaway.tasktimetracker.service.UserService;
import com.kiborisaway.tasktimetracker.support.WebMvcTestSecuritySupportConfig;
import com.kiborisaway.tasktimetracker.support.WithMockAuthenticatedUser;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * {@code /auth/me} がPrincipalではなくDBを読み直す実装（要件 §5.1）の単体テストです。
 *
 * <p>「セッションは有効だがDB上のユーザーが存在しない」状態は、既存の外部キー制約
 * （projects/tags 等が app_users を参照し ON DELETE CASCADE を持たない）により、
 * 実際のDBへ行を挿入・削除する統合テストでは再現できません。{@link UserService} を
 * モックした本テストで検証します。
 */
@WebMvcTest(AuthController.class)
@WithMockAuthenticatedUser(userId = 1)
@Import({WebMvcTestSecuritySupportConfig.class, JsonAuthenticationEntryPoint.class})
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private AuthenticationManager authenticationManager;

  @MockitoBean
  private SecurityContextRepository securityContextRepository;

  @MockitoBean
  private PasswordChangeService passwordChangeService;

  @MockitoBean
  private LoginRateLimiter loginRateLimiter;

  @MockitoBean
  private EmailSendRateLimiter emailSendRateLimiter;

  @MockitoBean
  private ErrorDetailsBuilder errorDetailsBuilder;

  @Test
  void me取得成功_DB上のユーザー情報を返すこと() throws Exception {
    LocalDateTime now = LocalDateTime.of(2026, 8, 23, 12, 0);
    AppUser user = new AppUser(
        1, "user@example.com", "{bcrypt}hash", true, false, null, now, now, now, true);
    when(userService.findById(1)).thenReturn(user);

    mockMvc.perform(MockMvcRequestBuilders.get("/auth/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.email").value("user@example.com"))
        .andExpect(jsonPath("$.emailVerified").value(true))
        .andExpect(jsonPath("$.onboardingCompleted").value(true));
  }

  @Test
  void me取得失敗_セッションのユーザーがDBに存在しなければ401を返すこと() throws Exception {
    when(userService.findById(1)).thenReturn(null);

    mockMvc.perform(MockMvcRequestBuilders.get("/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("authentication required"));
  }
}
