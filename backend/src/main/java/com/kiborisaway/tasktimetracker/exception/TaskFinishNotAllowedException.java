package com.kiborisaway.tasktimetracker.exception;

import lombok.Getter;

@Getter
public class TaskFinishNotAllowedException extends RuntimeException {

  private String field;

  public TaskFinishNotAllowedException(String field, String message) {
    super(message);
    this.field = field;
  }
}
