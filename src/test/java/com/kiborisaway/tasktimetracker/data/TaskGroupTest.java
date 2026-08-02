package com.kiborisaway.tasktimetracker.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.kiborisaway.tasktimetracker.data.entity.TaskGroup;
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

class TaskGroupTest {

  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void 登録時_titleにnullが渡されたときバリデーション違反になること() {
    // Arrange
    TaskGroup tg = new TaskGroup();
    tg.setTitle(null);
    tg.setDescription("不正なタスクグループ");

    // Act
    Set<ConstraintViolation<TaskGroup>> violations = validator.validate(tg, CreateGroup.class);

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
    TaskGroup tg = new TaskGroup();
    tg.setTitle(invalidTitle);
    tg.setDescription("不正なタスクグループ");

    // Act
    Set<ConstraintViolation<TaskGroup>> violations = validator.validate(tg, CreateGroup.class);

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
      "description,199,false",
      "description,200,false",
      "description,201,true",
  })
  void 登録時_文字列型フィールドの文字数境界値テスト(String fieldName, int length,
      boolean expectViolation) {
    // Arrange
    String testValue = "あ".repeat(length);
    TaskGroup tg = new TaskGroup();
    tg.setTitle(fieldName.equals("title") ? testValue : "タスクグループA");
    tg.setDescription(
        fieldName.equals("description") ? testValue : "タスクグループの説明");

    // Act
    Set<ConstraintViolation<TaskGroup>> violations = validator.validate(tg, CreateGroup.class);

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
  void 更新時_titleにnullが渡されたときバリデーション違反になること() {
    // Arrange
    TaskGroup tg = new TaskGroup();
    tg.setId(1);
    tg.setTitle(null);
    tg.setDescription("不正なタスクグループ");
    tg.setIsFinished(false);

    // Act
    Set<ConstraintViolation<TaskGroup>> violations = validator.validate(tg, UpdateGroup.class);

    // Assert
    assertThat(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("title")))
        .isTrue();
  }

  @Test
  void 更新時_isFinishedにnullが渡されたときバリデーション違反になること() {
    // Arrange
    TaskGroup tg = new TaskGroup();
    tg.setId(1);
    tg.setTitle("タイトル");
    tg.setDescription("不正なタスクグループ");
    tg.setIsFinished(null);

    // Act
    Set<ConstraintViolation<TaskGroup>> violations = validator.validate(tg, UpdateGroup.class);

    // Assert
    assertThat(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("isFinished")))
        .isTrue();
  }

  @ParameterizedTest(name = "[{index}]更新時_titleに{0}が渡されたときバリデーション違反になること")
  @ValueSource(strings = {"", " ", "　"})
    // 空文字、半角スペース、全角スペース
  void 更新時_titleに有効な文字が渡されないときバリデーション違反になること(String invalidTitle) {
    // Arrange
    TaskGroup tg = new TaskGroup();
    tg.setId(1);
    tg.setTitle(invalidTitle);
    tg.setDescription("不正なタスクグループ");
    tg.setIsFinished(false);

    // Act
    Set<ConstraintViolation<TaskGroup>> violations = validator.validate(tg, UpdateGroup.class);

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
      "description,199,false",
      "description,200,false",
      "description,201,true",
  })
  void 更新時_文字列型フィールドの文字数境界値テスト(String fieldName, int length,
      boolean expectViolation) {
    // Arrange
    String testValue = "あ".repeat(length);
    TaskGroup tg = new TaskGroup();
    tg.setId(1);
    tg.setTitle(fieldName.equals("title") ? testValue : "タスクグループA");
    tg.setDescription(
        fieldName.equals("description") ? testValue : "A社から受託したタスクグループ");
    tg.setIsFinished(false);

    // Act
    Set<ConstraintViolation<TaskGroup>> violations = validator.validate(tg, UpdateGroup.class);

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