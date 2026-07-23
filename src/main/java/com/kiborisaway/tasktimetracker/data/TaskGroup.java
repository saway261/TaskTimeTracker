package com.kiborisaway.tasktimetracker.data;

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
public class TaskGroup {

  private Integer id;
  private Integer projectId;
  private String title;
  private String description;
  private Boolean isFinished;

}
