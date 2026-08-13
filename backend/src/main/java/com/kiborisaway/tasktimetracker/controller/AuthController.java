package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.dto.auth.AuthenticatedUserResponse;
import com.kiborisaway.tasktimetracker.data.dto.auth.CsrfTokenResponse;
import com.kiborisaway.tasktimetracker.data.dto.auth.LoginRequest;
import com.kiborisaway.tasktimetracker.data.dto.auth.PasswordChangeRequest;
import com.kiborisaway.tasktimetracker.data.dto.auth.RegisterRequest;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorResponse;
import com.kiborisaway.tasktimetracker.security.AuthenticatedUser;
import com.kiborisaway.tasktimetracker.service.UserService;
import com.kiborisaway.tasktimetracker.service.PasswordChangeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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

  public AuthController(
      UserService userService,
      AuthenticationManager authenticationManager,
      SecurityContextRepository securityContextRepository,
      PasswordChangeService passwordChangeService) {
    this.userService = userService;
    this.authenticationManager = authenticationManager;
    this.securityContextRepository = securityContextRepository;
    this.passwordChangeService = passwordChangeService;
  }

  @GetMapping("/csrf")
  public CsrfTokenResponse csrf(CsrfToken csrfToken) {
    return new CsrfTokenResponse(csrfToken.getToken(), csrfToken.getHeaderName());
  }

  @PostMapping("/register")
  public ResponseEntity<AuthenticatedUserResponse> register(
      @RequestBody @Valid RegisterRequest requestBody,
      HttpServletRequest request,
      HttpServletResponse response) {
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
    try {
      Authentication authentication = authenticationManager.authenticate(
          UsernamePasswordAuthenticationToken.unauthenticated(
              UserService.normalizeEmail(requestBody.email()), requestBody.password()));
      saveAuthentication(authentication, request, response);
      return ResponseEntity.ok(
          new AuthenticatedUserResponse((AuthenticatedUser) authentication.getPrincipal()));
    } catch (AuthenticationException ex) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
          new ErrorResponse(HttpStatus.UNAUTHORIZED,
              "email or password is incorrect", List.of()));
    }
  }

  @GetMapping("/me")
  public AuthenticatedUserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
    return new AuthenticatedUserResponse(user);
  }

  @PutMapping("/password")
  public ResponseEntity<Void> changePassword(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestBody @Valid PasswordChangeRequest requestBody,
      HttpServletRequest request,
      HttpServletResponse response) {
    passwordChangeService.changePassword(user.getUserId(), requestBody);
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
    request.getSession();
    request.changeSessionId();
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, request, response);
  }
}
