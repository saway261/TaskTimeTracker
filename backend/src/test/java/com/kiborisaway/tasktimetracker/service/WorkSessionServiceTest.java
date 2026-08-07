package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
    when(repository.sumMinutesByTaskId(taskId)).thenReturn(75);

    // Act
    int actual = sut.getTaskActualTotalTime(taskId);

    // Assert
    assertThat(actual).isEqualTo(75);
    verify(repository, times(1)).sumMinutesByTaskId(taskId);
  }

  @Test
  void タスクグループ作業時間合計取得成功_リポジトリのタスクグループID指定合計取得を呼び出すこと() {
    // Arrange
    int taskGroupId = 1;
    when(repository.sumMinutesByTaskGroupId(taskGroupId)).thenReturn(120);

    // Act
    int actual = sut.getTaskGroupActualTotalTime(taskGroupId);

    // Assert
    assertThat(actual).isEqualTo(120);
    verify(repository, times(1)).sumMinutesByTaskGroupId(taskGroupId);
  }

  @Test
  void プロジェクト作業時間合計取得成功_リポジトリのプロジェクトID指定合計取得を呼び出すこと() {
    // Arrange
    int projectId = 1;
    when(repository.sumMinutesByProjectId(projectId)).thenReturn(180);

    // Act
    int actual = sut.getProjectActualTotalTime(projectId);

    // Assert
    assertThat(actual).isEqualTo(180);
    verify(repository, times(1)).sumMinutesByProjectId(projectId);
  }

  @Test
  void タスク内一覧取得成功_リポジトリのタスクID指定一覧取得を呼び出すこと() {
    // Arrange
    int taskId = 1;
    WorkSession workSession = new WorkSession();
    workSession.setId(1);
    workSession.setTaskId(taskId);
    List<WorkSession> expected = List.of(workSession);

    when(repository.findAllByTaskId(taskId)).thenReturn(expected);

    // Act
    List<WorkSession> actual = sut.getAllInTask(taskId);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(repository, times(1)).findAllByTaskId(taskId);
  }

  @Test
  void ID検索成功_リポジトリのID検索結果を返すこと() {
    // Arrange
    int wsId = 1;
    WorkSession expected = new WorkSession();
    expected.setId(wsId);

    when(repository.findById(wsId)).thenReturn(expected);

    // Act
    WorkSession actual = sut.get(wsId);

    // Assert
    assertThat(actual).isSameAs(expected);
    verify(repository, times(1)).findById(wsId);
  }

  @Test
  void ID検索失敗_リポジトリからnullが返ったらTargetNotFoundExceptionを投げること() {
    // Arrange
    int wsId = 999;
    when(repository.findById(wsId)).thenReturn(null);

    // Act & Assert
    assertThatThrownBy(() -> sut.get(wsId))
        .isInstanceOf(TargetNotFoundException.class);

    verify(repository, times(1)).findById(wsId);
  }

  @Test
  void 登録成功_受け取ったタスクIDをWorkSessionにセットしてリポジトリの登録処理を呼び出すこと() {
    // Arrange
    int taskId = 1;
    WorkSessionCreateRequest request = new WorkSessionCreateRequest();
    request.setType(WorkSessionType.MANUAL);
    request.setMinutes(30);

    when(taskRepository.existsById(taskId)).thenReturn(true);
    doAnswer(invocation -> {
      invocation.<WorkSession>getArgument(0).setId(10);
      return null;
    }).when(repository).insert(any(WorkSession.class));
    WorkSession registered = new WorkSession();
    registered.setId(10);
    registered.setTaskId(taskId);
    registered.setMinutes(30);
    registered.setType(WorkSessionType.MANUAL);
    when(repository.findById(10)).thenReturn(registered);

    // Act
    WorkSession actual = sut.create(taskId, request);

    // Assert
    assertThat(actual.getTaskId()).isEqualTo(taskId);
    assertThat(actual.getMinutes()).isEqualTo(30);
    verify(taskRepository, times(1)).existsById(taskId);
    verify(taskRepository, times(1)).isFinished(taskId);
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

    when(taskRepository.existsById(taskId)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.create(taskId, request))
        .isInstanceOf(TargetNotFoundException.class);

    verify(taskRepository, times(1)).existsById(taskId);
    verify(taskRepository, never()).isFinished(anyInt());
    verify(repository, never()).insert(any(WorkSession.class));
  }

  @Test
  void 登録失敗_指定したタスクが完了済みの場合は例外を投げて登録処理を呼び出さないこと() {
    // Arrange
    int taskId = 3;
    WorkSessionCreateRequest request = new WorkSessionCreateRequest();
    request.setType(WorkSessionType.MANUAL);
    request.setMinutes(30);

    when(taskRepository.existsById(taskId)).thenReturn(true);
    when(taskRepository.isFinished(taskId)).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> sut.create(taskId, request))
        .isInstanceOf(WorkSessionOperationNotAllowedException.class);

    verify(taskRepository, times(1)).existsById(taskId);
    verify(taskRepository, times(1)).isFinished(taskId);
    verify(repository, never()).insert(any(WorkSession.class));
  }

  @Test
  void 終了成功_終了可能ならリポジトリの終了更新処理を呼び出すこと() {
    // Arrange
    int wsId = 1;
    when(repository.canSetEnd(wsId)).thenReturn(true);
    when(repository.setEnd(wsId)).thenReturn(1);
    WorkSession ended = new WorkSession();
    ended.setId(wsId);
    ended.setEndedAt(LocalDateTime.of(2026, 1, 1, 9, 30));
    when(repository.findById(wsId)).thenReturn(ended);

    // Act
    WorkSession actual = sut.setEnd(wsId);

    // Assert
    verify(repository, times(1)).canSetEnd(wsId);
    verify(repository, times(1)).setEnd(wsId);
    assertThat(actual).isSameAs(ended);
  }

  @Test
  void 終了失敗_終了可能でない場合は例外を投げて終了更新処理を呼び出さないこと() {
    // Arrange
    int wsId = 1;
    when(repository.canSetEnd(wsId)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.setEnd(wsId))
        .isInstanceOf(WorkSessionEndNotAllowedException.class);

    verify(repository, times(1)).canSetEnd(wsId);
    verify(repository, never()).setEnd(anyInt());
  }

  @Test
  void 終了失敗_終了更新件数が0件のときTargetNotFoundExceptionを投げること() {
    // Arrange
    int wsId = 999;
    when(repository.canSetEnd(wsId)).thenReturn(true);
    when(repository.setEnd(wsId)).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.setEnd(wsId))
        .isInstanceOf(TargetNotFoundException.class);

    verify(repository, times(1)).canSetEnd(wsId);
    verify(repository, times(1)).setEnd(wsId);
  }

  @Test
  void 更新成功_受け取ったIDをWorkSessionにセットしてリポジトリの更新処理を呼び出すこと() {
    // Arrange
    int wsId = 1;
    int taskId = 1;
    WorkSessionUpdateRequest request = new WorkSessionUpdateRequest();
    request.setMinutes(30);
    request.setStartedAt(LocalDateTime.of(2026, 1, 1, 9, 0));
    request.setEndedAt(LocalDateTime.of(2026, 1, 1, 9, 30));

    when(repository.findTaskIdById(wsId)).thenReturn(taskId);
    when(taskRepository.isFinished(taskId)).thenReturn(false);
    when(repository.update(any(WorkSession.class))).thenReturn(1);
    WorkSession updated = new WorkSession();
    updated.setId(wsId);
    updated.setTaskId(taskId);
    updated.setMinutes(30);
    when(repository.findById(wsId)).thenReturn(updated);

    // Act
    WorkSession actual = sut.update(wsId, request);

    // Assert
    verify(repository, times(1)).findTaskIdById(wsId);
    verify(taskRepository, times(1)).isFinished(taskId);
    ArgumentCaptor<WorkSession> captor = ArgumentCaptor.forClass(WorkSession.class);
    verify(repository, times(1)).update(captor.capture());
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
    request.setMinutes(30);

    when(repository.findTaskIdById(wsId)).thenReturn(taskId);
    when(taskRepository.isFinished(taskId)).thenReturn(false);
    when(repository.update(any(WorkSession.class))).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.update(wsId, request))
        .isInstanceOf(TargetNotFoundException.class);

    verify(repository, times(1)).findTaskIdById(wsId);
    verify(taskRepository, times(1)).isFinished(taskId);
    verify(repository, times(1)).update(any(WorkSession.class));
  }

  @Test
  void 更新失敗_作業セッションが存在しない場合は例外を投げて更新処理を呼び出さないこと() {
    // Arrange
    int wsId = 999;
    WorkSessionUpdateRequest request = new WorkSessionUpdateRequest();
    request.setMinutes(30);

    when(repository.findTaskIdById(wsId)).thenReturn(null);

    // Act & Assert
    assertThatThrownBy(() -> sut.update(wsId, request))
        .isInstanceOf(TargetNotFoundException.class);

    verify(repository, times(1)).findTaskIdById(wsId);
    verify(taskRepository, never()).isFinished(anyInt());
    verify(repository, never()).update(any(WorkSession.class));
  }

  @Test
  void 更新失敗_完了済みタスクの作業セッションなら例外を投げて更新処理を呼び出さないこと() {
    // Arrange
    int wsId = 1;
    int taskId = 3;
    WorkSessionUpdateRequest request = new WorkSessionUpdateRequest();
    request.setMinutes(30);

    when(repository.findTaskIdById(wsId)).thenReturn(taskId);
    when(taskRepository.isFinished(taskId)).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> sut.update(wsId, request))
        .isInstanceOf(WorkSessionOperationNotAllowedException.class);

    verify(repository, times(1)).findTaskIdById(wsId);
    verify(taskRepository, times(1)).isFinished(taskId);
    verify(repository, never()).update(any(WorkSession.class));
  }

  @Test
  void 削除成功_リポジトリのID指定削除を呼び出すこと() {
    // Arrange
    int wsId = 1;
    int taskId = 1;

    when(repository.findTaskIdById(wsId)).thenReturn(taskId);
    when(taskRepository.isFinished(taskId)).thenReturn(false);
    when(repository.deleteById(wsId)).thenReturn(1);

    // Act
    sut.delete(wsId);

    // Assert
    verify(repository, times(1)).findTaskIdById(wsId);
    verify(taskRepository, times(1)).isFinished(taskId);
    verify(repository, times(1)).deleteById(wsId);
  }

  @Test
  void 削除失敗_削除件数が0件のときTargetNotFoundExceptionを投げること() {
    // Arrange
    int wsId = 999;
    int taskId = 1;

    when(repository.findTaskIdById(wsId)).thenReturn(taskId);
    when(taskRepository.isFinished(taskId)).thenReturn(false);
    when(repository.deleteById(wsId)).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.delete(wsId))
        .isInstanceOf(TargetNotFoundException.class);

    verify(repository, times(1)).findTaskIdById(wsId);
    verify(taskRepository, times(1)).isFinished(taskId);
    verify(repository, times(1)).deleteById(wsId);
  }

  @Test
  void 削除失敗_作業セッションが存在しない場合は例外を投げて削除処理を呼び出さないこと() {
    // Arrange
    int wsId = 999;

    when(repository.findTaskIdById(wsId)).thenReturn(null);

    // Act & Assert
    assertThatThrownBy(() -> sut.delete(wsId))
        .isInstanceOf(TargetNotFoundException.class);

    verify(repository, times(1)).findTaskIdById(wsId);
    verify(taskRepository, never()).isFinished(anyInt());
    verify(repository, never()).deleteById(anyInt());
  }

  @Test
  void 削除失敗_完了済みタスクの作業セッションなら例外を投げて削除処理を呼び出さないこと() {
    // Arrange
    int wsId = 1;
    int taskId = 3;

    when(repository.findTaskIdById(wsId)).thenReturn(taskId);
    when(taskRepository.isFinished(taskId)).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> sut.delete(wsId))
        .isInstanceOf(WorkSessionOperationNotAllowedException.class);

    verify(repository, times(1)).findTaskIdById(wsId);
    verify(taskRepository, times(1)).isFinished(taskId);
    verify(repository, never()).deleteById(anyInt());
  }

}
