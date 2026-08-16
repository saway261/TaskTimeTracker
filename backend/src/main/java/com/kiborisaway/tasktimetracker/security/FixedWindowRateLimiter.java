package com.kiborisaway.tasktimetracker.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * キー単位の固定ウィンドウ計数を行う汎用のレート制限エンジンです（仕様書14章）。
 *
 * <p>単一インスタンス前提のメモリ内実装です（仕様書14章）。複数インスタンス化時はRedis等の共有ストアへ移す判断が必要です。
 * このクラス自体はSpringのBeanではなく、用途ごとに閾値・ウィンドウ長を変えて複数インスタンスを生成して使います。
 */
public class FixedWindowRateLimiter {

  private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();
  private final Clock clock;
  private final int maximumCount;
  private final Duration windowDuration;

  public FixedWindowRateLimiter(Clock clock, int maximumCount, Duration windowDuration) {
    if (maximumCount < 1 || windowDuration.isZero() || windowDuration.isNegative()) {
      throw new IllegalArgumentException("rate limit settings must be positive");
    }
    this.clock = clock;
    this.maximumCount = maximumCount;
    this.windowDuration = windowDuration;
  }

  public boolean isBlocked(String key) {
    Instant now = clock.instant();
    Window current = windows.get(key);
    if (current == null) {
      return false;
    }
    if (!current.startedAt().plus(windowDuration).isAfter(now)) {
      windows.remove(key, current);
      return false;
    }
    return current.count() >= maximumCount;
  }

  public boolean recordAttempt(String key) {
    Instant now = clock.instant();
    Window updated = windows.compute(key, (ignored, current) -> {
      if (current == null || !current.startedAt().plus(windowDuration).isAfter(now)) {
        return new Window(now, 1);
      }
      return new Window(current.startedAt(), current.count() + 1);
    });
    return updated.count() >= maximumCount;
  }

  public void reset(String key) {
    windows.remove(key);
  }

  private record Window(Instant startedAt, int count) {
  }
}
