package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.dto.reflection.ProjectReflectionOverviewResponse;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionRequest;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionResponse;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionTaskGroupResponse;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionTaskResponse;
import com.kiborisaway.tasktimetracker.data.entity.Reflection;
import com.kiborisaway.tasktimetracker.data.entity.Project;
import com.kiborisaway.tasktimetracker.data.entity.Task;
import com.kiborisaway.tasktimetracker.exception.ReflectionAlreadyExistsException;
import com.kiborisaway.tasktimetracker.exception.ReflectionOperationNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import com.kiborisaway.tasktimetracker.repository.ReflectionRepository;
import com.kiborisaway.tasktimetracker.repository.ReflectionTaskRow;
import com.kiborisaway.tasktimetracker.repository.TaskRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReflectionService {

  private ReflectionRepository reflectionRepository;
  private TaskRepository taskRepository;
  private ProjectRepository projectRepository;

  @Autowired
  public ReflectionService(
      ReflectionRepository reflectionRepository,
      TaskRepository taskRepository,
      ProjectRepository projectRepository) {
    this.reflectionRepository = reflectionRepository;
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

    List<ReflectionTaskResponse> directTasks =
        reflectionRepository.findDirectTasksInProject(projectId).stream()
            .map(this::toTaskResponse)
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
                rows.stream().map(this::toTaskResponse).toList()))
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
  public Reflection register(int userId, int taskId, ReflectionRequest request) {
    requireFinishedTask(userId, taskId);

    if (reflectionRepository.existsByTaskId(taskId)) {
      throw new ReflectionAlreadyExistsException(
          "reflection.taskId", "指定したタスクの振り返りは既に登録されています");
    }

    Reflection reflection = new Reflection(
        null,
        taskId,
        request.getCause().trim(),
        normalizeNextAction(request.getNextAction()),
        null, // causeCategoryId: B2で設定する
        null,
        null);
    reflectionRepository.insert(reflection);
    return findByTaskId(taskId);
  }

  /**
   * 完了タスクの振り返りを更新します。
   *
   * @param userId  認証ユーザーID
   * @param taskId  対象タスクID
   * @param request 振り返り更新リクエスト
   * @return 更新した振り返り
   */
  @Transactional
  public Reflection update(int userId, int taskId, ReflectionRequest request) {
    requireFinishedTask(userId, taskId);

    Reflection reflection = reflectionRepository.findByTaskId(taskId);
    if (reflection == null) {
      throw new TargetNotFoundException(
          "reflection.taskId", "更新対象の振り返りが見つかりませんでした");
    }

    reflection.setCause(request.getCause().trim());
    reflection.setNextAction(normalizeNextAction(request.getNextAction()));
    int updated = reflectionRepository.updateByTaskId(reflection);
    if (updated == 0) {
      throw new TargetNotFoundException(
          "reflection.taskId", "更新対象の振り返りが見つかりませんでした");
    }
    return findByTaskId(taskId);
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

  private Reflection findByTaskId(int taskId) {
    Reflection reflection = reflectionRepository.findByTaskId(taskId);
    if (reflection == null) {
      throw new TargetNotFoundException(
          "reflection.taskId", "指定したタスクの振り返りが見つかりませんでした");
    }
    return reflection;
  }

  private ReflectionTaskResponse toTaskResponse(ReflectionTaskRow row) {
    ReflectionResponse reflection = row.getReflectionId() == null
        ? null
        : new ReflectionResponse(
            row.getReflectionId(),
            row.getTaskId(),
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

  private String normalizeNextAction(String nextAction) {
    if (nextAction == null) {
      return null;
    }
    String normalized = nextAction.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
