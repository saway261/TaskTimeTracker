package com.kiborisaway.tasktimetracker.validation;

import com.kiborisaway.tasktimetracker.data.dto.task.TaskUpdateParentRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UpdateParentRequestValidator implements
    ConstraintValidator<ValidUpdateParentRequest, TaskUpdateParentRequest> {

  @Override
  public boolean isValid(TaskUpdateParentRequest value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    boolean hasProjectId = value.projectId() != null;
    boolean hasTaskGroupId = value.taskGroupId() != null;
    return hasProjectId != hasTaskGroupId;
  }
}
