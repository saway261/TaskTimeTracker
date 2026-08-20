package com.kiborisaway.tasktimetracker.validation;

import com.kiborisaway.tasktimetracker.data.dto.analytics.AnalyticsQueryCondition;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AnalyticsPeriodValidator implements
    ConstraintValidator<ValidAnalyticsPeriod, AnalyticsQueryCondition> {

  @Override
  public boolean isValid(AnalyticsQueryCondition value, ConstraintValidatorContext context) {
    if (value == null || value.getFrom() == null || value.getTo() == null) {
      return true;
    }
    return !value.getFrom().isAfter(value.getTo());
  }
}
