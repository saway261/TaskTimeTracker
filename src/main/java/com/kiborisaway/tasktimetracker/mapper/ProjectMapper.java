package com.kiborisaway.tasktimetracker.mapper;

import com.kiborisaway.tasktimetracker.data.dto.project.ProjectCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.project.ProjectUpdateRequest;
import com.kiborisaway.tasktimetracker.data.entity.Project;

public class ProjectMapper {

  private ProjectMapper() {
  }

  public static Project toEntity(ProjectCreateRequest request) {
    return new Project(request);
  }

  public static Project toEntity(int id, ProjectUpdateRequest request) {
    return new Project(id, request);
  }
}
