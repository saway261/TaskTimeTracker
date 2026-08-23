package com.kiborisaway.tasktimetracker.data.dto.auth;

import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.security.AuthenticatedUser;

public record AuthenticatedUserResponse(
    int id,
    String email,
    boolean passwordChangeRequired,
    boolean emailVerified,
    boolean onboardingCompleted) {

  public AuthenticatedUserResponse(AuthenticatedUser user) {
    this(user.getUserId(), user.getEmail(), user.isPasswordChangeRequired(),
        user.isEmailVerified(), user.isOnboardingCompleted());
  }

  public AuthenticatedUserResponse(AppUser user) {
    this(user.getId(), user.getEmail(),
        Boolean.TRUE.equals(user.getPasswordChangeRequired()),
        user.getEmailVerifiedAt() != null,
        Boolean.TRUE.equals(user.getOnboardingCompleted()));
  }
}
