package com.kiborisaway.tasktimetracker.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AnalyticsPeriodValidator.class)
public @interface ValidAnalyticsPeriod {

  String message() default "fromはto以前の日時を指定してください";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
