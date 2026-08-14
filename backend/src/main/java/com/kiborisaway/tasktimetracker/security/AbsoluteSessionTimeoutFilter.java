package com.kiborisaway.tasktimetracker.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AbsoluteSessionTimeoutFilter extends OncePerRequestFilter {

  public static final String AUTHENTICATED_AT_SESSION_ATTRIBUTE =
      AbsoluteSessionTimeoutFilter.class.getName() + ".AUTHENTICATED_AT";
  public static final Duration AUTHENTICATED_SESSION_TIMEOUT = Duration.ofDays(30);

  private final Clock clock;
  private final AuthenticationEntryPoint authenticationEntryPoint;

  public AbsoluteSessionTimeoutFilter(
      Clock clock,
      JsonAuthenticationEntryPoint authenticationEntryPoint) {
    this.clock = clock;
    this.authenticationEntryPoint = authenticationEntryPoint;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    HttpSession session = request.getSession(false);
    if (!isAuthenticated(authentication) || session == null) {
      filterChain.doFilter(request, response);
      return;
    }

    Object value = session.getAttribute(AUTHENTICATED_AT_SESSION_ATTRIBUTE);
    if (value instanceof Instant authenticatedAt
        && authenticatedAt.plus(AUTHENTICATED_SESSION_TIMEOUT).isAfter(clock.instant())) {
      filterChain.doFilter(request, response);
      return;
    }

    session.invalidate();
    SecurityContextHolder.clearContext();
    authenticationEntryPoint.commence(
        request,
        response,
        new InsufficientAuthenticationException("authenticated session expired"));
  }

  private boolean isAuthenticated(Authentication authentication) {
    return authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken);
  }
}
