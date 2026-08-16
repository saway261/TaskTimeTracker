package com.kiborisaway.tasktimetracker.data.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record EmailChangeRequestBody(
    @NotBlank @Email @Size(max = 254) String newEmail,
    @NotNull String currentPassword) {

  public EmailChangeRequestBody {
    if (newEmail != null) {
      newEmail = newEmail.trim().toLowerCase(Locale.ROOT);
    }
  }
}
