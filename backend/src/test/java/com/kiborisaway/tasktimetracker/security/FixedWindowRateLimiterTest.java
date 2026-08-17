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
class FixedWindowRateLimiterTest {

  private static final Instant STARTED_AT = Instant.parse("2026-08-16T00:00:00Z");

  @Mock
  private Clock clock;

  private FixedWindowRateLimiter sut;

  @BeforeEach
  void setUp() {
    when(clock.instant()).thenReturn(STARTED_AT);
    sut = new FixedWindowRateLimiter(clock, 3, Duration.ofMinutes(15));
  }

  @Test
  void 記録_上限へ到達すると同一キーを拒否すること() {
    assertThat(sut.recordAttempt("key-a")).isFalse();
    assertThat(sut.recordAttempt("key-a")).isFalse();

    assertThat(sut.recordAttempt("key-a")).isTrue();
    assertThat(sut.isBlocked("key-a")).isTrue();
    assertThat(sut.isBlocked("key-b")).isFalse();
  }

  @Test
  void 時間経過_ウィンドウ経過時にカウントを解除すること() {
    for (int attempt = 0; attempt < 3; attempt++) {
      sut.recordAttempt("key-a");
    }
    when(clock.instant()).thenReturn(STARTED_AT.plus(Duration.ofMinutes(15)));

    assertThat(sut.isBlocked("key-a")).isFalse();
    assertThat(sut.recordAttempt("key-a")).isFalse();
  }

  @Test
  void 解除_対象キーのカウントを消去すること() {
    for (int attempt = 0; attempt < 3; attempt++) {
      sut.recordAttempt("key-a");
    }

    sut.reset("key-a");

    assertThat(sut.isBlocked("key-a")).isFalse();
  }
}
