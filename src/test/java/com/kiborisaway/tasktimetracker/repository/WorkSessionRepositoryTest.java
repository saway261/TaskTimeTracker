package com.kiborisaway.tasktimetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

@MybatisTest
class WorkSessionRepositoryTest {

  @Autowired
  private WorkSessionRepository sut;

  @Test
  void タスクの作業セッション存在チェック_指定したタスクIDに対して作業セッションが存在するならtrueを返すこと() {
    // Arrange
    int taskId = 1;

    // Act
    boolean exists = sut.existsByTaskId(taskId);

    // Assert
    assertThat(exists).isTrue();
  }

  @Test
  void タスクの作業セッション存在チェック_指定したタスクIDに対して作業セッションが存在しないならfalseを返すこと() {
    // Arrange
    int taskId = 2;

    // Act
    boolean exists = sut.existsByTaskId(taskId);

    // Assert
    assertThat(exists).isFalse();
  }

}