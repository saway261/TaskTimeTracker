package com.kiborisaway.tasktimetracker.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TokenConfirmationRateLimiterTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-16T00:00:00Z"), ZoneOffset.UTC);

  private TokenConfirmationRateLimiter sut;

  @BeforeEach
  void setUp() {
    sut = new TokenConfirmationRateLimiter(
        CLOCK,
        2, Duration.ofMinutes(15),
        2, Duration.ofMinutes(15),
        2, Duration.ofMinutes(15));
  }

  @Test
  void パスワードリセット確定_上限到達で対象IPだけブロックすること() {
    sut.recordPasswordResetConfirmFailure("192.0.2.1");
    sut.recordPasswordResetConfirmFailure("192.0.2.1");

    assertThat(sut.isPasswordResetConfirmBlocked("192.0.2.1")).isTrue();
    assertThat(sut.isPasswordResetConfirmBlocked("192.0.2.2")).isFalse();
  }

  @Test
  void パスワードリセット確定_成功でカウントを解除すること() {
    sut.recordPasswordResetConfirmFailure("192.0.2.1");

    sut.resetPasswordResetConfirm("192.0.2.1");

    assertThat(sut.isPasswordResetConfirmBlocked("192.0.2.1")).isFalse();
  }

  @Test
  void メール確認実行_上限到達で対象IPだけブロックすること() {
    sut.recordEmailVerificationConfirmFailure("192.0.2.1");
    sut.recordEmailVerificationConfirmFailure("192.0.2.1");

    assertThat(sut.isEmailVerificationConfirmBlocked("192.0.2.1")).isTrue();
    assertThat(sut.isEmailVerificationConfirmBlocked("192.0.2.2")).isFalse();
  }

  @Test
  void メールアドレス変更確定_上限到達で対象IPだけブロックすること() {
    sut.recordEmailChangeConfirmFailure("192.0.2.1");
    sut.recordEmailChangeConfirmFailure("192.0.2.1");

    assertThat(sut.isEmailChangeConfirmBlocked("192.0.2.1")).isTrue();
    assertThat(sut.isEmailChangeConfirmBlocked("192.0.2.2")).isFalse();
  }

  @Test
  void 独立性_同一IPでも用途ごとに別々のカウンタであること() {
    sut.recordPasswordResetConfirmFailure("192.0.2.1");
    sut.recordPasswordResetConfirmFailure("192.0.2.1");

    assertThat(sut.isPasswordResetConfirmBlocked("192.0.2.1")).isTrue();
    assertThat(sut.isEmailVerificationConfirmBlocked("192.0.2.1")).isFalse();
    assertThat(sut.isEmailChangeConfirmBlocked("192.0.2.1")).isFalse();
  }
}
