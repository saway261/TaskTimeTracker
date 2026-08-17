package com.kiborisaway.tasktimetracker.data.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record EmailChangeConfirmRequest(@NotBlank String token) {
}
