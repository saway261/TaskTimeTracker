package com.kiborisaway.tasktimetracker.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kiborisaway.tasktimetracker.data.entity.WorkSession;
import com.kiborisaway.tasktimetracker.data.entity.WorkSessionType;
import com.kiborisaway.tasktimetracker.exception.WorkSessionEndNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.WorkSessionOperationNotAllowedException;
import com.kiborisaway.tasktimetracker.repository.WorkSessionRepository;
import com.kiborisaway.tasktimetracker.service.WorkSessionService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class WorkSessionEndIntegrationTest {

  private static final int USER_ID = 1;

  @Autowired
  private WorkSessionService sut;

  @Autowired
  private WorkSessionRepository workSessionRepository;

  @Test
  void 終了失敗_終了済みセッションの終了日時と作業時間を上書きしないこと() {
    // Arrange
    int wsId = 2;
    WorkSession before = workSessionRepository.findById(wsId, USER_ID);
    LocalDateTime endedAt = before.getEndedAt();
    Integer minutes = before.getMinutes();

    // Act & Assert
    assertThatThrownBy(() -> sut.setEnd(USER_ID, wsId))
        .isInstanceOf(WorkSessionEndNotAllowedException.class);

    WorkSession after = workSessionRepository.findById(wsId, USER_ID);
    assertThat(after.getEndedAt()).isEqualTo(endedAt);
    assertThat(after.getMinutes()).isEqualTo(minutes);
  }

  @Test
  void 終了失敗_完了済みタスクの未終了セッションを終了できないこと() {
    // Arrange
    WorkSession workSession = new WorkSession();
    workSession.setTaskId(3);
    workSession.setType(WorkSessionType.TIMER);
    workSessionRepository.insert(workSession);

    // Act & Assert
    assertThatThrownBy(() -> sut.setEnd(USER_ID, workSession.getId()))
        .isInstanceOf(WorkSessionOperationNotAllowedException.class);

    WorkSession unchanged = workSessionRepository.findById(workSession.getId(), USER_ID);
    assertThat(unchanged.getEndedAt()).isNull();
    assertThat(unchanged.getMinutes()).isNull();
  }
}
