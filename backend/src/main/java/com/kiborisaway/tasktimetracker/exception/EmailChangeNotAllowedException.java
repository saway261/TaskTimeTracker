package com.kiborisaway.tasktimetracker.exception;

public class EmailChangeNotAllowedException extends RuntimeException {

  public EmailChangeNotAllowedException(String message) {
    super(message);
  }

  public static EmailChangeNotAllowedException currentPasswordIncorrect() {
    return new EmailChangeNotAllowedException("current password is incorrect");
  }

  public static EmailChangeNotAllowedException sameAsCurrentEmail() {
    return new EmailChangeNotAllowedException("new email must be different");
  }
}
