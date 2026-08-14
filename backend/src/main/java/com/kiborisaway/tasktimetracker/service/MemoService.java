package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.dto.memo.MemoRequest;
import com.kiborisaway.tasktimetracker.data.entity.Memo;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.MemoRepository;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import com.kiborisaway.tasktimetracker.repository.TaskGroupRepository;
import com.kiborisaway.tasktimetracker.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemoService {

  private MemoRepository memoRepository;
  private ProjectRepository projectRepository;
  private TaskGroupRepository taskGroupRepository;
  private TaskRepository taskRepository;

  @Autowired
  public MemoService(
      MemoRepository memoRepository,
      ProjectRepository projectRepository,
      TaskGroupRepository taskGroupRepository,
      TaskRepository taskRepository) {
    this.memoRepository = memoRepository;
    this.projectRepository = projectRepository;
    this.taskGroupRepository = taskGroupRepository;
    this.taskRepository = taskRepository;
  }

  /**
   * プロジェクトに紐づくメモを新規登録します。
   *
   * @param userId    認証ユーザーのID
   * @param projectId プロジェクトID
   * @param request   メモ作成リクエスト
   * @return 登録したメモ
   */
  @Transactional
  public Memo registerInProject(int userId, int projectId, MemoRequest request) {
    if (!projectRepository.existsByIdAndUserId(projectId, userId)) {
      throw new TargetNotFoundException("project.id",
          "指定したIDのプロジェクトは見つかりませんでした");
    }
    Memo memo = new Memo(null, projectId, null, null, request.getComment());
    memoRepository.insert(memo);
    return findById(memo.getId());
  }

  /**
   * タスクグループに紐づくメモを新規登録します。
   *
   * @param userId      認証ユーザーのID
   * @param taskGroupId タスクグループID
   * @param request     メモ作成リクエスト
   * @return 登録したメモ
   */
  @Transactional
  public Memo registerInTaskGroup(int userId, int taskGroupId, MemoRequest request) {
    if (!taskGroupRepository.existsByIdAndUserId(taskGroupId, userId)) {
      throw new TargetNotFoundException("taskGroup.id",
          "指定したIDのタスクグループは見つかりませんでした");
    }
    Memo memo = new Memo(null, null, taskGroupId, null, request.getComment());
    memoRepository.insert(memo);
    return findById(memo.getId());
  }

  /**
   * タスクに紐づくメモを新規登録します。
   *
   * @param userId  認証ユーザーのID
   * @param taskId  タスクID
   * @param request メモ作成リクエスト
   * @return 登録したメモ
   */
  @Transactional
  public Memo registerInTask(int userId, int taskId, MemoRequest request) {
    if (!taskRepository.existsByIdAndUserId(taskId, userId)) {
      throw new TargetNotFoundException("task.id",
          "指定したIDのタスクは見つかりませんでした");
    }
    Memo memo = new Memo(null, null, null, taskId, request.getComment());
    memoRepository.insert(memo);
    return findById(memo.getId());
  }

  /**
   * メモコメントを更新します。
   *
   * @param userId  認証ユーザーのID
   * @param id      メモID
   * @param request メモ更新リクエスト
   */
  @Transactional
  public Memo update(int userId, int id, MemoRequest request) {
    Memo memo = new Memo(id, null, null, null, request.getComment());
    int updated = memoRepository.update(memo, userId);
    if (updated == 0) {
      throw new TargetNotFoundException("memo.id",
          "更新対象のメモが見つかりませんでした");
    }
    return findById(id);
  }

  /**
   * メモを削除します。
   *
   * @param userId 認証ユーザーのID
   * @param id     メモID
   */
  @Transactional
  public void delete(int userId, int id) {
    int deleted = memoRepository.delete(id, userId);
    if (deleted == 0) {
      throw new TargetNotFoundException("memo.id",
          "削除対象のメモが見つかりませんでした");
    }
  }

  private Memo findById(int id) {
    Memo memo = memoRepository.findById(id);
    if (memo == null) {
      throw new TargetNotFoundException("memo.id",
          "指定したIDのメモは見つかりませんでした");
    }
    return memo;
  }
}
