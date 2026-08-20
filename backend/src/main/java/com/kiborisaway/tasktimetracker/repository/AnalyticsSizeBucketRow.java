package com.kiborisaway.tasktimetracker.repository;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * タスクサイズ帯別集計クエリの結果を受け取るフラット行。該当タスクが0件の帯はSQLの結果に現れないため、
 * 呼び出し側（サービス層）で全帯を埋める必要があります。
 */
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class AnalyticsSizeBucketRow {

  private String bucketCode;

  private Integer taskCount;

  private Double factorMedian;

  private Integer onTimeCount;
}
