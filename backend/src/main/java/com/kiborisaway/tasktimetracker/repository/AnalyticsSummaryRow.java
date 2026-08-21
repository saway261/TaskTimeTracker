package com.kiborisaway.tasktimetracker.repository;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * サマリー集計クエリ（件数・区分内訳・係数の中央値と四分位・MdAPE）の結果を受け取るフラット行。
 */
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class AnalyticsSummaryRow {

  private Integer analyzedCount;

  private Integer lateCount;

  private Integer earlyCount;

  private Integer onTimeCount;

  private Double factorMedian;

  private Double factorP25;

  private Double factorP75;

  private Double variancePercent;
}
