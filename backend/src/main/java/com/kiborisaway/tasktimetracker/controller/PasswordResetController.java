package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.dto.auth.PasswordResetConfirmRequest;
import com.kiborisaway.tasktimetracker.data.dto.auth.PasswordResetRequestRequest;
import com.kiborisaway.tasktimetracker.data.dto.auth.PasswordResetRequestResponse;
import com.kiborisaway.tasktimetracker.service.PasswordResetService;
import jakarta.validation.Valid;
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

  public PasswordResetController(PasswordResetService passwordResetService) {
    this.passwordResetService = passwordResetService;
  }

  @PostMapping("/password-reset-requests")
  public ResponseEntity<PasswordResetRequestResponse> requestReset(
      @RequestBody @Valid PasswordResetRequestRequest requestBody) {
    passwordResetService.requestReset(requestBody.email());
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(REQUEST_ACCEPTED_RESPONSE);
  }

  @PostMapping("/password-resets")
  public ResponseEntity<Void> confirmReset(
      @RequestBody @Valid PasswordResetConfirmRequest requestBody) {
    passwordResetService.confirmReset(requestBody.token(), requestBody.newPassword());
    return ResponseEntity.noContent().build();
  }
}
