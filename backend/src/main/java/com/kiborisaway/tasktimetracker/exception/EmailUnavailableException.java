package com.kiborisaway.tasktimetracker.exception;

public class EmailUnavailableException extends RuntimeException {

  public EmailUnavailableException() {
    super("email is unavailable");
  }
}
