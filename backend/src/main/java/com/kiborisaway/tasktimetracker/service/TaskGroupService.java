package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.dto.memo.MemoResponse;
import com.kiborisaway.tasktimetracker.data.dto.task_group.TaskGroupCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.task_group.TaskGroupResponse;
import com.kiborisaway.tasktimetracker.data.dto.task_group.TaskGroupUpdateRequest;
import com.kiborisaway.tasktimetracker.data.entity.Memo;
import com.kiborisaway.tasktimetracker.data.entity.TaskGroup;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.exception.TaskGroupFinishNotAllowedException;
import com.kiborisaway.tasktimetracker.repository.MemoRepository;
import com.kiborisaway.tasktimetracker.repository.ProjectItemOrderRepository;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import com.kiborisaway.tasktimetracker.repository.TaskGroupRepository;
import com.kiborisaway.tasktimetracker.repository.TaskRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskGroupService {

  private TaskGroupRepository tgRepository;
  private ProjectRepository prRepository;
  private MemoRepository memoRepository;
  private ProjectItemOrderRepository pjItemOrderRepository;
  private TaskRepository tsRepository;

  @Autowired
  public TaskGroupService(
      TaskGroupRepository tgRepository,
      ProjectRepository prRepository,
      MemoRepository memoRepository,
      ProjectItemOrderRepository pjItemOrderRepository,
      TaskRepository tsRepository) {
    this.tgRepository = tgRepository;
    this.prRepository = prRepository;
    this.memoRepository = memoRepository;
    this.pjItemOrderRepository = pjItemOrderRepository;
    this.tsRepository = tsRepository;
  }

  /**
   * プロジェクトIDに紐づくタスクグループの一覧検索を行います。完了フラグを指定した場合、指定した完了状態のタスクグループのみを取得します。
   *
   * @param userId     認証ユーザーのID
   * @param pId        プロジェクトID
   * @param isFinished 完了フラグ
   * @return 全件または指定した完了状態のタスクグループの一覧
   */
  public List<TaskGroupResponse> findAllByCondition(int userId, int pId, Boolean isFinished) {
    if (!prRepository.existsByIdAndUserId(pId, userId)) {
      throw new TargetNotFoundException("project.id",
          "指定したIDのプロジェクトは見つかりませんでした");
    }

    List<TaskGroup> taskGroups;
    if (isFinished == null) {
      taskGroups = tgRepository.findAllInProject(pId, userId);
    } else {
      taskGroups = tgRepository.findAllInProjectByIsFinished(pId, isFinished, userId);
    }
    if (taskGroups.isEmpty()) {
      return List.of();
    }

    List<Integer> taskGroupIds = taskGroups.stream().map(TaskGroup::getId).toList();
    Map<Integer, List<MemoResponse>> memosByTaskGroupId =
        memoRepository.findAllInTaskGroups(taskGroupIds).stream()
            .collect(Collectors.groupingBy(
                Memo::getTaskGroupId,
                Collectors.mapping(MemoResponse::new, Collectors.toList())));

    return taskGroups.stream()
        .map(taskGroup -> new TaskGroupResponse(
            taskGroup,
            memosByTaskGroupId.getOrDefault(taskGroup.getId(), List.of())))
        .toList();
  }

  /**
   * IDによるタスクグループの検索
   *
   * @param userId 認証ユーザーのID
   * @param id     タスクグループのID
   * @return タスクグループ
   */
  public TaskGroupResponse findById(int userId, int id) {
    return toResponse(findTaskGroupById(userId, id));
  }

  /**
   * タスクグループの新規登録を行います。
   *
   * @param userId  認証ユーザーのID
   * @param request 新規登録するタスクグループのリクエスト
   */
  @Transactional
  public TaskGroupResponse register(int userId, int pId, TaskGroupCreateRequest request) {
    if (!prRepository.existsByIdAndUserId(pId, userId)) {
      throw new TargetNotFoundException("project.id",
          "指定したIDのプロジェクトは見つかりませんでした");
    }
    TaskGroup tg = toEntity(request);
    tg.setProjectId(pId);
    tgRepository.insert(tg);
    pjItemOrderRepository.insertAppendForTaskGroup(pId, tg.getId());
    TaskGroup registeredTaskGroup = findTaskGroupById(userId, tg.getId());
    return new TaskGroupResponse(registeredTaskGroup, List.of());
  }

  /**
   * タスクグループIDを指定してタスクグループ名と説明と完了フラグを更新します
   *
   * @param userId  認証ユーザーのID
   * @param id      更新するタスクグループのID
   * @param request 更新するタスクグループのリクエスト
   */
  @Transactional
  public TaskGroupResponse update(int userId, int id, TaskGroupUpdateRequest request) {
    TaskGroup tg = toEntity(id, request);
    int updated = tgRepository.update(tg, userId);
    if (updated == 0) {
      throw new TargetNotFoundException("taskGroup",
          "更新対象のタスクグループが見つかりませんでした");
    }
    return toResponse(findTaskGroupById(userId, id));
  }

  /**
   * タスクグループIDを指定して完了状態を更新します。完了にする場合、未完了のタスクが1件でも存在すると更新できません。
   *
   * @param userId     認証ユーザーのID
   * @param id         更新するタスクグループのID
   * @param isFinished 完了状態
   */
  @Transactional
  public TaskGroupResponse updateFinished(int userId, int id, boolean isFinished) {
    if (isFinished && tsRepository.existsUnfinishedInTaskGroup(id, userId)) {
      throw new TaskGroupFinishNotAllowedException("taskGroup.id",
          "未完了のタスクがあるタスクグループは完了状態にできません");
    }

    int updated = tgRepository.updateFinished(id, isFinished, userId);
    if (updated == 0) {
      throw new TargetNotFoundException("taskGroup.id",
          "完了状態更新対象のタスクグループが見つかりませんでした");
    }
    return toResponse(findTaskGroupById(userId, id));
  }

  private TaskGroup toEntity(TaskGroupCreateRequest request) {
    return new TaskGroup(request);
  }

  private TaskGroup toEntity(int id, TaskGroupUpdateRequest request) {
    return new TaskGroup(id, request);
  }

  private TaskGroupResponse toResponse(TaskGroup taskGroup) {
    List<Memo> memos = memoRepository.findAllInTaskGroup(taskGroup.getId());
    List<MemoResponse> memoResponses = (memos == null ? List.<Memo>of() : memos).stream()
        .map(MemoResponse::new)
        .toList();
    return new TaskGroupResponse(taskGroup, memoResponses);
  }

  private TaskGroup findTaskGroupById(int userId, int id) {
    TaskGroup taskGroup = tgRepository.findById(id, userId);
    if (taskGroup == null) {
      throw new TargetNotFoundException("taskGroup.id",
          "指定したIDのタスクグループは見つかりませんでした");
    }
    return taskGroup;
  }
}
