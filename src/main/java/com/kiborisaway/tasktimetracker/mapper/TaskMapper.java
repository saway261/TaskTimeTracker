package com.kiborisaway.tasktimetracker.mapper;

import com.kiborisaway.tasktimetracker.data.dto.task.TaskCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.task.TaskUpdatePropertyRequest;
import com.kiborisaway.tasktimetracker.data.entity.Task;

public class TaskMapper {

  private TaskMapper() {
  }

  public static Task toEntity(Integer projectId, Integer taskGroupId, TaskCreateRequest request) {
    return new Task(projectId, taskGroupId, request);
  }

  public static Task toEntity(int id, TaskUpdatePropertyRequest request) {
    return new Task(id, request);
  }
}
