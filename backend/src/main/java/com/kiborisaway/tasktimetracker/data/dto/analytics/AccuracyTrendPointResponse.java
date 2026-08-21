package com.kiborisaway.tasktimetracker.data.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "精度推移の1点、移動中央値")
@Getter
@EqualsAndHashCode
public class AccuracyTrendPointResponse {

  @Schema(description = "完了順の連番（窓の右端）", example = "10")
  private final int sequence;

  @Schema(description = "窓の右端タスクの完了日時", example = "2026-08-10T10:00:00+09:00")
  private final LocalDateTime finishedAt;

  @Schema(description = "窓の左端タスクの完了日時", example = "2026-08-01T10:00:00+09:00")
  private final LocalDateTime windowFrom;

  @Schema(description = "窓内の代表係数の中央値", example = "1.5")
  private final double factorMedian;

  @Schema(description = "窓内のばらつき（MdAPE）", example = "22.0")
  private final double variancePercent;

  public AccuracyTrendPointResponse(
      int sequence, LocalDateTime finishedAt, LocalDateTime windowFrom, double factorMedian,
      double variancePercent) {
    this.sequence = sequence;
    this.finishedAt = finishedAt;
    this.windowFrom = windowFrom;
    this.factorMedian = factorMedian;
    this.variancePercent = variancePercent;
  }
}
