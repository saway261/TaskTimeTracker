package com.kiborisaway.tasktimetracker.data.dto.reflection;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
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
  void causeCategoryCodesがnullの場合はバリデーション違反になること() {
    ReflectionRequest request = request(null, "原因", null);

    Set<ConstraintViolation<ReflectionRequest>> violations = validator.validate(request);

    assertThat(hasViolation(violations, "causeCategoryCodes")).isTrue();
  }

  @Test
  void causeCategoryCodesが空リストの場合はバリデーション違反になること() {
    ReflectionRequest request = request(List.of(), "原因", null);

    Set<ConstraintViolation<ReflectionRequest>> violations = validator.validate(request);

    assertThat(hasViolation(violations, "causeCategoryCodes")).isTrue();
  }

  @ParameterizedTest(name = "[{index}]causeCategoryCodesが{0}件の場合はバリデーション違反にならないこと")
  @CsvSource({"1", "2", "3"})
  void causeCategoryCodesの件数境界値テスト_有効範囲(int size) {
    ReflectionRequest request = request(codes(size), "原因", null);

    Set<ConstraintViolation<ReflectionRequest>> violations = validator.validate(request);

    assertThat(hasViolation(violations, "causeCategoryCodes")).isFalse();
  }

  @Test
  void causeCategoryCodesが4件の場合はバリデーション違反になること() {
    ReflectionRequest request = request(codes(4), "原因", null);

    Set<ConstraintViolation<ReflectionRequest>> violations = validator.validate(request);

    assertThat(hasViolation(violations, "causeCategoryCodes")).isTrue();
  }

  @Test
  void causeCategoryCodesに重複がある場合はバリデーション違反になること() {
    ReflectionRequest request =
        request(List.of("TASK_BREAKDOWN", "TASK_BREAKDOWN"), "原因", null);

    Set<ConstraintViolation<ReflectionRequest>> violations = validator.validate(request);

    assertThat(hasViolation(violations, "causeCategoryCodes")).isTrue();
  }

  @ParameterizedTest(name = "[{index}]causeCategoryCodesの要素に有効な文字がない場合はバリデーション違反になること: {0}")
  @ValueSource(strings = {"", " ", "　"})
  void causeCategoryCodesの要素が空白文字だけの場合はバリデーション違反になること(String blank) {
    ReflectionRequest request = request(List.of(blank), "原因", null);

    Set<ConstraintViolation<ReflectionRequest>> violations = validator.validate(request);

    assertThat(violations).isNotEmpty();
  }

  @ParameterizedTest(name = "[{index}]causeCategoryCodesの要素が{0}文字の場合 violation={1}")
  @CsvSource({
      "39,false",
      "40,false",
      "41,true"
  })
  void causeCategoryCodesの要素の文字数境界値テスト(int length, boolean expectViolation) {
    ReflectionRequest request = request(List.of("A".repeat(length)), "原因", null);

    Set<ConstraintViolation<ReflectionRequest>> violations = validator.validate(request);

    assertThat(violations.isEmpty()).isEqualTo(!expectViolation);
  }

  @Test
  void causeがnullでもバリデーション違反にならないこと() {
    ReflectionRequest request = request(List.of("TASK_BREAKDOWN"), null, null);

    Set<ConstraintViolation<ReflectionRequest>> violations = validator.validate(request);

    assertThat(hasViolation(violations, "cause")).isFalse();
  }

  @ParameterizedTest(name = "[{index}]causeが空白文字だけでもバリデーション違反にならないこと: {0}")
  @ValueSource(strings = {"", " ", "　"})
  void causeが空白文字だけでもバリデーション違反にならないこと(String cause) {
    ReflectionRequest request = request(List.of("TASK_BREAKDOWN"), cause, null);

    Set<ConstraintViolation<ReflectionRequest>> violations = validator.validate(request);

    assertThat(hasViolation(violations, "cause")).isFalse();
  }

  @ParameterizedTest(name = "[{index}]causeが{0}文字の場合 violation={1}")
  @CsvSource({
      "199,false",
      "200,false",
      "201,true"
  })
  void causeの文字数境界値テスト(int length, boolean expectViolation) {
    ReflectionRequest request = request(List.of("TASK_BREAKDOWN"), "あ".repeat(length), null);

    Set<ConstraintViolation<ReflectionRequest>> violations = validator.validate(request);

    assertThat(hasViolation(violations, "cause")).isEqualTo(expectViolation);
  }

  @Test
  void nextActionがnullまたは空白文字だけの場合はバリデーション違反にならないこと() {
    assertThat(validator.validate(request(List.of("TASK_BREAKDOWN"), "原因", null))).isEmpty();
    assertThat(validator.validate(request(List.of("TASK_BREAKDOWN"), "原因", "   "))).isEmpty();
  }

  @ParameterizedTest(name = "[{index}]nextActionが{0}文字の場合 violation={1}")
  @CsvSource({
      "999,false",
      "1000,false",
      "1001,true"
  })
  void nextActionの文字数境界値テスト(int length, boolean expectViolation) {
    ReflectionRequest request =
        request(List.of("TASK_BREAKDOWN"), "原因", "あ".repeat(length));

    Set<ConstraintViolation<ReflectionRequest>> violations = validator.validate(request);

    assertThat(hasViolation(violations, "nextAction")).isEqualTo(expectViolation);
  }

  private static List<String> codes(int size) {
    return java.util.stream.IntStream.range(0, size)
        .mapToObj(i -> "CODE_" + i)
        .toList();
  }

  private static ReflectionRequest request(
      List<String> causeCategoryCodes, String cause, String nextAction) {
    ReflectionRequest request = new ReflectionRequest();
    request.setCauseCategoryCodes(causeCategoryCodes);
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
