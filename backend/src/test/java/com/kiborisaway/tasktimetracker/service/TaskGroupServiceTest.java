package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.data.dto.task_group.TaskGroupCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.task_group.TaskGroupResponse;
import com.kiborisaway.tasktimetracker.data.dto.task_group.TaskGroupUpdateRequest;
import com.kiborisaway.tasktimetracker.data.entity.TaskGroup;
import com.kiborisaway.tasktimetracker.data.entity.Memo;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.MemoRepository;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import com.kiborisaway.tasktimetracker.repository.TaskGroupRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class TaskGroupServiceTest {

  @Mock
  private TaskGroupRepository tgRepository;

  @Mock
  private ProjectRepository prRepository;

  @Mock
  private MemoRepository memoRepository;

  @InjectMocks
  private TaskGroupService sut;

  @Test
  void タスクグループ一覧検索成功_第2引数にnullを指定するとプロジェクト内全件検索用のリポジトリのメソッドを呼び出すこと() {
    // Arrange
    int pId = 1;
    TaskGroup tg1 = new TaskGroup(1, pId, "タスクグループ１", "説明", false);
    TaskGroup tg2 = new TaskGroup(2, pId, "タスクグループ２", null, true);

    List<TaskGroup> expected = List.of(tg1, tg2);

    when(prRepository.existsById(pId)).thenReturn(true);
    when(tgRepository.findAllInProject(pId)).thenReturn(expected);
    when(memoRepository.findAllInTaskGroups(List.of(1, 2))).thenReturn(List.of(
        new Memo(1, null, 1, null, "タスクグループ1メモ"),
        new Memo(2, null, 2, null, "タスクグループ2メモ")));

    // Act
    List<TaskGroupResponse> actual = sut.findAllByCondition(pId, null);

    // Assert
    assertThat(actual)
        .extracting(TaskGroupResponse::getId, TaskGroupResponse::getProjectId,
            TaskGroupResponse::getTitle, TaskGroupResponse::getDescription,
            TaskGroupResponse::getIsFinished)
        .containsExactly(
            org.assertj.core.api.Assertions.tuple(1, pId, "タスクグループ１", "説明", false),
            org.assertj.core.api.Assertions.tuple(2, pId, "タスクグループ２", null, true)
        );
    verify(prRepository, times(1)).existsById(pId);
    verify(tgRepository, times(1)).findAllInProject(pId);
    verify(tgRepository, never()).findAllInProjectByIsFinished(anyInt(), anyBoolean());
    verify(memoRepository, times(1)).findAllInTaskGroups(List.of(1, 2));
    verify(memoRepository, never()).findAllInTaskGroup(anyInt());
    assertThat(actual).allSatisfy(response -> assertThat(response.getMemos()).hasSize(1));
  }

  @ParameterizedTest(name = "[{index}]タスクグループ一覧検索_第2引数に{0}を指定すると完了フラグ指定検索用のリポジトリのメソッドに{0}を指定して呼び出すこと")
  @ValueSource(booleans = {true, false})
  void タスクグループ一覧検索失敗_第2引数に渡したbool値をそのまま完了フラグ指定検索用のリポジトリのメソッドに渡して呼び出すこと(
      boolean flg) {
    // Arrange
    int pId = 1;
    TaskGroup tg = new TaskGroup(2, pId, "タスクグループ２", null, true);

    List<TaskGroup> expected = List.of(tg);

    when(prRepository.existsById(pId)).thenReturn(true);
    when(tgRepository.findAllInProjectByIsFinished(pId, flg)).thenReturn(expected);

    // Act
    List<TaskGroupResponse> actual = sut.findAllByCondition(pId, flg);

    // Assert
    assertThat(actual)
        .extracting(TaskGroupResponse::getId, TaskGroupResponse::getProjectId,
            TaskGroupResponse::getTitle)
        .containsExactly(org.assertj.core.api.Assertions.tuple(2, pId, "タスクグループ２"));
    verify(prRepository, times(1)).existsById(pId);
    verify(tgRepository, times(1)).findAllInProjectByIsFinished(pId, flg);
    verify(tgRepository, never()).findAllInProject(anyInt());
  }

  @Test
  void タスクグループ一覧検索失敗_指定したプロジェクトのIDが存在しない場合は例外を投げてその後のリポジトリの処理を呼び出さないこと() {
    // Arrange
    int pId = 999;
    when(prRepository.existsById(pId)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.findAllByCondition(pId, null))
        .isInstanceOf(TargetNotFoundException.class);
    verify(prRepository, times(1)).existsById(pId);
    verify(tgRepository, never()).findAllInProject(anyInt());
    verify(tgRepository, never()).findAllInProjectByIsFinished(anyInt(), anyBoolean());
  }

  @Test
  void タスクグループ一覧検索成功_検索結果が空ならメモ検索を実行しないこと() {
    int pId = 1;
    when(prRepository.existsById(pId)).thenReturn(true);
    when(tgRepository.findAllInProject(pId)).thenReturn(List.of());

    List<TaskGroupResponse> actual = sut.findAllByCondition(pId, null);

    assertThat(actual).isEmpty();
    verify(memoRepository, never()).findAllInTaskGroups(any());
  }

  @Test
  void ID検索成功_タスクグループを取得できること() {
    // Arrange
    int id = 1;
    TaskGroup expected = new TaskGroup(id, 1, "タスクグループ１", "説明", false);

    when(tgRepository.findById(id)).thenReturn(expected);

    // Act
    TaskGroupResponse actual = sut.findById(id);

    // Assert
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getProjectId()).isEqualTo(expected.getProjectId());
    assertThat(actual.getTitle()).isEqualTo(expected.getTitle());
    assertThat(actual.getDescription()).isEqualTo(expected.getDescription());
    assertThat(actual.getIsFinished()).isEqualTo(expected.getIsFinished());
    assertThat(actual.getMemos()).isEmpty();
    verify(tgRepository, times(1)).findById(id);
  }

  @Test
  void ID検索失敗_リポジトリからnullが返ったら例外を投げること() {
    // Arrange
    int id = 999;

    when(tgRepository.findById(id)).thenReturn(null);

    // Assert
    assertThatThrownBy(() -> sut.findById(id))
        .isInstanceOf(TargetNotFoundException.class);
  }

  @Test
  void 登録成功_引数のタスクグループインスタンスにプロジェクトIDをセットしてリポジトリの処理を呼び出すこと() {
    // Arrange
    int pId = 2;
    TaskGroupCreateRequest request = new TaskGroupCreateRequest();
    request.setTitle("タスクグループ２");
    request.setDescription(null);
    when(prRepository.existsById(pId)).thenReturn(true);
    doAnswer(invocation -> {
      TaskGroup taskGroup = invocation.getArgument(0);
      taskGroup.setId(10);
      return null;
    }).when(tgRepository).insert(any(TaskGroup.class));
    TaskGroup registered = new TaskGroup(10, pId, "タスクグループ２", null, false);
    when(tgRepository.findById(10)).thenReturn(registered);

    // Act
    TaskGroupResponse actual = sut.register(pId, request);

    // Assert
    assertThat(actual.getProjectId()).isEqualTo(pId);
    assertThat(actual.getTitle()).isEqualTo("タスクグループ２");
    assertThat(actual.getMemos()).isEmpty();
    verify(prRepository, times(1)).existsById(pId);
    ArgumentCaptor<TaskGroup> captor = ArgumentCaptor.forClass(TaskGroup.class);
    verify(tgRepository, times(1)).insert(captor.capture());
    assertThat(captor.getValue().getProjectId()).isEqualTo(pId);
    assertThat(captor.getValue().getTitle()).isEqualTo("タスクグループ２");
  }

  @Test
  void 登録失敗_指定したプロジェクトが存在しない場合は例外を投げて以降のリポジトリの処理を呼び出さないこと() {
    // Arrange
    int pId = 999;
    TaskGroupCreateRequest request = new TaskGroupCreateRequest();
    request.setTitle("タイトル");
    request.setDescription("説明");
    when(prRepository.existsById(pId)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.register(pId, request))
        .isInstanceOf(TargetNotFoundException.class);
    verify(prRepository, times(1)).existsById(pId);
    verify(tgRepository, never()).insert(any());
  }

  @Test
  void 登録失敗_DB制約違反の例外をそのまま送出すること() {
    // Arrange
    int pId = 1;
    TaskGroupCreateRequest request = new TaskGroupCreateRequest();
    request.setTitle(null);
    request.setDescription("説明");

    when(prRepository.existsById(pId)).thenReturn(true);
    doThrow(new DataIntegrityViolationException("db constraint violation"))
        .when(tgRepository).insert(any(TaskGroup.class));

    // Act & Assert
    assertThatThrownBy(() -> sut.register(pId, request))
        .isInstanceOf(DataIntegrityViolationException.class);

    verify(prRepository, times(1)).existsById(pId);
    verify(tgRepository, times(1)).insert(any(TaskGroup.class));
  }

  @Test
  void 更新成功_リポジトリのメソッドを呼び出すこと() {
    // Arrange
    int id = 1;
    TaskGroupUpdateRequest request = new TaskGroupUpdateRequest();
    request.setTitle("タスクグループ１");
    request.setDescription("説明");
    request.setIsFinished(false);

    when(tgRepository.update(any(TaskGroup.class))).thenReturn(1);
    TaskGroup updated = new TaskGroup(id, 1, "DB更新後", "DB更新後説明", false);
    when(tgRepository.findById(id)).thenReturn(updated);

    // Act
    TaskGroupResponse actual = sut.update(id, request);

    // Assert
    ArgumentCaptor<TaskGroup> captor = ArgumentCaptor.forClass(TaskGroup.class);
    verify(tgRepository, times(1)).update(captor.capture());
    assertThat(captor.getValue().getId()).isEqualTo(id);
    assertThat(captor.getValue().getTitle()).isEqualTo("タスクグループ１");
    assertThat(actual.getTitle()).isEqualTo("DB更新後");
  }

  @Test
  void 更新失敗_DB制約違反の例外をそのまま送出すること() {
    // Arrange
    int id = 1;
    TaskGroupUpdateRequest request = new TaskGroupUpdateRequest();
    request.setTitle("タスクグループ１");
    request.setDescription("説明更新");
    request.setIsFinished(false);

    when(tgRepository.update(any(TaskGroup.class)))
        .thenThrow(new DataIntegrityViolationException("db constraint violation"));

    // Act & Assert
    assertThatThrownBy(() -> sut.update(id, request))
        .isInstanceOf(DataIntegrityViolationException.class);

    verify(tgRepository, times(1)).update(any(TaskGroup.class));
    verify(tgRepository, never()).findById(id);
  }

  @Test
  void 更新失敗_更新件数が0件のときTargetNotFoundExceptionを投げること() {
    // Arrange
    int id = 999;
    TaskGroupUpdateRequest request = new TaskGroupUpdateRequest();
    request.setTitle("タスクグループ１");
    request.setDescription("説明");
    request.setIsFinished(false);

    when(tgRepository.update(any(TaskGroup.class))).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.update(id, request))
        .isInstanceOf(TargetNotFoundException.class);

    verify(tgRepository, times(1)).update(any(TaskGroup.class));
  }

}
