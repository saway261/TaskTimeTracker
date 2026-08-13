package com.kiborisaway.tasktimetracker.data.entity;

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
public class TaskGroupItemOrder {

  private Integer id;

  private Integer taskGroupId;

  private Integer taskId;

  private Integer position;
}
