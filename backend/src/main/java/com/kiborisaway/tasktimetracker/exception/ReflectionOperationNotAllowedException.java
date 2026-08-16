package com.kiborisaway.tasktimetracker.exception;

import lombok.Getter;

@Getter
public class ReflectionOperationNotAllowedException extends RuntimeException {

  private String field;

  public ReflectionOperationNotAllowedException(String field, String message) {
    super(message);
    this.field = field;
  }
}
