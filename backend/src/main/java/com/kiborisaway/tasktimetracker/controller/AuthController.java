package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.dto.auth.AuthenticatedUserResponse;
import com.kiborisaway.tasktimetracker.data.dto.auth.CsrfTokenResponse;
import com.kiborisaway.tasktimetracker.data.dto.auth.LoginRequest;
import com.kiborisaway.tasktimetracker.data.dto.auth.PasswordChangeRequest;
import com.kiborisaway.tasktimetracker.data.dto.auth.RegisterRequest;
import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.exception.PasswordChangeNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.PasswordPolicyViolationException;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorResponse;
import com.kiborisaway.tasktimetracker.security.AbsoluteSessionTimeoutFilter;
import com.kiborisaway.tasktimetracker.security.AuthenticatedUser;
import com.kiborisaway.tasktimetracker.security.EmailSendRateLimiter;
import com.kiborisaway.tasktimetracker.security.LoginRateLimiter;
import com.kiborisaway.tasktimetracker.service.PasswordChangeService;
import com.kiborisaway.tasktimetracker.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final UserService userService;
  private final AuthenticationManager authenticationManager;
  private final SecurityContextRepository securityContextRepository;
  private final PasswordChangeService passwordChangeService;
  private final LoginRateLimiter loginRateLimiter;
  private final EmailSendRateLimiter emailSendRateLimiter;
  private final Clock clock;

  public AuthController(
      UserService userService,
      AuthenticationManager authenticationManager,
      SecurityContextRepository securityContextRepository,
      PasswordChangeService passwordChangeService,
      LoginRateLimiter loginRateLimiter,
      EmailSendRateLimiter emailSendRateLimiter,
      Clock clock) {
    this.userService = userService;
    this.authenticationManager = authenticationManager;
    this.securityContextRepository = securityContextRepository;
    this.passwordChangeService = passwordChangeService;
    this.loginRateLimiter = loginRateLimiter;
    this.emailSendRateLimiter = emailSendRateLimiter;
    this.clock = clock;
  }

  @GetMapping("/csrf")
  public CsrfTokenResponse csrf(CsrfToken csrfToken) {
    return new CsrfTokenResponse(csrfToken.getToken(), csrfToken.getHeaderName());
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(
      @RequestBody @Valid RegisterRequest requestBody,
      HttpServletRequest request,
      HttpServletResponse response) {
    String clientAddress = request.getRemoteAddr();
    if (emailSendRateLimiter.isRegistrationBlocked(clientAddress)) {
      return tooManyRequests("too many requests");
    }
    emailSendRateLimiter.recordRegistrationAttempt(clientAddress);
    AuthenticatedUser user = userService.register(requestBody);
    Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
        user, null, user.getAuthorities());
    saveAuthentication(authentication, request, response);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new AuthenticatedUserResponse(user));
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(
      @RequestBody @Valid LoginRequest requestBody,
      HttpServletRequest request,
      HttpServletResponse response) {
    String normalizedEmail = UserService.normalizeEmail(requestBody.email());
    String clientAddress = request.getRemoteAddr();
    if (loginRateLimiter.isBlocked(clientAddress, normalizedEmail)) {
      return tooManyRequests();
    }
    try {
      Authentication authentication = authenticationManager.authenticate(
          UsernamePasswordAuthenticationToken.unauthenticated(
              normalizedEmail, requestBody.password()));
      loginRateLimiter.reset(clientAddress, normalizedEmail);
      saveAuthentication(authentication, request, response);
      return ResponseEntity.ok(
          new AuthenticatedUserResponse((AuthenticatedUser) authentication.getPrincipal()));
    } catch (AuthenticationException ex) {
      if (loginRateLimiter.recordFailure(clientAddress, normalizedEmail)) {
        return tooManyRequests();
      }
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
          new ErrorResponse(HttpStatus.UNAUTHORIZED,
              "email or password is incorrect", List.of()));
    }
  }

  @GetMapping("/me")
  public ResponseEntity<?> me(@AuthenticationPrincipal AuthenticatedUser principal) {
    AppUser user = userService.findById(principal.getUserId());
    if (user == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
          new ErrorResponse(HttpStatus.UNAUTHORIZED, "authentication required", List.of()));
    }
    return ResponseEntity.ok(new AuthenticatedUserResponse(user));
  }

  @PutMapping("/password")
  public ResponseEntity<?> changePassword(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestBody @Valid PasswordChangeRequest requestBody,
      HttpServletRequest request,
      HttpServletResponse response) {
    String clientAddress = request.getRemoteAddr();
    if (loginRateLimiter.isBlocked(clientAddress, user.getEmail())) {
      return tooManyRequests();
    }
    try {
      passwordChangeService.changePassword(user.getUserId(), requestBody);
      loginRateLimiter.reset(clientAddress, user.getEmail());
    } catch (PasswordChangeNotAllowedException | PasswordPolicyViolationException ex) {
      if (loginRateLimiter.recordFailure(clientAddress, user.getEmail())) {
        return tooManyRequests();
      }
      throw ex;
    }
    SecurityContextHolder.clearContext();
    if (request.getSession(false) != null) {
      request.getSession(false).invalidate();
    }
    return ResponseEntity.noContent().build();
  }

  private void saveAuthentication(
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response) {
    HttpSession session = request.getSession();
    request.changeSessionId();
    session.setAttribute(
        AbsoluteSessionTimeoutFilter.AUTHENTICATED_AT_SESSION_ATTRIBUTE,
        Instant.now(clock));
    session.setMaxInactiveInterval(Math.toIntExact(
        AbsoluteSessionTimeoutFilter.AUTHENTICATED_SESSION_TIMEOUT.toSeconds()));
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, request, response);
  }

  private ResponseEntity<ErrorResponse> tooManyRequests() {
    return tooManyRequests("too many failed attempts");
  }

  private ResponseEntity<ErrorResponse> tooManyRequests(String message) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
        new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS, message, List.of()));
  }
}
