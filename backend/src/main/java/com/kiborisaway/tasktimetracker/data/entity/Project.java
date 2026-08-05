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

  private String title;

  private String description;

  private Boolean isFinished;

  // 新規登録用
  public Project(ProjectCreateRequest request) {
    this.title = request.getTitle();
    this.description = request.getDescription();
  }

  // 更新用
  public Project(int id, ProjectUpdateRequest request) {
    this.id = id;
    this.title = request.getTitle();
    this.description = request.getDescription();
    this.isFinished = request.getIsFinished();
  }

}
