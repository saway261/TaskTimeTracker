package com.kiborisaway.tasktimetracker.data.dto.memo;

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

class MemoRequestTest {

  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void commentにnullが渡されたときバリデーション違反になること() {
    // Arrange
    MemoRequest request = new MemoRequest();
    request.setComment(null);

    // Act
    Set<ConstraintViolation<MemoRequest>> violations = validator.validate(request);

    // Assert
    assertThat(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("comment")))
        .isTrue();
  }

  @ParameterizedTest(name = "[{index}]commentに{0}が渡されたときバリデーション違反になること")
  @ValueSource(strings = {"", " ", "　"})
// 空文字、半角スペース、全角スペース
  void commentに有効な文字が渡されないときバリデーション違反になること(String invalidComment) {
    // Arrange
    MemoRequest request = new MemoRequest();
    request.setComment(invalidComment);

    // Act
    Set<ConstraintViolation<MemoRequest>> violations = validator.validate(request);

    // Assert
    assertThat(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("comment")))
        .isTrue();
  }

  @ParameterizedTest(name = "[{index}]commentに{0}文字渡されたとき violation={1}")
  @CsvSource({
      "999,false",
      "1000,false",
      "1001,true",
  })
  void commentの文字数境界値テスト(int length, boolean expectViolation) {
    // Arrange
    MemoRequest request = new MemoRequest();
    request.setComment("あ".repeat(length));

    // Act
    Set<ConstraintViolation<MemoRequest>> violations = validator.validate(request);

    // Assert
    if (expectViolation) {
      assertThat(violations).isNotEmpty();
      assertThat(violations)
          .anyMatch(v -> v.getPropertyPath().toString().equals("comment"));
    } else {
      assertThat(violations).isEmpty();
    }
  }
}
