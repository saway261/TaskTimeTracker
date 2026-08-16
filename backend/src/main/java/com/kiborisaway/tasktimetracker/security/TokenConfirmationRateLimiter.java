package com.kiborisaway.tasktimetracker.security;

import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * トークンを提示して確定する操作（パスワードリセット実行、メールアドレス確認実行、
 * メールアドレス変更確定）のレート制限をまとめたコンポーネントです（仕様書14章）。
 *
 * <p>{@link com.kiborisaway.tasktimetracker.security.LoginRateLimiter}と同じ方式で、
 * トークンが不正・期限切れ・使用済みとして拒否された場合だけを失敗としてカウントし、
 * 確定成功時はカウントをリセットします。総当たりによるトークン推測を抑止することが目的であり、
 * トークン自体は正当だが別の理由（例：新パスワードのポリシー違反）で失敗したケースは対象に含めません。
 *
 * <p>対象ごとに独立した{@link FixedWindowRateLimiter}インスタンスを持ちます。
 */
@Component
public class TokenConfirmationRateLimiter {

  private final FixedWindowRateLimiter passwordResetConfirm;
  private final FixedWindowRateLimiter emailVerificationConfirm;
  private final FixedWindowRateLimiter emailChangeConfirm;

  public TokenConfirmationRateLimiter(
      Clock clock,
      @Value("${app.security.rate-limit.password-reset-confirm.maximum-failures:10}")
      int passwordResetConfirmMaxFailures,
      @Value("${app.security.rate-limit.password-reset-confirm.window:15m}")
      Duration passwordResetConfirmWindow,
      @Value("${app.security.rate-limit.email-verification-confirm.maximum-failures:10}")
      int emailVerificationConfirmMaxFailures,
      @Value("${app.security.rate-limit.email-verification-confirm.window:15m}")
      Duration emailVerificationConfirmWindow,
      @Value("${app.security.rate-limit.email-change-confirm.maximum-failures:10}")
      int emailChangeConfirmMaxFailures,
      @Value("${app.security.rate-limit.email-change-confirm.window:15m}")
      Duration emailChangeConfirmWindow) {
    this.passwordResetConfirm = new FixedWindowRateLimiter(
        clock, passwordResetConfirmMaxFailures, passwordResetConfirmWindow);
    this.emailVerificationConfirm = new FixedWindowRateLimiter(
        clock, emailVerificationConfirmMaxFailures, emailVerificationConfirmWindow);
    this.emailChangeConfirm = new FixedWindowRateLimiter(
        clock, emailChangeConfirmMaxFailures, emailChangeConfirmWindow);
  }

  public boolean isPasswordResetConfirmBlocked(String clientAddress) {
    return passwordResetConfirm.isBlocked(normalizeIp(clientAddress));
  }

  public boolean recordPasswordResetConfirmFailure(String clientAddress) {
    return passwordResetConfirm.recordAttempt(normalizeIp(clientAddress));
  }

  public void resetPasswordResetConfirm(String clientAddress) {
    passwordResetConfirm.reset(normalizeIp(clientAddress));
  }

  public boolean isEmailVerificationConfirmBlocked(String clientAddress) {
    return emailVerificationConfirm.isBlocked(normalizeIp(clientAddress));
  }

  public boolean recordEmailVerificationConfirmFailure(String clientAddress) {
    return emailVerificationConfirm.recordAttempt(normalizeIp(clientAddress));
  }

  public void resetEmailVerificationConfirm(String clientAddress) {
    emailVerificationConfirm.reset(normalizeIp(clientAddress));
  }

  public boolean isEmailChangeConfirmBlocked(String clientAddress) {
    return emailChangeConfirm.isBlocked(normalizeIp(clientAddress));
  }

  public boolean recordEmailChangeConfirmFailure(String clientAddress) {
    return emailChangeConfirm.recordAttempt(normalizeIp(clientAddress));
  }

  public void resetEmailChangeConfirm(String clientAddress) {
    emailChangeConfirm.reset(normalizeIp(clientAddress));
  }

  private String normalizeIp(String clientAddress) {
    return clientAddress == null ? "" : clientAddress;
  }
}
