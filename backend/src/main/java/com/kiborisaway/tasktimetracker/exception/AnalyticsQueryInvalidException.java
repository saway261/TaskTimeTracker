package com.kiborisaway.tasktimetracker.exception;

import lombok.Getter;

@Getter
public class AnalyticsQueryInvalidException extends RuntimeException {

  private String field;

  public AnalyticsQueryInvalidException(String field, String message) {
    super(message);
    this.field = field;
  }
}
