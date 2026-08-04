package com.kiborisaway.tasktimetracker.data.entity;

import com.kiborisaway.tasktimetracker.data.dto.task.TaskCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.task.TaskUpdatePropertyRequest;
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
public class Task {

  private Integer id;

  private Integer projectId;//taskGroupIdを持つならここは持たない

  private Integer taskGroupId;

  private String title;

  private String description;

  private Integer estimatedMinutes;

  private LocalDateTime createdAt;

  private LocalDateTime finishedAt;

  private Integer actualMinutesCached;

  private Integer gapMinutesCached;

  private Double gapRateCached;

  /**
   * タスク新規登録用。projectId・taskGroupIdはパスパラメータ由来のためDTOに含まれません。
   *
   * @param projectId   プロジェクトID
   * @param taskGroupId タスクグループID
   * @param request     タスク新規登録リクエスト
   */
  public Task(Integer projectId, Integer taskGroupId, TaskCreateRequest request) {
    this.projectId = projectId;
    this.taskGroupId = taskGroupId;
    this.title = request.getTitle();
    this.description = request.getDescription();
    this.estimatedMinutes = request.getEstimatedMinutes();
  }

  /**
   * タスク名・説明更新用。
   *
   * @param id      タスクID
   * @param request タスク名・説明更新リクエスト
   */
  public Task(int id, TaskUpdatePropertyRequest request) {
    this.id = id;
    this.title = request.getTitle();
    this.description = request.getDescription();
  }
}
