package com.kiborisaway.tasktimetracker.exception;

import lombok.Getter;

@Getter
public class ProjectFinishNotAllowedException extends RuntimeException {

  private String field;

  public ProjectFinishNotAllowedException(String field, String message) {
    super(message);
    this.field = field;
  }
}
