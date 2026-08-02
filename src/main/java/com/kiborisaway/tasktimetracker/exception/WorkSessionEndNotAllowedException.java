package com.kiborisaway.tasktimetracker.exception;

import lombok.Getter;

@Getter
public class WorkSessionEndNotAllowedException extends RuntimeException {

  private String field;

  public WorkSessionEndNotAllowedException(String field, String message) {
    super(message);
    this.field = field;
  }
}
