package com.kiborisaway.tasktimetracker.exception;

import lombok.Getter;

@Getter
public class WorkSessionOperationNotAllowedException extends RuntimeException {

  private String field;

  public WorkSessionOperationNotAllowedException(String field, String message) {
    super(message);
    this.field = field;
  }
}
