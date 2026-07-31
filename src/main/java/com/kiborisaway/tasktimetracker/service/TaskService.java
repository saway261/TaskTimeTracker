package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.Task;
import com.kiborisaway.tasktimetracker.exception.EstimateMinutesUpdateNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import com.kiborisaway.tasktimetracker.repository.TaskGroupRepository;
import com.kiborisaway.tasktimetracker.repository.TaskRepository;
import com.kiborisaway.tasktimetracker.repository.WorkSessionRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

  private TaskRepository tsRepository;
  private WorkSessionRepository wsRepository;
  private TaskGroupRepository tgRepository;
  private ProjectRepository pjRepository;

  @Autowired
  public TaskService(
      TaskRepository tsRepository,
      WorkSessionRepository wsRepository,
      TaskGroupRepository tgRepository,
      ProjectRepository pjRepository
  ) {
    this.tsRepository = tsRepository;
    this.wsRepository = wsRepository;
    this.tgRepository = tgRepository;
    this.pjRepository = pjRepository;
  }

  /**
   * タスクグループIDに紐づくタスクの一覧検索を行います。完了フラグを指定した場合、指定した完了状態のタスクのみを取得します。
   *
   * @param tgId       タスクグループID
   * @param isFinished 完了フラグ
   * @return タスクグループ内の全件または指定した完了状態のタスクの一覧
   */
  public List<Task> findAllInTaskGroupByCondition(int tgId, Boolean isFinished) {
    if (!tgRepository.existsById(tgId)) {
      throw new TargetNotFoundException("taskGroup.id",
          "指定したIDのタスクグループは見つかりませんでした");
    }

    if (isFinished == null) {
      return tsRepository.findAllInTaskGroup(tgId);
    }
    return tsRepository.findAllInTaskGroupByCondition(tgId, isFinished);
  }

  /**
   * プロジェクトIDに紐づくタスクの一覧検索を行います。完了フラグを指定した場合、指定した完了状態のタスクのみを取得します。
   *
   * @param pjId       プロジェクトID
   * @param isFinished 完了フラグ
   * @return プロジェクト内の全件または指定した完了状態のタスクの一覧
   */
  public List<Task> findAllInProjectByCondition(int pjId, Boolean isFinished) {
    if (!pjRepository.existsById(pjId)) {
      throw new TargetNotFoundException("project.id",
          "指定したIDのプロジェクトは見つかりませんでした");
    }

    if (isFinished == null) {
      return tsRepository.findAllInProject(pjId);
    }
    return tsRepository.findAllInProjectByCondition(pjId, isFinished);
  }

  /**
   * IDによるタスクグループの検索
   *
   * @param id タスクグループのID
   * @return タスクグループ
   */
  public Task findById(int id) {
    Task task = tsRepository.findById(id);
    if (task == null) {
      throw new TargetNotFoundException("task.id",
          "指定したIDのタスクは見つかりませんでした");
    }
    return task;
  }

  /**
   * タスクの新規登録を行います。projectIdとtaskGroupIdはXORになるようコントローラで制御する前提です。
   *
   * @param task 新規登録するタスク
   */
  public Task register(Task task) {
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
    return task;
  }

  /**
   * タスクIDを指定してタスク名と説明を更新します
   *
   * @param task 更新するタスク
   */
  public void updateProperty(Task task) {
    int updated = tsRepository.updateProperty(task);
    if (updated == 0) {
      throw new TargetNotFoundException("task.id",
          "更新対象のタスクが見つかりませんでした");
    }
  }

  /**
   * タスクIDを指定して見積もり作業時間を更新します。 紐づく作業セッションが存在する場合は更新できません。
   *
   * @param id               タスクID
   * @param estimatedMinutes 更新する見積作業時間
   */
  public void updateEstimateMinutes(int id, int estimatedMinutes) {
    if (wsRepository.existsByTaskId(id)) {
      throw new EstimateMinutesUpdateNotAllowedException("task.id",
          "作業セッションが存在するタスクの見積もり作業時間は変更できません");
    }
    int updated = tsRepository.updateEstimateMinutes(id, estimatedMinutes);
    if (updated == 0) {
      throw new TargetNotFoundException("task.id",
          "更新対象のタスクが見つかりませんでした");
    }
  }

  /**
   * タスクIDを指定してタスクを完了状態にします。 作業セッションの時間を合計して実作業時間と見積もりと実績の差と比率などをキャッシュして保存します。
   *
   * @param id
   */
  public void setFinished(int id) {
    int updated = tsRepository.setFinished(id);
    if (updated == 0) {
      throw new TargetNotFoundException("task.id",
          "完了対象のタスクが見つかりませんでした");
    }
  }

  /**
   * タスクIDを指定してタスクを削除します。
   *
   * @param id タスクのID
   */
  public void deleteById(int id) {
    int deleted = tsRepository.deleteById(id);
    if (deleted == 0) {
      throw new TargetNotFoundException("task.id",
          "削除対象のタスクが見つかりませんでした");
    }
  }

}
