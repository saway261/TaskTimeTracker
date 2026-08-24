package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.data.dto.auth.EmailChangeRequestBody;
import com.kiborisaway.tasktimetracker.data.dto.auth.PendingEmailResponse;
import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.data.entity.EmailChangeRequest;
import com.kiborisaway.tasktimetracker.event.EmailChangeConfirmationRequestedEvent;
import com.kiborisaway.tasktimetracker.event.EmailChangeNotificationRequestedEvent;
import com.kiborisaway.tasktimetracker.exception.EmailChangeNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.EmailChangeRequestInvalidException;
import com.kiborisaway.tasktimetracker.exception.EmailUnavailableException;
import com.kiborisaway.tasktimetracker.repository.EmailChangeRequestRepository;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class EmailChangeServiceTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-15T00:00:00Z"), ZoneOffset.UTC);
  private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

  @Mock
  private UserRepository userRepository;
  @Mock
  private EmailChangeRequestRepository requestRepository;
  @Mock
  private EmailVerificationTokenRepository verificationTokenRepository;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private TokenGenerator tokenGenerator;
  @Mock
  private ApplicationEventPublisher eventPublisher;
  @Mock
  private SessionInvalidationService sessionInvalidationService;

  private EmailChangeService sut;

  @BeforeEach
  void setUp() {
    sut = new EmailChangeService(
        userRepository, requestRepository, verificationTokenRepository, passwordEncoder,
        tokenGenerator, eventPublisher, sessionInvalidationService, CLOCK,
        Duration.ofHours(24));
  }

  @Test
  void 要求成功_確認済みユーザーなら確定と通知の両方のイベントを発行すること() {
    AppUser user = new AppUser(
        1, "old@example.com", "{bcrypt}hash", true, false, null, NOW, NOW, NOW, false);
    when(userRepository.findById(1)).thenReturn(user);
    when(passwordEncoder.matches("current-password", "{bcrypt}hash")).thenReturn(true);
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(tokenGenerator.generateRawToken()).thenReturn("raw-token");
    when(tokenGenerator.hash("raw-token")).thenReturn("hashed-token");

    PendingEmailResponse actual = sut.requestChange(
        1, new EmailChangeRequestBody("new@example.com", "current-password"));

    assertThat(actual).isEqualTo(new PendingEmailResponse("new@example.com"));
    verify(requestRepository).invalidateAllForUser(1, NOW);
    verify(requestRepository).insert(any());
    verify(eventPublisher).publishEvent(
        new EmailChangeConfirmationRequestedEvent("new@example.com", "raw-token"));
    verify(eventPublisher).publishEvent(
        new EmailChangeNotificationRequestedEvent("old@example.com", "new@example.com"));
  }

  @Test
  void 要求成功_未確認ユーザーなら通知イベントは発行しないこと() {
    AppUser user = new AppUser(
        1, "old@example.com", "{bcrypt}hash", true, false, null, NOW, NOW, null, false);
    when(userRepository.findById(1)).thenReturn(user);
    when(passwordEncoder.matches("current-password", "{bcrypt}hash")).thenReturn(true);
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(tokenGenerator.generateRawToken()).thenReturn("raw-token");
    when(tokenGenerator.hash("raw-token")).thenReturn("hashed-token");

    sut.requestChange(1, new EmailChangeRequestBody("new@example.com", "current-password"));

    verify(eventPublisher).publishEvent(
        new EmailChangeConfirmationRequestedEvent("new@example.com", "raw-token"));
    verify(eventPublisher, never()).publishEvent(any(EmailChangeNotificationRequestedEvent.class));
  }

  @Test
  void 要求失敗_現在パスワードが不一致なら例外を送出すること() {
    AppUser user = new AppUser(
        1, "old@example.com", "{bcrypt}hash", true, false, null, NOW, NOW, NOW, false);
    when(userRepository.findById(1)).thenReturn(user);
    when(passwordEncoder.matches("wrong-password", "{bcrypt}hash")).thenReturn(false);

    assertThatThrownBy(() -> sut.requestChange(
        1, new EmailChangeRequestBody("new@example.com", "wrong-password")))
        .isInstanceOf(EmailChangeNotAllowedException.class)
        .hasMessage("current password is incorrect");
    verify(requestRepository, never()).insert(any());
  }

  @Test
  void 要求失敗_現在と同じメールアドレスなら例外を送出すること() {
    AppUser user = new AppUser(
        1, "same@example.com", "{bcrypt}hash", true, false, null, NOW, NOW, NOW, false);
    when(userRepository.findById(1)).thenReturn(user);
    when(passwordEncoder.matches("current-password", "{bcrypt}hash")).thenReturn(true);

    assertThatThrownBy(() -> sut.requestChange(
        1, new EmailChangeRequestBody("same@example.com", "current-password")))
        .isInstanceOf(EmailChangeNotAllowedException.class)
        .hasMessage("new email must be different");
    verify(requestRepository, never()).insert(any());
  }

  @Test
  void 要求失敗_他ユーザーが使用中のメールアドレスなら例外を送出すること() {
    AppUser user = new AppUser(
        1, "old@example.com", "{bcrypt}hash", true, false, null, NOW, NOW, NOW, false);
    when(userRepository.findById(1)).thenReturn(user);
    when(passwordEncoder.matches("current-password", "{bcrypt}hash")).thenReturn(true);
    when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

    assertThatThrownBy(() -> sut.requestChange(
        1, new EmailChangeRequestBody("taken@example.com", "current-password")))
        .isInstanceOf(EmailUnavailableException.class);
    verify(requestRepository, never()).insert(any());
  }

  @Test
  void 確定成功_有効な要求ならメールを更新して関連トークンとセッションを失効すること() {
    when(tokenGenerator.hash("raw-token")).thenReturn("hashed-token");
    EmailChangeRequest request = new EmailChangeRequest(
        5, 1, "new@example.com", "hashed-token", NOW.plusHours(1), null, NOW);
    when(requestRepository.findValidForUpdate("hashed-token", NOW)).thenReturn(request);
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

    sut.confirm("raw-token");

    verify(userRepository).updateEmail(1, "new@example.com", NOW, NOW);
    verify(requestRepository).invalidateAllForUser(1, NOW);
    verify(verificationTokenRepository).invalidateAllForUser(1, NOW);
    verify(sessionInvalidationService).invalidateAll(1);
  }

  @Test
  void 確定失敗_要求が存在しなければ例外を送出すること() {
    when(tokenGenerator.hash("raw-token")).thenReturn("hashed-token");
    when(requestRepository.findValidForUpdate("hashed-token", NOW)).thenReturn(null);

    assertThatThrownBy(() -> sut.confirm("raw-token"))
        .isInstanceOf(EmailChangeRequestInvalidException.class);
  }

  @Test
  void 確定失敗_新アドレスが既に使用されていれば要求を消費して例外を送出すること() {
    when(tokenGenerator.hash("raw-token")).thenReturn("hashed-token");
    EmailChangeRequest request = new EmailChangeRequest(
        5, 1, "taken@example.com", "hashed-token", NOW.plusHours(1), null, NOW);
    when(requestRepository.findValidForUpdate("hashed-token", NOW)).thenReturn(request);
    when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

    assertThatThrownBy(() -> sut.confirm("raw-token"))
        .isInstanceOf(EmailUnavailableException.class);
    verify(requestRepository).markUsed(5, NOW);
    verify(userRepository, never()).updateEmail(anyInt(), any(), any(), any());
  }
}
