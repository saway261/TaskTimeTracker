package com.kiborisaway.tasktimetracker.security;

import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 外向きのメール送信を伴う操作（ユーザー登録、リセット要求、確認メール再送、メールアドレス変更の要求）の
 * レート制限をまとめたコンポーネントです（仕様書14章）。
 *
 * <p>これらは成功・失敗にかかわらず試行そのものをカウントします。メール送信枠の消費と送信ドメインの
 * レピュテーション毀損を防ぐことが目的であり、資格情報の正しさとは無関係だからです。
 *
 * <p>対象ごとに独立した{@link FixedWindowRateLimiter}インスタンスを持ちます。一部の上限値が
 * 同じ数値を共有していても、設定・実装レベルでは独立させています。
 */
@Component
public class EmailSendRateLimiter {

  private final TokenGenerator tokenGenerator;
  private final FixedWindowRateLimiter registration;
  private final FixedWindowRateLimiter passwordResetRequestByEmail;
  private final FixedWindowRateLimiter passwordResetRequestByIp;
  private final FixedWindowRateLimiter emailVerificationResend;
  private final FixedWindowRateLimiter emailChangeRequest;

  public EmailSendRateLimiter(
      Clock clock,
      TokenGenerator tokenGenerator,
      @Value("${app.security.rate-limit.registration.maximum-attempts:5}")
      int registrationMaxAttempts,
      @Value("${app.security.rate-limit.registration.window:1h}") Duration registrationWindow,
      @Value("${app.security.rate-limit.password-reset-request-email.maximum-attempts:5}")
      int passwordResetRequestByEmailMaxAttempts,
      @Value("${app.security.rate-limit.password-reset-request-email.window:1h}")
      Duration passwordResetRequestByEmailWindow,
      @Value("${app.security.rate-limit.password-reset-request-ip.maximum-attempts:20}")
      int passwordResetRequestByIpMaxAttempts,
      @Value("${app.security.rate-limit.password-reset-request-ip.window:1h}")
      Duration passwordResetRequestByIpWindow,
      @Value("${app.security.rate-limit.email-verification-resend.maximum-attempts:5}")
      int emailVerificationResendMaxAttempts,
      @Value("${app.security.rate-limit.email-verification-resend.window:1h}")
      Duration emailVerificationResendWindow,
      @Value("${app.security.rate-limit.email-change-request.maximum-attempts:5}")
      int emailChangeRequestMaxAttempts,
      @Value("${app.security.rate-limit.email-change-request.window:1h}")
      Duration emailChangeRequestWindow) {
    this.tokenGenerator = tokenGenerator;
    this.registration =
        new FixedWindowRateLimiter(clock, registrationMaxAttempts, registrationWindow);
    this.passwordResetRequestByEmail = new FixedWindowRateLimiter(
        clock, passwordResetRequestByEmailMaxAttempts, passwordResetRequestByEmailWindow);
    this.passwordResetRequestByIp = new FixedWindowRateLimiter(
        clock, passwordResetRequestByIpMaxAttempts, passwordResetRequestByIpWindow);
    this.emailVerificationResend = new FixedWindowRateLimiter(
        clock, emailVerificationResendMaxAttempts, emailVerificationResendWindow);
    this.emailChangeRequest = new FixedWindowRateLimiter(
        clock, emailChangeRequestMaxAttempts, emailChangeRequestWindow);
  }

  public boolean isRegistrationBlocked(String clientAddress) {
    return registration.isBlocked(normalizeIp(clientAddress));
  }

  public boolean recordRegistrationAttempt(String clientAddress) {
    return registration.recordAttempt(normalizeIp(clientAddress));
  }

  public boolean isPasswordResetRequestBlocked(String clientAddress, String normalizedEmail) {
    return passwordResetRequestByEmail.isBlocked(emailHash(normalizedEmail))
        || passwordResetRequestByIp.isBlocked(normalizeIp(clientAddress));
  }

  public void recordPasswordResetRequestAttempt(String clientAddress, String normalizedEmail) {
    passwordResetRequestByEmail.recordAttempt(emailHash(normalizedEmail));
    passwordResetRequestByIp.recordAttempt(normalizeIp(clientAddress));
  }

  public boolean isEmailVerificationResendBlocked(int userId) {
    return emailVerificationResend.isBlocked(Integer.toString(userId));
  }

  public boolean recordEmailVerificationResendAttempt(int userId) {
    return emailVerificationResend.recordAttempt(Integer.toString(userId));
  }

  public boolean isEmailChangeRequestBlocked(int userId) {
    return emailChangeRequest.isBlocked(Integer.toString(userId));
  }

  public boolean recordEmailChangeRequestAttempt(int userId) {
    return emailChangeRequest.recordAttempt(Integer.toString(userId));
  }

  private String normalizeIp(String clientAddress) {
    return clientAddress == null ? "" : clientAddress;
  }

  private String emailHash(String normalizedEmail) {
    return tokenGenerator.hash(normalizedEmail == null ? "" : normalizedEmail);
  }
}
