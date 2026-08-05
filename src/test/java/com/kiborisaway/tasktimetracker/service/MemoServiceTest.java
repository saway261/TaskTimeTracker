package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
    when(projectRepository.existsById(projectId)).thenReturn(true);

    // Act
    Memo actual = sut.registerInProject(projectId, request);

    // Assert
    assertThat(actual.getProjectId()).isEqualTo(projectId);
    assertThat(actual.getTaskGroupId()).isNull();
    assertThat(actual.getTaskId()).isNull();
    assertThat(actual.getComment()).isEqualTo("プロジェクトメモ");
    verify(projectRepository, times(1)).existsById(projectId);
    verify(taskGroupRepository, never()).existsById(anyInt());
    verify(taskRepository, never()).existsById(anyInt());
    verify(memoRepository, times(1)).insert(actual);
  }

  @Test
  void プロジェクトメモ登録失敗_親プロジェクトが存在しない場合は例外を投げて登録しないこと() {
    // Arrange
    int projectId = 999;
    when(projectRepository.existsById(projectId)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.registerInProject(projectId, request("メモ")))
        .isInstanceOf(TargetNotFoundException.class);

    verify(projectRepository, times(1)).existsById(projectId);
    verify(memoRepository, never()).insert(any());
  }

  @Test
  void タスクグループメモ登録成功_親タスクグループの存在チェック後にメモを登録すること() {
    // Arrange
    int taskGroupId = 1;
    MemoRequest request = request("タスクグループメモ");
    when(taskGroupRepository.existsById(taskGroupId)).thenReturn(true);

    // Act
    Memo actual = sut.registerInTaskGroup(taskGroupId, request);

    // Assert
    assertThat(actual.getProjectId()).isNull();
    assertThat(actual.getTaskGroupId()).isEqualTo(taskGroupId);
    assertThat(actual.getTaskId()).isNull();
    assertThat(actual.getComment()).isEqualTo("タスクグループメモ");
    verify(taskGroupRepository, times(1)).existsById(taskGroupId);
    verify(projectRepository, never()).existsById(anyInt());
    verify(taskRepository, never()).existsById(anyInt());
    verify(memoRepository, times(1)).insert(actual);
  }

  @Test
  void タスクグループメモ登録失敗_親タスクグループが存在しない場合は例外を投げて登録しないこと() {
    // Arrange
    int taskGroupId = 999;
    when(taskGroupRepository.existsById(taskGroupId)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.registerInTaskGroup(taskGroupId, request("メモ")))
        .isInstanceOf(TargetNotFoundException.class);

    verify(taskGroupRepository, times(1)).existsById(taskGroupId);
    verify(memoRepository, never()).insert(any());
  }

  @Test
  void タスクメモ登録成功_親タスクの存在チェック後にメモを登録すること() {
    // Arrange
    int taskId = 1;
    MemoRequest request = request("タスクメモ");
    when(taskRepository.existsById(taskId)).thenReturn(true);

    // Act
    Memo actual = sut.registerInTask(taskId, request);

    // Assert
    assertThat(actual.getProjectId()).isNull();
    assertThat(actual.getTaskGroupId()).isNull();
    assertThat(actual.getTaskId()).isEqualTo(taskId);
    assertThat(actual.getComment()).isEqualTo("タスクメモ");
    verify(taskRepository, times(1)).existsById(taskId);
    verify(projectRepository, never()).existsById(anyInt());
    verify(taskGroupRepository, never()).existsById(anyInt());
    verify(memoRepository, times(1)).insert(actual);
  }

  @Test
  void タスクメモ登録失敗_親タスクが存在しない場合は例外を投げて登録しないこと() {
    // Arrange
    int taskId = 999;
    when(taskRepository.existsById(taskId)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.registerInTask(taskId, request("メモ")))
        .isInstanceOf(TargetNotFoundException.class);

    verify(taskRepository, times(1)).existsById(taskId);
    verify(memoRepository, never()).insert(any());
  }

  @Test
  void メモ更新成功_リポジトリにIDとコメントを渡して更新すること() {
    // Arrange
    int id = 1;
    when(memoRepository.update(any(Memo.class))).thenReturn(1);

    // Act
    sut.update(id, request("更新後メモ"));

    // Assert
    ArgumentCaptor<Memo> captor = ArgumentCaptor.forClass(Memo.class);
    verify(memoRepository, times(1)).update(captor.capture());
    assertThat(captor.getValue().getId()).isEqualTo(id);
    assertThat(captor.getValue().getComment()).isEqualTo("更新後メモ");
  }

  @Test
  void メモ更新失敗_更新件数が0件のときTargetNotFoundExceptionを投げること() {
    // Arrange
    when(memoRepository.update(any(Memo.class))).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.update(999, request("更新後メモ")))
        .isInstanceOf(TargetNotFoundException.class);
  }

  @Test
  void メモ削除成功_リポジトリにIDを渡して削除すること() {
    // Arrange
    int id = 1;
    when(memoRepository.delete(id)).thenReturn(1);

    // Act
    sut.delete(id);

    // Assert
    verify(memoRepository, times(1)).delete(id);
  }

  @Test
  void メモ削除失敗_削除件数が0件のときTargetNotFoundExceptionを投げること() {
    // Arrange
    when(memoRepository.delete(999)).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.delete(999))
        .isInstanceOf(TargetNotFoundException.class);
  }
}
