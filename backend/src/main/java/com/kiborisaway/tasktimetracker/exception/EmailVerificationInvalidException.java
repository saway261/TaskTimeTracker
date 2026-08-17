package com.kiborisaway.tasktimetracker.exception;

public class EmailVerificationInvalidException extends RuntimeException {

  public EmailVerificationInvalidException() {
    super("email verification request is invalid or expired");
  }
}
