package com.kiborisaway.tasktimetracker.data.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Locale;

public record LoginRequest(
    @NotBlank @Email String email,
    @NotNull String password) {

  public LoginRequest {
    if (email != null) {
      email = email.trim().toLowerCase(Locale.ROOT);
    }
  }
}
