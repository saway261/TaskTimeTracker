package com.kiborisaway.tasktimetracker.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kiborisaway.tasktimetracker.data.dto.work_session.WorkSessionUpdateRequest;
import com.kiborisaway.tasktimetracker.data.entity.WorkSession;
import com.kiborisaway.tasktimetracker.data.entity.WorkSessionType;
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
class WorkSessionUpdateIntegrationTest {

  @Autowired
  private WorkSessionService sut;

  @Autowired
  private WorkSessionRepository workSessionRepository;

  @Test
  void 更新失敗_TIMERからMANUALへのtype変更を拒否してDBの値を変更しないこと() {
    // Arrange
    int wsId = 1;
    WorkSession before = workSessionRepository.findById(wsId);
    LocalDateTime startedAt = before.getStartedAt();

    WorkSessionUpdateRequest request = new WorkSessionUpdateRequest();
    request.setType(WorkSessionType.MANUAL);
    request.setMinutes(30);

    // Act & Assert
    assertThatThrownBy(() -> sut.update(wsId, request))
        .isInstanceOf(WorkSessionOperationNotAllowedException.class);

    WorkSession after = workSessionRepository.findById(wsId);
    assertThat(after.getType()).isEqualTo(WorkSessionType.TIMER);
    assertThat(after.getStartedAt()).isEqualTo(startedAt);
    assertThat(after.getEndedAt()).isNull();
    assertThat(after.getMinutes()).isNull();
  }
}
