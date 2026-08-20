package com.kiborisaway.tasktimetracker.data.entity;

import com.kiborisaway.tasktimetracker.data.dto.project.ProjectCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.project.ProjectUpdateRequest;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class Project {

  private Integer id;

  private Integer userId;

  private String title;

  private String description;

  private Boolean isFinished;

  public Project(int id, String title, String description, Boolean isFinished) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.isFinished = isFinished;
  }

  // 新規登録用
  public Project(ProjectCreateRequest request) {
    this.title = request.getTitle();
    this.description = request.getDescription();
  }

  // 更新用（名前・説明のみ。完了状態はupdateFinishedで別途更新する）
  public Project(int id, ProjectUpdateRequest request) {
    this.id = id;
    this.title = request.getTitle();
    this.description = request.getDescription();
  }

}
