package com.kiborisaway.tasktimetracker.data.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "原因カテゴリ別集計の1件")
@Getter
@EqualsAndHashCode
public class GapCauseItemResponse {

  @Schema(description = "原因カテゴリコード 未分類の場合はnull", example = "TASK_BREAKDOWN")
  private final String causeCategoryCode;

  @Schema(description = "原因カテゴリの表示ラベル 未分類の場合は「未分類」", example = "作業の洗い出しが足りなかった")
  private final String causeCategoryLabel;

  @Schema(description = "延べ件数（このカテゴリが付与された振り返りの件数）", example = "5")
  private final int taskCount;

  @Schema(description = "分析対象件数に対する付与率（百分率） 合計が100を超えうる", example = "18.5")
  private final double sharePercent;

  @Schema(description = "代表誤差率（中央値） 3件未満の場合はnull", example = "35.0")
  private final Double gapRateMedian;

  public GapCauseItemResponse(
      String causeCategoryCode, String causeCategoryLabel, int taskCount, double sharePercent,
      Double gapRateMedian) {
    this.causeCategoryCode = causeCategoryCode;
    this.causeCategoryLabel = causeCategoryLabel;
    this.taskCount = taskCount;
    this.sharePercent = sharePercent;
    this.gapRateMedian = gapRateMedian;
  }
}
