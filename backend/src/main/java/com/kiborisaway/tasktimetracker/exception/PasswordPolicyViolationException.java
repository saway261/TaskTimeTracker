package com.kiborisaway.tasktimetracker.exception;

public class PasswordPolicyViolationException extends RuntimeException {

  public PasswordPolicyViolationException() {
    super("password does not meet policy");
  }
}
