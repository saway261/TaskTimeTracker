package com.kiborisaway.tasktimetracker.exception;

public class PasswordResetInvalidException extends RuntimeException {

  public PasswordResetInvalidException() {
    super("password reset request is invalid or expired");
  }
}
