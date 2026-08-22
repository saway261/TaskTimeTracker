package com.kiborisaway.tasktimetracker.repository;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * プロジェクト分布集計クエリの結果を受け取るフラット行。
 */
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class ProjectBreakdownRow {

  private Integer projectId;

  private String projectTitle;

  private Integer count;
}
