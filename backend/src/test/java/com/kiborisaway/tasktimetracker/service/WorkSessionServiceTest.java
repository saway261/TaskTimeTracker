package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.data.dto.work_session.WorkSessionCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.work_session.WorkSessionUpdateRequest;
import com.kiborisaway.tasktimetracker.data.entity.WorkSession;
import com.kiborisaway.tasktimetracker.data.entity.WorkSessionType;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.exception.WorkSessionEndNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.WorkSessionOperationNotAllowedException;
import com.kiborisaway.tasktimetracker.repository.TaskRepository;
import com.kiborisaway.tasktimetracker.repository.WorkSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkSessionServiceTest {

  private static final int USER_ID = 1;

  @Mock
  private WorkSessionRepository repository;

  @Mock
  private TaskRepository taskRepository;

  @InjectMocks
  private WorkSessionService sut;

  @Test
  void タスク作業時間合計取得成功_リポジトリのタスクID指定合計取得を呼び出すこと() {
    // Arrange
    int taskId = 1;
    when(repository.sumMinutesByTaskId(taskId, USER_ID)).thenReturn(75);

    // Act
    int actual = sut.getTaskActualTotalTime(USER_ID, taskId);

    // Assert
    assertThat(actual).isEqualTo(75);
    verify(repository, times(1)).sumMinutesByTaskId(taskId, USER_ID);
  }

  @Test
  void タスクグループ作業時間合計取得成功_リポジトリのタスクグループID指定合計取得を呼び出すこと() {
    // Arrange
    int taskGroupId = 1;
    when(repository.sumMinutesByTaskGroupId(taskGroupId, USER_ID)).thenReturn(120);

    // Act
    int actual = sut.getTaskGroupActualTotalTime(USER_ID, taskGroupId);

    // Assert
    assertThat(actual).isEqualTo(120);
    verify(repository, times(1)).sumMinutesByTaskGroupId(taskGroupId, USER_ID);
  }

  @Test
  void プロジェクト作業時間合計取得成功_リポジトリのプロジェクトID指定合計取得を呼び出すこと() {
    // Arrange
    int projectId = 1;
    when(repository.sumMinutesByProjectId(projectId, USER_ID)).thenReturn(180);

    // Act
    int actual = sut.getProjectActualTotalTime(USER_ID, projectId);

    // Assert
    assertThat(actual).isEqualTo(180);
    verify(repository, times(1)).sumMinutesByProjectId(projectId, USER_ID);
  }

  @Test
  void タスク内一覧取得成功_リポジトリのタスクID指定一覧取得を呼び出すこと() {
    // Arrange
    int taskId = 1;
    WorkSession workSession = new WorkSession();
    workSession.setId(1);
    workSession.setTaskId(taskId);
    List<WorkSession> expected = List.of(workSession);

    when(repository.findAllByTaskId(taskId, USER_ID)).thenReturn(expected);

    // Act
    List<WorkSession> actual = sut.getAllInTask(USER_ID, taskId);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(repository, times(1)).findAllByTaskId(taskId, USER_ID);
  }

  @Test
  void ID検索成功_リポジトリのID検索結果を返すこと() {
    // Arrange
    int wsId = 1;
    WorkSession expected = new WorkSession();
    expected.setId(wsId);

    when(repository.findById(wsId, USER_ID)).thenReturn(expected);

    // Act
    WorkSession actual = sut.get(USER_ID, wsId);

    // Assert
    assertThat(actual).isSameAs(expected);
    verify(repository, times(1)).findById(wsId, USER_ID);
  }

  @Test
  void ID検索失敗_リポジトリからnullが返ったらTargetNotFoundExceptionを投げること() {
    // Arrange
    int wsId = 999;
    when(repository.findById(wsId, USER_ID)).thenReturn(null);

    // Act & Assert
    assertThatThrownBy(() -> sut.get(USER_ID, wsId))
        .isInstanceOf(TargetNotFoundException.class);

    verify(repository, times(1)).findById(wsId, USER_ID);
  }

  @Test
  void 登録成功_受け取ったタスクIDをWorkSessionにセットしてリポジトリの登録処理を呼び出すこと() {
    // Arrange
    int taskId = 1;
    WorkSessionCreateRequest request = new WorkSessionCreateRequest();
    request.setType(WorkSessionType.MANUAL);
    request.setMinutes(30);

    when(taskRepository.existsByIdAndUserId(taskId, USER_ID)).thenReturn(true);
    doAnswer(invocation -> {
      invocation.<WorkSession>getArgument(0).setId(10);
      return null;
    }).when(repository).insert(any(WorkSession.class));
    WorkSession registered = new WorkSession();
    registered.setId(10);
    registered.setTaskId(taskId);
    registered.setMinutes(30);
    registered.setType(WorkSessionType.MANUAL);
    when(repository.findById(10, USER_ID)).thenReturn(registered);

    // Act
    WorkSession actual = sut.create(USER_ID, taskId, request);

    // Assert
    assertThat(actual.getTaskId()).isEqualTo(taskId);
    assertThat(actual.getMinutes()).isEqualTo(30);
    verify(taskRepository, times(1)).existsByIdAndUserId(taskId, USER_ID);
    verify(taskRepository, times(1)).isFinished(taskId, USER_ID);
    ArgumentCaptor<WorkSession> captor = ArgumentCaptor.forClass(WorkSession.class);
    verify(repository, times(1)).insert(captor.capture());
    assertThat(captor.getValue()).isNotSameAs(actual);
  }

  @Test
  void 登録失敗_指定したタスクIDが存在しない場合は例外を投げて登録処理を呼び出さないこと() {
    // Arrange
    int taskId = 999;
    WorkSessionCreateRequest request = new WorkSessionCreateRequest();
    request.setType(WorkSessionType.MANUAL);
    request.setMinutes(30);

    when(taskRepository.existsByIdAndUserId(taskId, USER_ID)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.create(USER_ID, taskId, request))
        .isInstanceOf(TargetNotFoundException.class);

    verify(taskRepository, times(1)).existsByIdAndUserId(taskId, USER_ID);
    verify(taskRepository, never()).isFinished(anyInt(), anyInt());
    verify(repository, never()).insert(any(WorkSession.class));
  }

  @Test
  void 登録失敗_指定したタスクが完了済みの場合は例外を投げて登録処理を呼び出さないこと() {
    // Arrange
    int taskId = 3;
    WorkSessionCreateRequest request = new WorkSessionCreateRequest();
    request.setType(WorkSessionType.MANUAL);
    request.setMinutes(30);

    when(taskRepository.existsByIdAndUserId(taskId, USER_ID)).thenReturn(true);
    when(taskRepository.isFinished(taskId, USER_ID)).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> sut.create(USER_ID, taskId, request))
        .isInstanceOf(WorkSessionOperationNotAllowedException.class);

    verify(taskRepository, times(1)).existsByIdAndUserId(taskId, USER_ID);
    verify(taskRepository, times(1)).isFinished(taskId, USER_ID);
    verify(repository, never()).insert(any(WorkSession.class));
  }

  @Test
  void 終了成功_終了可能ならリポジトリの終了更新処理を呼び出すこと() {
    // Arrange
    int wsId = 1;
    int taskId = 1;
    when(repository.findTaskIdById(wsId, USER_ID)).thenReturn(taskId);
    when(taskRepository.isFinished(taskId, USER_ID)).thenReturn(false);
    when(repository.canSetEnd(wsId, USER_ID)).thenReturn(true);
    when(repository.setEnd(wsId, USER_ID)).thenReturn(1);
    WorkSession ended = new WorkSession();
    ended.setId(wsId);
    ended.setEndedAt(LocalDateTime.of(2026, 1, 1, 9, 30));
    when(repository.findById(wsId, USER_ID)).thenReturn(ended);

    // Act
    WorkSession actual = sut.setEnd(USER_ID, wsId);

    // Assert
    verify(repository, times(1)).findTaskIdById(wsId, USER_ID);
    verify(taskRepository, times(1)).isFinished(taskId, USER_ID);
    verify(repository, times(1)).canSetEnd(wsId, USER_ID);
    verify(repository, times(1)).setEnd(wsId, USER_ID);
    assertThat(actual).isSameAs(ended);
  }

  @Test
  void 終了失敗_終了済みの場合は例外を投げて終了更新処理を呼び出さないこと() {
    // Arrange
    int wsId = 1;
    int taskId = 1;
    when(repository.findTaskIdById(wsId, USER_ID)).thenReturn(taskId);
    when(taskRepository.isFinished(taskId, USER_ID)).thenReturn(false);
    when(repository.canSetEnd(wsId, USER_ID)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.setEnd(USER_ID, wsId))
        .isInstanceOf(WorkSessionEndNotAllowedException.class);

    verify(repository, times(1)).findTaskIdById(wsId, USER_ID);
    verify(taskRepository, times(1)).isFinished(taskId, USER_ID);
    verify(repository, times(1)).canSetEnd(wsId, USER_ID);
    verify(repository, never()).setEnd(anyInt(), anyInt());
  }

  @Test
  void 終了失敗_作業セッションが存在しないなら404用例外を投げること() {
    // Arrange
    int wsId = 999;
    when(repository.findTaskIdById(wsId, USER_ID)).thenReturn(null);

    // Act & Assert
    assertThatThrownBy(() -> sut.setEnd(USER_ID, wsId))
        .isInstanceOf(TargetNotFoundException.class);

    verify(repository, times(1)).findTaskIdById(wsId, USER_ID);
    verify(taskRepository, never()).isFinished(anyInt(), anyInt());
    verify(repository, never()).canSetEnd(anyInt(), anyInt());
    verify(repository, never()).setEnd(anyInt(), anyInt());
  }

  @Test
  void 終了失敗_親タスクが完了済みなら例外を投げて終了可否判定を呼び出さないこと() {
    // Arrange
    int wsId = 1;
    int taskId = 3;
    when(repository.findTaskIdById(wsId, USER_ID)).thenReturn(taskId);
    when(taskRepository.isFinished(taskId, USER_ID)).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> sut.setEnd(USER_ID, wsId))
        .isInstanceOf(WorkSessionOperationNotAllowedException.class);

    verify(repository, times(1)).findTaskIdById(wsId, USER_ID);
    verify(taskRepository, times(1)).isFinished(taskId, USER_ID);
    verify(repository, never()).canSetEnd(anyInt(), anyInt());
    verify(repository, never()).setEnd(anyInt(), anyInt());
  }

  @Test
  void 終了失敗_終了更新件数が0件なら終了不可例外を投げること() {
    // Arrange
    int wsId = 1;
    int taskId = 1;
    when(repository.findTaskIdById(wsId, USER_ID)).thenReturn(taskId);
    when(taskRepository.isFinished(taskId, USER_ID)).thenReturn(false);
    when(repository.canSetEnd(wsId, USER_ID)).thenReturn(true);
    when(repository.setEnd(wsId, USER_ID)).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.setEnd(USER_ID, wsId))
        .isInstanceOf(WorkSessionEndNotAllowedException.class);

    verify(repository, times(1)).findTaskIdById(wsId, USER_ID);
    verify(taskRepository, times(1)).isFinished(taskId, USER_ID);
    verify(repository, times(1)).canSetEnd(wsId, USER_ID);
    verify(repository, times(1)).setEnd(wsId, USER_ID);
  }

  @Test
  void 更新成功_受け取ったIDをWorkSessionにセットしてリポジトリの更新処理を呼び出すこと() {
    // Arrange
    int wsId = 1;
    int taskId = 1;
    WorkSessionUpdateRequest request = new WorkSessionUpdateRequest();
    request.setType(WorkSessionType.MANUAL);
    request.setMinutes(30);
    request.setStartedAt(LocalDateTime.of(2026, 1, 1, 9, 0));
    request.setEndedAt(LocalDateTime.of(2026, 1, 1, 9, 30));

    WorkSession current = new WorkSession();
    current.setId(wsId);
    current.setTaskId(taskId);
    current.setType(WorkSessionType.MANUAL);
    when(taskRepository.isFinished(taskId, USER_ID)).thenReturn(false);
    when(repository.update(any(WorkSession.class), eq(USER_ID))).thenReturn(1);
    WorkSession updated = new WorkSession();
    updated.setId(wsId);
    updated.setTaskId(taskId);
    updated.setMinutes(30);
    updated.setType(WorkSessionType.MANUAL);
    when(repository.findById(wsId, USER_ID)).thenReturn(current, updated);

    // Act
    WorkSession actual = sut.update(USER_ID, wsId, request);

    // Assert
    verify(repository, times(2)).findById(wsId, USER_ID);
    verify(taskRepository, times(1)).isFinished(taskId, USER_ID);
    ArgumentCaptor<WorkSession> captor = ArgumentCaptor.forClass(WorkSession.class);
    verify(repository, times(1)).update(captor.capture(), eq(USER_ID));
    assertThat(captor.getValue().getId()).isEqualTo(wsId);
    assertThat(captor.getValue().getMinutes()).isEqualTo(30);
    assertThat(actual).isSameAs(updated);
  }

  @Test
  void 更新失敗_更新件数が0件のときTargetNotFoundExceptionを投げること() {
    // Arrange
    int wsId = 999;
    int taskId = 1;
    WorkSessionUpdateRequest request = new WorkSessionUpdateRequest();
    request.setType(WorkSessionType.MANUAL);
    request.setMinutes(30);

    WorkSession current = new WorkSession();
    current.setId(wsId);
    current.setTaskId(taskId);
    current.setType(WorkSessionType.MANUAL);
    when(repository.findById(wsId, USER_ID)).thenReturn(current);
    when(taskRepository.isFinished(taskId, USER_ID)).thenReturn(false);
    when(repository.update(any(WorkSession.class), eq(USER_ID))).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.update(USER_ID, wsId, request))
        .isInstanceOf(TargetNotFoundException.class);

    verify(repository, times(1)).findById(wsId, USER_ID);
    verify(taskRepository, times(1)).isFinished(taskId, USER_ID);
    verify(repository, times(1)).update(any(WorkSession.class), anyInt());
  }

  @Test
  void 更新失敗_作業セッションが存在しない場合は例外を投げて更新処理を呼び出さないこと() {
    // Arrange
    int wsId = 999;
    WorkSessionUpdateRequest request = new WorkSessionUpdateRequest();
    request.setType(WorkSessionType.MANUAL);
    request.setMinutes(30);

    when(repository.findById(wsId, USER_ID)).thenReturn(null);

    // Act & Assert
    assertThatThrownBy(() -> sut.update(USER_ID, wsId, request))
        .isInstanceOf(TargetNotFoundException.class);

    verify(repository, times(1)).findById(wsId, USER_ID);
    verify(taskRepository, never()).isFinished(anyInt(), anyInt());
    verify(repository, never()).update(any(WorkSession.class), anyInt());
  }

  @Test
  void 更新失敗_完了済みタスクの作業セッションなら例外を投げて更新処理を呼び出さないこと() {
    // Arrange
    int wsId = 1;
    int taskId = 3;
    WorkSessionUpdateRequest request = new WorkSessionUpdateRequest();
    request.setType(WorkSessionType.MANUAL);
    request.setMinutes(30);

    WorkSession current = new WorkSession();
    current.setId(wsId);
    current.setTaskId(taskId);
    current.setType(WorkSessionType.MANUAL);
    when(repository.findById(wsId, USER_ID)).thenReturn(current);
    when(taskRepository.isFinished(taskId, USER_ID)).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> sut.update(USER_ID, wsId, request))
        .isInstanceOf(WorkSessionOperationNotAllowedException.class);

    verify(repository, times(1)).findById(wsId, USER_ID);
    verify(taskRepository, times(1)).isFinished(taskId, USER_ID);
    verify(repository, never()).update(any(WorkSession.class), anyInt());
  }

  @Test
  void 更新失敗_DB登録済みのtypeと異なる場合は例外を投げて更新処理を呼び出さないこと() {
    // Arrange
    int wsId = 1;
    int taskId = 1;
    WorkSessionUpdateRequest request = new WorkSessionUpdateRequest();
    request.setType(WorkSessionType.MANUAL);
    request.setMinutes(30);

    WorkSession current = new WorkSession();
    current.setId(wsId);
    current.setTaskId(taskId);
    current.setType(WorkSessionType.TIMER);
    when(repository.findById(wsId, USER_ID)).thenReturn(current);
    when(taskRepository.isFinished(taskId, USER_ID)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.update(USER_ID, wsId, request))
        .isInstanceOf(WorkSessionOperationNotAllowedException.class);

    verify(repository, times(1)).findById(wsId, USER_ID);
    verify(taskRepository, times(1)).isFinished(taskId, USER_ID);
    verify(repository, never()).update(any(WorkSession.class), anyInt());
  }

  @Test
  void 削除成功_リポジトリのID指定削除を呼び出すこと() {
    // Arrange
    int wsId = 1;
    int taskId = 1;

    when(repository.findTaskIdById(wsId, USER_ID)).thenReturn(taskId);
    when(taskRepository.isFinished(taskId, USER_ID)).thenReturn(false);
    when(repository.deleteById(wsId, USER_ID)).thenReturn(1);

    // Act
    sut.delete(USER_ID, wsId);

    // Assert
    verify(repository, times(1)).findTaskIdById(wsId, USER_ID);
    verify(taskRepository, times(1)).isFinished(taskId, USER_ID);
    verify(repository, times(1)).deleteById(wsId, USER_ID);
  }

  @Test
  void 削除失敗_削除件数が0件のときTargetNotFoundExceptionを投げること() {
    // Arrange
    int wsId = 999;
    int taskId = 1;

    when(repository.findTaskIdById(wsId, USER_ID)).thenReturn(taskId);
    when(taskRepository.isFinished(taskId, USER_ID)).thenReturn(false);
    when(repository.deleteById(wsId, USER_ID)).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.delete(USER_ID, wsId))
        .isInstanceOf(TargetNotFoundException.class);

    verify(repository, times(1)).findTaskIdById(wsId, USER_ID);
    verify(taskRepository, times(1)).isFinished(taskId, USER_ID);
    verify(repository, times(1)).deleteById(wsId, USER_ID);
  }

  @Test
  void 削除失敗_作業セッションが存在しない場合は例外を投げて削除処理を呼び出さないこと() {
    // Arrange
    int wsId = 999;

    when(repository.findTaskIdById(wsId, USER_ID)).thenReturn(null);

    // Act & Assert
    assertThatThrownBy(() -> sut.delete(USER_ID, wsId))
        .isInstanceOf(TargetNotFoundException.class);

    verify(repository, times(1)).findTaskIdById(wsId, USER_ID);
    verify(taskRepository, never()).isFinished(anyInt(), anyInt());
    verify(repository, never()).deleteById(anyInt(), anyInt());
  }

  @Test
  void 削除失敗_完了済みタスクの作業セッションなら例外を投げて削除処理を呼び出さないこと() {
    // Arrange
    int wsId = 1;
    int taskId = 3;

    when(repository.findTaskIdById(wsId, USER_ID)).thenReturn(taskId);
    when(taskRepository.isFinished(taskId, USER_ID)).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> sut.delete(USER_ID, wsId))
        .isInstanceOf(WorkSessionOperationNotAllowedException.class);

    verify(repository, times(1)).findTaskIdById(wsId, USER_ID);
    verify(taskRepository, times(1)).isFinished(taskId, USER_ID);
    verify(repository, never()).deleteById(anyInt(), anyInt());
  }

}
