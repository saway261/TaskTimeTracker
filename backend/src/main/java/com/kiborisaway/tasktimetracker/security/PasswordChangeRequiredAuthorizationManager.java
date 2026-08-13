package com.kiborisaway.tasktimetracker.security;

import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

@Component
public class PasswordChangeRequiredAuthorizationManager
    implements AuthorizationManager<RequestAuthorizationContext> {

  public static final String DENIAL_REASON_ATTRIBUTE =
      PasswordChangeRequiredAuthorizationManager.class.getName() + ".denialReason";
  public static final String PASSWORD_CHANGE_REQUIRED = "password change required";

  @Override
  public AuthorizationDecision authorize(
      Supplier<? extends Authentication> authenticationSupplier,
      RequestAuthorizationContext context) {
    Authentication authentication = authenticationSupplier.get();
    Object principal = authentication == null ? null : authentication.getPrincipal();
    boolean granted = authentication != null
        && authentication.isAuthenticated()
        && principal instanceof AuthenticatedUser user
        && !user.isPasswordChangeRequired();
    if (!granted && principal instanceof AuthenticatedUser user
        && user.isPasswordChangeRequired()) {
      context.getRequest().setAttribute(DENIAL_REASON_ATTRIBUTE, PASSWORD_CHANGE_REQUIRED);
    }
    return new AuthorizationDecision(granted);
  }

  public AuthorizationDecision authorizePublicAuthenticationEndpoint(
      Supplier<? extends Authentication> authenticationSupplier,
      RequestAuthorizationContext context) {
    Authentication authentication = authenticationSupplier.get();
    Object principal = authentication == null ? null : authentication.getPrincipal();
    if (principal instanceof AuthenticatedUser user && user.isPasswordChangeRequired()) {
      context.getRequest().setAttribute(DENIAL_REASON_ATTRIBUTE, PASSWORD_CHANGE_REQUIRED);
      return new AuthorizationDecision(false);
    }
    return new AuthorizationDecision(true);
  }
}
