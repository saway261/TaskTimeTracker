package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.dto.auth.EmailChangeConfirmRequest;
import com.kiborisaway.tasktimetracker.data.dto.auth.EmailChangeRequestBody;
import com.kiborisaway.tasktimetracker.data.dto.auth.PendingEmailResponse;
import com.kiborisaway.tasktimetracker.security.AuthenticatedUser;
import com.kiborisaway.tasktimetracker.service.EmailChangeService;
import jakarta.validation.Valid;
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

  public EmailChangeController(EmailChangeService emailChangeService) {
    this.emailChangeService = emailChangeService;
  }

  @PutMapping("/email")
  public ResponseEntity<PendingEmailResponse> requestChange(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestBody @Valid EmailChangeRequestBody requestBody) {
    PendingEmailResponse response = emailChangeService.requestChange(
        user.getUserId(), requestBody);
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
  }

  @PostMapping("/email-changes")
  public ResponseEntity<Void> confirm(
      @RequestBody @Valid EmailChangeConfirmRequest requestBody) {
    emailChangeService.confirm(requestBody.token());
    return ResponseEntity.noContent().build();
  }
}
