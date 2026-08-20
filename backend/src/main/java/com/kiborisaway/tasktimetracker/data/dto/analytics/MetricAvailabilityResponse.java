package com.kiborisaway.tasktimetracker.data.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "指標を表示するための最小件数を満たしているかどうか")
@Getter
@EqualsAndHashCode
public class MetricAvailabilityResponse {

  @Schema(description = "最小件数を満たしているか", example = "true")
  private final boolean available;

  @Schema(description = "表示に必要な件数", example = "5")
  private final int requiredCount;

  @Schema(description = "現在の分析対象件数", example = "7")
  private final int currentCount;

  public MetricAvailabilityResponse(boolean available, int requiredCount, int currentCount) {
    this.available = available;
    this.requiredCount = requiredCount;
    this.currentCount = currentCount;
  }
}
