package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.data.entity.PasswordResetToken;
import com.kiborisaway.tasktimetracker.event.PasswordResetRequestedEvent;
import com.kiborisaway.tasktimetracker.exception.PasswordPolicyViolationException;
import com.kiborisaway.tasktimetracker.exception.PasswordResetInvalidException;
import com.kiborisaway.tasktimetracker.repository.EmailChangeRequestRepository;
import com.kiborisaway.tasktimetracker.repository.EmailVerificationTokenRepository;
import com.kiborisaway.tasktimetracker.repository.PasswordResetTokenRepository;
import com.kiborisaway.tasktimetracker.repository.UserRepository;
import com.kiborisaway.tasktimetracker.security.TokenGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-15T00:00:00Z"), ZoneOffset.UTC);
  private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

  @Mock
  private UserRepository userRepository;
  @Mock
  private PasswordResetTokenRepository passwordResetTokenRepository;
  @Mock
  private EmailVerificationTokenRepository emailVerificationTokenRepository;
  @Mock
  private EmailChangeRequestRepository emailChangeRequestRepository;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private PasswordPolicy passwordPolicy;
  @Mock
  private TokenGenerator tokenGenerator;
  @Mock
  private ApplicationEventPublisher eventPublisher;
  @Mock
  private SessionInvalidationService sessionInvalidationService;

  private PasswordResetService sut;

  @BeforeEach
  void setUp() {
    sut = new PasswordResetService(
        userRepository, passwordResetTokenRepository, emailVerificationTokenRepository,
        emailChangeRequestRepository, passwordEncoder, passwordPolicy, tokenGenerator,
        eventPublisher, sessionInvalidationService, CLOCK, Duration.ofMinutes(30));
  }

  @Test
  void 要求成功_登録済みメールならトークンを発行してイベントを発行すること() {
    AppUser user = new AppUser(
        1, "user@example.com", "{bcrypt}hash", true, false, null, NOW, NOW, NOW);
    when(userRepository.findByEmail("user@example.com")).thenReturn(user);
    when(tokenGenerator.generateRawToken()).thenReturn("raw-token");
    when(tokenGenerator.hash("raw-token")).thenReturn("hashed-token");

    sut.requestReset("user@example.com");

    ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
    verify(passwordResetTokenRepository).invalidateAllForUser(1, NOW);
    verify(passwordResetTokenRepository).insert(captor.capture());
    assertThatCaptorMatches(captor.getValue());
    verify(eventPublisher).publishEvent(
        new PasswordResetRequestedEvent(1, "user@example.com", "raw-token"));
  }

  private void assertThatCaptorMatches(PasswordResetToken token) {
    assertThat(token.getUserId()).isEqualTo(1);
    assertThat(token.getTokenHash()).isEqualTo("hashed-token");
    assertThat(token.getExpiresAt()).isEqualTo(NOW.plusMinutes(30));
  }

  @Test
  void 要求成功_未登録メールなら何もせず正常終了すること() {
    when(userRepository.findByEmail("missing@example.com")).thenReturn(null);

    sut.requestReset("missing@example.com");

    verify(passwordResetTokenRepository, never()).invalidateAllForUser(anyInt(), any());
    verify(passwordResetTokenRepository, never()).insert(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void 確定成功_未確認ユーザーなら確認状態へ昇格させ確認トークンを無効化すること() {
    when(tokenGenerator.hash("raw-token")).thenReturn("hashed-token");
    PasswordResetToken token =
        new PasswordResetToken(10, 1, "hashed-token", NOW.plusMinutes(10), null, NOW);
    when(passwordResetTokenRepository.findValidForUpdate("hashed-token", NOW)).thenReturn(token);
    AppUser user = new AppUser(
        1, "user@example.com", "{bcrypt}old", true, false, null, NOW, NOW, null);
    when(userRepository.findById(1)).thenReturn(user);
    when(passwordPolicy.isValid("new-password-123", "user@example.com")).thenReturn(true);
    when(passwordEncoder.encode("new-password-123")).thenReturn("{bcrypt}new");

    sut.confirmReset("raw-token", "new-password-123");

    verify(userRepository).updatePassword(1, "{bcrypt}new", NOW);
    verify(userRepository).updateEmailVerified(1, NOW, NOW);
    verify(emailVerificationTokenRepository).invalidateAllForUser(1, NOW);
    verify(passwordResetTokenRepository).invalidateAllForUser(1, NOW);
    verify(emailChangeRequestRepository).invalidateAllForUser(1, NOW);
    verify(sessionInvalidationService).invalidateAll(1);
  }

  @Test
  void 確定成功_確認済みユーザーなら確認関連の更新をしないこと() {
    when(tokenGenerator.hash("raw-token")).thenReturn("hashed-token");
    PasswordResetToken token =
        new PasswordResetToken(10, 1, "hashed-token", NOW.plusMinutes(10), null, NOW);
    when(passwordResetTokenRepository.findValidForUpdate("hashed-token", NOW)).thenReturn(token);
    AppUser user = new AppUser(
        1, "user@example.com", "{bcrypt}old", true, false, null, NOW, NOW, NOW);
    when(userRepository.findById(1)).thenReturn(user);
    when(passwordPolicy.isValid("new-password-123", "user@example.com")).thenReturn(true);
    when(passwordEncoder.encode("new-password-123")).thenReturn("{bcrypt}new");

    sut.confirmReset("raw-token", "new-password-123");

    verify(userRepository, never()).updateEmailVerified(anyInt(), any(), any());
    verify(emailVerificationTokenRepository, never()).invalidateAllForUser(anyInt(), any());
  }

  @Test
  void 確定失敗_トークンが存在しなければ例外を送出すること() {
    when(tokenGenerator.hash("raw-token")).thenReturn("hashed-token");
    when(passwordResetTokenRepository.findValidForUpdate("hashed-token", NOW)).thenReturn(null);

    assertThatThrownBy(() -> sut.confirmReset("raw-token", "new-password-123"))
        .isInstanceOf(PasswordResetInvalidException.class);
    verify(userRepository, never()).updatePassword(anyInt(), any(), any());
  }

  @Test
  void 確定失敗_新パスワードがポリシー違反なら更新しないこと() {
    when(tokenGenerator.hash("raw-token")).thenReturn("hashed-token");
    PasswordResetToken token =
        new PasswordResetToken(10, 1, "hashed-token", NOW.plusMinutes(10), null, NOW);
    when(passwordResetTokenRepository.findValidForUpdate("hashed-token", NOW)).thenReturn(token);
    AppUser user = new AppUser(
        1, "user@example.com", "{bcrypt}old", true, false, null, NOW, NOW, NOW);
    when(userRepository.findById(1)).thenReturn(user);
    when(passwordPolicy.isValid("invalid-pass", "user@example.com")).thenReturn(false);

    assertThatThrownBy(() -> sut.confirmReset("raw-token", "invalid-pass"))
        .isInstanceOf(PasswordPolicyViolationException.class);
    verify(userRepository, never()).updatePassword(anyInt(), any(), any());
    verify(sessionInvalidationService, never()).invalidateAll(anyInt());
  }
}
