package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.dto.auth.EmailChangeConfirmRequest;
import com.kiborisaway.tasktimetracker.data.dto.auth.EmailChangeRequestBody;
import com.kiborisaway.tasktimetracker.data.dto.auth.PendingEmailResponse;
import com.kiborisaway.tasktimetracker.exception.EmailChangeNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.EmailChangeRequestInvalidException;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorResponse;
import com.kiborisaway.tasktimetracker.security.AuthenticatedUser;
import com.kiborisaway.tasktimetracker.security.EmailSendRateLimiter;
import com.kiborisaway.tasktimetracker.security.LoginRateLimiter;
import com.kiborisaway.tasktimetracker.security.TokenConfirmationRateLimiter;
import com.kiborisaway.tasktimetracker.service.EmailChangeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class EmailChangeController {

  private final EmailChangeService emailChangeService;
  private final EmailSendRateLimiter emailSendRateLimiter;
  private final TokenConfirmationRateLimiter tokenConfirmationRateLimiter;
  private final LoginRateLimiter loginRateLimiter;

  public EmailChangeController(
      EmailChangeService emailChangeService,
      EmailSendRateLimiter emailSendRateLimiter,
      TokenConfirmationRateLimiter tokenConfirmationRateLimiter,
      LoginRateLimiter loginRateLimiter) {
    this.emailChangeService = emailChangeService;
    this.emailSendRateLimiter = emailSendRateLimiter;
    this.tokenConfirmationRateLimiter = tokenConfirmationRateLimiter;
    this.loginRateLimiter = loginRateLimiter;
  }

  @PutMapping("/email")
  public ResponseEntity<?> requestChange(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestBody @Valid EmailChangeRequestBody requestBody,
      HttpServletRequest request) {
    String clientAddress = request.getRemoteAddr();
    if (emailSendRateLimiter.isEmailChangeRequestBlocked(user.getUserId())
        || loginRateLimiter.isBlocked(clientAddress, user.getEmail())) {
      return tooManyRequests();
    }
    emailSendRateLimiter.recordEmailChangeRequestAttempt(user.getUserId());
    try {
      PendingEmailResponse response = emailChangeService.requestChange(
          user.getUserId(), requestBody);
      loginRateLimiter.reset(clientAddress, user.getEmail());
      return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    } catch (EmailChangeNotAllowedException ex) {
      if (ex.isCredentialFailure()
          && loginRateLimiter.recordFailure(clientAddress, user.getEmail())) {
        return tooManyRequests();
      }
      throw ex;
    }
  }

  @PostMapping("/email-changes")
  public ResponseEntity<?> confirm(
      @RequestBody @Valid EmailChangeConfirmRequest requestBody,
      HttpServletRequest request) {
    String clientAddress = request.getRemoteAddr();
    if (tokenConfirmationRateLimiter.isEmailChangeConfirmBlocked(clientAddress)) {
      return tooManyRequests();
    }
    try {
      emailChangeService.confirm(requestBody.token());
      tokenConfirmationRateLimiter.resetEmailChangeConfirm(clientAddress);
    } catch (EmailChangeRequestInvalidException ex) {
      if (tokenConfirmationRateLimiter.recordEmailChangeConfirmFailure(clientAddress)) {
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
