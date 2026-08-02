package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.WorkSession;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.exception.WorkSessionEndNotAllowedException;
import com.kiborisaway.tasktimetracker.repository.TaskRepository;
import com.kiborisaway.tasktimetracker.repository.WorkSessionRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkSessionService {

  private WorkSessionRepository wsRepository;
  private TaskRepository tsRepository;

  @Autowired
  public WorkSessionService(WorkSessionRepository wsRepository, TaskRepository tsRepository) {
    this.wsRepository = wsRepository;
    this.tsRepository = tsRepository;
  }

  /**
   * 指定したタスクの作業時間合計を取得します。
   *
   * @param taskId タスクID
   * @return 作業時間合計(分)
   */
  public int getTaskActualTotalTime(int taskId) {
    return wsRepository.sumMinutesByTaskId(taskId);
  }

  /**
   * 指定したタスクグループ配下の作業時間合計を取得します。
   *
   * @param taskGroupId タスクグループID
   * @return 作業時間合計(分)
   */
  public int getTaskGroupActualTotalTime(int taskGroupId) {
    return wsRepository.sumMinutesByTaskGroupId(taskGroupId);
  }

  /**
   * 指定したプロジェクト配下の作業時間合計を取得します。
   *
   * @param projectId プロジェクトID
   * @return 作業時間合計(分)
   */
  public int getProjectActualTotalTime(int projectId) {
    return wsRepository.sumMinutesByProjectId(projectId);
  }

  /**
   * 指定したタスクに紐づく作業セッション一覧を取得します。
   *
   * @param taskId タスクID
   * @return 作業セッション一覧
   */
  public List<WorkSession> getAllInTask(int taskId) {
    return wsRepository.findAllByTaskId(taskId);
  }

  /**
   * IDを指定して作業セッションを取得します。
   *
   * @param wsId 作業セッションID
   * @return 作業セッション
   */
  public WorkSession get(int wsId) {
    WorkSession workSession = wsRepository.findById(wsId);
    if (workSession == null) {
      throw new TargetNotFoundException("workSession.id",
          "指定したIDの作業セッションは見つかりませんでした");
    }
    return workSession;
  }

  /**
   * タスクIDを作業セッションに設定して新規登録します。
   *
   * @param taskId      タスクID
   * @param workSession 作業セッション
   * @return 登録した作業セッション
   */
  @Transactional
  public WorkSession create(int taskId, WorkSession workSession) {
    if (!tsRepository.existsById(taskId)) {
      throw new TargetNotFoundException("task.id",
          "指定したIDのタスクは見つかりませんでした");
    }

    workSession.setTaskId(taskId);
    wsRepository.insert(workSession);
    return workSession;
  }

  /**
   * 作業セッションを終了します。終了可能でない場合は更新しません。
   *
   * @param wsId 作業セッションID
   */
  @Transactional
  public void setEnd(int wsId) {
    if (!wsRepository.canSetEnd(wsId)) {
      throw new WorkSessionEndNotAllowedException("workSession.id",
          "指定した作業セッションは終了できません");
    }

    int updated = wsRepository.setEnd(wsId);
    if (updated == 0) {
      throw new TargetNotFoundException("workSession.id",
          "終了対象の作業セッションが見つかりませんでした");
    }
  }

  /**
   * 作業セッションIDを作業セッションに設定して更新します。
   *
   * @param wsId        作業セッションID
   * @param workSession 作業セッション
   */
  @Transactional
  public void update(int wsId, WorkSession workSession) {
    workSession.setId(wsId);
    int updated = wsRepository.update(workSession);
    if (updated == 0) {
      throw new TargetNotFoundException("workSession.id",
          "更新対象の作業セッションが見つかりませんでした");
    }
  }

  /**
   * IDを指定して作業セッションを削除します。
   *
   * @param wsId 作業セッションID
   */
  @Transactional
  public void delete(int wsId) {
    int deleted = wsRepository.deleteById(wsId);
    if (deleted == 0) {
      throw new TargetNotFoundException("workSession.id",
          "削除対象の作業セッションが見つかりませんでした");
    }
  }

}
