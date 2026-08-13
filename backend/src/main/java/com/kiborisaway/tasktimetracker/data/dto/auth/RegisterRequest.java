package com.kiborisaway.tasktimetracker.data.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record RegisterRequest(
    @NotBlank @Email @Size(max = 254) String email,
    @NotNull @Size(min = 12) String password) {

  public RegisterRequest {
    if (email != null) {
      email = email.trim().toLowerCase(Locale.ROOT);
    }
  }
}
