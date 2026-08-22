package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.dto.work_session.WorkSessionCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.work_session.WorkSessionUpdateRequest;
import com.kiborisaway.tasktimetracker.data.dto.work_session.ActiveTimerResponse;
import com.kiborisaway.tasktimetracker.data.entity.WorkSession;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.exception.WorkSessionEndNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.WorkSessionOperationNotAllowedException;
import com.kiborisaway.tasktimetracker.publicid.id.ProjectId;
import com.kiborisaway.tasktimetracker.publicid.id.TaskGroupId;
import com.kiborisaway.tasktimetracker.publicid.id.TaskId;
import com.kiborisaway.tasktimetracker.publicid.id.WorkSessionId;
import com.kiborisaway.tasktimetracker.repository.ActiveTimerRow;
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
   * @param userId 認証ユーザーのID
   * @param taskId タスクID
   * @return 作業時間合計(分)
   */
  public int getTaskActualTotalTime(int userId, int taskId) {
    return wsRepository.sumMinutesByTaskId(taskId, userId);
  }

  /**
   * 指定したタスクグループ配下の作業時間合計を取得します。
   *
   * @param userId      認証ユーザーのID
   * @param taskGroupId タスクグループID
   * @return 作業時間合計(分)
   */
  public int getTaskGroupActualTotalTime(int userId, int taskGroupId) {
    return wsRepository.sumMinutesByTaskGroupId(taskGroupId, userId);
  }

  /**
   * 指定したプロジェクト配下の作業時間合計を取得します。
   *
   * @param userId    認証ユーザーのID
   * @param projectId プロジェクトID
   * @return 作業時間合計(分)
   */
  public int getProjectActualTotalTime(int userId, int projectId) {
    return wsRepository.sumMinutesByProjectId(projectId, userId);
  }

  /**
   * 指定したタスクに紐づく作業セッション一覧を取得します。
   *
   * @param userId 認証ユーザーのID
   * @param taskId タスクID
   * @return 作業セッション一覧
   */
  public List<WorkSession> getAllInTask(int userId, int taskId) {
    return wsRepository.findAllByTaskId(taskId, userId);
  }

  /**
   * ログインユーザーが所有する全タスクの未終了タイマーを取得します。
   */
  public List<ActiveTimerResponse> getAllActive(int userId) {
    return wsRepository.findAllActiveByUserId(userId).stream()
        .map(WorkSessionService::toActiveTimerResponse)
        .toList();
  }

  private static ActiveTimerResponse toActiveTimerResponse(ActiveTimerRow row) {
    return new ActiveTimerResponse(
        new WorkSessionId(row.getSessionId()),
        new TaskId(row.getTaskId()),
        row.getTaskTitle(),
        new ProjectId(row.getProjectId()),
        row.getTaskGroupId() == null ? null : new TaskGroupId(row.getTaskGroupId()),
        row.getStartedAt());
  }

  /**
   * IDを指定して作業セッションを取得します。
   *
   * @param userId 認証ユーザーのID
   * @param wsId   作業セッションID
   * @return 作業セッション
   */
  public WorkSession get(int userId, int wsId) {
    WorkSession workSession = wsRepository.findById(wsId, userId);
    if (workSession == null) {
      throw new TargetNotFoundException("workSession.id",
          "指定したIDの作業セッションは見つかりませんでした");
    }
    return workSession;
  }

  /**
   * タスクIDを作業セッションに設定して新規登録します。
   *
   * @param userId  認証ユーザーのID
   * @param taskId  タスクID
   * @param request 新規登録する作業セッションのリクエスト
   * @return 登録した作業セッション
   */
  @Transactional
  public WorkSession create(int userId, int taskId, WorkSessionCreateRequest request) {
    if (!tsRepository.existsByIdAndUserId(taskId, userId)) {
      throw new TargetNotFoundException("task.id",
          "指定したIDのタスクは見つかりませんでした");
    }
    validateTaskIsNotFinished(userId, taskId);

    WorkSession workSession = toEntity(request);
    workSession.setTaskId(taskId);
    wsRepository.insert(workSession);
    return get(userId, workSession.getId());
  }

  /**
   * 作業セッションを終了します。終了可能でない場合は更新しません。
   *
   * @param userId 認証ユーザーのID
   * @param wsId   作業セッションID
   */
  @Transactional
  public WorkSession setEnd(int userId, int wsId) {
    int taskId = findTaskIdByWorkSessionId(userId, wsId);
    validateTaskIsNotFinished(userId, taskId);

    if (!wsRepository.canSetEnd(wsId, userId)) {
      throw new WorkSessionEndNotAllowedException("workSession.id",
          "指定した作業セッションは終了できません");
    }

    int updated = wsRepository.setEnd(wsId, userId);
    if (updated == 0) {
      throw new WorkSessionEndNotAllowedException("workSession.id",
          "指定した作業セッションは既に終了しています");
    }
    return get(userId, wsId);
  }

  /**
   * 作業セッションIDを作業セッションに設定して更新します。
   *
   * @param userId  認証ユーザーのID
   * @param wsId    作業セッションID
   * @param request 更新する作業セッションのリクエスト
   */
  @Transactional
  public WorkSession update(int userId, int wsId, WorkSessionUpdateRequest request) {
    WorkSession current = get(userId, wsId);
    validateTaskIsNotFinished(userId, current.getTaskId());
    if (current.getType() != request.getType()) {
      throw new WorkSessionOperationNotAllowedException("workSession.type",
          "作業セッションの記録タイプは変更できません");
    }

    WorkSession workSession = toEntity(wsId, request);
    int updated = wsRepository.update(workSession, userId);
    if (updated == 0) {
      throw new TargetNotFoundException("workSession.id",
          "更新対象の作業セッションが見つかりませんでした");
    }
    return get(userId, wsId);
  }

  /**
   * IDを指定して作業セッションを削除します。
   *
   * @param userId 認証ユーザーのID
   * @param wsId   作業セッションID
   */
  @Transactional
  public void delete(int userId, int wsId) {
    int taskId = findTaskIdByWorkSessionId(userId, wsId);
    validateTaskIsNotFinished(userId, taskId);

    int deleted = wsRepository.deleteById(wsId, userId);
    if (deleted == 0) {
      throw new TargetNotFoundException("workSession.id",
          "削除対象の作業セッションが見つかりませんでした");
    }
  }

  private int findTaskIdByWorkSessionId(int userId, int wsId) {
    Integer taskId = wsRepository.findTaskIdById(wsId, userId);
    if (taskId == null) {
      throw new TargetNotFoundException("workSession.id",
          "指定したIDの作業セッションは見つかりませんでした");
    }
    return taskId;
  }

  private void validateTaskIsNotFinished(int userId, int taskId) {
    if (tsRepository.isFinished(taskId, userId)) {
      throw new WorkSessionOperationNotAllowedException("task.id",
          "完了済みタスクの作業セッションは追加・終了・更新・削除できません");
    }
  }

  private WorkSession toEntity(WorkSessionCreateRequest request) {
    return new WorkSession(request);
  }

  private WorkSession toEntity(int id, WorkSessionUpdateRequest request) {
    return new WorkSession(id, request);
  }

}
