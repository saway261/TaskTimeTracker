package com.kiborisaway.tasktimetracker.data.dto.task;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TaskUpdateEstimatedMinutesRequestTest {

  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void estimatedMinutesにnullが渡されたときバリデーション違反になること() {
    // Arrange
    TaskUpdateEstimatedMinutesRequest request = new TaskUpdateEstimatedMinutesRequest();
    request.setEstimatedMinutes(null);

    // Act
    Set<ConstraintViolation<TaskUpdateEstimatedMinutesRequest>> violations = validator.validate(
        request);

    // Assert
    assertThat(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("estimatedMinutes")))
        .isTrue();
  }

  @ParameterizedTest(name = "[{index}]estimatedMinutesに{0}が渡されたときバリデーション違反になること")
  @ValueSource(ints = {0, -1})
  void estimatedMinutesに0以下が渡されたときバリデーション違反になること(int invalidEstimatedMinutes) {
    // Arrange
    TaskUpdateEstimatedMinutesRequest request = new TaskUpdateEstimatedMinutesRequest();
    request.setEstimatedMinutes(invalidEstimatedMinutes);

    // Act
    Set<ConstraintViolation<TaskUpdateEstimatedMinutesRequest>> violations = validator.validate(
        request);

    // Assert
    assertThat(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("estimatedMinutes")))
        .isTrue();
  }

  @Test
  void estimatedMinutesに正の値が渡されたときバリデーション違反にならないこと() {
    // Arrange
    TaskUpdateEstimatedMinutesRequest request = new TaskUpdateEstimatedMinutesRequest();
    request.setEstimatedMinutes(60);

    // Act
    Set<ConstraintViolation<TaskUpdateEstimatedMinutesRequest>> violations = validator.validate(
        request);

    // Assert
    assertThat(violations).isEmpty();
  }

}
