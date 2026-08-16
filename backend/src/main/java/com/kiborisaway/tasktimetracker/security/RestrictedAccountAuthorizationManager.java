package com.kiborisaway.tasktimetracker.security;

import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

/**
 * 認証済みであっても業務APIの利用を制限すべき状態（パスワード変更必須・メールアドレス未確認）を判定します（仕様書11.2）。
 *
 * <p>両方の制限状態が同時に成立する場合は、資格情報の健全性を先に回復させるためパスワード変更を優先する。
 */
@Component
public class RestrictedAccountAuthorizationManager
    implements AuthorizationManager<RequestAuthorizationContext> {

  public static final String DENIAL_REASON_ATTRIBUTE =
      RestrictedAccountAuthorizationManager.class.getName() + ".denialReason";
  public static final String PASSWORD_CHANGE_REQUIRED = "password change required";
  public static final String EMAIL_VERIFICATION_REQUIRED = "email verification required";

  @Override
  public AuthorizationDecision authorize(
      Supplier<? extends Authentication> authenticationSupplier,
      RequestAuthorizationContext context) {
    Authentication authentication = authenticationSupplier.get();
    Object principal = authentication == null ? null : authentication.getPrincipal();
    if (authentication == null || !authentication.isAuthenticated()
        || !(principal instanceof AuthenticatedUser user)) {
      return new AuthorizationDecision(false);
    }

    String denialReason = denialReason(user);
    if (denialReason != null) {
      context.getRequest().setAttribute(DENIAL_REASON_ATTRIBUTE, denialReason);
      return new AuthorizationDecision(false);
    }
    return new AuthorizationDecision(true);
  }

  public AuthorizationDecision authorizePublicAuthenticationEndpoint(
      Supplier<? extends Authentication> authenticationSupplier,
      RequestAuthorizationContext context) {
    Authentication authentication = authenticationSupplier.get();
    Object principal = authentication == null ? null : authentication.getPrincipal();
    if (principal instanceof AuthenticatedUser user) {
      String denialReason = denialReason(user);
      if (denialReason != null) {
        context.getRequest().setAttribute(DENIAL_REASON_ATTRIBUTE, denialReason);
        return new AuthorizationDecision(false);
      }
    }
    return new AuthorizationDecision(true);
  }

  private String denialReason(AuthenticatedUser user) {
    if (user.isPasswordChangeRequired()) {
      return PASSWORD_CHANGE_REQUIRED;
    }
    if (!user.isEmailVerified()) {
      return EMAIL_VERIFICATION_REQUIRED;
    }
    return null;
  }
}
