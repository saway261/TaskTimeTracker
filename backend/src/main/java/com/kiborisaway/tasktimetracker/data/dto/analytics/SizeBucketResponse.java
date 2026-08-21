package com.kiborisaway.tasktimetracker.data.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "タスクサイズ帯別の集計（B6で実装。B4時点ではレスポンスの配列は常に空）")
@Getter
@EqualsAndHashCode
public class SizeBucketResponse {

  @Schema(description = "帯コード", example = "M15",
      allowableValues = {"M15", "M30", "M60", "M120", "OVER120"})
  private final String bucketCode;

  @Schema(description = "表示ラベル", example = "〜15分")
  private final String label;

  @Schema(description = "件数", example = "8")
  private final int taskCount;

  @Schema(description = "代表係数 3件未満の場合はnull", example = "1.4")
  private final Double factorMedian;

  @Schema(description = "オンタイム率 3件未満の場合はnull", example = "50.0")
  private final Double onTimeRate;

  public SizeBucketResponse(
      String bucketCode, String label, int taskCount, Double factorMedian, Double onTimeRate) {
    this.bucketCode = bucketCode;
    this.label = label;
    this.taskCount = taskCount;
    this.factorMedian = factorMedian;
    this.onTimeRate = onTimeRate;
  }
}
