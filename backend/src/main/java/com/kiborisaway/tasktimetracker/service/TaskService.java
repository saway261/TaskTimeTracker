package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.dto.memo.MemoResponse;
import com.kiborisaway.tasktimetracker.data.dto.task.TaskCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.task.TaskResponse;
import com.kiborisaway.tasktimetracker.data.dto.task.TaskUpdateEstimatedMinutesRequest;
import com.kiborisaway.tasktimetracker.data.dto.task.TaskUpdatePropertyRequest;
import com.kiborisaway.tasktimetracker.data.entity.Memo;
import com.kiborisaway.tasktimetracker.data.entity.Task;
import com.kiborisaway.tasktimetracker.data.entity.TaskGroup;
import com.kiborisaway.tasktimetracker.exception.EstimateMinutesUpdateNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.exception.TaskFinishNotAllowedException;
import com.kiborisaway.tasktimetracker.repository.MemoRepository;
import com.kiborisaway.tasktimetracker.repository.ProjectItemOrderRepository;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import com.kiborisaway.tasktimetracker.repository.ReflectionRepository;
import com.kiborisaway.tasktimetracker.repository.TaskGroupItemOrderRepository;
import com.kiborisaway.tasktimetracker.repository.TaskGroupRepository;
import com.kiborisaway.tasktimetracker.repository.TaskRepository;
import com.kiborisaway.tasktimetracker.repository.WorkSessionRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

  private TaskRepository tsRepository;
  private WorkSessionRepository wsRepository;
  private TaskGroupRepository tgRepository;
  private ProjectRepository pjRepository;
  private MemoRepository memoRepository;
  private ReflectionRepository reflectionRepository;
  private ProjectItemOrderRepository pjItemOrderRepository;
  private TaskGroupItemOrderRepository tgItemOrderRepository;

  @Autowired
  public TaskService(
      TaskRepository tsRepository,
      WorkSessionRepository wsRepository,
      TaskGroupRepository tgRepository,
      ProjectRepository pjRepository,
      MemoRepository memoRepository,
      ReflectionRepository reflectionRepository,
      ProjectItemOrderRepository pjItemOrderRepository,
      TaskGroupItemOrderRepository tgItemOrderRepository
  ) {
    this.tsRepository = tsRepository;
    this.wsRepository = wsRepository;
    this.tgRepository = tgRepository;
    this.pjRepository = pjRepository;
    this.memoRepository = memoRepository;
    this.reflectionRepository = reflectionRepository;
    this.pjItemOrderRepository = pjItemOrderRepository;
    this.tgItemOrderRepository = tgItemOrderRepository;
  }

  /**
   * タスクグループIDに紐づくタスクの一覧検索を行います。完了フラグを指定した場合、指定した完了状態のタスクのみを取得します。
   *
   * @param userId     認証ユーザーのID
   * @param tgId       タスクグループID
   * @param isFinished 完了フラグ
   * @return タスクグループ内の全件または指定した完了状態のタスクの一覧
   */
  public List<TaskResponse> findAllInTaskGroupByCondition(int userId, int tgId, Boolean isFinished) {
    if (!tgRepository.existsByIdAndUserId(tgId, userId)) {
      throw new TargetNotFoundException("taskGroup.id",
          "指定したIDのタスクグループは見つかりませんでした");
    }

    List<Task> tasks;
    if (isFinished == null) {
      tasks = tsRepository.findAllInTaskGroup(tgId, userId);
    } else {
      tasks = tsRepository.findAllInTaskGroupByCondition(tgId, isFinished, userId);
    }
    return toResponses(tasks);
  }

  /**
   * プロジェクトIDに紐づくタスクの一覧検索を行います。完了フラグを指定した場合、指定した完了状態のタスクのみを取得します。
   *
   * @param userId     認証ユーザーのID
   * @param pjId       プロジェクトID
   * @param isFinished 完了フラグ
   * @return プロジェクト内の全件または指定した完了状態のタスクの一覧
   */
  public List<TaskResponse> findAllInProjectByCondition(int userId, int pjId, Boolean isFinished) {
    if (!pjRepository.existsByIdAndUserId(pjId, userId)) {
      throw new TargetNotFoundException("project.id",
          "指定したIDのプロジェクトは見つかりませんでした");
    }

    List<Task> tasks;
    if (isFinished == null) {
      tasks = tsRepository.findAllInProject(pjId, userId);
    } else {
      tasks = tsRepository.findAllInProjectByCondition(pjId, isFinished, userId);
    }
    return toResponses(tasks);
  }

  /**
   * IDによるタスクグループの検索
   *
   * @param userId 認証ユーザーのID
   * @param id     タスクグループのID
   * @return タスクグループ
   */
  public TaskResponse findById(int userId, int id) {
    return toResponse(findTaskById(userId, id));
  }

  /**
   * タスクの新規登録を行います。projectIdとtaskGroupIdはXORになるようコントローラで制御する前提です。
   *
   * @param userId      認証ユーザーのID
   * @param projectId   親となるプロジェクトID
   * @param taskGroupId 親となるタスクグループID
   * @param request     新規登録するタスクのリクエスト
   */
  @Transactional
  public TaskResponse register(int userId, Integer projectId, Integer taskGroupId,
      TaskCreateRequest request) {
    Task task = toEntity(projectId, taskGroupId, request);

    String parentField = "";
    boolean existsParent = false;

    if (task.getProjectId() != null) {
      parentField = "project.id";
      existsParent = pjRepository.existsByIdAndUserId(task.getProjectId(), userId);
    }
    if (task.getTaskGroupId() != null) {
      parentField = "taskGroup.id";
      existsParent = tgRepository.existsByIdAndUserId(task.getTaskGroupId(), userId);
    }

    if (!existsParent) {
      throw new TargetNotFoundException(parentField,
          "指定したIDの親項目は見つかりませんでした");
    }

    tsRepository.insert(task);
    if (task.getTaskGroupId() != null) {
      tgItemOrderRepository.insertAppendForTask(task.getTaskGroupId(), task.getId());
    } else {
      pjItemOrderRepository.insertAppendForTask(task.getProjectId(), task.getId());
    }
    Task registeredTask = findTaskById(userId, task.getId());
    return new TaskResponse(registeredTask, List.of());
  }

  /**
   * タスクIDを指定してタスク名と説明を更新します
   *
   * @param userId  認証ユーザーのID
   * @param id      更新するタスクのID
   * @param request 更新するタスクのリクエスト
   */
  @Transactional
  public TaskResponse updateProperty(int userId, int id, TaskUpdatePropertyRequest request) {
    Task task = toEntity(id, request);
    int updated = tsRepository.updateProperty(task, userId);
    if (updated == 0) {
      throw new TargetNotFoundException("task.id",
          "更新対象のタスクが見つかりませんでした");
    }
    return findById(userId, id);
  }

  /**
   * タスクの所属を同根プロジェクト直下または同一プロジェクト内のタスクグループ配下へ変更します。
   *
   * @param userId      認証ユーザーのID
   * @param id          タスクID
   * @param projectId   移動先プロジェクトID
   * @param taskGroupId 移動先タスクグループID
   */
  @Transactional
  public TaskResponse updateParent(int userId, int id, Integer projectId, Integer taskGroupId) {
    Task task = tsRepository.findById(id, userId);
    if (task == null) {
      throw new TargetNotFoundException("task.id",
          "所属変更対象のタスクが見つかりませんでした");
    }

    if (projectId != null && !pjRepository.existsByIdAndUserId(projectId, userId)) {
      throw new TargetNotFoundException("project.id",
          "移動先のプロジェクトが見つかりませんでした");
    }

    Integer currentProjectId = task.getProjectId();
    if (currentProjectId == null) {
      TaskGroup currentTaskGroup = tgRepository.findById(task.getTaskGroupId(), userId);
      if (currentTaskGroup == null) {
        throw new TargetNotFoundException("task.taskGroupId",
            "タスクの所属元タスクグループが見つかりませんでした");
      }
      currentProjectId = currentTaskGroup.getProjectId();
    }

    int updated;
    if (taskGroupId != null) {
      TaskGroup targetTaskGroup = tgRepository.findById(taskGroupId, userId);
      if (targetTaskGroup == null) {
        throw new TargetNotFoundException("taskGroup.id",
            "移動先のタスクグループが見つかりませんでした");
      }
      if (!currentProjectId.equals(targetTaskGroup.getProjectId())) {
        throw new TargetNotFoundException("taskGroup.id",
            "移動先のタスクグループはタスクの所属プロジェクト内に見つかりませんでした");
      }
      updated = tsRepository.updateTaskGroup(id, taskGroupId, userId);
    } else {
      if (!currentProjectId.equals(projectId)) {
        throw new TargetNotFoundException("project.id",
            "移動先のプロジェクトはタスクの所属元プロジェクトと一致しませんでした");
      }
      updated = tsRepository.updateProject(id, projectId, userId);
    }

    if (updated == 0) {
      throw new TargetNotFoundException("task.id",
          "所属変更対象のタスクが見つかりませんでした");
    }

    // 移動元がプロジェクト直下・タスクグループ直下のどちらでも、該当する方にのみ行が存在するため両方削除して問題ない
    pjItemOrderRepository.deleteByTaskId(id);
    tgItemOrderRepository.deleteByTaskId(id);
    if (taskGroupId != null) {
      tgItemOrderRepository.insertAppendForTask(taskGroupId, id);
    } else {
      pjItemOrderRepository.insertAppendForTask(projectId, id);
    }

    return findById(userId, id);
  }

  /**
   * タスクIDを指定して見積もり作業時間を更新します。 紐づく作業セッションが存在する場合、またはタスクが完了済みの場合は更新できません。
   *
   * @param userId  認証ユーザーのID
   * @param id      タスクID
   * @param request 更新する見積作業時間のリクエスト
   */
  @Transactional
  public TaskResponse updateEstimateMinutes(int userId, int id,
      TaskUpdateEstimatedMinutesRequest request) {
    if (wsRepository.existsByTaskId(id, userId)) {
      throw new EstimateMinutesUpdateNotAllowedException("task.id",
          "作業セッションが存在するタスクの見積もり作業時間は変更できません");
    }
    if (tsRepository.isFinished(id, userId)) {
      throw new EstimateMinutesUpdateNotAllowedException("task.id",
          "完了済みタスクの見積もり作業時間は変更できません");
    }
    int updated = tsRepository.updateEstimateMinutes(id, request.getEstimatedMinutes(), userId);
    if (updated == 0) {
      throw new TargetNotFoundException("task.id",
          "更新対象のタスクが見つかりませんでした");
    }
    return findById(userId, id);
  }

  /**
   * タスクIDを指定してタスクの完了状態を更新します。完了にする場合は作業セッションの時間を集計してキャッシュし、未完了に戻す場合は完了日時とキャッシュを削除します。
   *
   * @param userId     認証ユーザーのID
   * @param id         タスクID
   * @param isFinished 完了状態
   */
  @Transactional
  public TaskResponse updateFinished(int userId, int id, boolean isFinished) {
    if (isFinished && wsRepository.existsUnfinishedByTaskId(id, userId)) {
      throw new TaskFinishNotAllowedException("task.id",
          "未終了の作業セッションがあるタスクは完了状態にできません");
    }

    int updated = tsRepository.updateFinished(id, isFinished, userId);
    if (!isFinished) {
      reflectionRepository.deleteByTaskId(id);
    }
    if (updated == 0) {
      throw new TargetNotFoundException("task.id",
          "完了状態更新対象のタスクが見つかりませんでした");
    }
    return findById(userId, id);
  }

  /**
   * タスクIDを指定してタスクを削除します。
   *
   * @param userId 認証ユーザーのID
   * @param id     タスクのID
   */
  @Transactional
  public void deleteById(int userId, int id) {
    if (!tsRepository.existsByIdAndUserId(id, userId)) {
      throw new TargetNotFoundException("task.id",
          "削除対象のタスクが見つかりませんでした");
    }
    wsRepository.deleteAllByTaskId(id);
    memoRepository.deleteAllInTask(id);
    reflectionRepository.deleteByTaskId(id);
    pjItemOrderRepository.deleteByTaskId(id);
    tgItemOrderRepository.deleteByTaskId(id);
    int deleted = tsRepository.deleteById(id, userId);
    if (deleted == 0) {
      throw new TargetNotFoundException("task.id",
          "削除対象のタスクが見つかりませんでした");
    }
  }

  private Task toEntity(Integer projectId, Integer taskGroupId, TaskCreateRequest request) {
    return new Task(projectId, taskGroupId, request);
  }

  private Task toEntity(int id, TaskUpdatePropertyRequest request) {
    return new Task(id, request);
  }

  private TaskResponse toResponse(Task task) {
    List<Memo> memos = memoRepository.findAllInTask(task.getId());
    List<MemoResponse> memoResponses = (memos == null ? List.<Memo>of() : memos).stream()
        .map(MemoResponse::new)
        .toList();
    return new TaskResponse(task, memoResponses);
  }

  private Task findTaskById(int userId, int id) {
    Task task = tsRepository.findById(id, userId);
    if (task == null) {
      throw new TargetNotFoundException("task.id",
          "指定したIDのタスクは見つかりませんでした");
    }
    return task;
  }

  private List<TaskResponse> toResponses(List<Task> tasks) {
    if (tasks.isEmpty()) {
      return List.of();
    }

    List<Integer> taskIds = tasks.stream().map(Task::getId).toList();
    Map<Integer, List<MemoResponse>> memosByTaskId = memoRepository.findAllInTasks(taskIds).stream()
        .collect(Collectors.groupingBy(
            Memo::getTaskId,
            Collectors.mapping(MemoResponse::new, Collectors.toList())));

    return tasks.stream()
        .map(task -> new TaskResponse(
            task,
            memosByTaskId.getOrDefault(task.getId(), List.of())))
        .toList();
  }
}
