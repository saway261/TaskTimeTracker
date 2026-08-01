package com.kiborisaway.tasktimetracker.exception;

import lombok.Getter;

@Getter
public class EstimateMinutesUpdateNotAllowedException extends RuntimeException {

  private String field;

  public EstimateMinutesUpdateNotAllowedException(String field, String message) {
    super(message);
    this.field = field;
  }
}
