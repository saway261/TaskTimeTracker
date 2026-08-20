package com.kiborisaway.tasktimetracker.repository;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 振り返りタイムライン取得クエリの結果を受け取るフラット行。
 */
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class ReflectionTimelineRow {

  private Integer taskId;

  private String taskTitle;

  private Integer projectId;

  private String projectTitle;

  private LocalDateTime finishedAt;

  private Integer estimatedMinutes;

  private Integer actualMinutes;

  private Integer gapMinutes;

  private Double gapRate;

  private String outcome;

  private Integer reflectionId;

  private String cause;

  private String nextAction;
}
