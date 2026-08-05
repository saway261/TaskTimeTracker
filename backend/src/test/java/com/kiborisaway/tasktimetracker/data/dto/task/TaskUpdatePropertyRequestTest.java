package com.kiborisaway.tasktimetracker.data.dto.task;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TaskUpdatePropertyRequestTest {

  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void titleにnullが渡されたときバリデーション違反になること() {
    // Arrange
    TaskUpdatePropertyRequest request = new TaskUpdatePropertyRequest();
    request.setTitle(null);
    request.setDescription("不正なタスク");

    // Act
    Set<ConstraintViolation<TaskUpdatePropertyRequest>> violations = validator.validate(request);

    // Assert
    assertThat(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("title")))
        .isTrue();
  }

  @ParameterizedTest(name = "[{index}]titleに{0}が渡されたときバリデーション違反になること")
  @ValueSource(strings = {"", " ", "　"})
// 空文字、半角スペース、全角スペース
  void titleに有効な文字が渡されないときバリデーション違反になること(String invalidTitle) {
    // Arrange
    TaskUpdatePropertyRequest request = new TaskUpdatePropertyRequest();
    request.setTitle(invalidTitle);
    request.setDescription("不正なタスク");

    // Act
    Set<ConstraintViolation<TaskUpdatePropertyRequest>> violations = validator.validate(request);

    // Assert
    assertThat(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("title")))
        .isTrue();
  }

  @ParameterizedTest(name = "[{index}]フィールド:{0}に{1}文字渡されたとき violation={2}")
  @CsvSource({
      "title,19,false",
      "title,20,false",
      "title,21,true",
      "description,199,false",
      "description,200,false",
      "description,201,true",
  })
  void 文字列型フィールドの文字数境界値テスト(String fieldName, int length, boolean expectViolation) {
    // Arrange
    String testValue = "あ".repeat(length);
    TaskUpdatePropertyRequest request = new TaskUpdatePropertyRequest();
    request.setTitle(fieldName.equals("title") ? testValue : "タスクA");
    request.setDescription(fieldName.equals("description") ? testValue : "タスクの説明");

    // Act
    Set<ConstraintViolation<TaskUpdatePropertyRequest>> violations = validator.validate(request);

    // Assert
    if (expectViolation) {
      assertThat(violations).isNotEmpty();
      assertThat(violations)
          .anyMatch(v -> v.getPropertyPath().toString().equals(fieldName));
    } else {
      assertThat(violations).isEmpty();
    }
  }

}
