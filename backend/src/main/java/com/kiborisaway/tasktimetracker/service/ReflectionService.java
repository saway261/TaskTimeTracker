package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.dto.reflection.ProjectReflectionOverviewResponse;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionCauseCategorySummaryResponse;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionRequest;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionResponse;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionTaskGroupResponse;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionTaskResponse;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReflectionService {

  private ReflectionRepository reflectionRepository;
  private ReflectionCauseCategoryRepository causeCategoryRepository;
  private ReflectionCauseCategoryLinkRepository causeCategoryLinkRepository;
  private TaskRepository taskRepository;
  private ProjectRepository projectRepository;

  @Autowired
  public ReflectionService(
      ReflectionRepository reflectionRepository,
      ReflectionCauseCategoryRepository causeCategoryRepository,
      ReflectionCauseCategoryLinkRepository causeCategoryLinkRepository,
      TaskRepository taskRepository,
      ProjectRepository projectRepository) {
    this.reflectionRepository = reflectionRepository;
    this.causeCategoryRepository = causeCategoryRepository;
    this.causeCategoryLinkRepository = causeCategoryLinkRepository;
    this.taskRepository = taskRepository;
    this.projectRepository = projectRepository;
  }

  /**
   * プロジェクト内の完了タスクを、登録済みの振り返りと表示順を含めて取得します。
   *
   * @param userId    認証ユーザーID
   * @param projectId 対象プロジェクトID
   * @return プロジェクトの振り返り対象一覧
   */
  @Transactional(readOnly = true)
  public ProjectReflectionOverviewResponse getOverview(int userId, int projectId) {
    Project project = projectRepository.findByIdAndUserId(projectId, userId);
    if (project == null) {
      throw new TargetNotFoundException(
          "project.id", "指定したIDのプロジェクトは見つかりませんでした");
    }

    Map<Integer, List<ReflectionCauseCategorySummaryResponse>> categoriesByReflectionId =
        causeCategoryLinkRepository.findCategoriesInProject(projectId).stream()
            .collect(Collectors.groupingBy(
                ReflectionCauseCategoryLinkRow::getReflectionId,
                LinkedHashMap::new,
                Collectors.mapping(
                    row -> new ReflectionCauseCategorySummaryResponse(
                        row.getCauseCategoryCode(), row.getCauseCategoryLabel()),
                    Collectors.toList())));

    List<ReflectionTaskResponse> directTasks =
        reflectionRepository.findDirectTasksInProject(projectId).stream()
            .map(row -> toTaskResponse(row, categoriesByReflectionId))
            .toList();

    List<ReflectionTaskGroupResponse> taskGroups =
        reflectionRepository.findGroupedTasksInProject(projectId).stream()
            .collect(Collectors.groupingBy(
                ReflectionTaskRow::getTaskGroupId,
                LinkedHashMap::new,
                Collectors.toList()))
            .values().stream()
            .map(rows -> new ReflectionTaskGroupResponse(
                rows.getFirst().getTaskGroupId(),
                rows.getFirst().getTaskGroupTitle(),
                rows.stream().map(row -> toTaskResponse(row, categoriesByReflectionId)).toList()))
            .toList();

    return new ProjectReflectionOverviewResponse(
        project.getId(), project.getTitle(), directTasks, taskGroups);
  }

  /**
   * 完了タスクへ振り返りを登録します。
   *
   * @param userId  認証ユーザーID
   * @param taskId  対象タスクID
   * @param request 振り返り登録リクエスト
   * @return 登録した振り返り
   */
  @Transactional
  public ReflectionResponse register(int userId, int taskId, ReflectionRequest request) {
    requireFinishedTask(userId, taskId);
    List<ReflectionCauseCategory> categories =
        requireActiveCategories(request.getCauseCategoryCodes());
    String cause = requireCauseIfNeeded(request.getCause(), categories);

    if (reflectionRepository.existsByTaskId(taskId)) {
      throw new ReflectionAlreadyExistsException(
          "reflection.taskId", "指定したタスクの振り返りは既に登録されています");
    }

    Reflection reflection = new Reflection(
        null,
        taskId,
        cause,
        normalizeToNullIfBlank(request.getNextAction()),
        null,
        null);
    reflectionRepository.insert(reflection);
    linkCategories(reflection.getId(), categories);
    return new ReflectionResponse(findByTaskId(taskId), categories);
  }

  /**
   * 完了タスクの振り返りを更新します。原因カテゴリは全置換です。
   *
   * @param userId  認証ユーザーID
   * @param taskId  対象タスクID
   * @param request 振り返り更新リクエスト
   * @return 更新した振り返り
   */
  @Transactional
  public ReflectionResponse update(int userId, int taskId, ReflectionRequest request) {
    requireFinishedTask(userId, taskId);
    List<ReflectionCauseCategory> categories =
        requireActiveCategories(request.getCauseCategoryCodes());
    String cause = requireCauseIfNeeded(request.getCause(), categories);

    Reflection reflection = reflectionRepository.findByTaskId(taskId);
    if (reflection == null) {
      throw new TargetNotFoundException(
          "reflection.taskId", "更新対象の振り返りが見つかりませんでした");
    }

    reflection.setCause(cause);
    reflection.setNextAction(normalizeToNullIfBlank(request.getNextAction()));
    int updated = reflectionRepository.updateByTaskId(reflection);
    if (updated == 0) {
      throw new TargetNotFoundException(
          "reflection.taskId", "更新対象の振り返りが見つかりませんでした");
    }
    causeCategoryLinkRepository.deleteByReflectionId(reflection.getId());
    linkCategories(reflection.getId(), categories);
    return new ReflectionResponse(findByTaskId(taskId), categories);
  }

  private void requireFinishedTask(int userId, int taskId) {
    Task task = taskRepository.findById(taskId, userId);
    if (task == null) {
      throw new TargetNotFoundException(
          "task.id", "指定したIDのタスクは見つかりませんでした");
    }
    if (task.getFinishedAt() == null) {
      throw new ReflectionOperationNotAllowedException(
          "task.finishedAt", "未完了のタスクには振り返りを登録・更新できません");
    }
  }

  /**
   * 原因カテゴリコードを検証し、表示順に整列したカテゴリ一覧を返します。
   * 選択順は保持しません（要件 §5.2）。
   */
  private List<ReflectionCauseCategory> requireActiveCategories(List<String> codes) {
    List<ReflectionCauseCategory> categories = new ArrayList<>();
    for (String code : codes) {
      ReflectionCauseCategory category = causeCategoryRepository.findActiveByCode(code);
      if (category == null) {
        throw new ReflectionCauseCategoryInvalidException(
            "reflection.causeCategoryCodes", "指定した原因カテゴリは選択できません");
      }
      categories.add(category);
    }
    categories.sort(
        Comparator.comparing(ReflectionCauseCategory::getDisplayOrder)
            .thenComparing(ReflectionCauseCategory::getId));
    return categories;
  }

  /**
   * 選択したカテゴリのいずれかが自由記述を必須とする場合、未入力を400として拒否します（要件 §5.2）。
   * 判定はカテゴリの requiresCause 属性で行い、コードで分岐しません。
   */
  private String requireCauseIfNeeded(String cause, List<ReflectionCauseCategory> categories) {
    String normalized = normalizeToNullIfBlank(cause);
    boolean required = categories.stream()
        .anyMatch(category -> Boolean.TRUE.equals(category.getRequiresCause()));
    if (required && normalized == null) {
      throw new ReflectionCauseRequiredException(
          "reflection.cause", "選択した原因カテゴリでは、原因の記述が必要です");
    }
    return normalized;
  }

  private void linkCategories(int reflectionId, List<ReflectionCauseCategory> categories) {
    for (ReflectionCauseCategory category : categories) {
      causeCategoryLinkRepository.insert(reflectionId, category.getId());
    }
  }

  private Reflection findByTaskId(int taskId) {
    Reflection reflection = reflectionRepository.findByTaskId(taskId);
    if (reflection == null) {
      throw new TargetNotFoundException(
          "reflection.taskId", "指定したタスクの振り返りが見つかりませんでした");
    }
    return reflection;
  }

  private ReflectionTaskResponse toTaskResponse(
      ReflectionTaskRow row,
      Map<Integer, List<ReflectionCauseCategorySummaryResponse>> categoriesByReflectionId) {
    ReflectionResponse reflection = row.getReflectionId() == null
        ? null
        : new ReflectionResponse(
            row.getReflectionId(),
            row.getTaskId(),
            categoriesByReflectionId.getOrDefault(row.getReflectionId(), List.of()),
            row.getCause(),
            row.getNextAction(),
            row.getReflectionCreatedAt(),
            row.getReflectionUpdatedAt());

    return new ReflectionTaskResponse(
        row.getTaskId(),
        row.getTaskTitle(),
        row.getFinishedAt(),
        row.getActualMinutesCached(),
        row.getGapMinutesCached(),
        row.getGapRateCached(),
        reflection);
  }

  private String normalizeToNullIfBlank(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
