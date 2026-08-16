package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.dto.auth.PasswordResetConfirmRequest;
import com.kiborisaway.tasktimetracker.data.dto.auth.PasswordResetRequestRequest;
import com.kiborisaway.tasktimetracker.data.dto.auth.PasswordResetRequestResponse;
import com.kiborisaway.tasktimetracker.exception.PasswordResetInvalidException;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorResponse;
import com.kiborisaway.tasktimetracker.security.EmailSendRateLimiter;
import com.kiborisaway.tasktimetracker.security.TokenConfirmationRateLimiter;
import com.kiborisaway.tasktimetracker.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class PasswordResetController {

  private static final PasswordResetRequestResponse REQUEST_ACCEPTED_RESPONSE =
      new PasswordResetRequestResponse(
          "If the email address is registered, a password reset email will be sent.");

  private final PasswordResetService passwordResetService;
  private final EmailSendRateLimiter emailSendRateLimiter;
  private final TokenConfirmationRateLimiter tokenConfirmationRateLimiter;

  public PasswordResetController(
      PasswordResetService passwordResetService,
      EmailSendRateLimiter emailSendRateLimiter,
      TokenConfirmationRateLimiter tokenConfirmationRateLimiter) {
    this.passwordResetService = passwordResetService;
    this.emailSendRateLimiter = emailSendRateLimiter;
    this.tokenConfirmationRateLimiter = tokenConfirmationRateLimiter;
  }

  @PostMapping("/password-reset-requests")
  public ResponseEntity<?> requestReset(
      @RequestBody @Valid PasswordResetRequestRequest requestBody,
      HttpServletRequest request) {
    String clientAddress = request.getRemoteAddr();
    if (emailSendRateLimiter.isPasswordResetRequestBlocked(clientAddress, requestBody.email())) {
      return tooManyRequests();
    }
    emailSendRateLimiter.recordPasswordResetRequestAttempt(clientAddress, requestBody.email());
    passwordResetService.requestReset(requestBody.email());
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(REQUEST_ACCEPTED_RESPONSE);
  }

  @PostMapping("/password-resets")
  public ResponseEntity<?> confirmReset(
      @RequestBody @Valid PasswordResetConfirmRequest requestBody,
      HttpServletRequest request) {
    String clientAddress = request.getRemoteAddr();
    if (tokenConfirmationRateLimiter.isPasswordResetConfirmBlocked(clientAddress)) {
      return tooManyRequests();
    }
    try {
      passwordResetService.confirmReset(requestBody.token(), requestBody.newPassword());
      tokenConfirmationRateLimiter.resetPasswordResetConfirm(clientAddress);
    } catch (PasswordResetInvalidException ex) {
      if (tokenConfirmationRateLimiter.recordPasswordResetConfirmFailure(clientAddress)) {
        return tooManyRequests();
      }
      throw ex;
    }
    return ResponseEntity.noContent().build();
  }

  private ResponseEntity<ErrorResponse> tooManyRequests() {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
        new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS, "too many requests", List.of()));
  }
}
