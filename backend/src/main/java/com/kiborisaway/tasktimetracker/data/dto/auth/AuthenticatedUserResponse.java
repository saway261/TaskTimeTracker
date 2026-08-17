package com.kiborisaway.tasktimetracker.data.dto.auth;

import com.kiborisaway.tasktimetracker.security.AuthenticatedUser;

public record AuthenticatedUserResponse(
    int id,
    String email,
    boolean passwordChangeRequired,
    boolean emailVerified) {

  public AuthenticatedUserResponse(AuthenticatedUser user) {
    this(user.getUserId(), user.getEmail(), user.isPasswordChangeRequired(),
        user.isEmailVerified());
  }
}
