package com.kiborisaway.tasktimetracker.data.dto.auth;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
    @NotNull String currentPassword,
    @NotNull @Size(min = 12) String newPassword) {
}
