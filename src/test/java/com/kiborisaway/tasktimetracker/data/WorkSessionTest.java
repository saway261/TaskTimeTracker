package com.kiborisaway.tasktimetracker.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.kiborisaway.tasktimetracker.data.entity.WorkSession;
import com.kiborisaway.tasktimetracker.data.entity.WorkSessionType;
import com.kiborisaway.tasktimetracker.validation.CreateGroup;
import com.kiborisaway.tasktimetracker.validation.UpdateGroup;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WorkSessionTest {

  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void 登録時_typeにnullが渡されたときバリデーション違反になること() {
    // Arrange
    WorkSession ws = new WorkSession();
    ws.setType(null);

    // Act
    Set<ConstraintViolation<WorkSession>> violations = validator.validate(ws, CreateGroup.class);

    // Assert
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("type"));
  }

  @Test
  void 更新時_typeにnullが渡されたときバリデーション違反になること() {
    // Arrange
    WorkSession ws = new WorkSession();
    ws.setType(null);

    // Act
    Set<ConstraintViolation<WorkSession>> violations = validator.validate(ws, UpdateGroup.class);

    // Assert
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("type"));
  }

  @Test
  void 登録時_TIMERでstartedAtがnullのときバリデーション違反になること() {
    // Arrange
    WorkSession ws = new WorkSession();
    ws.setType(WorkSessionType.TIMER);
    ws.setStartedAt(null);

    // Act
    Set<ConstraintViolation<WorkSession>> violations = validator.validate(ws, CreateGroup.class);

    // Assert
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("validCreateTimer"));
  }

  @Test
  void 登録時_TIMERでstartedAtがあるときバリデーション違反にならないこと() {
    // Arrange
    WorkSession ws = new WorkSession();
    ws.setType(WorkSessionType.TIMER);
    ws.setStartedAt(LocalDateTime.of(2026, 1, 1, 9, 0));

    // Act
    Set<ConstraintViolation<WorkSession>> violations = validator.validate(ws, CreateGroup.class);

    // Assert
    assertThat(violations).isEmpty();
  }

  @Test
  void 登録時_MANUALでminutesがnullのときバリデーション違反になること() {
    // Arrange
    WorkSession ws = new WorkSession();
    ws.setType(WorkSessionType.MANUAL);
    ws.setMinutes(null);

    // Act
    Set<ConstraintViolation<WorkSession>> violations = validator.validate(ws, CreateGroup.class);

    // Assert
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("validCreateManual"));
  }

  @Test
  void 登録時_MANUALでminutesがあるときバリデーション違反にならないこと() {
    // Arrange
    WorkSession ws = new WorkSession();
    ws.setType(WorkSessionType.MANUAL);
    ws.setMinutes(30);

    // Act
    Set<ConstraintViolation<WorkSession>> violations = validator.validate(ws, CreateGroup.class);

    // Assert
    assertThat(violations).isEmpty();
  }

  @Test
  void 更新時_TIMERでstartedAtがnullのときバリデーション違反になること() {
    // Arrange
    WorkSession ws = new WorkSession();
    ws.setType(WorkSessionType.TIMER);
    ws.setEndedAt(LocalDateTime.of(2026, 1, 1, 10, 0));

    // Act
    Set<ConstraintViolation<WorkSession>> violations = validator.validate(ws, UpdateGroup.class);

    // Assert
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("validUpdateTimer"));
  }

  @Test
  void 更新時_TIMERでendedAtがnullのときバリデーション違反になること() {
    // Arrange
    WorkSession ws = new WorkSession();
    ws.setType(WorkSessionType.TIMER);
    ws.setStartedAt(LocalDateTime.of(2026, 1, 1, 9, 0));

    // Act
    Set<ConstraintViolation<WorkSession>> violations = validator.validate(ws, UpdateGroup.class);

    // Assert
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("validUpdateTimer"));
  }

  @Test
  void 更新時_TIMERでstartedAtとendedAtがあるときバリデーション違反にならないこと() {
    // Arrange
    WorkSession ws = new WorkSession();
    ws.setType(WorkSessionType.TIMER);
    ws.setStartedAt(LocalDateTime.of(2026, 1, 1, 9, 0));
    ws.setEndedAt(LocalDateTime.of(2026, 1, 1, 10, 0));

    // Act
    Set<ConstraintViolation<WorkSession>> violations = validator.validate(ws, UpdateGroup.class);

    // Assert
    assertThat(violations).isEmpty();
  }

  @Test
  void 更新時_MANUALでminutesがnullのときバリデーション違反になること() {
    // Arrange
    WorkSession ws = new WorkSession();
    ws.setType(WorkSessionType.MANUAL);
    ws.setMinutes(null);

    // Act
    Set<ConstraintViolation<WorkSession>> violations = validator.validate(ws, UpdateGroup.class);

    // Assert
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("validUpdateManual"));
  }

  @Test
  void 更新時_MANUALでminutesがあるときバリデーション違反にならないこと() {
    // Arrange
    WorkSession ws = new WorkSession();
    ws.setType(WorkSessionType.MANUAL);
    ws.setMinutes(30);

    // Act
    Set<ConstraintViolation<WorkSession>> violations = validator.validate(ws, UpdateGroup.class);

    // Assert
    assertThat(violations).isEmpty();
  }

}
