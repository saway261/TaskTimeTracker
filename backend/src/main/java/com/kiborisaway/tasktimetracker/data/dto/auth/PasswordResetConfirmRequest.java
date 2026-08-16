package com.kiborisaway.tasktimetracker.data.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
    @NotBlank String token,
    @NotNull @Size(min = 12) String newPassword) {
}
