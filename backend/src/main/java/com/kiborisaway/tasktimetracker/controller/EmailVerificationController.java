package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.dto.auth.EmailVerificationConfirmRequest;
import com.kiborisaway.tasktimetracker.security.AuthenticatedUser;
import com.kiborisaway.tasktimetracker.service.EmailVerificationService;
import jakarta.validation.Valid;
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

  public EmailVerificationController(EmailVerificationService emailVerificationService) {
    this.emailVerificationService = emailVerificationService;
  }

  @PostMapping
  public ResponseEntity<Void> confirm(
      @RequestBody @Valid EmailVerificationConfirmRequest requestBody) {
    emailVerificationService.confirm(requestBody.token());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/resend")
  public ResponseEntity<Void> resend(@AuthenticationPrincipal AuthenticatedUser user) {
    emailVerificationService.resend(user.getUserId());
    return ResponseEntity.noContent().build();
  }
}
