package com.kiborisaway.tasktimetracker.data.dto.work_session;

import static org.assertj.core.api.Assertions.assertThat;

import com.kiborisaway.tasktimetracker.data.entity.WorkSessionType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WorkSessionUpdateRequestTest {

  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void typeにnullが渡されたときバリデーション違反になること() {
    // Arrange
    WorkSessionUpdateRequest request = new WorkSessionUpdateRequest();
    request.setType(null);

    // Act
    Set<ConstraintViolation<WorkSessionUpdateRequest>> violations = validator.validate(request);

    // Assert
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("type"));
  }

  @Test
  void TIMERでstartedAtがnullのときバリデーション違反になること() {
    // Arrange
    WorkSessionUpdateRequest request = new WorkSessionUpdateRequest();
    request.setType(WorkSessionType.TIMER);
    request.setEndedAt(LocalDateTime.of(2026, 1, 1, 10, 0));

    // Act
    Set<ConstraintViolation<WorkSessionUpdateRequest>> violations = validator.validate(request);

    // Assert
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("validTimer"));
  }

  @Test
  void TIMERでendedAtがnullのときバリデーション違反になること() {
    // Arrange
    WorkSessionUpdateRequest request = new WorkSessionUpdateRequest();
    request.setType(WorkSessionType.TIMER);
    request.setStartedAt(LocalDateTime.of(2026, 1, 1, 9, 0));

    // Act
    Set<ConstraintViolation<WorkSessionUpdateRequest>> violations = validator.validate(request);

    // Assert
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("validTimer"));
  }

  @Test
  void TIMERでstartedAtとendedAtがあるときバリデーション違反にならないこと() {
    // Arrange
    WorkSessionUpdateRequest request = new WorkSessionUpdateRequest();
    request.setType(WorkSessionType.TIMER);
    request.setStartedAt(LocalDateTime.of(2026, 1, 1, 9, 0));
    request.setEndedAt(LocalDateTime.of(2026, 1, 1, 10, 0));

    // Act
    Set<ConstraintViolation<WorkSessionUpdateRequest>> violations = validator.validate(request);

    // Assert
    assertThat(violations).isEmpty();
  }

  @Test
  void MANUALでminutesがnullのときバリデーション違反になること() {
    // Arrange
    WorkSessionUpdateRequest request = new WorkSessionUpdateRequest();
    request.setType(WorkSessionType.MANUAL);
    request.setMinutes(null);

    // Act
    Set<ConstraintViolation<WorkSessionUpdateRequest>> violations = validator.validate(request);

    // Assert
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("validManual"));
  }

  @Test
  void MANUALでminutesがあるときバリデーション違反にならないこと() {
    // Arrange
    WorkSessionUpdateRequest request = new WorkSessionUpdateRequest();
    request.setType(WorkSessionType.MANUAL);
    request.setMinutes(30);

    // Act
    Set<ConstraintViolation<WorkSessionUpdateRequest>> violations = validator.validate(request);

    // Assert
    assertThat(violations).isEmpty();
  }

}
