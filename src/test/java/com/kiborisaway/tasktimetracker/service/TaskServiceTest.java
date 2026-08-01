package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.data.Task;
import com.kiborisaway.tasktimetracker.exception.EstimateMinutesUpdateNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import com.kiborisaway.tasktimetracker.repository.TaskGroupRepository;
import com.kiborisaway.tasktimetracker.repository.TaskRepository;
import com.kiborisaway.tasktimetracker.repository.WorkSessionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  @Mock
  private TaskRepository tsRepository;

  @Mock
  private WorkSessionRepository wsRepository;

  @Mock
  private TaskGroupRepository tgRepository;

  @Mock
  private ProjectRepository pjRepository;

  @InjectMocks
  private TaskService sut;

  @Test
  void タスクグループ内タスク一覧検索成功_第2引数にnullを指定するとタスクグループ内全件検索用のリポジトリのメソッドを呼び出すこと() {
    // Arrange
    int tgId = 1;
    Task task1 = new Task(1, null, tgId, "タスク１", "説明", 60, null, null, null, null, null);
    Task task2 = new Task(2, null, tgId, "タスク２", null, 120, null, null, null, null, null);

    List<Task> expected = List.of(task1, task2);

    when(tgRepository.existsById(tgId)).thenReturn(true);
    when(tsRepository.findAllInTaskGroup(tgId)).thenReturn(expected);

    // Act
    List<Task> actual = sut.findAllInTaskGroupByCondition(tgId, null);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(tgRepository, times(1)).existsById(tgId);
    verify(tsRepository, times(1)).findAllInTaskGroup(tgId);
    verify(tsRepository, never()).findAllInTaskGroupByCondition(anyInt(), anyBoolean());
  }

  @ParameterizedTest(name = "[{index}]タスクグループ内タスク一覧検索_第2引数に{0}を指定すると完了フラグ指定検索用のリポジトリのメソッドに{0}を指定して呼び出すこと")
  @ValueSource(booleans = {true, false})
  void タスクグループ内タスク一覧検索成功_第2引数に渡したbool値をそのまま完了フラグ指定検索用のリポジトリのメソッドに渡して呼び出すこと(
      boolean flg) {
    // Arrange
    int tgId = 1;
    Task task = new Task(1, null, tgId, "タスク１", "説明", 60, null, null, null, null, null);

    List<Task> expected = List.of(task);

    when(tgRepository.existsById(tgId)).thenReturn(true);
    when(tsRepository.findAllInTaskGroupByCondition(tgId, flg)).thenReturn(expected);

    // Act
    List<Task> actual = sut.findAllInTaskGroupByCondition(tgId, flg);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(tgRepository, times(1)).existsById(tgId);
    verify(tsRepository, times(1)).findAllInTaskGroupByCondition(tgId, flg);
    verify(tsRepository, never()).findAllInTaskGroup(anyInt());
  }

  @Test
  void タスクグループ内タスク一覧検索失敗_指定したタスクグループのIDが存在しない場合は例外を投げてその後のリポジトリの処理を呼び出さないこと() {
    // Arrange
    int tgId = 999;
    when(tgRepository.existsById(tgId)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.findAllInTaskGroupByCondition(tgId, null))
        .isInstanceOf(TargetNotFoundException.class);

    verify(tgRepository, times(1)).existsById(tgId);
    verify(tsRepository, never()).findAllInTaskGroup(anyInt());
    verify(tsRepository, never()).findAllInTaskGroupByCondition(anyInt(), anyBoolean());
  }

  @Test
  void プロジェクト内タスク一覧検索成功_第2引数にnullを指定するとプロジェクト内全件検索用のリポジトリのメソッドを呼び出すこと() {
    // Arrange
    int pId = 1;
    Task task1 = new Task(1, pId, null, "タスク１", "説明", 60, null, null, null, null, null);
    Task task2 = new Task(2, null, 1, "タスク２", null, 120, null, null, null, null, null);

    List<Task> expected = List.of(task1, task2);

    when(pjRepository.existsById(pId)).thenReturn(true);
    when(tsRepository.findAllInProject(pId)).thenReturn(expected);

    // Act
    List<Task> actual = sut.findAllInProjectByCondition(pId, null);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(pjRepository, times(1)).existsById(pId);
    verify(tsRepository, times(1)).findAllInProject(pId);
    verify(tsRepository, never()).findAllInProjectByCondition(anyInt(), anyBoolean());
  }

  @ParameterizedTest(name = "[{index}]プロジェクト内タスク一覧検索_第2引数に{0}を指定すると完了フラグ指定検索用のリポジトリのメソッドに{0}を指定して呼び出すこと")
  @ValueSource(booleans = {true, false})
  void プロジェクト内タスク一覧検索成功_第2引数に渡したbool値をそのまま完了フラグ指定検索用のリポジトリのメソッドに渡して呼び出すこと(
      boolean flg) {
    // Arrange
    int pId = 1;
    Task task = new Task(1, pId, null, "タスク１", "説明", 60, null, null, null, null, null);

    List<Task> expected = List.of(task);

    when(pjRepository.existsById(pId)).thenReturn(true);
    when(tsRepository.findAllInProjectByCondition(pId, flg)).thenReturn(expected);

    // Act
    List<Task> actual = sut.findAllInProjectByCondition(pId, flg);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(pjRepository, times(1)).existsById(pId);
    verify(tsRepository, times(1)).findAllInProjectByCondition(pId, flg);
    verify(tsRepository, never()).findAllInProject(anyInt());
  }

  @Test
  void プロジェクト内タスク一覧検索失敗_指定したプロジェクトのIDが存在しない場合は例外を投げてその後のリポジトリの処理を呼び出さないこと() {
    // Arrange
    int pId = 999;
    when(pjRepository.existsById(pId)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.findAllInProjectByCondition(pId, null))
        .isInstanceOf(TargetNotFoundException.class);

    verify(pjRepository, times(1)).existsById(pId);
    verify(tsRepository, never()).findAllInProject(anyInt());
    verify(tsRepository, never()).findAllInProjectByCondition(anyInt(), anyBoolean());
  }

  @Test
  void ID検索成功_タスクを取得できること() {
    // Arrange
    int id = 1;
    Task expected = new Task(id, 1, null, "タスク１", "説明", 60, null, null, null, null, null);

    when(tsRepository.findById(id)).thenReturn(expected);

    // Act
    Task actual = sut.findById(id);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(tsRepository, times(1)).findById(id);
  }

  @Test
  void ID検索失敗_リポジトリからnullが返ったら例外を投げること() {
    // Arrange
    int id = 999;

    when(tsRepository.findById(id)).thenReturn(null);

    // Act & Assert
    assertThatThrownBy(() -> sut.findById(id))
        .isInstanceOf(TargetNotFoundException.class);
  }

  @Test
  void 登録成功_プロジェクトIDを持つタスクの場合はプロジェクトの存在チェックをしてリポジトリの処理を呼び出すこと() {
    // Arrange
    int pId = 1;
    Task task = new Task(pId, null, "タスク１", "説明", 60);

    when(pjRepository.existsById(pId)).thenReturn(true);

    // Act
    Task actual = sut.register(task);

    // Assert
    assertThat(actual).isSameAs(task);
    verify(pjRepository, times(1)).existsById(pId);
    verify(tgRepository, never()).existsById(anyInt());
    verify(tsRepository, times(1)).insert(same(task));
  }

  @Test
  void 登録成功_タスクグループIDを持つタスクの場合はタスクグループの存在チェックをしてリポジトリの処理を呼び出すこと() {
    // Arrange
    int tgId = 1;
    Task task = new Task(null, tgId, "タスク１", "説明", 60);

    when(tgRepository.existsById(tgId)).thenReturn(true);

    // Act
    Task actual = sut.register(task);

    // Assert
    assertThat(actual).isSameAs(task);
    verify(tgRepository, times(1)).existsById(tgId);
    verify(pjRepository, never()).existsById(anyInt());
    verify(tsRepository, times(1)).insert(same(task));
  }

  @Test
  void 登録失敗_指定したプロジェクトが存在しない場合は例外を投げて以降のリポジトリの処理を呼び出さないこと() {
    // Arrange
    int pId = 999;
    Task task = new Task(pId, null, "タスク１", "説明", 60);

    when(pjRepository.existsById(pId)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.register(task))
        .isInstanceOf(TargetNotFoundException.class);

    verify(pjRepository, times(1)).existsById(pId);
    verify(tgRepository, never()).existsById(anyInt());
    verify(tsRepository, never()).insert(any());
  }

  @Test
  void 登録失敗_指定したタスクグループが存在しない場合は例外を投げて以降のリポジトリの処理を呼び出さないこと() {
    // Arrange
    int tgId = 999;
    Task task = new Task(null, tgId, "タスク１", "説明", 60);

    when(tgRepository.existsById(tgId)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.register(task))
        .isInstanceOf(TargetNotFoundException.class);

    verify(tgRepository, times(1)).existsById(tgId);
    verify(pjRepository, never()).existsById(anyInt());
    verify(tsRepository, never()).insert(any());
  }

  @Test
  void 登録失敗_DB制約違反の例外をそのまま送出すること() {
    // Arrange
    int pId = 1;
    Task task = new Task(pId, null, null, "説明", 60);

    when(pjRepository.existsById(pId)).thenReturn(true);
    doThrow(new DataIntegrityViolationException("db constraint violation"))
        .when(tsRepository).insert(same(task));

    // Act & Assert
    assertThatThrownBy(() -> sut.register(task))
        .isInstanceOf(DataIntegrityViolationException.class);

    verify(pjRepository, times(1)).existsById(pId);
    verify(tsRepository, times(1)).insert(same(task));
  }

  @Test
  void タスク名説明更新成功_リポジトリのメソッドを呼び出すこと() {
    // Arrange
    Task task = new Task(1, 1, null, "更新後タスク", "説明更新", 60, null, null, null, null, null);

    when(tsRepository.updateProperty(task)).thenReturn(1);

    // Act
    sut.updateProperty(task);

    // Assert
    verify(tsRepository, times(1)).updateProperty(same(task));
  }

  @Test
  void タスク名説明更新失敗_DB制約違反の例外をそのまま送出すること() {
    // Arrange
    Task task = new Task(1, 1, null, null, "説明更新", 60, null, null, null, null, null);

    when(tsRepository.updateProperty(task))
        .thenThrow(new DataIntegrityViolationException("db constraint violation"));

    // Act & Assert
    assertThatThrownBy(() -> sut.updateProperty(task))
        .isInstanceOf(DataIntegrityViolationException.class);

    verify(tsRepository, times(1)).updateProperty(same(task));
  }

  @Test
  void タスク名説明更新失敗_更新件数が0件のときTargetNotFoundExceptionを投げること() {
    // Arrange
    Task task = new Task(999, 1, null, "更新後タスク", "説明更新", 60, null, null, null, null,
        null);

    when(tsRepository.updateProperty(task)).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.updateProperty(task))
        .isInstanceOf(TargetNotFoundException.class);

    verify(tsRepository, times(1)).updateProperty(same(task));
  }

  @Test
  void 見積もり作業時間更新成功_WorkSessionが存在しない場合はtsRepositoryのメソッドを呼び出すこと() {
    // Arrange
    int taskId = 1;
    int estimatedMinutes = 120;

    when(wsRepository.existsByTaskId(taskId)).thenReturn(false);
    when(tsRepository.updateEstimateMinutes(taskId, estimatedMinutes)).thenReturn(1);

    // Act
    sut.updateEstimateMinutes(taskId, estimatedMinutes);

    // Assert
    verify(wsRepository, times(1)).existsByTaskId(taskId);
    verify(tsRepository, times(1)).updateEstimateMinutes(taskId, estimatedMinutes);
  }

  @Test
  void 見積もり作業時間更新失敗_WorkSessionが存在する場合は例外を投げて更新処理を呼び出さないこと() {
    // Arrange
    int taskId = 1;
    int estimatedMinutes = 120;

    when(wsRepository.existsByTaskId(taskId)).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> sut.updateEstimateMinutes(taskId, estimatedMinutes))
        .isInstanceOf(EstimateMinutesUpdateNotAllowedException.class);

    verify(wsRepository, times(1)).existsByTaskId(taskId);
    verify(tsRepository, never()).updateEstimateMinutes(anyInt(), anyInt());
  }

  @Test
  void 見積もり作業時間更新失敗_更新件数が0件のときTargetNotFoundExceptionを投げること() {
    // Arrange
    int taskId = 999;
    int estimatedMinutes = 120;

    when(wsRepository.existsByTaskId(taskId)).thenReturn(false);
    when(tsRepository.updateEstimateMinutes(taskId, estimatedMinutes)).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.updateEstimateMinutes(taskId, estimatedMinutes))
        .isInstanceOf(TargetNotFoundException.class);

    verify(wsRepository, times(1)).existsByTaskId(taskId);
    verify(tsRepository, times(1)).updateEstimateMinutes(taskId, estimatedMinutes);
  }

  @Test
  void 完了状態更新成功_リポジトリのメソッドに引数のIDと完了状態を渡して呼び出すこと() {
    // Arrange
    int id = 1;
    boolean isFinished = true;

    when(tsRepository.updateFinished(id, isFinished)).thenReturn(1);

    // Act
    sut.updateFinished(id, isFinished);

    // Assert
    verify(tsRepository, times(1)).updateFinished(id, isFinished);
  }

  @Test
  void 完了状態更新成功_未完了に戻す場合もリポジトリのメソッドにfalseを渡して呼び出すこと() {
    // Arrange
    int id = 1;
    boolean isFinished = false;

    when(tsRepository.updateFinished(id, isFinished)).thenReturn(1);

    // Act
    sut.updateFinished(id, isFinished);

    // Assert
    verify(tsRepository, times(1)).updateFinished(id, isFinished);
  }

  @Test
  void 完了状態更新失敗_更新件数が0件のときTargetNotFoundExceptionを投げること() {
    // Arrange
    int id = 999;
    boolean isFinished = true;

    when(tsRepository.updateFinished(id, isFinished)).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.updateFinished(id, isFinished))
        .isInstanceOf(TargetNotFoundException.class);

    verify(tsRepository, times(1)).updateFinished(id, isFinished);
  }

  @Test
  void 削除成功_リポジトリのメソッドに引数のIDを渡して呼び出すこと() {
    // Arrange
    int id = 1;

    when(tsRepository.deleteById(id)).thenReturn(1);

    // Act
    sut.deleteById(id);

    // Assert
    verify(tsRepository, times(1)).deleteById(id);
  }

  @Test
  void 削除失敗_削除件数が0件のときTargetNotFoundExceptionを投げること() {
    // Arrange
    int id = 999;

    when(tsRepository.deleteById(id)).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.deleteById(id))
        .isInstanceOf(TargetNotFoundException.class);

    verify(tsRepository, times(1)).deleteById(id);
  }

}
