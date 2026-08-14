package com.kiborisaway.tasktimetracker.security;

import com.kiborisaway.tasktimetracker.service.UserService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {

  private final ConcurrentMap<String, FailureWindow> failures = new ConcurrentHashMap<>();
  private final Clock clock;
  private final int maximumFailures;
  private final Duration windowDuration;

  public LoginRateLimiter(
      Clock clock,
      @Value("${app.security.rate-limit.login.maximum-failures:10}") int maximumFailures,
      @Value("${app.security.rate-limit.login.window:15m}") Duration windowDuration) {
    if (maximumFailures < 1 || windowDuration.isZero() || windowDuration.isNegative()) {
      throw new IllegalArgumentException("rate limit settings must be positive");
    }
    this.clock = clock;
    this.maximumFailures = maximumFailures;
    this.windowDuration = windowDuration;
  }

  public boolean isBlocked(String clientAddress, String email) {
    String key = key(clientAddress, email);
    Instant now = clock.instant();
    FailureWindow current = failures.get(key);
    if (current == null) {
      return false;
    }
    if (!current.startedAt().plus(windowDuration).isAfter(now)) {
      failures.remove(key, current);
      return false;
    }
    return current.count() >= maximumFailures;
  }

  public boolean recordFailure(String clientAddress, String email) {
    String key = key(clientAddress, email);
    Instant now = clock.instant();
    FailureWindow updated = failures.compute(key, (ignored, current) -> {
      if (current == null || !current.startedAt().plus(windowDuration).isAfter(now)) {
        return new FailureWindow(now, 1);
      }
      return new FailureWindow(current.startedAt(), current.count() + 1);
    });
    return updated.count() >= maximumFailures;
  }

  public void reset(String clientAddress, String email) {
    failures.remove(key(clientAddress, email));
  }

  private String key(String clientAddress, String email) {
    String normalizedEmail = UserService.normalizeEmail(email == null ? "" : email);
    return (clientAddress == null ? "" : clientAddress) + ":" + sha256(normalizedEmail);
  }

  private String sha256(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is unavailable", ex);
    }
  }

  private record FailureWindow(Instant startedAt, int count) {
  }
}
