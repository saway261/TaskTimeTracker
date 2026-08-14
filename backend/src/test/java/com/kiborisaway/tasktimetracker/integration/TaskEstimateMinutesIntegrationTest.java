package com.kiborisaway.tasktimetracker.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kiborisaway.tasktimetracker.data.dto.task.TaskUpdateEstimatedMinutesRequest;
import com.kiborisaway.tasktimetracker.data.entity.Task;
import com.kiborisaway.tasktimetracker.exception.EstimateMinutesUpdateNotAllowedException;
import com.kiborisaway.tasktimetracker.repository.TaskRepository;
import com.kiborisaway.tasktimetracker.repository.WorkSessionRepository;
import com.kiborisaway.tasktimetracker.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TaskEstimateMinutesIntegrationTest {

  private static final int USER_ID = 1;

  @Autowired
  private TaskService sut;

  @Autowired
  private TaskRepository taskRepository;

  @Autowired
  private WorkSessionRepository workSessionRepository;

  @Test
  void 見積もり作業時間更新失敗_WorkSessionがなくても完了済みタスクは更新できないこと() {
    // Arrange
    int taskId = 3;
    Task before = taskRepository.findById(taskId, USER_ID);
    assertThat(before.getFinishedAt()).isNotNull();
    assertThat(workSessionRepository.findAllByTaskId(taskId, USER_ID)).isEmpty();

    TaskUpdateEstimatedMinutesRequest request = new TaskUpdateEstimatedMinutesRequest();
    request.setEstimatedMinutes(120);

    // Act & Assert
    assertThatThrownBy(() -> sut.updateEstimateMinutes(USER_ID, taskId, request))
        .isInstanceOf(EstimateMinutesUpdateNotAllowedException.class);

    Task after = taskRepository.findById(taskId, USER_ID);
    assertThat(after.getEstimatedMinutes()).isEqualTo(before.getEstimatedMinutes());
  }
}
