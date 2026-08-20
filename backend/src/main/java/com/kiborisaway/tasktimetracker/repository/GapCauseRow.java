package com.kiborisaway.tasktimetracker.repository;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 原因カテゴリ別集計クエリの結果を受け取るフラット行。原因カテゴリが未設定（未分類）の場合、
 * {@code direction} / {@code causeCategoryCode} / {@code causeCategoryLabel} / {@code displayOrder}
 * はすべてnullになります。
 */
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class GapCauseRow {

  private String direction;

  private String causeCategoryCode;

  private String causeCategoryLabel;

  private Integer displayOrder;

  private Integer taskCount;

  private Double gapRateMedian;
}
