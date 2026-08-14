package com.kiborisaway.tasktimetracker.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kiborisaway.tasktimetracker.repository.MemoRepository;
import com.kiborisaway.tasktimetracker.repository.TaskRepository;
import com.kiborisaway.tasktimetracker.repository.WorkSessionRepository;
import com.kiborisaway.tasktimetracker.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TaskDeletionIntegrationTest {

  private static final int USER_ID = 1;

  @Autowired
  private TaskService sut;

  @Autowired
  private TaskRepository taskRepository;

  @Autowired
  private WorkSessionRepository workSessionRepository;

  @Autowired
  private MemoRepository memoRepository;

  @Test
  void 削除成功_作業セッションとメモを持つタスクを子データごと削除できること() {
    // Arrange
    int taskId = 1;
    assertThat(workSessionRepository.findAllByTaskId(taskId, USER_ID)).hasSize(1);
    assertThat(memoRepository.findAllInTask(taskId)).hasSize(1);

    // Act
    sut.deleteById(USER_ID, taskId);

    // Assert
    assertThat(taskRepository.findById(taskId, USER_ID)).isNull();
    assertThat(workSessionRepository.findAllByTaskId(taskId, USER_ID)).isEmpty();
    assertThat(memoRepository.findAllInTask(taskId)).isEmpty();
  }
}
