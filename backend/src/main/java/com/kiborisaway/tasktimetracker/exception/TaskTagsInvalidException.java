package com.kiborisaway.tasktimetracker.exception;

import lombok.Getter;

@Getter
public class TaskTagsInvalidException extends RuntimeException {

  private String field;

  public TaskTagsInvalidException(String field, String message) {
    super(message);
    this.field = field;
  }
}
