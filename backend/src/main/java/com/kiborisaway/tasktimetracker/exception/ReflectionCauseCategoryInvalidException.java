package com.kiborisaway.tasktimetracker.exception;

import lombok.Getter;

@Getter
public class ReflectionCauseCategoryInvalidException extends RuntimeException {

  private String field;

  public ReflectionCauseCategoryInvalidException(String field, String message) {
    super(message);
    this.field = field;
  }
}
