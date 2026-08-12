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
public class ProjectItemOrder {

  private Integer id;

  private Integer projectId;

  private Integer taskId;//taskGroupIdを持つならここは持たない

  private Integer taskGroupId;

  private Integer position;
}
