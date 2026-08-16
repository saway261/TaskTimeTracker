package com.kiborisaway.tasktimetracker.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmailSendRateLimiterTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-16T00:00:00Z"), ZoneOffset.UTC);

  private EmailSendRateLimiter sut;

  @BeforeEach
  void setUp() {
    sut = new EmailSendRateLimiter(
        CLOCK, new TokenGenerator(),
        2, Duration.ofHours(1),
        2, Duration.ofHours(1),
        2, Duration.ofHours(1),
        2, Duration.ofHours(1),
        2, Duration.ofHours(1));
  }

  @Test
  void ユーザー登録_上限到達で対象IPだけブロックすること() {
    sut.recordRegistrationAttempt("192.0.2.1");
    sut.recordRegistrationAttempt("192.0.2.1");

    assertThat(sut.isRegistrationBlocked("192.0.2.1")).isTrue();
    assertThat(sut.isRegistrationBlocked("192.0.2.2")).isFalse();
  }

  @Test
  void リセット要求_メールハッシュの上限だけ到達してもブロックすること() {
    sut.recordPasswordResetRequestAttempt("192.0.2.1", "user@example.com");
    sut.recordPasswordResetRequestAttempt("192.0.2.2", "user@example.com");

    assertThat(sut.isPasswordResetRequestBlocked("192.0.2.3", "user@example.com")).isTrue();
    assertThat(sut.isPasswordResetRequestBlocked("192.0.2.3", "other@example.com")).isFalse();
  }

  @Test
  void リセット要求_IPの上限だけ到達してもブロックすること() {
    sut.recordPasswordResetRequestAttempt("192.0.2.1", "user-a@example.com");
    sut.recordPasswordResetRequestAttempt("192.0.2.1", "user-b@example.com");

    assertThat(sut.isPasswordResetRequestBlocked("192.0.2.1", "user-c@example.com")).isTrue();
    assertThat(sut.isPasswordResetRequestBlocked("192.0.2.9", "user-c@example.com")).isFalse();
  }

  @Test
  void 確認メール再送_上限到達で対象ユーザーだけブロックすること() {
    sut.recordEmailVerificationResendAttempt(1);
    sut.recordEmailVerificationResendAttempt(1);

    assertThat(sut.isEmailVerificationResendBlocked(1)).isTrue();
    assertThat(sut.isEmailVerificationResendBlocked(2)).isFalse();
  }

  @Test
  void メールアドレス変更要求_上限到達で対象ユーザーだけブロックすること() {
    sut.recordEmailChangeRequestAttempt(1);
    sut.recordEmailChangeRequestAttempt(1);

    assertThat(sut.isEmailChangeRequestBlocked(1)).isTrue();
    assertThat(sut.isEmailChangeRequestBlocked(2)).isFalse();
  }

  @Test
  void 独立性_登録と再送は別々のカウンタであること() {
    sut.recordRegistrationAttempt("192.0.2.1");
    sut.recordRegistrationAttempt("192.0.2.1");

    assertThat(sut.isRegistrationBlocked("192.0.2.1")).isTrue();
    assertThat(sut.isEmailVerificationResendBlocked(1)).isFalse();
  }
}
