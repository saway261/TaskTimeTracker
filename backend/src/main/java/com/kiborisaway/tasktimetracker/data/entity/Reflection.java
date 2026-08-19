package com.kiborisaway.tasktimetracker.data.entity;

import java.time.LocalDateTime;
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
public class Reflection {

  private Integer id;

  private Integer taskId;

  private String cause;

  private String nextAction;

  private Integer causeCategoryId;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}
