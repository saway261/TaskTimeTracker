package com.kiborisaway.tasktimetracker.data.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "見積もり精度サマリー")
@Getter
@EqualsAndHashCode
public class AccuracySummaryResponse {

  @Schema(description = "サマリー指標を表示できるか（5件以上）")
  private final MetricAvailabilityResponse availability;

  @Schema(description = "判定区分の内訳件数 件数はavailabilityに関係なく常に返す")
  private final OutcomeBreakdownResponse outcomeBreakdown;

  @Schema(description = "オンタイム率（0〜100の百分率） 表示不可の場合はnull", example = "44.0")
  private final Double onTimeRate;

  @Schema(description = "代表係数（見積もり係数の中央値） 表示不可の場合はnull", example = "1.6")
  private final Double factorMedian;

  @Schema(description = "係数の第1四分位 表示不可の場合はnull", example = "1.2")
  private final Double factorP25;

  @Schema(description = "係数の第3四分位 表示不可の場合はnull", example = "2.1")
  private final Double factorP75;

  @Schema(description = "誤差率ばらつき（MdAPE、百分率） 表示不可の場合はnull", example = "28.0")
  private final Double variancePercent;

  @Schema(description = "直近の傾向 表示不可またはウィンドウが揃わない場合はnull",
      example = "IMPROVED")
  private final String recentTrend;

  @Schema(description = "直近10件のばらつき 表示不可の場合はnull", example = "20.0")
  private final Double recentVariancePercent;

  @Schema(description = "その前10件のばらつき 表示不可の場合はnull", example = "30.0")
  private final Double previousVariancePercent;

  public AccuracySummaryResponse(
      MetricAvailabilityResponse availability,
      OutcomeBreakdownResponse outcomeBreakdown,
      Double onTimeRate,
      Double factorMedian,
      Double factorP25,
      Double factorP75,
      Double variancePercent,
      String recentTrend,
      Double recentVariancePercent,
      Double previousVariancePercent) {
    this.availability = availability;
    this.outcomeBreakdown = outcomeBreakdown;
    this.onTimeRate = onTimeRate;
    this.factorMedian = factorMedian;
    this.factorP25 = factorP25;
    this.factorP75 = factorP75;
    this.variancePercent = variancePercent;
    this.recentTrend = recentTrend;
    this.recentVariancePercent = recentVariancePercent;
    this.previousVariancePercent = previousVariancePercent;
  }
}
