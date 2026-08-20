package com.kiborisaway.tasktimetracker.data.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "見積もり精度の集計")
@Getter
@EqualsAndHashCode
public class EstimationAccuracyResponse {

  @Schema(description = "判定区分しきい値（百分率） グラフの帯描画にも使う", example = "10.0")
  private final double onTimeThresholdPercent;

  @Schema(description = "分析対象件数", example = "27")
  private final int analyzedTaskCount;

  @Schema(description = "除外件数の内訳")
  private final ExcludedTaskCountResponse excluded;

  @Schema(description = "見積もり精度サマリー")
  private final AccuracySummaryResponse summary;

  @Schema(description = "4象限診断 分析対象10件未満の場合はnull")
  private final DiagnosisResponse diagnosis;

  @Schema(description = "散布図データ（B6で追加。それまでは空配列）")
  private final List<ScatterPointResponse> scatter;

  @Schema(description = "散布図が表示件数の上限で切り詰められているか（B6で追加）")
  private final boolean scatterTruncated;

  @Schema(description = "タスクサイズ帯別の集計（B6で追加。それまでは空配列）")
  private final List<SizeBucketResponse> sizeBuckets;

  @Schema(description = "精度推移（B7で追加。それまでは空配列）")
  private final List<AccuracyTrendPointResponse> trend;

  @Schema(description = "精度推移を表示できるか（20件以上、B7で追加）")
  private final MetricAvailabilityResponse trendAvailability;

  public EstimationAccuracyResponse(
      double onTimeThresholdPercent,
      int analyzedTaskCount,
      ExcludedTaskCountResponse excluded,
      AccuracySummaryResponse summary,
      DiagnosisResponse diagnosis,
      List<ScatterPointResponse> scatter,
      boolean scatterTruncated,
      List<SizeBucketResponse> sizeBuckets,
      List<AccuracyTrendPointResponse> trend,
      MetricAvailabilityResponse trendAvailability) {
    this.onTimeThresholdPercent = onTimeThresholdPercent;
    this.analyzedTaskCount = analyzedTaskCount;
    this.excluded = excluded;
    this.summary = summary;
    this.diagnosis = diagnosis;
    this.scatter = scatter;
    this.scatterTruncated = scatterTruncated;
    this.sizeBuckets = sizeBuckets;
    this.trend = trend;
    this.trendAvailability = trendAvailability;
  }
}
