package com.kiborisaway.tasktimetracker.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.kiborisaway.tasktimetracker.validation.CreateGroup;
import com.kiborisaway.tasktimetracker.validation.UpdateGroup;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TaskTest {

  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void 登録時_titleにnullが渡されたときバリデーション違反になること() {
    // Arrange
    Task task = new Task();
    task.setTitle(null);
    task.setDescription("不正なタスク");
    task.setEstimatedMinutes(60);

    // Act
    Set<ConstraintViolation<Task>> violations = validator.validate(task, CreateGroup.class);

    // Assert
    assertThat(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("title")))
        .isTrue();
  }

  @ParameterizedTest(name = "[{index}]登録時_titleに{0}が渡されたときバリデーション違反になること")
  @ValueSource(strings = {"", " ", "　"})
// 空文字、半角スペース、全角スペース
  void 登録時_titleに有効な文字が渡されないときバリデーション違反になること(String invalidTitle) {
    // Arrange
    Task task = new Task();
    task.setTitle(invalidTitle);
    task.setDescription("不正なタスク");
    task.setEstimatedMinutes(60);

    // Act
    Set<ConstraintViolation<Task>> violations = validator.validate(task, CreateGroup.class);

    // Assert
    assertThat(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("title")))
        .isTrue();
  }

  @ParameterizedTest(name = "[{index}]登録時_フィールド:{0}に{1}文字渡されたとき violation={2}")
  @CsvSource({
      "title,19,false",
      "title,20,false",
      "title,21,true",
      "description,19,false",
      "description,20,false",
      "description,21,true",
  })
  void 登録時_文字列型フィールドの文字数境界値テスト(String fieldName, int length,
      boolean expectViolation) {
    // Arrange
    String testValue = "あ".repeat(length);
    Task task = new Task();
    task.setTitle(fieldName.equals("title") ? testValue : "タスクA");
    task.setDescription(fieldName.equals("description") ? testValue : "タスクの説明");
    task.setEstimatedMinutes(60);

    // Act
    Set<ConstraintViolation<Task>> violations = validator.validate(task, CreateGroup.class);

    // Assert
    if (expectViolation) {
      assertThat(violations).isNotEmpty();
      assertThat(violations)
          .anyMatch(v -> v.getPropertyPath().toString().equals(fieldName));
    } else {
      assertThat(violations).isEmpty();
    }
  }

  @Test
  void 登録時_estimatedMinutesにnullが渡されたときバリデーション違反になること() {
    // Arrange
    Task task = new Task();
    task.setTitle("タスクA");
    task.setDescription("不正なタスク");
    task.setEstimatedMinutes(null);

    // Act
    Set<ConstraintViolation<Task>> violations = validator.validate(task, CreateGroup.class);

    // Assert
    assertThat(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("estimatedMinutes")))
        .isTrue();
  }

  @ParameterizedTest(name = "[{index}]登録時_estimatedMinutesに{0}が渡されたときバリデーション違反になること")
  @ValueSource(ints = {0, -1})
  void 登録時_estimatedMinutesに0以下が渡されたときバリデーション違反になること(
      int invalidEstimatedMinutes) {
    // Arrange
    Task task = new Task();
    task.setTitle("タスクA");
    task.setDescription("不正なタスク");
    task.setEstimatedMinutes(invalidEstimatedMinutes);

    // Act
    Set<ConstraintViolation<Task>> violations = validator.validate(task, CreateGroup.class);

    // Assert
    assertThat(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("estimatedMinutes")))
        .isTrue();
  }

  @Test
  void 更新時_titleにnullが渡されたときバリデーション違反になること() {
    // Arrange
    Task task = new Task();
    task.setId(1);
    task.setTitle(null);
    task.setDescription("不正なタスク");
    task.setEstimatedMinutes(60);

    // Act
    Set<ConstraintViolation<Task>> violations = validator.validate(task, UpdateGroup.class);

    // Assert
    assertThat(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("title")))
        .isTrue();
  }

  @ParameterizedTest(name = "[{index}]更新時_titleに{0}が渡されたときバリデーション違反になること")
  @ValueSource(strings = {"", " ", "　"})
// 空文字、半角スペース、全角スペース
  void 更新時_titleに有効な文字が渡されないときバリデーション違反になること(String invalidTitle) {
    // Arrange
    Task task = new Task();
    task.setId(1);
    task.setTitle(invalidTitle);
    task.setDescription("不正なタスク");
    task.setEstimatedMinutes(60);

    // Act
    Set<ConstraintViolation<Task>> violations = validator.validate(task, UpdateGroup.class);

    // Assert
    assertThat(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("title")))
        .isTrue();
  }

  @ParameterizedTest(name = "[{index}]更新時_フィールド:{0}に{1}文字渡されたとき violation={2}")
  @CsvSource({
      "title,19,false",
      "title,20,false",
      "title,21,true",
      "description,19,false",
      "description,20,false",
      "description,21,true",
  })
  void 更新時_文字列型フィールドの文字数境界値テスト(String fieldName, int length,
      boolean expectViolation) {
    // Arrange
    String testValue = "あ".repeat(length);
    Task task = new Task();
    task.setId(1);
    task.setTitle(fieldName.equals("title") ? testValue : "タスクA");
    task.setDescription(fieldName.equals("description") ? testValue : "タスクの説明");
    task.setEstimatedMinutes(60);

    // Act
    Set<ConstraintViolation<Task>> violations = validator.validate(task, UpdateGroup.class);

    // Assert
    if (expectViolation) {
      assertThat(violations).isNotEmpty();
      assertThat(violations)
          .anyMatch(v -> v.getPropertyPath().toString().equals(fieldName));
    } else {
      assertThat(violations).isEmpty();
    }
  }

  @Test
  void 更新時_estimatedMinutesにnullが渡されたときバリデーション違反にならないこと() {
    // Arrange
    Task task = new Task();
    task.setId(1);
    task.setTitle("タスクA");
    task.setDescription("タスクの説明");
    task.setEstimatedMinutes(null);

    // Act
    Set<ConstraintViolation<Task>> violations = validator.validate(task, UpdateGroup.class);

    // Assert
    assertThat(violations).isEmpty();
  }

  @ParameterizedTest(name = "[{index}]更新時_estimatedMinutesに{0}が渡されたときバリデーション違反になること")
  @ValueSource(ints = {0, -1})
  void 更新時_estimatedMinutesに0以下が渡されたときバリデーション違反になること(
      int invalidEstimatedMinutes) {
    // Arrange
    Task task = new Task();
    task.setId(1);
    task.setTitle("タスクA");
    task.setDescription("不正なタスク");
    task.setEstimatedMinutes(invalidEstimatedMinutes);

    // Act
    Set<ConstraintViolation<Task>> violations = validator.validate(task, UpdateGroup.class);

    // Assert
    assertThat(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("estimatedMinutes")))
        .isTrue();
  }

}
