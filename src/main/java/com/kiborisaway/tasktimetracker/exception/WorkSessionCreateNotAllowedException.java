package com.kiborisaway.tasktimetracker.exception;

import lombok.Getter;

@Getter
public class WorkSessionCreateNotAllowedException extends RuntimeException {

  private String field;

  public WorkSessionCreateNotAllowedException(String field, String message) {
    super(message);
    this.field = field;
  }
}
