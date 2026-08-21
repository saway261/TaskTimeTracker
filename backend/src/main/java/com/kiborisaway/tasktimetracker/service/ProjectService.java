package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.dto.project.ProjectCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.memo.MemoResponse;
import com.kiborisaway.tasktimetracker.data.dto.project.ProjectResponse;
import com.kiborisaway.tasktimetracker.data.dto.project.ProjectUpdateRequest;
import com.kiborisaway.tasktimetracker.data.entity.Memo;
import com.kiborisaway.tasktimetracker.data.entity.Project;
import com.kiborisaway.tasktimetracker.exception.ProjectFinishNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.MemoRepository;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import com.kiborisaway.tasktimetracker.repository.TaskRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

  private ProjectRepository repository;
  private MemoRepository memoRepository;
  private TaskRepository tsRepository;

  @Autowired
  public ProjectService(ProjectRepository repository, MemoRepository memoRepository,
      TaskRepository tsRepository) {
    this.repository = repository;
    this.memoRepository = memoRepository;
    this.tsRepository = tsRepository;
  }

  /**
   * 認証ユーザーが所有するプロジェクトの一覧検索を行います。完了フラグを指定した場合、指定した完了状態のプロジェクトのみを取得します。
   *
   * @param userId     認証ユーザーのID
   * @param isFinished 完了フラグ
   * @return 全件または指定した完了状態のプロジェクトの一覧
   */
  public List<ProjectResponse> findAllByCondition(int userId, Boolean isFinished) {
    List<Project> projects;
    if (isFinished == null) {
      projects = repository.findAllByUserId(userId);
    } else {
      projects = repository.findAllByIsFinishedAndUserId(isFinished, userId);
    }
    if (projects.isEmpty()) {
      return List.of();
    }

    List<Integer> projectIds = projects.stream().map(Project::getId).toList();
    Map<Integer, List<MemoResponse>> memosByProjectId =
        memoRepository.findAllInProjects(projectIds).stream()
            .collect(Collectors.groupingBy(
                Memo::getProjectId,
                Collectors.mapping(MemoResponse::new, Collectors.toList())));

    return projects.stream()
        .map(project -> new ProjectResponse(
            project,
            memosByProjectId.getOrDefault(project.getId(), List.of())))
        .toList();
  }

  /**
   * IDによるプロジェクトの検索
   *
   * @param userId 認証ユーザーのID
   * @param id     プロジェクトのID
   * @return プロジェクト
   */
  public ProjectResponse findById(int userId, int id) {
    return toResponse(findProjectById(userId, id));
  }

  /**
   * プロジェクトの新規登録を行います。
   *
   * @param userId  認証ユーザーのID
   * @param request 新規登録するプロジェクトのリクエスト
   */
  @Transactional
  public ProjectResponse register(int userId, ProjectCreateRequest request) {
    Project project = toEntity(request);
    project.setUserId(userId);
    repository.insert(project);
    Project registeredProject = findProjectById(userId, project.getId());
    return new ProjectResponse(registeredProject, List.of());
  }

  /**
   * プロジェクトのIDを指定してプロジェクト名と説明を更新します
   *
   * @param userId  認証ユーザーのID
   * @param id      更新するプロジェクトのID
   * @param request 更新するプロジェクトのリクエスト
   */
  @Transactional
  public ProjectResponse update(int userId, int id, ProjectUpdateRequest request) {
    Project project = toEntity(id, request);
    project.setUserId(userId);
    int updated = repository.update(project);
    if (updated == 0) {
      throw new TargetNotFoundException("project", "更新対象のプロジェクトが見つかりませんでした");
    }
    return toResponse(findProjectById(userId, id));
  }

  /**
   * プロジェクトIDを指定して完了状態を更新します。完了にする場合、未完了のタスクが1件でも存在すると更新できません。
   *
   * @param userId     認証ユーザーのID
   * @param id         更新するプロジェクトのID
   * @param isFinished 完了状態
   */
  @Transactional
  public ProjectResponse updateFinished(int userId, int id, boolean isFinished) {
    if (isFinished && tsRepository.existsUnfinishedInProject(id, userId)) {
      throw new ProjectFinishNotAllowedException("project.id",
          "未完了のタスクがあるプロジェクトは完了状態にできません");
    }

    int updated = repository.updateFinished(id, isFinished, userId);
    if (updated == 0) {
      throw new TargetNotFoundException("project.id",
          "完了状態更新対象のプロジェクトが見つかりませんでした");
    }
    return toResponse(findProjectById(userId, id));
  }

  private Project toEntity(ProjectCreateRequest request) {
    return new Project(request);
  }

  private Project toEntity(int id, ProjectUpdateRequest request) {
    return new Project(id, request);
  }

  private ProjectResponse toResponse(Project project) {
    List<Memo> memos = memoRepository.findAllInProject(project.getId());
    return new ProjectResponse(project,
        (memos == null ? List.<Memo>of() : memos).stream()
            .map(MemoResponse::new)
            .toList());
  }

  private Project findProjectById(int userId, int id) {
    Project project = repository.findByIdAndUserId(id, userId);
    if (project == null) {
      throw new TargetNotFoundException("project.id",
          "指定したIDのプロジェクトは見つかりませんでした");
    }
    return project;
  }
}
