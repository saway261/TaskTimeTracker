package com.kiborisaway.tasktimetracker.repository;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 散布図データ取得クエリの結果を受け取るフラット行。
 */
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class AnalyticsScatterPointRow {

  private Integer taskId;

  private String taskTitle;

  private Integer estimatedMinutes;

  private Integer actualMinutes;

  private Double gapRate;

  private String outcome;
}
