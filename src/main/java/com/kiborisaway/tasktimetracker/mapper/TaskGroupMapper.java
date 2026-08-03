package com.kiborisaway.tasktimetracker.mapper;

import com.kiborisaway.tasktimetracker.data.dto.task_group.TaskGroupCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.task_group.TaskGroupUpdateRequest;
import com.kiborisaway.tasktimetracker.data.entity.TaskGroup;

public class TaskGroupMapper {

  private TaskGroupMapper() {
  }

  public static TaskGroup toEntity(TaskGroupCreateRequest request) {
    return new TaskGroup(request);
  }

  public static TaskGroup toEntity(int id, TaskGroupUpdateRequest request) {
    return new TaskGroup(id, request);
  }
}
