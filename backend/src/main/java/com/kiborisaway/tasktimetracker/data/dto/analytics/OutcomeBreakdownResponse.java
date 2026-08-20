package com.kiborisaway.tasktimetracker.data.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "判定区分ごとの内訳件数")
@Getter
@EqualsAndHashCode
public class OutcomeBreakdownResponse {

  @Schema(description = "超過の件数", example = "12")
  private final int lateCount;

  @Schema(description = "おおむね見積もりどおりの件数", example = "12")
  private final int onTimeCount;

  @Schema(description = "短縮の件数", example = "3")
  private final int earlyCount;

  public OutcomeBreakdownResponse(int lateCount, int onTimeCount, int earlyCount) {
    this.lateCount = lateCount;
    this.onTimeCount = onTimeCount;
    this.earlyCount = earlyCount;
  }
}
