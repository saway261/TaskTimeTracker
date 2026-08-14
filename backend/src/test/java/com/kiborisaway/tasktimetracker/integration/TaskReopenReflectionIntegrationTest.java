package com.kiborisaway.tasktimetracker.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kiborisaway.tasktimetracker.data.entity.Reflection;
import com.kiborisaway.tasktimetracker.data.entity.Task;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.ReflectionRepository;
import com.kiborisaway.tasktimetracker.repository.TaskRepository;
import com.kiborisaway.tasktimetracker.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TaskReopenReflectionIntegrationTest {

  private static final int OWNER_USER_ID = 2;
  private static final int OTHER_USER_ID = 1;
  private static final int TASK_ID = 6;

  @Autowired
  private TaskService sut;

  @Autowired
  private TaskRepository taskRepository;

  @Autowired
  private ReflectionRepository reflectionRepository;

  @Test
  void 再開成功_完了情報とReflectionを削除し再完了してもReflectionを復元しないこと() {
    // Arrange
    Task before = taskRepository.findById(TASK_ID, OWNER_USER_ID);
    assertThat(before.getFinishedAt()).isNotNull();
    assertThat(before.getActualMinutesCached()).isNotNull();
    assertThat(before.getGapMinutesCached()).isNotNull();
    assertThat(before.getGapRateCached()).isNotNull();
    assertThat(reflectionRepository.findByTaskId(TASK_ID)).isNotNull();

    // Act
    sut.updateFinished(OWNER_USER_ID, TASK_ID, false);

    // Assert
    Task reopened = taskRepository.findById(TASK_ID, OWNER_USER_ID);
    assertThat(reopened.getFinishedAt()).isNull();
    assertThat(reopened.getActualMinutesCached()).isNull();
    assertThat(reopened.getGapMinutesCached()).isNull();
    assertThat(reopened.getGapRateCached()).isNull();
    assertThat(reflectionRepository.findByTaskId(TASK_ID)).isNull();

    // Act
    sut.updateFinished(OWNER_USER_ID, TASK_ID, true);

    // Assert
    Task completedAgain = taskRepository.findById(TASK_ID, OWNER_USER_ID);
    assertThat(completedAgain.getFinishedAt()).isNotNull();
    assertThat(completedAgain.getActualMinutesCached()).isZero();
    assertThat(completedAgain.getGapMinutesCached()).isEqualTo(-60);
    assertThat(completedAgain.getGapRateCached()).isEqualTo(-100.0);
    assertThat(reflectionRepository.findByTaskId(TASK_ID)).isNull();
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void 再開失敗_所有者不一致で状態更新できない場合はTaskとReflectionの両方を変更しないこと() {
    // Arrange
    Task taskBefore = taskRepository.findById(TASK_ID, OWNER_USER_ID);
    Reflection reflectionBefore = reflectionRepository.findByTaskId(TASK_ID);
    assertThat(taskBefore).isNotNull();
    assertThat(reflectionBefore).isNotNull();

    // Act & Assert
    assertThatThrownBy(() -> sut.updateFinished(OTHER_USER_ID, TASK_ID, false))
        .isInstanceOf(TargetNotFoundException.class);

    assertThat(taskRepository.findById(TASK_ID, OWNER_USER_ID))
        .usingRecursiveComparison()
        .isEqualTo(taskBefore);
    assertThat(reflectionRepository.findByTaskId(TASK_ID))
        .usingRecursiveComparison()
        .isEqualTo(reflectionBefore);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void 再開失敗_存在しないタスクIDを指定しても他タスクのReflectionを削除しないこと() {
    // Arrange
    int missingTaskId = 999;
    Reflection reflectionBefore = reflectionRepository.findByTaskId(TASK_ID);
    assertThat(reflectionBefore).isNotNull();

    // Act & Assert
    assertThatThrownBy(() -> sut.updateFinished(OWNER_USER_ID, missingTaskId, false))
        .isInstanceOf(TargetNotFoundException.class);

    assertThat(reflectionRepository.findByTaskId(TASK_ID))
        .usingRecursiveComparison()
        .isEqualTo(reflectionBefore);
  }
}
