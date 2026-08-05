package com.kiborisaway.tasktimetracker.exception;

import lombok.Getter;

@Getter
public class TargetNotFoundException extends RuntimeException {

  private String field;

  public TargetNotFoundException(String field, String message) {
    super(message);
    this.field = field;
  }
}
