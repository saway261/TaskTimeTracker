package com.kiborisaway.tasktimetracker.repository;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code ActiveTimerResponse} のMyBatis結果マッピング用。
 *
 * <p>公開ID化により {@code ActiveTimerResponse} のID項目は型付きIDになったが、MyBatisの自動結果
 * マッピングは素の {@code Integer} 列にしか対応できない（{@code ProjectId} 等へ変換する
 * TypeHandlerを持たない）ため、SQL結果はいったんこの素朴なRowで受け、
 * {@code WorkSessionService} で {@code ActiveTimerResponse} へ変換する。
 */
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class ActiveTimerRow {

  private Integer sessionId;

  private Integer taskId;

  private String taskTitle;

  private Integer projectId;

  private Integer taskGroupId;

  private LocalDateTime startedAt;
}
