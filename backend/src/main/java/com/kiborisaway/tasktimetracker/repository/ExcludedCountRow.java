package com.kiborisaway.tasktimetracker.repository;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 除外理由別の件数と、絞り込み条件に一致する完了タスクの総数を受け取るフラット行。
 */
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class ExcludedCountRow {

  private Integer missingGapRate;

  private Integer missingActualMinutes;

  private Integer finishedCount;
}
