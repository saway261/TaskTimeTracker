package com.kiborisaway.tasktimetracker.data.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "散布図の1点（B6で実装。B4時点ではレスポンスの配列は常に空）")
@Getter
@EqualsAndHashCode
public class ScatterPointResponse {

  @Schema(description = "タスクID", example = "1")
  private final int taskId;

  @Schema(description = "タスク名", example = "画面設計")
  private final String taskTitle;

  @Schema(description = "見積時間（分）", example = "60")
  private final int estimatedMinutes;

  @Schema(description = "実績時間（分）", example = "90")
  private final int actualMinutes;

  @Schema(description = "誤差率（百分率）", example = "50.0")
  private final double gapRate;

  @Schema(description = "判定区分", example = "LATE", allowableValues = {"LATE", "ON_TIME", "EARLY"})
  private final String outcome;

  public ScatterPointResponse(
      int taskId, String taskTitle, int estimatedMinutes, int actualMinutes, double gapRate,
      String outcome) {
    this.taskId = taskId;
    this.taskTitle = taskTitle;
    this.estimatedMinutes = estimatedMinutes;
    this.actualMinutes = actualMinutes;
    this.gapRate = gapRate;
    this.outcome = outcome;
  }
}
