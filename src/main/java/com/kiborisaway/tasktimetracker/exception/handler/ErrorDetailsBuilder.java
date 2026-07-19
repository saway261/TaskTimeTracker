package com.kiborisaway.tasktimetracker.exception.handler;

import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.TypeMismatchException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;

@Component
public class ErrorDetailsBuilder {

  /**
   * MethodArgumentNotValidExceptionを受け取り、すべてのエラー発生個所とエラーメッセージをリストで返します。
   *
   * @param ex MethodArgumentNotValidException
   * @return エラー発生個所とエラーメッセージ
   */
  public List<Map<String, String>> buildErrorDetails(MethodArgumentNotValidException ex) {
    List<Map<String, String>> errors = new ArrayList<>();

    ex.getBindingResult().getFieldErrors().forEach(fieldError -> {
      Map<String, String> error = new HashMap<>();
      error.put("field", fieldError.getField());
      error.put("message", fieldError.getDefaultMessage());
      errors.add(error);
    });
    return errors;
  }

  /**
   * ConstraintViolationExceptionを受け取り、すべてのエラー発生個所とエラーメッセージをリストで返します。
   *
   * @param ex ConstraintViolationException
   * @return エラー発生個所とエラーメッセージ
   */
  public List<Map<String, String>> buildErrorDetails(ConstraintViolationException ex) {
    List<Map<String, String>> errors = new ArrayList<>();

    ex.getConstraintViolations().forEach(violation -> {
      Map<String, String> error = new HashMap<>();
      error.put("field", violation.getPropertyPath().toString());
      error.put("message", violation.getMessage());
      errors.add(error);
    });
    return errors;
  }

  /**
   * TypeMismatchExceptionを受け取り、エラー発生個所とエラーメッセージをリストで返します。
   *
   * @param ex TypeMismatchException
   * @return エラー発生個所とエラーメッセージ
   */
  public List<Map<String, String>> buildErrorDetails(TypeMismatchException ex) {
    List<Map<String, String>> errors = new ArrayList<>();
    Map<String, String> error = new HashMap<>();

    String fieldName = ex.getPropertyName() != null ? ex.getPropertyName() : "unknown";
    String requiredType =
        ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown type";
    String message = String.format("値を %s 型に変換できませんでした", requiredType);

    error.put("field", fieldName);
    error.put("message", message);
    errors.add(error);
    return errors;
  }

  /**
   * TargetNotFoundExceptionを受け取り、エラー発生個所とエラーメッセージをリストで返します。
   *
   * @param ex TargetNotFoundException
   * @return エラー発生個所とエラーメッセージ
   */
  public List<Map<String, String>> buildErrorDetails(TargetNotFoundException ex) {
    List<Map<String, String>> errors = new ArrayList<>();

    Map<String, String> error = new HashMap<>();
    error.put("field", ex.getField());
    error.put("message", ex.getMessage());
    errors.add(error);

    return errors;
  }

}
