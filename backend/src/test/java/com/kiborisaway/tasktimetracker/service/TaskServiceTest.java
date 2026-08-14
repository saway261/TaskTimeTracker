package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.data.dto.task.TaskCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.task.TaskResponse;
import com.kiborisaway.tasktimetracker.data.dto.task.TaskUpdateEstimatedMinutesRequest;
import com.kiborisaway.tasktimetracker.data.dto.task.TaskUpdatePropertyRequest;
import com.kiborisaway.tasktimetracker.data.entity.Task;
import com.kiborisaway.tasktimetracker.data.entity.TaskGroup;
import com.kiborisaway.tasktimetracker.data.entity.Memo;
import com.kiborisaway.tasktimetracker.exception.EstimateMinutesUpdateNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.exception.TaskFinishNotAllowedException;
import com.kiborisaway.tasktimetracker.repository.MemoRepository;
import com.kiborisaway.tasktimetracker.repository.ProjectItemOrderRepository;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import com.kiborisaway.tasktimetracker.repository.TaskGroupItemOrderRepository;
import com.kiborisaway.tasktimetracker.repository.TaskGroupRepository;
import com.kiborisaway.tasktimetracker.repository.TaskRepository;
import com.kiborisaway.tasktimetracker.repository.WorkSessionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  private static final int USER_ID = 1;

  @Mock
  private TaskRepository tsRepository;

  @Mock
  private WorkSessionRepository wsRepository;

  @Mock
  private TaskGroupRepository tgRepository;

  @Mock
  private ProjectRepository pjRepository;

  @Mock
  private MemoRepository memoRepository;

  @Mock
  private ProjectItemOrderRepository pjItemOrderRepository;

  @Mock
  private TaskGroupItemOrderRepository tgItemOrderRepository;

  @InjectMocks
  private TaskService sut;

  @Test
  void タスクグループ内タスク一覧検索成功_第2引数にnullを指定するとタスクグループ内全件検索用のリポジトリのメソッドを呼び出すこと() {
    // Arrange
    int tgId = 1;
    Task task1 = new Task(1, null, tgId, "タスク１", "説明", 60, null, null, null, null, null);
    Task task2 = new Task(2, null, tgId, "タスク２", null, 120, null, null, null, null, null);

    List<Task> expected = List.of(task1, task2);

    when(tgRepository.existsByIdAndUserId(tgId, USER_ID)).thenReturn(true);
    when(tsRepository.findAllInTaskGroup(tgId, USER_ID)).thenReturn(expected);
    when(memoRepository.findAllInTasks(List.of(1, 2))).thenReturn(List.of(
        new Memo(1, null, null, 1, "タスク1メモ"),
        new Memo(2, null, null, 2, "タスク2メモ")));

    // Act
    List<TaskResponse> actual = sut.findAllInTaskGroupByCondition(USER_ID, tgId,null);

    // Assert
    assertThat(actual)
        .extracting(TaskResponse::getId, TaskResponse::getTaskGroupId, TaskResponse::getTitle,
            TaskResponse::getDescription, TaskResponse::getEstimatedMinutes)
        .containsExactly(
            tuple(1, tgId, "タスク１", "説明", 60),
            tuple(2, tgId, "タスク２", null, 120)
        );
    verify(tgRepository, times(1)).existsByIdAndUserId(tgId, USER_ID);
    verify(tsRepository, times(1)).findAllInTaskGroup(tgId, USER_ID);
    verify(tsRepository, never()).findAllInTaskGroupByCondition(anyInt(), anyBoolean(), anyInt());
    verify(memoRepository, times(1)).findAllInTasks(List.of(1, 2));
    verify(memoRepository, never()).findAllInTask(anyInt());
    assertThat(actual).allSatisfy(response -> assertThat(response.getMemos()).hasSize(1));
  }

  @ParameterizedTest(name = "[{index}]タスクグループ内タスク一覧検索_第2引数に{0}を指定すると完了フラグ指定検索用のリポジトリのメソッドに{0}を指定して呼び出すこと")
  @ValueSource(booleans = {true, false})
  void タスクグループ内タスク一覧検索成功_第2引数に渡したbool値をそのまま完了フラグ指定検索用のリポジトリのメソッドに渡して呼び出すこと(
      boolean flg) {
    // Arrange
    int tgId = 1;
    Task task = new Task(1, null, tgId, "タスク１", "説明", 60, null, null, null, null, null);

    List<Task> expected = List.of(task);

    when(tgRepository.existsByIdAndUserId(tgId, USER_ID)).thenReturn(true);
    when(tsRepository.findAllInTaskGroupByCondition(tgId, flg, USER_ID)).thenReturn(expected);

    // Act
    List<TaskResponse> actual = sut.findAllInTaskGroupByCondition(USER_ID, tgId, flg);

    // Assert
    assertThat(actual)
        .extracting(TaskResponse::getId, TaskResponse::getTaskGroupId, TaskResponse::getTitle)
        .containsExactly(tuple(1, tgId, "タスク１"));
    verify(tgRepository, times(1)).existsByIdAndUserId(tgId, USER_ID);
    verify(tsRepository, times(1)).findAllInTaskGroupByCondition(tgId, flg, USER_ID);
    verify(tsRepository, never()).findAllInTaskGroup(anyInt(), anyInt());
  }

  @Test
  void タスクグループ内タスク一覧検索失敗_指定したタスクグループのIDが存在しない場合は例外を投げてその後のリポジトリの処理を呼び出さないこと() {
    // Arrange
    int tgId = 999;
    when(tgRepository.existsByIdAndUserId(tgId, USER_ID)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.findAllInTaskGroupByCondition(USER_ID, tgId,null))
        .isInstanceOf(TargetNotFoundException.class);

    verify(tgRepository, times(1)).existsByIdAndUserId(tgId, USER_ID);
    verify(tsRepository, never()).findAllInTaskGroup(anyInt(), anyInt());
    verify(tsRepository, never()).findAllInTaskGroupByCondition(anyInt(), anyBoolean(), anyInt());
  }

  @Test
  void タスクグループ内タスク一覧検索成功_検索結果が空ならメモ検索を実行しないこと() {
    int tgId = 1;
    when(tgRepository.existsByIdAndUserId(tgId, USER_ID)).thenReturn(true);
    when(tsRepository.findAllInTaskGroup(tgId, USER_ID)).thenReturn(List.of());

    List<TaskResponse> actual = sut.findAllInTaskGroupByCondition(USER_ID, tgId,null);

    assertThat(actual).isEmpty();
    verify(memoRepository, never()).findAllInTasks(any());
  }

  @Test
  void プロジェクト内タスク一覧検索成功_第2引数にnullを指定するとプロジェクト内全件検索用のリポジトリのメソッドを呼び出すこと() {
    // Arrange
    int pId = 1;
    Task task1 = new Task(1, pId, null, "タスク１", "説明", 60, null, null, null, null, null);
    Task task2 = new Task(2, null, 1, "タスク２", null, 120, null, null, null, null, null);

    List<Task> expected = List.of(task1, task2);

    when(pjRepository.existsByIdAndUserId(pId, USER_ID)).thenReturn(true);
    when(tsRepository.findAllInProject(pId, USER_ID)).thenReturn(expected);

    // Act
    List<TaskResponse> actual = sut.findAllInProjectByCondition(USER_ID, pId,null);

    // Assert
    assertThat(actual)
        .extracting(TaskResponse::getId, TaskResponse::getProjectId, TaskResponse::getTaskGroupId,
            TaskResponse::getTitle)
        .containsExactly(
            tuple(1, pId, null, "タスク１"),
            tuple(2, null, 1, "タスク２")
        );
    verify(pjRepository, times(1)).existsByIdAndUserId(pId, USER_ID);
    verify(tsRepository, times(1)).findAllInProject(pId, USER_ID);
    verify(tsRepository, never()).findAllInProjectByCondition(anyInt(), anyBoolean(), anyInt());
  }

  @ParameterizedTest(name = "[{index}]プロジェクト内タスク一覧検索_第2引数に{0}を指定すると完了フラグ指定検索用のリポジトリのメソッドに{0}を指定して呼び出すこと")
  @ValueSource(booleans = {true, false})
  void プロジェクト内タスク一覧検索成功_第2引数に渡したbool値をそのまま完了フラグ指定検索用のリポジトリのメソッドに渡して呼び出すこと(
      boolean flg) {
    // Arrange
    int pId = 1;
    Task task = new Task(1, pId, null, "タスク１", "説明", 60, null, null, null, null, null);

    List<Task> expected = List.of(task);

    when(pjRepository.existsByIdAndUserId(pId, USER_ID)).thenReturn(true);
    when(tsRepository.findAllInProjectByCondition(pId, flg, USER_ID)).thenReturn(expected);

    // Act
    List<TaskResponse> actual = sut.findAllInProjectByCondition(USER_ID, pId, flg);

    // Assert
    assertThat(actual)
        .extracting(TaskResponse::getId, TaskResponse::getProjectId, TaskResponse::getTitle)
        .containsExactly(tuple(1, pId, "タスク１"));
    verify(pjRepository, times(1)).existsByIdAndUserId(pId, USER_ID);
    verify(tsRepository, times(1)).findAllInProjectByCondition(pId, flg, USER_ID);
    verify(tsRepository, never()).findAllInProject(anyInt(), anyInt());
  }

  @Test
  void プロジェクト内タスク一覧検索失敗_指定したプロジェクトのIDが存在しない場合は例外を投げてその後のリポジトリの処理を呼び出さないこと() {
    // Arrange
    int pId = 999;
    when(pjRepository.existsByIdAndUserId(pId, USER_ID)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.findAllInProjectByCondition(USER_ID, pId,null))
        .isInstanceOf(TargetNotFoundException.class);

    verify(pjRepository, times(1)).existsByIdAndUserId(pId, USER_ID);
    verify(tsRepository, never()).findAllInProject(anyInt(), anyInt());
    verify(tsRepository, never()).findAllInProjectByCondition(anyInt(), anyBoolean(), anyInt());
  }

  @Test
  void ID検索成功_タスクを取得できること() {
    // Arrange
    int id = 1;
    Task expected = new Task(id, 1, null, "タスク１", "説明", 60, null, null, null, null, null);

    when(tsRepository.findById(id, USER_ID)).thenReturn(expected);

    // Act
    TaskResponse actual = sut.findById(USER_ID, id);

    // Assert
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getProjectId()).isEqualTo(expected.getProjectId());
    assertThat(actual.getTaskGroupId()).isEqualTo(expected.getTaskGroupId());
    assertThat(actual.getTitle()).isEqualTo(expected.getTitle());
    assertThat(actual.getDescription()).isEqualTo(expected.getDescription());
    assertThat(actual.getEstimatedMinutes()).isEqualTo(expected.getEstimatedMinutes());
    assertThat(actual.getMemos()).isEmpty();
    verify(tsRepository, times(1)).findById(id, USER_ID);
  }

  @Test
  void ID検索失敗_リポジトリからnullが返ったら例外を投げること() {
    // Arrange
    int id = 999;

    when(tsRepository.findById(id, USER_ID)).thenReturn(null);

    // Act & Assert
    assertThatThrownBy(() -> sut.findById(USER_ID, id))
        .isInstanceOf(TargetNotFoundException.class);
  }

  @Test
  void 登録成功_プロジェクトIDを持つタスクの場合はプロジェクトの存在チェックをしてリポジトリの処理を呼び出すこと() {
    // Arrange
    int pId = 1;
    TaskCreateRequest request = new TaskCreateRequest();
    request.setTitle("タスク１");
    request.setDescription("説明");
    request.setEstimatedMinutes(60);

    when(pjRepository.existsByIdAndUserId(pId, USER_ID)).thenReturn(true);
    doAnswer(invocation -> {
      Task task = invocation.getArgument(0);
      task.setId(10);
      return null;
    }).when(tsRepository).insert(any(Task.class));
    Task registered = new Task(10, pId, null, "DB登録後タスク", "説明", 60,
        null, null, null, null, null);
    when(tsRepository.findById(10, USER_ID)).thenReturn(registered);

    // Act
    TaskResponse actual = sut.register(USER_ID, pId, null, request);

    // Assert
    assertThat(actual.getProjectId()).isEqualTo(pId);
    assertThat(actual.getTaskGroupId()).isNull();
    assertThat(actual.getMemos()).isEmpty();
    verify(pjRepository, times(1)).existsByIdAndUserId(pId, USER_ID);
    verify(tgRepository, never()).existsByIdAndUserId(anyInt(), anyInt());
    ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
    verify(tsRepository, times(1)).insert(captor.capture());
    assertThat(captor.getValue().getProjectId()).isEqualTo(pId);
    assertThat(captor.getValue().getTaskGroupId()).isNull();
    assertThat(captor.getValue().getTitle()).isEqualTo("タスク１");
    verify(pjItemOrderRepository, times(1)).insertAppendForTask(pId, 10);
    verify(tgItemOrderRepository, never()).insertAppendForTask(anyInt(), anyInt());
  }

  @Test
  void 登録成功_タスクグループIDを持つタスクの場合はタスクグループの存在チェックをしてリポジトリの処理を呼び出すこと() {
    // Arrange
    int tgId = 1;
    TaskCreateRequest request = new TaskCreateRequest();
    request.setTitle("タスク１");
    request.setDescription("説明");
    request.setEstimatedMinutes(60);

    when(tgRepository.existsByIdAndUserId(tgId, USER_ID)).thenReturn(true);
    doAnswer(invocation -> {
      Task task = invocation.getArgument(0);
      task.setId(10);
      return null;
    }).when(tsRepository).insert(any(Task.class));
    Task registered = new Task(10, null, tgId, "DB登録後タスク", "説明", 60,
        null, null, null, null, null);
    when(tsRepository.findById(10, USER_ID)).thenReturn(registered);

    // Act
    TaskResponse actual = sut.register(USER_ID, null, tgId, request);

    // Assert
    assertThat(actual.getTaskGroupId()).isEqualTo(tgId);
    assertThat(actual.getProjectId()).isNull();
    assertThat(actual.getMemos()).isEmpty();
    verify(tgRepository, times(1)).existsByIdAndUserId(tgId, USER_ID);
    verify(pjRepository, never()).existsByIdAndUserId(anyInt(), anyInt());
    ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
    verify(tsRepository, times(1)).insert(captor.capture());
    assertThat(captor.getValue().getTaskGroupId()).isEqualTo(tgId);
    assertThat(captor.getValue().getProjectId()).isNull();
    assertThat(captor.getValue().getTitle()).isEqualTo("タスク１");
    verify(tgItemOrderRepository, times(1)).insertAppendForTask(tgId, 10);
    verify(pjItemOrderRepository, never()).insertAppendForTask(anyInt(), anyInt());
  }

  @Test
  void 登録失敗_指定したプロジェクトが存在しない場合は例外を投げて以降のリポジトリの処理を呼び出さないこと() {
    // Arrange
    int pId = 999;
    TaskCreateRequest request = new TaskCreateRequest();
    request.setTitle("タスク１");
    request.setDescription("説明");
    request.setEstimatedMinutes(60);

    when(pjRepository.existsByIdAndUserId(pId, USER_ID)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.register(USER_ID, pId, null, request))
        .isInstanceOf(TargetNotFoundException.class);

    verify(pjRepository, times(1)).existsByIdAndUserId(pId, USER_ID);
    verify(tgRepository, never()).existsByIdAndUserId(anyInt(), anyInt());
    verify(tsRepository, never()).insert(any());
  }

  @Test
  void 登録失敗_指定したタスクグループが存在しない場合は例外を投げて以降のリポジトリの処理を呼び出さないこと() {
    // Arrange
    int tgId = 999;
    TaskCreateRequest request = new TaskCreateRequest();
    request.setTitle("タスク１");
    request.setDescription("説明");
    request.setEstimatedMinutes(60);

    when(tgRepository.existsByIdAndUserId(tgId, USER_ID)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.register(USER_ID, null, tgId, request))
        .isInstanceOf(TargetNotFoundException.class);

    verify(tgRepository, times(1)).existsByIdAndUserId(tgId, USER_ID);
    verify(pjRepository, never()).existsByIdAndUserId(anyInt(), anyInt());
    verify(tsRepository, never()).insert(any());
  }

  @Test
  void 登録失敗_DB制約違反の例外をそのまま送出すること() {
    // Arrange
    int pId = 1;
    TaskCreateRequest request = new TaskCreateRequest();
    request.setTitle(null);
    request.setDescription("説明");
    request.setEstimatedMinutes(60);

    when(pjRepository.existsByIdAndUserId(pId, USER_ID)).thenReturn(true);
    doThrow(new DataIntegrityViolationException("db constraint violation"))
        .when(tsRepository).insert(any(Task.class));

    // Act & Assert
    assertThatThrownBy(() -> sut.register(USER_ID, pId, null, request))
        .isInstanceOf(DataIntegrityViolationException.class);

    verify(pjRepository, times(1)).existsByIdAndUserId(pId, USER_ID);
    verify(tsRepository, times(1)).insert(any(Task.class));
  }

  @Test
  void タスク名説明更新成功_リポジトリのメソッドを呼び出すこと() {
    // Arrange
    int id = 1;
    TaskUpdatePropertyRequest request = new TaskUpdatePropertyRequest();
    request.setTitle("更新後タスク");
    request.setDescription("説明更新");

    when(tsRepository.updateProperty(any(Task.class), eq(USER_ID))).thenReturn(1);
    Task updated = new Task(id, 1, null, "DB更新後タスク", "DB更新後説明", 60,
        null, null, null, null, null);
    when(tsRepository.findById(id, USER_ID)).thenReturn(updated);

    // Act
    TaskResponse actual = sut.updateProperty(USER_ID, id, request);

    // Assert
    ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
    verify(tsRepository, times(1)).updateProperty(captor.capture(), eq(USER_ID));
    assertThat(captor.getValue().getId()).isEqualTo(id);
    assertThat(captor.getValue().getTitle()).isEqualTo("更新後タスク");
    assertThat(actual.getTitle()).isEqualTo("DB更新後タスク");
  }

  @Test
  void タスク名説明更新失敗_DB制約違反の例外をそのまま送出すること() {
    // Arrange
    int id = 1;
    TaskUpdatePropertyRequest request = new TaskUpdatePropertyRequest();
    request.setTitle(null);
    request.setDescription("説明更新");

    when(tsRepository.updateProperty(any(Task.class), eq(USER_ID)))
        .thenThrow(new DataIntegrityViolationException("db constraint violation"));

    // Act & Assert
    assertThatThrownBy(() -> sut.updateProperty(USER_ID, id, request))
        .isInstanceOf(DataIntegrityViolationException.class);

    verify(tsRepository, times(1)).updateProperty(any(Task.class), eq(USER_ID));
  }

  @Test
  void タスク名説明更新失敗_更新件数が0件のときTargetNotFoundExceptionを投げること() {
    // Arrange
    int id = 999;
    TaskUpdatePropertyRequest request = new TaskUpdatePropertyRequest();
    request.setTitle("更新後タスク");
    request.setDescription("説明更新");

    when(tsRepository.updateProperty(any(Task.class), eq(USER_ID))).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.updateProperty(USER_ID, id, request))
        .isInstanceOf(TargetNotFoundException.class);

    verify(tsRepository, times(1)).updateProperty(any(Task.class), eq(USER_ID));
  }

  @Test
  void 所属変更成功_プロジェクト直下タスクを同一プロジェクトのタスクグループ配下に変更できること() {
    // Arrange
    int taskId = 4;
    int taskGroupId = 2;
    Task task = new Task(taskId, 1, null, "タスク１", "説明", 60, null, null, null, null, null);
    TaskGroup targetTaskGroup = new TaskGroup();
    targetTaskGroup.setId(taskGroupId);
    targetTaskGroup.setProjectId(1);

    Task updated = new Task(taskId, null, taskGroupId, "タスク１", "説明", 60,
        null, null, null, null, null);
    when(tsRepository.findById(taskId, USER_ID)).thenReturn(task, updated);
    when(tgRepository.findById(taskGroupId, USER_ID)).thenReturn(targetTaskGroup);
    when(tsRepository.updateTaskGroup(taskId, taskGroupId, USER_ID)).thenReturn(1);

    // Act
    sut.updateParent(USER_ID, taskId, null, taskGroupId);

    // Assert
    verify(tsRepository, times(2)).findById(taskId, USER_ID);
    verify(tgRepository, times(1)).findById(taskGroupId, USER_ID);
    verify(tsRepository, times(1)).updateTaskGroup(taskId, taskGroupId, USER_ID);
    verify(pjItemOrderRepository, times(1)).deleteByTaskId(taskId);
    verify(tgItemOrderRepository, times(1)).deleteByTaskId(taskId);
    verify(tgItemOrderRepository, times(1)).insertAppendForTask(taskGroupId, taskId);
  }

  @Test
  void 所属変更成功_タスクグループ配下タスクを同一プロジェクトの別タスクグループ配下に変更できること() {
    // Arrange
    int taskId = 1;
    int currentTaskGroupId = 1;
    int targetTaskGroupId = 2;
    Task task = new Task(taskId, null, currentTaskGroupId, "タスク１", "説明", 60, null, null,
        null, null, null);
    TaskGroup currentTaskGroup = new TaskGroup();
    currentTaskGroup.setId(currentTaskGroupId);
    currentTaskGroup.setProjectId(1);
    TaskGroup targetTaskGroup = new TaskGroup();
    targetTaskGroup.setId(targetTaskGroupId);
    targetTaskGroup.setProjectId(1);

    Task updated = new Task(taskId, null, targetTaskGroupId, "タスク１", "説明", 60,
        null, null, null, null, null);
    when(tsRepository.findById(taskId, USER_ID)).thenReturn(task, updated);
    when(tgRepository.findById(targetTaskGroupId, USER_ID)).thenReturn(targetTaskGroup);
    when(tgRepository.findById(currentTaskGroupId, USER_ID)).thenReturn(currentTaskGroup);
    when(tsRepository.updateTaskGroup(taskId, targetTaskGroupId, USER_ID)).thenReturn(1);

    // Act
    sut.updateParent(USER_ID, taskId, null, targetTaskGroupId);

    // Assert
    verify(tsRepository, times(2)).findById(taskId, USER_ID);
    verify(tgRepository, times(1)).findById(targetTaskGroupId, USER_ID);
    verify(tgRepository, times(1)).findById(currentTaskGroupId, USER_ID);
    verify(tsRepository, times(1)).updateTaskGroup(taskId, targetTaskGroupId, USER_ID);
    verify(pjItemOrderRepository, times(1)).deleteByTaskId(taskId);
    verify(tgItemOrderRepository, times(1)).deleteByTaskId(taskId);
    verify(tgItemOrderRepository, times(1)).insertAppendForTask(targetTaskGroupId, taskId);
  }

  @Test
  void 所属変更失敗_タスクが存在しない場合は例外を投げて以降の処理を呼び出さないこと() {
    // Arrange
    int taskId = 999;
    int taskGroupId = 2;

    when(tsRepository.findById(taskId, USER_ID)).thenReturn(null);

    // Act & Assert
    assertThatThrownBy(() -> sut.updateParent(USER_ID, taskId, null, taskGroupId))
        .isInstanceOf(TargetNotFoundException.class);

    verify(tsRepository, times(1)).findById(taskId, USER_ID);
    verify(tgRepository, never()).findById(anyInt(), anyInt());
    verify(tsRepository, never()).updateTaskGroup(anyInt(), anyInt(), anyInt());
  }

  @Test
  void 所属変更失敗_移動先タスクグループが存在しない場合は例外を投げて更新処理を呼び出さないこと() {
    // Arrange
    int taskId = 4;
    int taskGroupId = 999;
    Task task = new Task(taskId, 1, null, "タスク１", "説明", 60, null, null, null, null, null);

    when(tsRepository.findById(taskId, USER_ID)).thenReturn(task);
    when(tgRepository.findById(taskGroupId, USER_ID)).thenReturn(null);

    // Act & Assert
    assertThatThrownBy(() -> sut.updateParent(USER_ID, taskId, null, taskGroupId))
        .isInstanceOf(TargetNotFoundException.class);

    verify(tsRepository, times(1)).findById(taskId, USER_ID);
    verify(tgRepository, times(1)).findById(taskGroupId, USER_ID);
    verify(tsRepository, never()).updateTaskGroup(anyInt(), anyInt(), anyInt());
  }

  @Test
  void 所属変更失敗_移動先タスクグループが別プロジェクト配下の場合は例外を投げて更新処理を呼び出さないこと() {
    // Arrange
    int taskId = 4;
    int taskGroupId = 3;
    Task task = new Task(taskId, 1, null, "タスク１", "説明", 60, null, null, null, null, null);
    TaskGroup targetTaskGroup = new TaskGroup();
    targetTaskGroup.setId(taskGroupId);
    targetTaskGroup.setProjectId(2);

    when(tsRepository.findById(taskId, USER_ID)).thenReturn(task);
    when(tgRepository.findById(taskGroupId, USER_ID)).thenReturn(targetTaskGroup);

    // Act & Assert
    assertThatThrownBy(() -> sut.updateParent(USER_ID, taskId, null, taskGroupId))
        .isInstanceOf(TargetNotFoundException.class);

    verify(tsRepository, times(1)).findById(taskId, USER_ID);
    verify(tgRepository, times(1)).findById(taskGroupId, USER_ID);
    verify(tsRepository, never()).updateTaskGroup(anyInt(), anyInt(), anyInt());
  }

  @Test
  void 所属変更成功_タスクグループ配下タスクを同根プロジェクト直下に変更できること() {
    // Arrange
    int taskId = 1;
    int currentTaskGroupId = 1;
    int projectId = 1;
    Task task = new Task(taskId, null, currentTaskGroupId, "タスク１", "説明", 60, null, null,
        null, null, null);
    TaskGroup currentTaskGroup = new TaskGroup();
    currentTaskGroup.setId(currentTaskGroupId);
    currentTaskGroup.setProjectId(projectId);

    Task updated = new Task(taskId, projectId, null, "タスク１", "説明", 60,
        null, null, null, null, null);
    when(tsRepository.findById(taskId, USER_ID)).thenReturn(task, updated);
    when(pjRepository.existsByIdAndUserId(projectId, USER_ID)).thenReturn(true);
    when(tgRepository.findById(currentTaskGroupId, USER_ID)).thenReturn(currentTaskGroup);
    when(tsRepository.updateProject(taskId, projectId, USER_ID)).thenReturn(1);

    // Act
    sut.updateParent(USER_ID, taskId, projectId, null);

    // Assert
    verify(tsRepository, times(2)).findById(taskId, USER_ID);
    verify(pjRepository, times(1)).existsByIdAndUserId(projectId, USER_ID);
    verify(tgRepository, times(1)).findById(currentTaskGroupId, USER_ID);
    verify(tsRepository, times(1)).updateProject(taskId, projectId, USER_ID);
    verify(pjItemOrderRepository, times(1)).deleteByTaskId(taskId);
    verify(tgItemOrderRepository, times(1)).deleteByTaskId(taskId);
    verify(pjItemOrderRepository, times(1)).insertAppendForTask(projectId, taskId);
  }

  @Test
  void 所属変更失敗_移動先プロジェクトが存在しない場合は例外を投げて更新処理を呼び出さないこと() {
    // Arrange
    int taskId = 1;
    int projectId = 999;
    Task task = new Task(taskId, null, 1, "タスク１", "説明", 60, null, null, null, null, null);

    when(tsRepository.findById(taskId, USER_ID)).thenReturn(task);
    when(pjRepository.existsByIdAndUserId(projectId, USER_ID)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.updateParent(USER_ID, taskId, projectId, null))
        .isInstanceOf(TargetNotFoundException.class);

    verify(tsRepository, times(1)).findById(taskId, USER_ID);
    verify(pjRepository, times(1)).existsByIdAndUserId(projectId, USER_ID);
    verify(tsRepository, never()).updateProject(anyInt(), anyInt(), anyInt());
  }

  @Test
  void 所属変更失敗_移動先プロジェクトが同根でない場合は例外を投げて更新処理を呼び出さないこと() {
    // Arrange
    int taskId = 1;
    int currentTaskGroupId = 1;
    int projectId = 2;
    Task task = new Task(taskId, null, currentTaskGroupId, "タスク１", "説明", 60, null, null,
        null, null, null);
    TaskGroup currentTaskGroup = new TaskGroup();
    currentTaskGroup.setId(currentTaskGroupId);
    currentTaskGroup.setProjectId(1);

    when(tsRepository.findById(taskId, USER_ID)).thenReturn(task);
    when(pjRepository.existsByIdAndUserId(projectId, USER_ID)).thenReturn(true);
    when(tgRepository.findById(currentTaskGroupId, USER_ID)).thenReturn(currentTaskGroup);

    // Act & Assert
    assertThatThrownBy(() -> sut.updateParent(USER_ID, taskId, projectId, null))
        .isInstanceOf(TargetNotFoundException.class);

    verify(tsRepository, times(1)).findById(taskId, USER_ID);
    verify(pjRepository, times(1)).existsByIdAndUserId(projectId, USER_ID);
    verify(tgRepository, times(1)).findById(currentTaskGroupId, USER_ID);
    verify(tsRepository, never()).updateProject(anyInt(), anyInt(), anyInt());
  }

  @Test
  void 見積もり作業時間更新成功_WorkSessionが存在しない場合はtsRepositoryのメソッドを呼び出すこと() {
    // Arrange
    int taskId = 1;
    int estimatedMinutes = 120;
    TaskUpdateEstimatedMinutesRequest request = new TaskUpdateEstimatedMinutesRequest();
    request.setEstimatedMinutes(estimatedMinutes);

    when(wsRepository.existsByTaskId(taskId, USER_ID)).thenReturn(false);
    when(tsRepository.isFinished(taskId, USER_ID)).thenReturn(false);
    when(tsRepository.updateEstimateMinutes(taskId, estimatedMinutes, USER_ID)).thenReturn(1);
    Task updated = new Task(taskId, 1, null, "タスク１", "説明", estimatedMinutes,
        null, null, null, null, null);
    when(tsRepository.findById(taskId, USER_ID)).thenReturn(updated);

    // Act
    sut.updateEstimateMinutes(USER_ID, taskId, request);

    // Assert
    verify(wsRepository, times(1)).existsByTaskId(taskId, USER_ID);
    verify(tsRepository, times(1)).isFinished(taskId, USER_ID);
    verify(tsRepository, times(1)).updateEstimateMinutes(taskId, estimatedMinutes, USER_ID);
  }

  @Test
  void 見積もり作業時間更新失敗_WorkSessionが存在する場合は例外を投げて更新処理を呼び出さないこと() {
    // Arrange
    int taskId = 1;
    int estimatedMinutes = 120;
    TaskUpdateEstimatedMinutesRequest request = new TaskUpdateEstimatedMinutesRequest();
    request.setEstimatedMinutes(estimatedMinutes);

    when(wsRepository.existsByTaskId(taskId, USER_ID)).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> sut.updateEstimateMinutes(USER_ID, taskId, request))
        .isInstanceOf(EstimateMinutesUpdateNotAllowedException.class);

    verify(wsRepository, times(1)).existsByTaskId(taskId, USER_ID);
    verify(tsRepository, never()).isFinished(anyInt(), anyInt());
    verify(tsRepository, never()).updateEstimateMinutes(anyInt(), anyInt(), anyInt());
  }

  @Test
  void 見積もり作業時間更新失敗_完了済みの場合は例外を投げて更新処理を呼び出さないこと() {
    // Arrange
    int taskId = 3;
    int estimatedMinutes = 120;
    TaskUpdateEstimatedMinutesRequest request = new TaskUpdateEstimatedMinutesRequest();
    request.setEstimatedMinutes(estimatedMinutes);

    when(wsRepository.existsByTaskId(taskId, USER_ID)).thenReturn(false);
    when(tsRepository.isFinished(taskId, USER_ID)).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> sut.updateEstimateMinutes(USER_ID, taskId, request))
        .isInstanceOf(EstimateMinutesUpdateNotAllowedException.class);

    verify(wsRepository, times(1)).existsByTaskId(taskId, USER_ID);
    verify(tsRepository, times(1)).isFinished(taskId, USER_ID);
    verify(tsRepository, never()).updateEstimateMinutes(anyInt(), anyInt(), anyInt());
  }

  @Test
  void 見積もり作業時間更新失敗_更新件数が0件のときTargetNotFoundExceptionを投げること() {
    // Arrange
    int taskId = 999;
    int estimatedMinutes = 120;
    TaskUpdateEstimatedMinutesRequest request = new TaskUpdateEstimatedMinutesRequest();
    request.setEstimatedMinutes(estimatedMinutes);

    when(wsRepository.existsByTaskId(taskId, USER_ID)).thenReturn(false);
    when(tsRepository.isFinished(taskId, USER_ID)).thenReturn(false);
    when(tsRepository.updateEstimateMinutes(taskId, estimatedMinutes, USER_ID)).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.updateEstimateMinutes(USER_ID, taskId, request))
        .isInstanceOf(TargetNotFoundException.class);

    verify(wsRepository, times(1)).existsByTaskId(taskId, USER_ID);
    verify(tsRepository, times(1)).isFinished(taskId, USER_ID);
    verify(tsRepository, times(1)).updateEstimateMinutes(taskId, estimatedMinutes, USER_ID);
  }

  @Test
  void 完了状態更新成功_リポジトリのメソッドに引数のIDと完了状態を渡して呼び出すこと() {
    // Arrange
    int id = 1;
    boolean isFinished = true;

    when(tsRepository.updateFinished(id, isFinished, USER_ID)).thenReturn(1);
    Task updated = new Task(id, 1, null, "タスク１", "説明", 60,
        null, java.time.LocalDateTime.of(2026, 1, 1, 10, 0), 70, 10, 16.666);
    when(tsRepository.findById(id, USER_ID)).thenReturn(updated);

    // Act
    sut.updateFinished(USER_ID, id, isFinished);

    // Assert
    verify(wsRepository, times(1)).existsUnfinishedByTaskId(id, USER_ID);
    verify(tsRepository, times(1)).updateFinished(id, isFinished, USER_ID);
  }

  @Test
  void 完了状態更新成功_未完了に戻す場合もリポジトリのメソッドにfalseを渡して呼び出すこと() {
    // Arrange
    int id = 1;
    boolean isFinished = false;

    when(tsRepository.updateFinished(id, isFinished, USER_ID)).thenReturn(1);
    Task updated = new Task(id, 1, null, "タスク１", "説明", 60,
        null, null, null, null, null);
    when(tsRepository.findById(id, USER_ID)).thenReturn(updated);

    // Act
    sut.updateFinished(USER_ID, id, isFinished);

    // Assert
    verify(wsRepository, never()).existsUnfinishedByTaskId(anyInt(), anyInt());
    verify(tsRepository, times(1)).updateFinished(id, isFinished, USER_ID);
  }

  @Test
  void 完了状態更新失敗_完了にする際に未終了WorkSessionが存在する場合は例外を投げて更新処理を呼び出さないこと() {
    // Arrange
    int id = 1;
    boolean isFinished = true;

    when(wsRepository.existsUnfinishedByTaskId(id, USER_ID)).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> sut.updateFinished(USER_ID, id, isFinished))
        .isInstanceOf(TaskFinishNotAllowedException.class);

    verify(wsRepository, times(1)).existsUnfinishedByTaskId(id, USER_ID);
    verify(tsRepository, never()).updateFinished(anyInt(), anyBoolean(), anyInt());
  }

  @Test
  void 完了状態更新失敗_更新件数が0件のときTargetNotFoundExceptionを投げること() {
    // Arrange
    int id = 999;
    boolean isFinished = true;

    when(tsRepository.updateFinished(id, isFinished, USER_ID)).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.updateFinished(USER_ID, id, isFinished))
        .isInstanceOf(TargetNotFoundException.class);

    verify(wsRepository, times(1)).existsUnfinishedByTaskId(id, USER_ID);
    verify(tsRepository, times(1)).updateFinished(id, isFinished, USER_ID);
  }

  @Test
  void 削除成功_リポジトリのメソッドに引数のIDを渡して呼び出すこと() {
    // Arrange
    int id = 1;

    when(tsRepository.existsByIdAndUserId(id, USER_ID)).thenReturn(true);
    when(tsRepository.deleteById(id, USER_ID)).thenReturn(1);

    // Act
    sut.deleteById(USER_ID, id);

    // Assert
    InOrder inOrder = org.mockito.Mockito.inOrder(wsRepository, memoRepository,
        pjItemOrderRepository, tgItemOrderRepository, tsRepository);
    inOrder.verify(wsRepository, times(1)).deleteAllByTaskId(id);
    inOrder.verify(memoRepository, times(1)).deleteAllInTask(id);
    inOrder.verify(pjItemOrderRepository, times(1)).deleteByTaskId(id);
    inOrder.verify(tgItemOrderRepository, times(1)).deleteByTaskId(id);
    inOrder.verify(tsRepository, times(1)).deleteById(id, USER_ID);
  }

  @Test
  void 削除失敗_対象が存在しない場合は例外を投げてカスケード削除を実行しないこと() {
    // Arrange
    int id = 999;

    when(tsRepository.existsByIdAndUserId(id, USER_ID)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.deleteById(USER_ID, id))
        .isInstanceOf(TargetNotFoundException.class);

    verify(wsRepository, never()).deleteAllByTaskId(anyInt());
    verify(memoRepository, never()).deleteAllInTask(anyInt());
    verify(pjItemOrderRepository, never()).deleteByTaskId(anyInt());
    verify(tgItemOrderRepository, never()).deleteByTaskId(anyInt());
    verify(tsRepository, never()).deleteById(anyInt(), anyInt());
  }

  @Test
  void 削除失敗_所有権確認後に削除件数が0件のときTargetNotFoundExceptionを投げること() {
    // Arrange
    int id = 1;

    when(tsRepository.existsByIdAndUserId(id, USER_ID)).thenReturn(true);
    when(tsRepository.deleteById(id, USER_ID)).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.deleteById(USER_ID, id))
        .isInstanceOf(TargetNotFoundException.class);

    InOrder inOrder = org.mockito.Mockito.inOrder(wsRepository, memoRepository,
        pjItemOrderRepository, tgItemOrderRepository, tsRepository);
    inOrder.verify(wsRepository, times(1)).deleteAllByTaskId(id);
    inOrder.verify(memoRepository, times(1)).deleteAllInTask(id);
    inOrder.verify(pjItemOrderRepository, times(1)).deleteByTaskId(id);
    inOrder.verify(tgItemOrderRepository, times(1)).deleteByTaskId(id);
    inOrder.verify(tsRepository, times(1)).deleteById(id, USER_ID);
  }

}
