package com.kiborisaway.tasktimetracker.validation;

import com.kiborisaway.tasktimetracker.controller.TaskController.UpdateParentRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UpdateParentRequestValidator implements
    ConstraintValidator<ValidUpdateParentRequest, UpdateParentRequest> {

  @Override
  public boolean isValid(UpdateParentRequest value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    boolean hasProjectId = value.projectId() != null;
    boolean hasTaskGroupId = value.taskGroupId() != null;
    return hasProjectId != hasTaskGroupId;
  }
}
