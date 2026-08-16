package com.kiborisaway.tasktimetracker.data.dto.reflection;

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

class ReflectionRequestTest {

  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void causeがnullの場合はバリデーション違反になること() {
    ReflectionRequest request = request(null, null);

    Set<ConstraintViolation<ReflectionRequest>> violations = validator.validate(request);

    assertThat(violations)
        .anyMatch(violation -> violation.getPropertyPath().toString().equals("cause"));
  }

  @ParameterizedTest(name = "[{index}]causeに有効な文字がない場合はバリデーション違反になること: {0}")
  @ValueSource(strings = {"", " ", "　"})
  void causeが空白文字だけの場合はバリデーション違反になること(String cause) {
    ReflectionRequest request = request(cause, null);

    Set<ConstraintViolation<ReflectionRequest>> violations = validator.validate(request);

    assertThat(violations)
        .anyMatch(violation -> violation.getPropertyPath().toString().equals("cause"));
  }

  @ParameterizedTest(name = "[{index}]causeが{0}文字の場合 violation={1}")
  @CsvSource({
      "199,false",
      "200,false",
      "201,true"
  })
  void causeの文字数境界値テスト(int length, boolean expectViolation) {
    ReflectionRequest request = request("あ".repeat(length), null);

    Set<ConstraintViolation<ReflectionRequest>> violations = validator.validate(request);

    assertThat(hasViolation(violations, "cause")).isEqualTo(expectViolation);
  }

  @Test
  void nextActionがnullまたは空白文字だけの場合はバリデーション違反にならないこと() {
    assertThat(validator.validate(request("原因", null))).isEmpty();
    assertThat(validator.validate(request("原因", "   "))).isEmpty();
  }

  @ParameterizedTest(name = "[{index}]nextActionが{0}文字の場合 violation={1}")
  @CsvSource({
      "999,false",
      "1000,false",
      "1001,true"
  })
  void nextActionの文字数境界値テスト(int length, boolean expectViolation) {
    ReflectionRequest request = request("原因", "あ".repeat(length));

    Set<ConstraintViolation<ReflectionRequest>> violations = validator.validate(request);

    assertThat(hasViolation(violations, "nextAction")).isEqualTo(expectViolation);
  }

  private static ReflectionRequest request(String cause, String nextAction) {
    ReflectionRequest request = new ReflectionRequest();
    request.setCause(cause);
    request.setNextAction(nextAction);
    return request;
  }

  private static boolean hasViolation(
      Set<ConstraintViolation<ReflectionRequest>> violations, String field) {
    return violations.stream()
        .anyMatch(violation -> violation.getPropertyPath().toString().equals(field));
  }
}
