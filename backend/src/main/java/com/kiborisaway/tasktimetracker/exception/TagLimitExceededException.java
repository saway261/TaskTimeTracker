package com.kiborisaway.tasktimetracker.exception;

import lombok.Getter;

@Getter
public class TagLimitExceededException extends RuntimeException {

  private String field;

  public TagLimitExceededException(String field, String message) {
    super(message);
    this.field = field;
  }
}
