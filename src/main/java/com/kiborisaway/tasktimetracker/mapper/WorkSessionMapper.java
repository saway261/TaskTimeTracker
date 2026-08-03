package com.kiborisaway.tasktimetracker.mapper;

import com.kiborisaway.tasktimetracker.data.dto.work_session.WorkSessionCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.work_session.WorkSessionUpdateRequest;
import com.kiborisaway.tasktimetracker.data.entity.WorkSession;

public class WorkSessionMapper {

  private WorkSessionMapper() {
  }

  public static WorkSession toEntity(WorkSessionCreateRequest request) {
    return new WorkSession(request);
  }

  public static WorkSession toEntity(int id, WorkSessionUpdateRequest request) {
    return new WorkSession(id, request);
  }
}
