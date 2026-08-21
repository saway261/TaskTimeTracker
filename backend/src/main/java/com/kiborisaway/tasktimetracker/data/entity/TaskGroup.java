package com.kiborisaway.tasktimetracker.data.entity;

import com.kiborisaway.tasktimetracker.data.dto.task_group.TaskGroupCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.task_group.TaskGroupUpdateRequest;
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

  // 新規登録用
  public TaskGroup(TaskGroupCreateRequest request) {
    this.title = request.getTitle();
    this.description = request.getDescription();
  }

  // 更新用（名前・説明のみ。完了状態はupdateFinishedで別途更新する）
  public TaskGroup(int id, TaskGroupUpdateRequest request) {
    this.id = id;
    this.title = request.getTitle();
    this.description = request.getDescription();
  }

}
