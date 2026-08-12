package com.kiborisaway.tasktimetracker.exception;

import lombok.Getter;

@Getter
public class InvalidItemOrderException extends RuntimeException {

  private String field;

  public InvalidItemOrderException(String field, String message) {
    super(message);
    this.field = field;
  }
}
