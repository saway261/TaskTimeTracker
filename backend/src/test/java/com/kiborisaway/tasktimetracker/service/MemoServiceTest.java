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

import com.kiborisaway.tasktimetracker.data.dto.memo.MemoRequest;
import com.kiborisaway.tasktimetracker.data.entity.Memo;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.MemoRepository;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import com.kiborisaway.tasktimetracker.repository.TaskGroupRepository;
import com.kiborisaway.tasktimetracker.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemoServiceTest {

  private static final int USER_ID = 1;

  @Mock
  private MemoRepository memoRepository;

  @Mock
  private ProjectRepository projectRepository;

  @Mock
  private TaskGroupRepository taskGroupRepository;

  @Mock
  private TaskRepository taskRepository;

  @InjectMocks
  private MemoService sut;

  private static MemoRequest request(String comment) {
    MemoRequest request = new MemoRequest();
    request.setComment(comment);
    return request;
  }

  @Test
  void プロジェクトメモ登録成功_親プロジェクトの存在チェック後にメモを登録すること() {
    // Arrange
    int projectId = 1;
    MemoRequest request = request("プロジェクトメモ");
    when(projectRepository.existsByIdAndUserId(projectId, USER_ID)).thenReturn(true);
    doAnswer(invocation -> {
      invocation.<Memo>getArgument(0).setId(10);
      return null;
    }).when(memoRepository).insert(any(Memo.class));
    Memo registered = new Memo(10, projectId, null, null, "プロジェクトメモ");
    when(memoRepository.findById(10)).thenReturn(registered);

    // Act
    Memo actual = sut.registerInProject(USER_ID, projectId, request);

    // Assert
    assertThat(actual.getProjectId()).isEqualTo(projectId);
    assertThat(actual.getTaskGroupId()).isNull();
    assertThat(actual.getTaskId()).isNull();
    assertThat(actual.getComment()).isEqualTo("プロジェクトメモ");
    verify(projectRepository, times(1)).existsByIdAndUserId(projectId, USER_ID);
    verify(taskGroupRepository, never()).existsByIdAndUserId(anyInt(), anyInt());
    verify(taskRepository, never()).existsByIdAndUserId(anyInt(), anyInt());
    verify(memoRepository, times(1)).insert(any(Memo.class));
  }

  @Test
  void プロジェクトメモ登録失敗_親プロジェクトが存在しない場合は例外を投げて登録しないこと() {
    // Arrange
    int projectId = 999;
    when(projectRepository.existsByIdAndUserId(projectId, USER_ID)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.registerInProject(USER_ID, projectId, request("メモ")))
        .isInstanceOf(TargetNotFoundException.class);

    verify(projectRepository, times(1)).existsByIdAndUserId(projectId, USER_ID);
    verify(memoRepository, never()).insert(any());
  }

  @Test
  void タスクグループメモ登録成功_親タスクグループの存在チェック後にメモを登録すること() {
    // Arrange
    int taskGroupId = 1;
    MemoRequest request = request("タスクグループメモ");
    when(taskGroupRepository.existsByIdAndUserId(taskGroupId, USER_ID)).thenReturn(true);
    doAnswer(invocation -> {
      invocation.<Memo>getArgument(0).setId(10);
      return null;
    }).when(memoRepository).insert(any(Memo.class));
    Memo registered = new Memo(10, null, taskGroupId, null, "タスクグループメモ");
    when(memoRepository.findById(10)).thenReturn(registered);

    // Act
    Memo actual = sut.registerInTaskGroup(USER_ID, taskGroupId, request);

    // Assert
    assertThat(actual.getProjectId()).isNull();
    assertThat(actual.getTaskGroupId()).isEqualTo(taskGroupId);
    assertThat(actual.getTaskId()).isNull();
    assertThat(actual.getComment()).isEqualTo("タスクグループメモ");
    verify(taskGroupRepository, times(1)).existsByIdAndUserId(taskGroupId, USER_ID);
    verify(projectRepository, never()).existsByIdAndUserId(anyInt(), anyInt());
    verify(taskRepository, never()).existsByIdAndUserId(anyInt(), anyInt());
    verify(memoRepository, times(1)).insert(any(Memo.class));
  }

  @Test
  void タスクグループメモ登録失敗_親タスクグループが存在しない場合は例外を投げて登録しないこと() {
    // Arrange
    int taskGroupId = 999;
    when(taskGroupRepository.existsByIdAndUserId(taskGroupId, USER_ID)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.registerInTaskGroup(USER_ID, taskGroupId, request("メモ")))
        .isInstanceOf(TargetNotFoundException.class);

    verify(taskGroupRepository, times(1)).existsByIdAndUserId(taskGroupId, USER_ID);
    verify(memoRepository, never()).insert(any());
  }

  @Test
  void タスクメモ登録成功_親タスクの存在チェック後にメモを登録すること() {
    // Arrange
    int taskId = 1;
    MemoRequest request = request("タスクメモ");
    when(taskRepository.existsByIdAndUserId(taskId, USER_ID)).thenReturn(true);
    doAnswer(invocation -> {
      invocation.<Memo>getArgument(0).setId(10);
      return null;
    }).when(memoRepository).insert(any(Memo.class));
    Memo registered = new Memo(10, null, null, taskId, "タスクメモ");
    when(memoRepository.findById(10)).thenReturn(registered);

    // Act
    Memo actual = sut.registerInTask(USER_ID, taskId, request);

    // Assert
    assertThat(actual.getProjectId()).isNull();
    assertThat(actual.getTaskGroupId()).isNull();
    assertThat(actual.getTaskId()).isEqualTo(taskId);
    assertThat(actual.getComment()).isEqualTo("タスクメモ");
    verify(taskRepository, times(1)).existsByIdAndUserId(taskId, USER_ID);
    verify(projectRepository, never()).existsByIdAndUserId(anyInt(), anyInt());
    verify(taskGroupRepository, never()).existsByIdAndUserId(anyInt(), anyInt());
    verify(memoRepository, times(1)).insert(any(Memo.class));
  }

  @Test
  void タスクメモ登録失敗_親タスクが存在しない場合は例外を投げて登録しないこと() {
    // Arrange
    int taskId = 999;
    when(taskRepository.existsByIdAndUserId(taskId, USER_ID)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.registerInTask(USER_ID, taskId, request("メモ")))
        .isInstanceOf(TargetNotFoundException.class);

    verify(taskRepository, times(1)).existsByIdAndUserId(taskId, USER_ID);
    verify(memoRepository, never()).insert(any());
  }

  @Test
  void メモ更新成功_リポジトリにIDとコメントを渡して更新すること() {
    // Arrange
    int id = 1;
    when(memoRepository.update(any(Memo.class), eq(USER_ID))).thenReturn(1);
    Memo updated = new Memo(id, null, null, 1, "DB更新後メモ");
    when(memoRepository.findById(id)).thenReturn(updated);

    // Act
    Memo actual = sut.update(USER_ID, id, request("更新後メモ"));

    // Assert
    ArgumentCaptor<Memo> captor = ArgumentCaptor.forClass(Memo.class);
    verify(memoRepository, times(1)).update(captor.capture(), eq(USER_ID));
    assertThat(captor.getValue().getId()).isEqualTo(id);
    assertThat(captor.getValue().getComment()).isEqualTo("更新後メモ");
    assertThat(actual.getComment()).isEqualTo("DB更新後メモ");
  }

  @Test
  void メモ更新失敗_更新件数が0件のときTargetNotFoundExceptionを投げること() {
    // Arrange
    when(memoRepository.update(any(Memo.class), eq(USER_ID))).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.update(USER_ID, 999, request("更新後メモ")))
        .isInstanceOf(TargetNotFoundException.class);
  }

  @Test
  void メモ削除成功_リポジトリにIDを渡して削除すること() {
    // Arrange
    int id = 1;
    when(memoRepository.delete(id, USER_ID)).thenReturn(1);

    // Act
    sut.delete(USER_ID, id);

    // Assert
    verify(memoRepository, times(1)).delete(id, USER_ID);
  }

  @Test
  void メモ削除失敗_削除件数が0件のときTargetNotFoundExceptionを投げること() {
    // Arrange
    when(memoRepository.delete(999, USER_ID)).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.delete(USER_ID, 999))
        .isInstanceOf(TargetNotFoundException.class);
  }
}
