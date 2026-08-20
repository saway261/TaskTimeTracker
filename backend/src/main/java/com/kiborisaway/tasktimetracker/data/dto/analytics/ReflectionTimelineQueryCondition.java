package com.kiborisaway.tasktimetracker.data.dto.analytics;

import com.kiborisaway.tasktimetracker.validation.ValidAnalyticsPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@ValidAnalyticsPeriod
@Schema(description = "振り返りタイムラインの絞り込み条件")
@Getter
@Setter
public class ReflectionTimelineQueryCondition implements AnalyticsPeriod {

  @Schema(description = "絞り込み対象プロジェクトID 未指定で全プロジェクト横断", example = "1")
  @Positive
  private Integer projectId;

  @Schema(description = "完了日時の下限（この日時以降） 未指定で下限なし",
      example = "2026-01-01T00:00:00+09:00")
  private LocalDateTime from;

  @Schema(description = "完了日時の上限（この日時未満） 未指定で上限なし",
      example = "2026-02-01T00:00:00+09:00")
  private LocalDateTime to;

  @Schema(description = "原因カテゴリコードによる絞り込み 未指定で絞り込まない", example = "TASK_BREAKDOWN")
  @Size(max = 40)
  private String causeCategory;

  @Schema(description = "判定区分による絞り込み", example = "ALL")
  private ReflectionOutcomeFilter outcome = ReflectionOutcomeFilter.ALL;

  @Schema(description = "ページ番号（0始まり）", example = "0")
  @PositiveOrZero
  private int page = 0;

  @Schema(description = "1ページの件数", example = "20")
  @Min(1)
  @Max(100)
  private int size = 20;
}
