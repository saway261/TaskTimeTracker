package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionRequest;
import com.kiborisaway.tasktimetracker.data.entity.Reflection;
import com.kiborisaway.tasktimetracker.data.entity.Task;
import com.kiborisaway.tasktimetracker.exception.ReflectionAlreadyExistsException;
import com.kiborisaway.tasktimetracker.exception.ReflectionOperationNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.ReflectionRepository;
import com.kiborisaway.tasktimetracker.repository.TaskRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReflectionServiceTest {

  private static final int USER_ID = 1;
  private static final int TASK_ID = 10;

  @Mock
  private ReflectionRepository reflectionRepository;

  @Mock
  private TaskRepository taskRepository;

  @InjectMocks
  private ReflectionService sut;

  @Test
  void 登録成功_完了状態と重複を確認して正規化した振り返りを登録すること() {
    Task task = finishedTask();
    Reflection stored = reflection(20, "原因", null);
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(task);
    when(reflectionRepository.existsByTaskId(TASK_ID)).thenReturn(false);
    doAnswer(invocation -> {
      invocation.<Reflection>getArgument(0).setId(20);
      return null;
    }).when(reflectionRepository).insert(any(Reflection.class));
    when(reflectionRepository.findByTaskId(TASK_ID)).thenReturn(stored);

    Reflection actual = sut.register(
        USER_ID, TASK_ID, request("  原因  ", "   "));

    ArgumentCaptor<Reflection> captor = ArgumentCaptor.forClass(Reflection.class);
    verify(reflectionRepository).insert(captor.capture());
    assertThat(captor.getValue().getTaskId()).isEqualTo(TASK_ID);
    assertThat(captor.getValue().getCause()).isEqualTo("原因");
    assertThat(captor.getValue().getNextAction()).isNull();
    assertThat(actual).isEqualTo(stored);
  }

  @Test
  void 登録失敗_タスクが存在しない場合は404用例外を投げてReflectionへアクセスしないこと() {
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(null);

    assertThatThrownBy(() -> sut.register(USER_ID, TASK_ID, request("原因", null)))
        .isInstanceOfSatisfying(TargetNotFoundException.class, ex -> {
          assertThat(ex.getField()).isEqualTo("task.id");
          assertThat(ex.getMessage()).isEqualTo("指定したIDのタスクは見つかりませんでした");
        });
    verifyNoInteractions(reflectionRepository);
  }

  @Test
  void 登録失敗_未完了タスクの場合は409用例外を投げて重複確認も登録もしないこと() {
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(unfinishedTask());

    assertThatThrownBy(() -> sut.register(USER_ID, TASK_ID, request("原因", null)))
        .isInstanceOfSatisfying(ReflectionOperationNotAllowedException.class, ex -> {
          assertThat(ex.getField()).isEqualTo("task.finishedAt");
          assertThat(ex.getMessage()).isEqualTo("未完了のタスクには振り返りを登録・更新できません");
        });
    verify(reflectionRepository, never()).existsByTaskId(TASK_ID);
    verify(reflectionRepository, never()).insert(any());
  }

  @Test
  void 登録失敗_振り返りが既に存在する場合は409用例外を投げて登録しないこと() {
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(finishedTask());
    when(reflectionRepository.existsByTaskId(TASK_ID)).thenReturn(true);

    assertThatThrownBy(() -> sut.register(USER_ID, TASK_ID, request("原因", null)))
        .isInstanceOfSatisfying(ReflectionAlreadyExistsException.class, ex -> {
          assertThat(ex.getField()).isEqualTo("reflection.taskId");
          assertThat(ex.getMessage()).isEqualTo("指定したタスクの振り返りは既に登録されています");
        });
    verify(reflectionRepository, never()).insert(any());
  }

  @Test
  void 更新成功_現在状態を確認して正規化した振り返りを更新すること() {
    Reflection existing = reflection(20, "更新前", "更新前アクション");
    Reflection stored = reflection(20, "更新後", "改善する");
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(finishedTask());
    when(reflectionRepository.findByTaskId(TASK_ID)).thenReturn(existing, stored);
    when(reflectionRepository.updateByTaskId(any(Reflection.class))).thenReturn(1);

    Reflection actual = sut.update(
        USER_ID, TASK_ID, request("  更新後  ", "  改善する  "));

    ArgumentCaptor<Reflection> captor = ArgumentCaptor.forClass(Reflection.class);
    verify(reflectionRepository).updateByTaskId(captor.capture());
    assertThat(captor.getValue().getId()).isEqualTo(20);
    assertThat(captor.getValue().getTaskId()).isEqualTo(TASK_ID);
    assertThat(captor.getValue().getCause()).isEqualTo("更新後");
    assertThat(captor.getValue().getNextAction()).isEqualTo("改善する");
    assertThat(actual).isEqualTo(stored);
  }

  @Test
  void 更新失敗_タスクが存在しない場合は404用例外を投げてReflectionへアクセスしないこと() {
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(null);

    assertThatThrownBy(() -> sut.update(USER_ID, TASK_ID, request("原因", null)))
        .isInstanceOf(TargetNotFoundException.class);
    verifyNoInteractions(reflectionRepository);
  }

  @Test
  void 更新失敗_未完了タスクの場合は409用例外を投げて振り返りを検索しないこと() {
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(unfinishedTask());

    assertThatThrownBy(() -> sut.update(USER_ID, TASK_ID, request("原因", null)))
        .isInstanceOf(ReflectionOperationNotAllowedException.class);
    verify(reflectionRepository, never()).findByTaskId(TASK_ID);
    verify(reflectionRepository, never()).updateByTaskId(any());
  }

  @Test
  void 更新失敗_振り返りが存在しない場合は404用例外を投げて更新しないこと() {
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(finishedTask());
    when(reflectionRepository.findByTaskId(TASK_ID)).thenReturn(null);

    assertThatThrownBy(() -> sut.update(USER_ID, TASK_ID, request("原因", null)))
        .isInstanceOfSatisfying(TargetNotFoundException.class, ex -> {
          assertThat(ex.getField()).isEqualTo("reflection.taskId");
          assertThat(ex.getMessage()).isEqualTo("更新対象の振り返りが見つかりませんでした");
        });
    verify(reflectionRepository, never()).updateByTaskId(any());
  }

  @Test
  void 更新失敗_更新件数が0件の場合は404用例外を投げること() {
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(finishedTask());
    when(reflectionRepository.findByTaskId(TASK_ID))
        .thenReturn(reflection(20, "更新前", null));
    when(reflectionRepository.updateByTaskId(any(Reflection.class))).thenReturn(0);

    assertThatThrownBy(() -> sut.update(USER_ID, TASK_ID, request("更新後", null)))
        .isInstanceOfSatisfying(TargetNotFoundException.class, ex ->
            assertThat(ex.getField()).isEqualTo("reflection.taskId"));
    verify(reflectionRepository).updateByTaskId(any(Reflection.class));
  }

  private static ReflectionRequest request(String cause, String nextAction) {
    ReflectionRequest request = new ReflectionRequest();
    request.setCause(cause);
    request.setNextAction(nextAction);
    return request;
  }

  private static Task finishedTask() {
    Task task = new Task();
    task.setId(TASK_ID);
    task.setFinishedAt(LocalDateTime.of(2026, 8, 10, 10, 0));
    return task;
  }

  private static Task unfinishedTask() {
    Task task = new Task();
    task.setId(TASK_ID);
    return task;
  }

  private static Reflection reflection(int id, String cause, String nextAction) {
    return new Reflection(
        id,
        TASK_ID,
        cause,
        nextAction,
        LocalDateTime.of(2026, 8, 10, 10, 5),
        LocalDateTime.of(2026, 8, 10, 10, 5));
  }
}
