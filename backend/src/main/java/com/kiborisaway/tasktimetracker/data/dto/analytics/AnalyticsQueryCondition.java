package com.kiborisaway.tasktimetracker.data.dto.analytics;

import com.kiborisaway.tasktimetracker.validation.ValidAnalyticsPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@ValidAnalyticsPeriod
@Schema(description = "分析クエリの絞り込み条件")
@Getter
@Setter
public class AnalyticsQueryCondition {

  @Schema(description = "絞り込み対象プロジェクトID 未指定で全プロジェクト横断", example = "1")
  @Positive
  private Integer projectId;

  @Schema(description = "完了日時の下限（この日時以降） 未指定で下限なし",
      example = "2026-01-01T00:00:00+09:00")
  private LocalDateTime from;

  @Schema(description = "完了日時の上限（この日時未満） 未指定で上限なし",
      example = "2026-02-01T00:00:00+09:00")
  private LocalDateTime to;
}
