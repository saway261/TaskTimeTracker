package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.data.dto.reflection.ProjectReflectionOverviewResponse;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionCauseCategorySummaryResponse;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionRequest;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionResponse;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionTaskGroupResponse;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionTaskResponse;
import com.kiborisaway.tasktimetracker.data.entity.CauseDirection;
import com.kiborisaway.tasktimetracker.data.entity.Project;
import com.kiborisaway.tasktimetracker.data.entity.Reflection;
import com.kiborisaway.tasktimetracker.data.entity.ReflectionCauseCategory;
import com.kiborisaway.tasktimetracker.data.entity.Task;
import com.kiborisaway.tasktimetracker.exception.ReflectionAlreadyExistsException;
import com.kiborisaway.tasktimetracker.exception.ReflectionCauseCategoryInvalidException;
import com.kiborisaway.tasktimetracker.exception.ReflectionCauseRequiredException;
import com.kiborisaway.tasktimetracker.exception.ReflectionOperationNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import com.kiborisaway.tasktimetracker.repository.ReflectionCauseCategoryLinkRepository;
import com.kiborisaway.tasktimetracker.repository.ReflectionCauseCategoryLinkRow;
import com.kiborisaway.tasktimetracker.repository.ReflectionCauseCategoryRepository;
import com.kiborisaway.tasktimetracker.repository.ReflectionRepository;
import com.kiborisaway.tasktimetracker.repository.ReflectionTaskRow;
import com.kiborisaway.tasktimetracker.repository.TaskRepository;
import java.time.LocalDateTime;
import java.util.List;
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
  private static final String CATEGORY_CODE = "TASK_BREAKDOWN";

  @Mock
  private ReflectionRepository reflectionRepository;

  @Mock
  private ReflectionCauseCategoryRepository causeCategoryRepository;

  @Mock
  private ReflectionCauseCategoryLinkRepository causeCategoryLinkRepository;

  @Mock
  private TaskRepository taskRepository;

  @Mock
  private ProjectRepository projectRepository;

  @InjectMocks
  private ReflectionService sut;

  @Test
  void 一覧取得成功_直下とグループ配下の完了タスクを取得順のまま階層化すること() {
    int projectId = 3;
    Project project = new Project(projectId, 2, "プロジェクトX", "説明", true);
    when(projectRepository.findByIdAndUserId(projectId, USER_ID)).thenReturn(project);
    when(reflectionRepository.findDirectTasksInProject(projectId)).thenReturn(List.of(
        row(7, "直下B", null, null, null, null, null, 100, -20, -16.6666666667),
        row(6, "直下A", null, null, 1, "原因A", "改善A", 90, 30, 50.0)));
    when(reflectionRepository.findGroupedTasksInProject(projectId)).thenReturn(List.of(
        row(12, "別配下", 5, "グループB", null, null, null, 25, -5, -16.6666666667),
        row(10, "配下B", 4, "グループA", null, null, null, 90, 0, 0.0),
        row(9, "配下A", 4, "グループA", 2, "原因B", null, null, null, null)));

    ProjectReflectionOverviewResponse actual = sut.getOverview(USER_ID, projectId);

    assertThat(actual.getProjectId()).isEqualTo(projectId);
    assertThat(actual.getProjectTitle()).isEqualTo("プロジェクトX");
    assertThat(actual.getTasks())
        .extracting(
            ReflectionTaskResponse::getId,
            ReflectionTaskResponse::getTitle,
            ReflectionTaskResponse::getGapRateCached)
        .containsExactly(
            tuple(7, "直下B", -16.6666666667),
            tuple(6, "直下A", 50.0));
    assertThat(actual.getTasks().get(0).getReflection()).isNull();
    assertThat(actual.getTasks().get(1).getReflection().getCause()).isEqualTo("原因A");
    assertThat(actual.getTasks().get(1).getReflection().getTaskId()).isEqualTo(6);
    assertThat(actual.getTasks().get(1).getReflection().getCauseCategories()).isEmpty();

    assertThat(actual.getTaskGroups())
        .extracting(ReflectionTaskGroupResponse::getId, ReflectionTaskGroupResponse::getTitle)
        .containsExactly(
            tuple(5, "グループB"),
            tuple(4, "グループA"));
    assertThat(actual.getTaskGroups().get(0).getTasks())
        .extracting(ReflectionTaskResponse::getId)
        .containsExactly(12);
    assertThat(actual.getTaskGroups().get(1).getTasks())
        .extracting(ReflectionTaskResponse::getId)
        .containsExactly(10, 9);
    assertThat(actual.getTaskGroups().get(1).getTasks().get(1).getActualMinutesCached())
        .isNull();

    verify(projectRepository).findByIdAndUserId(projectId, USER_ID);
    verify(reflectionRepository).findDirectTasksInProject(projectId);
    verify(reflectionRepository).findGroupedTasksInProject(projectId);
  }

  @Test
  void 一覧取得成功_複数の原因カテゴリを持つ振り返りはcauseCategoriesに複数件含むこと() {
    int projectId = 3;
    Project project = new Project(projectId, 2, "プロジェクトX", "説明", true);
    when(projectRepository.findByIdAndUserId(projectId, USER_ID)).thenReturn(project);
    when(reflectionRepository.findDirectTasksInProject(projectId)).thenReturn(List.of(
        row(6, "直下A", null, null, 1, "原因A", "改善A", 90, 30, 50.0)));
    when(reflectionRepository.findGroupedTasksInProject(projectId)).thenReturn(List.of());
    when(causeCategoryLinkRepository.findCategoriesInProject(projectId)).thenReturn(List.of(
        new ReflectionCauseCategoryLinkRow(1, "TASK_BREAKDOWN", "作業の洗い出しが足りなかった"),
        new ReflectionCauseCategoryLinkRow(1, "OTHER", "その他")));

    ProjectReflectionOverviewResponse actual = sut.getOverview(USER_ID, projectId);

    assertThat(actual.getTasks().get(0).getReflection().getCauseCategories())
        .extracting(ReflectionCauseCategorySummaryResponse::getCode)
        .containsExactly("TASK_BREAKDOWN", "OTHER");
  }

  @Test
  void 一覧取得成功_完了タスクがない場合は空のタスク配列とグループ配列を返すこと() {
    int projectId = 2;
    Project project = new Project(projectId, USER_ID, "空プロジェクト", null, false);
    when(projectRepository.findByIdAndUserId(projectId, USER_ID)).thenReturn(project);
    when(reflectionRepository.findDirectTasksInProject(projectId)).thenReturn(List.of());
    when(reflectionRepository.findGroupedTasksInProject(projectId)).thenReturn(List.of());

    ProjectReflectionOverviewResponse actual = sut.getOverview(USER_ID, projectId);

    assertThat(actual.getTasks()).isEmpty();
    assertThat(actual.getTaskGroups()).isEmpty();
  }

  @Test
  void 一覧取得失敗_プロジェクトが存在しない場合は404用例外を投げてタスクを検索しないこと() {
    int projectId = 999;
    when(projectRepository.findByIdAndUserId(projectId, USER_ID)).thenReturn(null);

    assertThatThrownBy(() -> sut.getOverview(USER_ID, projectId))
        .isInstanceOfSatisfying(TargetNotFoundException.class, ex -> {
          assertThat(ex.getField()).isEqualTo("project.id");
          assertThat(ex.getMessage()).isEqualTo("指定したIDのプロジェクトは見つかりませんでした");
        });
    verify(reflectionRepository, never()).findDirectTasksInProject(projectId);
    verify(reflectionRepository, never()).findGroupedTasksInProject(projectId);
  }

  @Test
  void 登録成功_完了状態と重複を確認して正規化した振り返りを登録すること() {
    Task task = finishedTask();
    ReflectionCauseCategory category = category();
    Reflection stored = reflection(20, "原因", null);
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(task);
    when(causeCategoryRepository.findActiveByCode(CATEGORY_CODE)).thenReturn(category);
    when(reflectionRepository.existsByTaskId(TASK_ID)).thenReturn(false);
    doAnswer(invocation -> {
      invocation.<Reflection>getArgument(0).setId(20);
      return null;
    }).when(reflectionRepository).insert(any(Reflection.class));
    when(reflectionRepository.findByTaskId(TASK_ID)).thenReturn(stored);

    ReflectionResponse actual = sut.register(
        USER_ID, TASK_ID, request(List.of(CATEGORY_CODE), "  原因  ", "   "));

    ArgumentCaptor<Reflection> captor = ArgumentCaptor.forClass(Reflection.class);
    verify(reflectionRepository).insert(captor.capture());
    assertThat(captor.getValue().getTaskId()).isEqualTo(TASK_ID);
    assertThat(captor.getValue().getCause()).isEqualTo("原因");
    assertThat(captor.getValue().getNextAction()).isNull();
    verify(causeCategoryLinkRepository).insert(20, category.getId());
    assertThat(actual).isEqualTo(new ReflectionResponse(stored, List.of(category)));
  }

  @Test
  void 登録成功_複数の原因カテゴリを表示順で紐づけて登録すること() {
    ReflectionCauseCategory first = category();       // displayOrder=10
    ReflectionCauseCategory second = secondCategory(); // displayOrder=50
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(finishedTask());
    // リクエストの並び順を意図的に逆にし、display_order順へ整列されることを確認する
    when(causeCategoryRepository.findActiveByCode(second.getCode())).thenReturn(second);
    when(causeCategoryRepository.findActiveByCode(first.getCode())).thenReturn(first);
    when(reflectionRepository.existsByTaskId(TASK_ID)).thenReturn(false);
    doAnswer(invocation -> {
      invocation.<Reflection>getArgument(0).setId(30);
      return null;
    }).when(reflectionRepository).insert(any(Reflection.class));
    when(reflectionRepository.findByTaskId(TASK_ID))
        .thenReturn(reflection(30, "原因", null));

    ReflectionResponse actual = sut.register(
        USER_ID, TASK_ID,
        request(List.of(second.getCode(), first.getCode()), "原因", null));

    verify(causeCategoryLinkRepository).insert(30, first.getId());
    verify(causeCategoryLinkRepository).insert(30, second.getId());
    assertThat(actual.getCauseCategories())
        .extracting(ReflectionCauseCategorySummaryResponse::getCode)
        .containsExactly(first.getCode(), second.getCode());
  }

  @Test
  void 登録成功_causeを省略するとNULLとして保存されること() {
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(finishedTask());
    when(causeCategoryRepository.findActiveByCode(CATEGORY_CODE)).thenReturn(category());
    when(reflectionRepository.existsByTaskId(TASK_ID)).thenReturn(false);
    doAnswer(invocation -> {
      invocation.<Reflection>getArgument(0).setId(20);
      return null;
    }).when(reflectionRepository).insert(any(Reflection.class));
    when(reflectionRepository.findByTaskId(TASK_ID)).thenReturn(reflection(20, null, null));

    sut.register(USER_ID, TASK_ID, request(List.of(CATEGORY_CODE), null, null));

    ArgumentCaptor<Reflection> captor = ArgumentCaptor.forClass(Reflection.class);
    verify(reflectionRepository).insert(captor.capture());
    assertThat(captor.getValue().getCause()).isNull();
  }

  @Test
  void 登録失敗_タスクが存在しない場合は404用例外を投げてReflectionへアクセスしないこと() {
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(null);

    assertThatThrownBy(() -> sut.register(
        USER_ID, TASK_ID, request(List.of(CATEGORY_CODE), "原因", null)))
        .isInstanceOfSatisfying(TargetNotFoundException.class, ex -> {
          assertThat(ex.getField()).isEqualTo("task.id");
          assertThat(ex.getMessage()).isEqualTo("指定したIDのタスクは見つかりませんでした");
        });
    verifyNoInteractions(reflectionRepository);
  }

  @Test
  void 登録失敗_未完了タスクの場合は409用例外を投げて重複確認も登録もしないこと() {
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(unfinishedTask());

    assertThatThrownBy(() -> sut.register(
        USER_ID, TASK_ID, request(List.of(CATEGORY_CODE), "原因", null)))
        .isInstanceOfSatisfying(ReflectionOperationNotAllowedException.class, ex -> {
          assertThat(ex.getField()).isEqualTo("task.finishedAt");
          assertThat(ex.getMessage()).isEqualTo("未完了のタスクには振り返りを登録・更新できません");
        });
    verify(reflectionRepository, never()).existsByTaskId(TASK_ID);
    verify(reflectionRepository, never()).insert(any());
  }

  @Test
  void 登録失敗_原因カテゴリが存在しないまたは無効な場合は400用例外を投げて重複確認も登録もしないこと() {
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(finishedTask());
    when(causeCategoryRepository.findActiveByCode("UNKNOWN")).thenReturn(null);

    assertThatThrownBy(() -> sut.register(
        USER_ID, TASK_ID, request(List.of("UNKNOWN"), "原因", null)))
        .isInstanceOfSatisfying(ReflectionCauseCategoryInvalidException.class, ex -> {
          assertThat(ex.getField()).isEqualTo("reflection.causeCategoryCodes");
          assertThat(ex.getMessage()).isEqualTo("指定した原因カテゴリは選択できません");
        });
    verify(reflectionRepository, never()).existsByTaskId(TASK_ID);
    verify(reflectionRepository, never()).insert(any());
  }

  @Test
  void 登録失敗_選択したカテゴリが原因の記述を必須とする場合は400用例外を投げて重複確認も登録もしないこと() {
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(finishedTask());
    when(causeCategoryRepository.findActiveByCode("OTHER")).thenReturn(otherCategory());

    assertThatThrownBy(() -> sut.register(
        USER_ID, TASK_ID, request(List.of("OTHER"), null, null)))
        .isInstanceOfSatisfying(ReflectionCauseRequiredException.class, ex -> {
          assertThat(ex.getField()).isEqualTo("reflection.cause");
          assertThat(ex.getMessage()).isEqualTo("選択した原因カテゴリでは、原因の記述が必要です");
        });
    verify(reflectionRepository, never()).existsByTaskId(TASK_ID);
    verify(reflectionRepository, never()).insert(any());
  }

  @Test
  void 登録失敗_必須カテゴリと他カテゴリを併せて選んでも原因の記述が必須になること() {
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(finishedTask());
    when(causeCategoryRepository.findActiveByCode(CATEGORY_CODE)).thenReturn(category());
    when(causeCategoryRepository.findActiveByCode("OTHER")).thenReturn(otherCategory());

    assertThatThrownBy(() -> sut.register(
        USER_ID, TASK_ID, request(List.of(CATEGORY_CODE, "OTHER"), "   ", null)))
        .isInstanceOf(ReflectionCauseRequiredException.class);
  }

  @Test
  void 登録失敗_振り返りが既に存在する場合は409用例外を投げて登録しないこと() {
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(finishedTask());
    when(causeCategoryRepository.findActiveByCode(CATEGORY_CODE)).thenReturn(category());
    when(reflectionRepository.existsByTaskId(TASK_ID)).thenReturn(true);

    assertThatThrownBy(() -> sut.register(
        USER_ID, TASK_ID, request(List.of(CATEGORY_CODE), "原因", null)))
        .isInstanceOfSatisfying(ReflectionAlreadyExistsException.class, ex -> {
          assertThat(ex.getField()).isEqualTo("reflection.taskId");
          assertThat(ex.getMessage()).isEqualTo("指定したタスクの振り返りは既に登録されています");
        });
    verify(reflectionRepository, never()).insert(any());
  }

  @Test
  void 更新成功_現在状態を確認して正規化した振り返りを更新すること() {
    ReflectionCauseCategory category = category();
    Reflection existing = reflection(20, "更新前", "更新前アクション");
    Reflection stored = reflection(20, "更新後", "改善する");
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(finishedTask());
    when(causeCategoryRepository.findActiveByCode(CATEGORY_CODE)).thenReturn(category);
    when(reflectionRepository.findByTaskId(TASK_ID)).thenReturn(existing, stored);
    when(reflectionRepository.updateByTaskId(any(Reflection.class))).thenReturn(1);

    ReflectionResponse actual = sut.update(
        USER_ID, TASK_ID, request(List.of(CATEGORY_CODE), "  更新後  ", "  改善する  "));

    ArgumentCaptor<Reflection> captor = ArgumentCaptor.forClass(Reflection.class);
    verify(reflectionRepository).updateByTaskId(captor.capture());
    assertThat(captor.getValue().getId()).isEqualTo(20);
    assertThat(captor.getValue().getTaskId()).isEqualTo(TASK_ID);
    assertThat(captor.getValue().getCause()).isEqualTo("更新後");
    assertThat(captor.getValue().getNextAction()).isEqualTo("改善する");
    verify(causeCategoryLinkRepository).deleteByReflectionId(20);
    verify(causeCategoryLinkRepository).insert(20, category.getId());
    assertThat(actual).isEqualTo(new ReflectionResponse(stored, List.of(category)));
  }

  @Test
  void 更新成功_カテゴリを全置換すること() {
    ReflectionCauseCategory newCategory = secondCategory();
    Reflection existing = reflection(20, "更新前", null);
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(finishedTask());
    when(causeCategoryRepository.findActiveByCode(newCategory.getCode())).thenReturn(newCategory);
    when(reflectionRepository.findByTaskId(TASK_ID))
        .thenReturn(existing, reflection(20, "更新後", null));
    when(reflectionRepository.updateByTaskId(any(Reflection.class))).thenReturn(1);

    sut.update(USER_ID, TASK_ID, request(List.of(newCategory.getCode()), "更新後", null));

    verify(causeCategoryLinkRepository, times(1)).deleteByReflectionId(20);
    verify(causeCategoryLinkRepository, times(1)).insert(20, newCategory.getId());
  }

  @Test
  void 更新失敗_タスクが存在しない場合は404用例外を投げてReflectionへアクセスしないこと() {
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(null);

    assertThatThrownBy(() -> sut.update(
        USER_ID, TASK_ID, request(List.of(CATEGORY_CODE), "原因", null)))
        .isInstanceOf(TargetNotFoundException.class);
    verifyNoInteractions(reflectionRepository);
  }

  @Test
  void 更新失敗_未完了タスクの場合は409用例外を投げて振り返りを検索しないこと() {
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(unfinishedTask());

    assertThatThrownBy(() -> sut.update(
        USER_ID, TASK_ID, request(List.of(CATEGORY_CODE), "原因", null)))
        .isInstanceOf(ReflectionOperationNotAllowedException.class);
    verify(reflectionRepository, never()).findByTaskId(TASK_ID);
    verify(reflectionRepository, never()).updateByTaskId(any());
  }

  @Test
  void 更新失敗_原因カテゴリが存在しないまたは無効な場合は400用例外を投げて振り返りを検索しないこと() {
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(finishedTask());
    when(causeCategoryRepository.findActiveByCode("UNKNOWN")).thenReturn(null);

    assertThatThrownBy(() -> sut.update(
        USER_ID, TASK_ID, request(List.of("UNKNOWN"), "原因", null)))
        .isInstanceOfSatisfying(ReflectionCauseCategoryInvalidException.class, ex -> {
          assertThat(ex.getField()).isEqualTo("reflection.causeCategoryCodes");
          assertThat(ex.getMessage()).isEqualTo("指定した原因カテゴリは選択できません");
        });
    verify(reflectionRepository, never()).findByTaskId(TASK_ID);
    verify(reflectionRepository, never()).updateByTaskId(any());
  }

  @Test
  void 更新失敗_振り返りが存在しない場合は404用例外を投げて更新しないこと() {
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(finishedTask());
    when(causeCategoryRepository.findActiveByCode(CATEGORY_CODE)).thenReturn(category());
    when(reflectionRepository.findByTaskId(TASK_ID)).thenReturn(null);

    assertThatThrownBy(() -> sut.update(
        USER_ID, TASK_ID, request(List.of(CATEGORY_CODE), "原因", null)))
        .isInstanceOfSatisfying(TargetNotFoundException.class, ex -> {
          assertThat(ex.getField()).isEqualTo("reflection.taskId");
          assertThat(ex.getMessage()).isEqualTo("更新対象の振り返りが見つかりませんでした");
        });
    verify(reflectionRepository, never()).updateByTaskId(any());
  }

  @Test
  void 更新失敗_更新件数が0件の場合は404用例外を投げてカテゴリを置換しないこと() {
    when(taskRepository.findById(TASK_ID, USER_ID)).thenReturn(finishedTask());
    when(causeCategoryRepository.findActiveByCode(CATEGORY_CODE)).thenReturn(category());
    when(reflectionRepository.findByTaskId(TASK_ID))
        .thenReturn(reflection(20, "更新前", null));
    when(reflectionRepository.updateByTaskId(any(Reflection.class))).thenReturn(0);

    assertThatThrownBy(() -> sut.update(
        USER_ID, TASK_ID, request(List.of(CATEGORY_CODE), "更新後", null)))
        .isInstanceOfSatisfying(TargetNotFoundException.class, ex ->
            assertThat(ex.getField()).isEqualTo("reflection.taskId"));
    verify(reflectionRepository).updateByTaskId(any(Reflection.class));
    verifyNoInteractions(causeCategoryLinkRepository);
  }

  private static ReflectionRequest request(
      List<String> causeCategoryCodes, String cause, String nextAction) {
    ReflectionRequest request = new ReflectionRequest();
    request.setCauseCategoryCodes(causeCategoryCodes);
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

  private static ReflectionCauseCategory category() {
    return new ReflectionCauseCategory(
        3, CATEGORY_CODE, "作業の洗い出しが足りなかった", CauseDirection.OVER,
        "着手前に手順を書き出す", false, 10, true);
  }

  private static ReflectionCauseCategory secondCategory() {
    return new ReflectionCauseCategory(
        5, "REWORK", "手戻り・やり直しが発生した", CauseDirection.OVER,
        "早い段階で方針を確認する", false, 50, true);
  }

  private static ReflectionCauseCategory otherCategory() {
    return new ReflectionCauseCategory(
        18, "OTHER", "その他", CauseDirection.BOTH, null, true, 220, true);
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

  private static ReflectionTaskRow row(
      int taskId,
      String taskTitle,
      Integer taskGroupId,
      String taskGroupTitle,
      Integer reflectionId,
      String cause,
      String nextAction,
      Integer actualMinutesCached,
      Integer gapMinutesCached,
      Double gapRateCached) {
    LocalDateTime finishedAt = LocalDateTime.of(2026, 8, 10, 10, 0);
    LocalDateTime reflectionAt = reflectionId == null
        ? null
        : LocalDateTime.of(2026, 8, 10, 10, 5);
    return new ReflectionTaskRow(
        taskId,
        taskTitle,
        finishedAt,
        actualMinutesCached,
        gapMinutesCached,
        gapRateCached,
        taskGroupId,
        taskGroupTitle,
        reflectionId,
        cause,
        nextAction,
        reflectionAt,
        reflectionAt);
  }
}
