package com.kiborisaway.tasktimetracker.exception;

public class EmailChangeRequestInvalidException extends RuntimeException {

  public EmailChangeRequestInvalidException() {
    super("email change request is invalid or expired");
  }
}
