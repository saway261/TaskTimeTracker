package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.dto.project.ProjectCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.memo.MemoResponse;
import com.kiborisaway.tasktimetracker.data.dto.project.ProjectResponse;
import com.kiborisaway.tasktimetracker.data.dto.project.ProjectUpdateRequest;
import com.kiborisaway.tasktimetracker.data.entity.Memo;
import com.kiborisaway.tasktimetracker.data.entity.Project;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.MemoRepository;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

  private ProjectRepository repository;
  private MemoRepository memoRepository;

  @Autowired
  public ProjectService(ProjectRepository repository, MemoRepository memoRepository) {
    this.repository = repository;
    this.memoRepository = memoRepository;
  }

  /**
   * プロジェクトの一覧検索を行います。完了フラグを指定した場合、指定した完了状態のプロジェクトのみを取得します。
   *
   * @param isFinished 完了フラグ
   * @return 全件または指定した完了状態のプロジェクトの一覧
   */
  public List<ProjectResponse> findAllByCondition(Boolean isFinished) {
    List<Project> projects;
    if (isFinished == null) {
      projects = repository.findAll();
    } else {
      projects = repository.findAllByIsFinished(isFinished);
    }
    return projects.stream().map(this::toResponse).toList();
  }

  /**
   * IDによるプロジェクトの
   *
   * @param id プロジェクトのID
   * @return プロジェクト
   */
  public ProjectResponse findById(int id) {
    Project project = repository.findById(id);
    if (project == null) {
      throw new TargetNotFoundException("project.id",
          "指定したIDのプロジェクトは見つかりませんでした");
    }
    return toResponse(project);
  }

  /**
   * プロジェクトの新規登録を行います。
   *
   * @param request 新規登録するプロジェクトのリクエスト
   */
  @Transactional
  public ProjectResponse register(ProjectCreateRequest request) {
    Project project = toEntity(request);
    repository.insert(project);
    return toResponse(project);
  }

  /**
   * プロジェクトのIDを指定してプロジェクト名と説明を更新します
   *
   * @param id      更新するプロジェクトのID
   * @param request 更新するプロジェクトのリクエスト
   */
  @Transactional
  public void update(int id, ProjectUpdateRequest request) {
    Project project = toEntity(id, request);
    int updated = repository.update(project);
    if (updated == 0) {
      throw new TargetNotFoundException("project", "更新対象のプロジェクトが見つかりませんでした");
    }
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
}
