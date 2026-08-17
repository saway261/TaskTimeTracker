package com.kiborisaway.tasktimetracker.data.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record PasswordResetRequestRequest(
    @NotBlank @Email @Size(max = 254) String email) {

  public PasswordResetRequestRequest {
    if (email != null) {
      email = email.trim().toLowerCase(Locale.ROOT);
    }
  }
}
