package com.kiborisaway.tasktimetracker.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginRateLimiterTest {

  private static final Instant STARTED_AT = Instant.parse("2026-08-14T00:00:00Z");

  @Mock
  private Clock clock;

  private LoginRateLimiter sut;

  @BeforeEach
  void setUp() {
    when(clock.instant()).thenReturn(STARTED_AT);
    sut = new LoginRateLimiter(clock, 3, Duration.ofMinutes(15));
  }

  @Test
  void 失敗記録_上限へ到達すると同一IPとメールを拒否すること() {
    assertThat(sut.recordFailure("192.0.2.1", "user@example.com")).isFalse();
    assertThat(sut.recordFailure("192.0.2.1", "user@example.com")).isFalse();

    assertThat(sut.recordFailure("192.0.2.1", "user@example.com")).isTrue();
    assertThat(sut.isBlocked("192.0.2.1", "user@example.com")).isTrue();
    assertThat(sut.isBlocked("192.0.2.2", "user@example.com")).isFalse();
  }

  @Test
  void メールキー_大文字小文字と前後空白を正規化すること() {
    sut.recordFailure("192.0.2.1", " User@Example.com ");
    sut.recordFailure("192.0.2.1", "user@example.com");

    assertThat(sut.recordFailure("192.0.2.1", "USER@EXAMPLE.COM")).isTrue();
  }

  @Test
  void 時間経過_15分経過時に失敗回数を解除すること() {
    for (int attempt = 0; attempt < 3; attempt++) {
      sut.recordFailure("192.0.2.1", "user@example.com");
    }
    when(clock.instant()).thenReturn(STARTED_AT.plus(Duration.ofMinutes(15)));

    assertThat(sut.isBlocked("192.0.2.1", "user@example.com")).isFalse();
    assertThat(sut.recordFailure("192.0.2.1", "user@example.com")).isFalse();
  }

  @Test
  void 成功記録_対象キーの失敗回数を解除すること() {
    for (int attempt = 0; attempt < 3; attempt++) {
      sut.recordFailure("192.0.2.1", "user@example.com");
    }

    sut.reset("192.0.2.1", "user@example.com");

    assertThat(sut.isBlocked("192.0.2.1", "user@example.com")).isFalse();
  }
}
