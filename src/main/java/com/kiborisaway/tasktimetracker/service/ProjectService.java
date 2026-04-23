package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.Project;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {

  private ProjectRepository repository;

  @Autowired
  public ProjectService(ProjectRepository repository) {
    this.repository = repository;
  }

  /**
   * プロジェクトの一覧検索を行います。
   *
   * @return プロジェクトの一覧
   */
  public List<Project> searchProjectList() {
    return repository.searchProjectList();
  }

  /**
   * IDによるプロジェクトの
   *
   * @param id プロジェクトのID
   * @return プロジェクト
   */
  public Project searchProjectById(int id) {
    Project project = repository.searchProjectById(id);
    if (project == null) {
      throw new TargetNotFoundException("project.id",
          "指定したIDのプロジェクトは見つかりませんでした");
    }
    return project;
  }

  /**
   * プロジェクトの新規登録を行います。
   *
   * @param project 新規登録するプロジェクト
   */
  public Project registerProject(Project project) {
    repository.registerProject(project);
    return project;
  }

  /**
   * プロジェクトのIDを指定してプロジェクト名と説明を更新します
   *
   * @param project 更新するプロジェクト
   */
  public void updateProject(Project project) {
    int updated = repository.updateProject(project);
    if (updated == 0) {
      throw new TargetNotFoundException("project", "更新対象のプロジェクトが見つかりませんでした");
    }
  }
}
