package com.kiborisaway.tasktimetracker.security;

import com.kiborisaway.tasktimetracker.service.UserService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {

  private final FixedWindowRateLimiter delegate;

  public LoginRateLimiter(
      Clock clock,
      @Value("${app.security.rate-limit.login.maximum-failures:10}") int maximumFailures,
      @Value("${app.security.rate-limit.login.window:15m}") Duration windowDuration) {
    this.delegate = new FixedWindowRateLimiter(clock, maximumFailures, windowDuration);
  }

  public boolean isBlocked(String clientAddress, String email) {
    return delegate.isBlocked(key(clientAddress, email));
  }

  public boolean recordFailure(String clientAddress, String email) {
    return delegate.recordAttempt(key(clientAddress, email));
  }

  public void reset(String clientAddress, String email) {
    delegate.reset(key(clientAddress, email));
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
}
