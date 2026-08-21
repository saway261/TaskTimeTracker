package com.kiborisaway.tasktimetracker.exception;

import lombok.Getter;

@Getter
public class TagNameDuplicateException extends RuntimeException {

  private String field;

  public TagNameDuplicateException(String field, String message) {
    super(message);
    this.field = field;
  }
}
