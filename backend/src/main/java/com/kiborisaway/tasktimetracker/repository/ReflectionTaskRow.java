package com.kiborisaway.tasktimetracker.repository;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 振り返り一覧取得クエリの結果を受け取るフラット行。
 */
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class ReflectionTaskRow {

  private Integer taskId;

  private String taskTitle;

  private LocalDateTime finishedAt;

  private Integer actualMinutesCached;

  private Integer gapMinutesCached;

  private Double gapRateCached;

  private Integer taskGroupId;

  private String taskGroupTitle;

  private Integer reflectionId;

  private String cause;

  private String nextAction;

  private LocalDateTime reflectionCreatedAt;

  private LocalDateTime reflectionUpdatedAt;
}
