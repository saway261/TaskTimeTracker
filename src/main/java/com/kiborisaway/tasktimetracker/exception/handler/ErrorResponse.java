package com.kiborisaway.tasktimetracker.exception.handler;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public class ErrorResponse {

  private final HttpStatus status;

  private final String message;

  private final List<Map<String, String>> errors;

}