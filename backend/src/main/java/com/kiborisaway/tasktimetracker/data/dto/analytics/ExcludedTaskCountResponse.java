package com.kiborisaway.tasktimetracker.data.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "分析対象から除外した完了タスクの件数")
@Getter
@EqualsAndHashCode
public class ExcludedTaskCountResponse {

  @Schema(description = "除外件数の合計", example = "3")
  private final int total;

  @Schema(description = "誤差率を算出できず除外した件数", example = "1")
  private final int missingGapRate;

  @Schema(description = "実績時間が記録されておらず除外した件数", example = "2")
  private final int missingActualMinutes;

  public ExcludedTaskCountResponse(int total, int missingGapRate, int missingActualMinutes) {
    this.total = total;
    this.missingGapRate = missingGapRate;
    this.missingActualMinutes = missingActualMinutes;
  }
}
