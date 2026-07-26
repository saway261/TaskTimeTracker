package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.TaskGroup;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import com.kiborisaway.tasktimetracker.repository.TaskGroupRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskGroupService {

  private TaskGroupRepository tgRepository;
  private ProjectRepository prRepository;

  @Autowired
  public TaskGroupService(TaskGroupRepository tgRepository, ProjectRepository prRepository) {
    this.tgRepository = tgRepository;
    this.prRepository = prRepository;
  }

  /**
   * プロジェクトIDに紐づくタスクグループの一覧検索を行います。完了フラグを指定した場合、指定した完了状態のタスクグループのみを取得します。
   *
   * @param pId        プロジェクトID
   * @param isFinished 完了フラグ
   * @return 全件または指定した完了状態のタスクグループの一覧
   */
  public List<TaskGroup> findAllByCondition(int pId, Boolean isFinished) {
    if (!prRepository.existsById(pId)) {
      throw new TargetNotFoundException("project.id",
          "指定したIDのプロジェクトは見つかりませんでした");
    }

    if (isFinished == null) {
      return tgRepository.findAllInProject(pId);
    }
    return tgRepository.findAllInProjectByIsFinished(pId, isFinished);
  }

  /**
   * IDによるタスクグループの検索
   *
   * @param id タスクグループのID
   * @return タスクグループ
   */
  public TaskGroup findById(int id) {
    TaskGroup project = tgRepository.findById(id);
    if (project == null) {
      throw new TargetNotFoundException("taskGroup.id",
          "指定したIDのタスクグループは見つかりませんでした");
    }
    return project;
  }

  /**
   * タスクグループの新規登録を行います。
   *
   * @param tg 新規登録するタスクグループ
   */
  public TaskGroup register(int pId, TaskGroup tg) {
    if (!prRepository.existsById(pId)) {
      throw new TargetNotFoundException("project.id",
          "指定したIDのプロジェクトは見つかりませんでした");
    }
    tg.setProjectId(pId);
    tgRepository.insert(tg);
    return tg;
  }

  /**
   * タスクグループIDを指定してタスクグループ名と説明と完了フラグを更新します
   *
   * @param tg 更新するタスクグループ
   */
  public void update(TaskGroup tg) {
    int updated = tgRepository.update(tg);
    if (updated == 0) {
      throw new TargetNotFoundException("taskGroup",
          "更新対象のタスクグループが見つかりませんでした");
    }
  }

}
