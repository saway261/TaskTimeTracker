package com.kiborisaway.tasktimetracker.repository;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 精度推移（移動中央値）クエリの結果を受け取るフラット行。
 */
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class AnalyticsTrendRow {

  private Integer sequence;

  private LocalDateTime finishedAt;

  private LocalDateTime windowFrom;

  private Double factorMedian;

  private Double variancePercent;
}
