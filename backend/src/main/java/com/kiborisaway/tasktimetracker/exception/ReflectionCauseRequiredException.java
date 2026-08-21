package com.kiborisaway.tasktimetracker.exception;

import lombok.Getter;

@Getter
public class ReflectionCauseRequiredException extends RuntimeException {

  private String field;

  public ReflectionCauseRequiredException(String field, String message) {
    super(message);
    this.field = field;
  }
}
