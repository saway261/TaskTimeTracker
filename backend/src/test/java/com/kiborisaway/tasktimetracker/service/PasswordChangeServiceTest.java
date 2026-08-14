package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.data.dto.auth.PasswordChangeRequest;
import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.exception.PasswordChangeNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.PasswordPolicyViolationException;
import com.kiborisaway.tasktimetracker.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordChangeServiceTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC);

  @Mock
  private UserRepository userRepository;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private PasswordPolicy passwordPolicy;
  @Mock
  private SessionInvalidationService sessionInvalidationService;

  private PasswordChangeService sut;
  private AppUser user;

  @BeforeEach
  void setUp() {
    sut = new PasswordChangeService(
        userRepository, passwordEncoder, passwordPolicy, sessionInvalidationService, CLOCK);
    user = new AppUser(
        1, "user@example.com", "{bcrypt}current", true, false, null,
        LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK));
    when(userRepository.findById(1)).thenReturn(user);
  }

  @Test
  void 変更成功_ハッシュを更新して全セッションを失効すること() {
    PasswordChangeRequest request =
        new PasswordChangeRequest("current-password", "new-password-123");
    when(passwordEncoder.matches("current-password", user.getPasswordHash())).thenReturn(true);
    when(passwordPolicy.isValid("new-password-123", user.getEmail())).thenReturn(true);
    when(passwordEncoder.matches("new-password-123", user.getPasswordHash())).thenReturn(false);
    when(passwordEncoder.encode("new-password-123")).thenReturn("{bcrypt}new");

    sut.changePassword(1, request);

    verify(userRepository).updatePassword(
        1, "{bcrypt}new", LocalDateTime.of(2026, 8, 14, 0, 0));
    verify(sessionInvalidationService).invalidateAll(1);
  }

  @Test
  void 変更失敗_現在パスワードが不一致なら更新しないこと() {
    PasswordChangeRequest request =
        new PasswordChangeRequest("wrong-password", "new-password-123");
    when(passwordEncoder.matches("wrong-password", user.getPasswordHash())).thenReturn(false);

    assertThatThrownBy(() -> sut.changePassword(1, request))
        .isInstanceOf(PasswordChangeNotAllowedException.class)
        .hasMessage("current password is incorrect");
    verify(userRepository, never()).updatePassword(
        org.mockito.ArgumentMatchers.anyInt(),
        org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.any());
    verify(sessionInvalidationService, never()).invalidateAll(1);
  }

  @Test
  void 変更失敗_新パスワードがポリシー違反なら更新しないこと() {
    PasswordChangeRequest request =
        new PasswordChangeRequest("current-password", "invalid-pass");
    when(passwordEncoder.matches("current-password", user.getPasswordHash())).thenReturn(true);
    when(passwordPolicy.isValid("invalid-pass", user.getEmail())).thenReturn(false);

    assertThatThrownBy(() -> sut.changePassword(1, request))
        .isInstanceOf(PasswordPolicyViolationException.class);
    verify(sessionInvalidationService, never()).invalidateAll(1);
  }

  @Test
  void 変更失敗_現在と新パスワードが同じなら更新しないこと() {
    PasswordChangeRequest request =
        new PasswordChangeRequest("current-password", "current-password");
    when(passwordEncoder.matches("current-password", user.getPasswordHash())).thenReturn(true);
    when(passwordPolicy.isValid("current-password", user.getEmail())).thenReturn(true);

    assertThatThrownBy(() -> sut.changePassword(1, request))
        .isInstanceOf(PasswordChangeNotAllowedException.class)
        .hasMessage("new password must be different");
    verify(sessionInvalidationService, never()).invalidateAll(1);
  }
}
