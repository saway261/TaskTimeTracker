package com.kiborisaway.tasktimetracker.exception;

public class PasswordChangeNotAllowedException extends RuntimeException {

  public PasswordChangeNotAllowedException(String message) {
    super(message);
  }

  public static PasswordChangeNotAllowedException currentPasswordIncorrect() {
    return new PasswordChangeNotAllowedException("current password is incorrect");
  }

  public static PasswordChangeNotAllowedException newPasswordUnchanged() {
    return new PasswordChangeNotAllowedException("new password must be different");
  }
}
