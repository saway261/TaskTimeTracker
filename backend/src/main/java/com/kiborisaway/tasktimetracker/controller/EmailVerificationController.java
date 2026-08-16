package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.dto.auth.EmailVerificationConfirmRequest;
import com.kiborisaway.tasktimetracker.exception.EmailVerificationInvalidException;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorResponse;
import com.kiborisaway.tasktimetracker.security.AuthenticatedUser;
import com.kiborisaway.tasktimetracker.security.EmailSendRateLimiter;
import com.kiborisaway.tasktimetracker.security.TokenConfirmationRateLimiter;
import com.kiborisaway.tasktimetracker.service.EmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/email-verifications")
public class EmailVerificationController {

  private final EmailVerificationService emailVerificationService;
  private final EmailSendRateLimiter emailSendRateLimiter;
  private final TokenConfirmationRateLimiter tokenConfirmationRateLimiter;

  public EmailVerificationController(
      EmailVerificationService emailVerificationService,
      EmailSendRateLimiter emailSendRateLimiter,
      TokenConfirmationRateLimiter tokenConfirmationRateLimiter) {
    this.emailVerificationService = emailVerificationService;
    this.emailSendRateLimiter = emailSendRateLimiter;
    this.tokenConfirmationRateLimiter = tokenConfirmationRateLimiter;
  }

  @PostMapping
  public ResponseEntity<?> confirm(
      @RequestBody @Valid EmailVerificationConfirmRequest requestBody,
      HttpServletRequest request) {
    String clientAddress = request.getRemoteAddr();
    if (tokenConfirmationRateLimiter.isEmailVerificationConfirmBlocked(clientAddress)) {
      return tooManyRequests();
    }
    try {
      emailVerificationService.confirm(requestBody.token());
      tokenConfirmationRateLimiter.resetEmailVerificationConfirm(clientAddress);
    } catch (EmailVerificationInvalidException ex) {
      if (tokenConfirmationRateLimiter.recordEmailVerificationConfirmFailure(clientAddress)) {
        return tooManyRequests();
      }
      throw ex;
    }
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/resend")
  public ResponseEntity<?> resend(@AuthenticationPrincipal AuthenticatedUser user) {
    if (emailSendRateLimiter.isEmailVerificationResendBlocked(user.getUserId())) {
      return tooManyRequests();
    }
    emailSendRateLimiter.recordEmailVerificationResendAttempt(user.getUserId());
    emailVerificationService.resend(user.getUserId());
    return ResponseEntity.noContent().build();
  }

  private ResponseEntity<ErrorResponse> tooManyRequests() {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
        new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS, "too many requests", List.of()));
  }
}
