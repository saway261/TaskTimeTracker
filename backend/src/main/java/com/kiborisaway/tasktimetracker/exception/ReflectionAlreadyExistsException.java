package com.kiborisaway.tasktimetracker.exception;

import lombok.Getter;

@Getter
public class ReflectionAlreadyExistsException extends RuntimeException {

  private String field;

  public ReflectionAlreadyExistsException(String field, String message) {
    super(message);
    this.field = field;
  }
}
