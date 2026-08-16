package com.kiborisaway.tasktimetracker.support;

import com.kiborisaway.tasktimetracker.security.AuthenticatedUser;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockAuthenticatedUserSecurityContextFactory
    implements WithSecurityContextFactory<WithMockAuthenticatedUser> {

  @Override
  public SecurityContext createSecurityContext(WithMockAuthenticatedUser annotation) {
    AuthenticatedUser principal = AuthenticatedUserTestFactory.create(
        annotation.userId(), annotation.email(), annotation.passwordChangeRequired(),
        annotation.emailVerified());

    UsernamePasswordAuthenticationToken authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            principal, principal.getPassword(), principal.getAuthorities());

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    return context;
  }
}
