package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.data.entity.EmailVerificationToken;
import com.kiborisaway.tasktimetracker.event.EmailVerificationRequestedEvent;
import com.kiborisaway.tasktimetracker.exception.EmailVerificationInvalidException;
import com.kiborisaway.tasktimetracker.repository.EmailVerificationTokenRepository;
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

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-15T00:00:00Z"), ZoneOffset.UTC);
  private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

  @Mock
  private UserRepository userRepository;
  @Mock
  private EmailVerificationTokenRepository tokenRepository;
  @Mock
  private TokenGenerator tokenGenerator;
  @Mock
  private ApplicationEventPublisher eventPublisher;
  @Mock
  private SessionInvalidationService sessionInvalidationService;

  private EmailVerificationService sut;

  @BeforeEach
  void setUp() {
    sut = new EmailVerificationService(
        userRepository, tokenRepository, tokenGenerator, eventPublisher,
        sessionInvalidationService, CLOCK, Duration.ofHours(24));
  }

  @Test
  void 登録時発行_トークンを保存してイベントを発行すること() {
    when(tokenGenerator.generateRawToken()).thenReturn("raw-token");
    when(tokenGenerator.hash("raw-token")).thenReturn("hashed-token");

    sut.issueForRegistration(1, "user@example.com");

    ArgumentCaptor<EmailVerificationToken> captor =
        ArgumentCaptor.forClass(EmailVerificationToken.class);
    verify(tokenRepository).insert(captor.capture());
    assertThat(captor.getValue().getUserId()).isEqualTo(1);
    assertThat(captor.getValue().getTokenHash()).isEqualTo("hashed-token");
    assertThat(captor.getValue().getExpiresAt()).isEqualTo(NOW.plusHours(24));
    verify(eventPublisher).publishEvent(
        new EmailVerificationRequestedEvent(1, "user@example.com", "raw-token"));
  }

  @Test
  void 再送成功_未確認ユーザーなら既存トークンを無効化して再発行すること() {
    AppUser user = new AppUser(
        1, "user@example.com", "{bcrypt}hash", true, false, null, NOW, NOW, null, false);
    when(userRepository.findById(1)).thenReturn(user);
    when(tokenGenerator.generateRawToken()).thenReturn("raw-token");
    when(tokenGenerator.hash("raw-token")).thenReturn("hashed-token");

    sut.resend(1);

    verify(tokenRepository).invalidateAllForUser(1, NOW);
    verify(tokenRepository).insert(any());
    verify(eventPublisher).publishEvent(
        new EmailVerificationRequestedEvent(1, "user@example.com", "raw-token"));
  }

  @Test
  void 再送成功_確認済みユーザーなら何もしないこと() {
    AppUser user = new AppUser(
        1, "user@example.com", "{bcrypt}hash", true, false, null, NOW, NOW, NOW, false);
    when(userRepository.findById(1)).thenReturn(user);

    sut.resend(1);

    verify(tokenRepository, never()).invalidateAllForUser(anyInt(), any());
    verify(tokenRepository, never()).insert(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void 確認成功_トークンが有効なら確認状態にしてセッションを失効すること() {
    when(tokenGenerator.hash("raw-token")).thenReturn("hashed-token");
    EmailVerificationToken token =
        new EmailVerificationToken(10, 1, "hashed-token", NOW.plusHours(1), null, NOW);
    when(tokenRepository.findByTokenHashForUpdate("hashed-token")).thenReturn(token);
    AppUser user = new AppUser(
        1, "user@example.com", "{bcrypt}hash", true, false, null, NOW, NOW, null, false);
    when(userRepository.findById(1)).thenReturn(user);

    sut.confirm("raw-token");

    verify(userRepository).updateEmailVerified(1, NOW, NOW);
    verify(tokenRepository).invalidateAllForUser(1, NOW);
    verify(sessionInvalidationService).invalidateAll(1);
  }

  @Test
  void 確認成功_確認済みユーザーのトークン再提示は何もせず成功すること() {
    when(tokenGenerator.hash("raw-token")).thenReturn("hashed-token");
    EmailVerificationToken token =
        new EmailVerificationToken(10, 1, "hashed-token", NOW.plusHours(1), NOW, NOW);
    when(tokenRepository.findByTokenHashForUpdate("hashed-token")).thenReturn(token);
    AppUser user = new AppUser(
        1, "user@example.com", "{bcrypt}hash", true, false, null, NOW, NOW, NOW, false);
    when(userRepository.findById(1)).thenReturn(user);

    sut.confirm("raw-token");

    verify(userRepository, never()).updateEmailVerified(anyInt(), any(), any());
    verify(sessionInvalidationService, never()).invalidateAll(anyInt());
  }

  @Test
  void 確認失敗_トークンが存在しなければ例外を送出すること() {
    when(tokenGenerator.hash("raw-token")).thenReturn("hashed-token");
    when(tokenRepository.findByTokenHashForUpdate("hashed-token")).thenReturn(null);

    assertThatThrownBy(() -> sut.confirm("raw-token"))
        .isInstanceOf(EmailVerificationInvalidException.class);
  }

  @Test
  void 確認失敗_期限切れなら例外を送出すること() {
    when(tokenGenerator.hash("raw-token")).thenReturn("hashed-token");
    EmailVerificationToken token =
        new EmailVerificationToken(10, 1, "hashed-token", NOW.minusMinutes(1), null, NOW);
    when(tokenRepository.findByTokenHashForUpdate("hashed-token")).thenReturn(token);
    AppUser user = new AppUser(
        1, "user@example.com", "{bcrypt}hash", true, false, null, NOW, NOW, null, false);
    when(userRepository.findById(1)).thenReturn(user);

    assertThatThrownBy(() -> sut.confirm("raw-token"))
        .isInstanceOf(EmailVerificationInvalidException.class);
    verify(userRepository, never()).updateEmailVerified(anyInt(), any(), any());
  }
}
