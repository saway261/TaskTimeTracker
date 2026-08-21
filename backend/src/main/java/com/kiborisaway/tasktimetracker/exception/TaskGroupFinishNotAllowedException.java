package com.kiborisaway.tasktimetracker.exception;

import lombok.Getter;

@Getter
public class TaskGroupFinishNotAllowedException extends RuntimeException {

  private String field;

  public TaskGroupFinishNotAllowedException(String field, String message) {
    super(message);
    this.field = field;
  }
}
