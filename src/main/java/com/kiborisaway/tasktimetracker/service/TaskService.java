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
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import com.kiborisaway.tasktimetracker.repository.TaskGroupRepository;
import com.kiborisaway.tasktimetracker.repository.TaskRepository;
import com.kiborisaway.tasktimetracker.repository.WorkSessionRepository;
import java.util.List;
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

  @Autowired
  public TaskService(
      TaskRepository tsRepository,
      WorkSessionRepository wsRepository,
      TaskGroupRepository tgRepository,
      ProjectRepository pjRepository,
      MemoRepository memoRepository
  ) {
    this.tsRepository = tsRepository;
    this.wsRepository = wsRepository;
    this.tgRepository = tgRepository;
    this.pjRepository = pjRepository;
    this.memoRepository = memoRepository;
  }

  /**
   * タスクグループIDに紐づくタスクの一覧検索を行います。完了フラグを指定した場合、指定した完了状態のタスクのみを取得します。
   *
   * @param tgId       タスクグループID
   * @param isFinished 完了フラグ
   * @return タスクグループ内の全件または指定した完了状態のタスクの一覧
   */
  public List<TaskResponse> findAllInTaskGroupByCondition(int tgId, Boolean isFinished) {
    if (!tgRepository.existsById(tgId)) {
      throw new TargetNotFoundException("taskGroup.id",
          "指定したIDのタスクグループは見つかりませんでした");
    }

    List<Task> tasks;
    if (isFinished == null) {
      tasks = tsRepository.findAllInTaskGroup(tgId);
    } else {
      tasks = tsRepository.findAllInTaskGroupByCondition(tgId, isFinished);
    }
    return tasks.stream().map(this::toResponse).toList();
  }

  /**
   * プロジェクトIDに紐づくタスクの一覧検索を行います。完了フラグを指定した場合、指定した完了状態のタスクのみを取得します。
   *
   * @param pjId       プロジェクトID
   * @param isFinished 完了フラグ
   * @return プロジェクト内の全件または指定した完了状態のタスクの一覧
   */
  public List<TaskResponse> findAllInProjectByCondition(int pjId, Boolean isFinished) {
    if (!pjRepository.existsById(pjId)) {
      throw new TargetNotFoundException("project.id",
          "指定したIDのプロジェクトは見つかりませんでした");
    }

    List<Task> tasks;
    if (isFinished == null) {
      tasks = tsRepository.findAllInProject(pjId);
    } else {
      tasks = tsRepository.findAllInProjectByCondition(pjId, isFinished);
    }
    return tasks.stream().map(this::toResponse).toList();
  }

  /**
   * IDによるタスクグループの検索
   *
   * @param id タスクグループのID
   * @return タスクグループ
   */
  public TaskResponse findById(int id) {
    Task task = tsRepository.findById(id);
    if (task == null) {
      throw new TargetNotFoundException("task.id",
          "指定したIDのタスクは見つかりませんでした");
    }
    return toResponse(task);
  }

  /**
   * タスクの新規登録を行います。projectIdとtaskGroupIdはXORになるようコントローラで制御する前提です。
   *
   * @param projectId   親となるプロジェクトID
   * @param taskGroupId 親となるタスクグループID
   * @param request     新規登録するタスクのリクエスト
   */
  @Transactional
  public TaskResponse register(Integer projectId, Integer taskGroupId, TaskCreateRequest request) {
    Task task = toEntity(projectId, taskGroupId, request);

    String parentField = "";
    boolean existsParent = false;

    if (task.getProjectId() != null) {
      parentField = "project.id";
      existsParent = pjRepository.existsById(task.getProjectId());
    }
    if (task.getTaskGroupId() != null) {
      parentField = "taskGroup.id";
      existsParent = tgRepository.existsById(task.getTaskGroupId());
    }

    if (!existsParent) {
      throw new TargetNotFoundException(parentField,
          "指定したIDの親項目は見つかりませんでした");
    }

    tsRepository.insert(task);
    return toResponse(task);
  }

  /**
   * タスクIDを指定してタスク名と説明を更新します
   *
   * @param id      更新するタスクのID
   * @param request 更新するタスクのリクエスト
   */
  @Transactional
  public void updateProperty(int id, TaskUpdatePropertyRequest request) {
    Task task = toEntity(id, request);
    int updated = tsRepository.updateProperty(task);
    if (updated == 0) {
      throw new TargetNotFoundException("task.id",
          "更新対象のタスクが見つかりませんでした");
    }
  }

  /**
   * タスクの所属を同根プロジェクト直下または同一プロジェクト内のタスクグループ配下へ変更します。
   *
   * @param id          タスクID
   * @param projectId   移動先プロジェクトID
   * @param taskGroupId 移動先タスクグループID
   */
  @Transactional
  public void updateParent(int id, Integer projectId, Integer taskGroupId) {
    Task task = tsRepository.findById(id);
    if (task == null) {
      throw new TargetNotFoundException("task.id",
          "所属変更対象のタスクが見つかりませんでした");
    }

    if (projectId != null && !pjRepository.existsById(projectId)) {
      throw new TargetNotFoundException("project.id",
          "移動先のプロジェクトが見つかりませんでした");
    }

    Integer currentProjectId = task.getProjectId();
    if (currentProjectId == null) {
      TaskGroup currentTaskGroup = tgRepository.findById(task.getTaskGroupId());
      if (currentTaskGroup == null) {
        throw new TargetNotFoundException("task.taskGroupId",
            "タスクの所属元タスクグループが見つかりませんでした");
      }
      currentProjectId = currentTaskGroup.getProjectId();
    }

    int updated;
    if (taskGroupId != null) {
      TaskGroup targetTaskGroup = tgRepository.findById(taskGroupId);
      if (targetTaskGroup == null) {
        throw new TargetNotFoundException("taskGroup.id",
            "移動先のタスクグループが見つかりませんでした");
      }
      if (!currentProjectId.equals(targetTaskGroup.getProjectId())) {
        throw new TargetNotFoundException("taskGroup.id",
            "移動先のタスクグループはタスクの所属プロジェクト内に見つかりませんでした");
      }
      updated = tsRepository.updateTaskGroup(id, taskGroupId);
    } else {
      if (!currentProjectId.equals(projectId)) {
        throw new TargetNotFoundException("project.id",
            "移動先のプロジェクトはタスクの所属元プロジェクトと一致しませんでした");
      }
      updated = tsRepository.updateProject(id, projectId);
    }

    if (updated == 0) {
      throw new TargetNotFoundException("task.id",
          "所属変更対象のタスクが見つかりませんでした");
    }
  }

  /**
   * タスクIDを指定して見積もり作業時間を更新します。 紐づく作業セッションが存在する場合は更新できません。
   *
   * @param id      タスクID
   * @param request 更新する見積作業時間のリクエスト
   */
  @Transactional
  public void updateEstimateMinutes(int id, TaskUpdateEstimatedMinutesRequest request) {
    if (wsRepository.existsByTaskId(id)) {
      throw new EstimateMinutesUpdateNotAllowedException("task.id",
          "作業セッションが存在するタスクの見積もり作業時間は変更できません");
    }
    int updated = tsRepository.updateEstimateMinutes(id, request.getEstimatedMinutes());
    if (updated == 0) {
      throw new TargetNotFoundException("task.id",
          "更新対象のタスクが見つかりませんでした");
    }
  }

  /**
   * タスクIDを指定してタスクの完了状態を更新します。完了にする場合は作業セッションの時間を集計してキャッシュし、未完了に戻す場合は完了日時とキャッシュを削除します。
   *
   * @param id         タスクID
   * @param isFinished 完了状態
   */
  @Transactional
  public void updateFinished(int id, boolean isFinished) {
    if (isFinished && wsRepository.existsUnfinishedByTaskId(id)) {
      throw new TaskFinishNotAllowedException("task.id",
          "未終了の作業セッションがあるタスクは完了状態にできません");
    }

    int updated = tsRepository.updateFinished(id, isFinished);
    if (updated == 0) {
      throw new TargetNotFoundException("task.id",
          "完了状態更新対象のタスクが見つかりませんでした");
    }
  }

  /**
   * タスクIDを指定してタスクを削除します。
   *
   * @param id タスクのID
   */
  @Transactional
  public void deleteById(int id) {
    int deleted = tsRepository.deleteById(id);
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
}
