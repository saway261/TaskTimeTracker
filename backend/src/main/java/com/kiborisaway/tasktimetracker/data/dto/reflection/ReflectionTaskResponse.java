package com.kiborisaway.tasktimetracker.data.dto.reflection;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "振り返り一覧内の完了タスク")
@Getter
@EqualsAndHashCode
public class ReflectionTaskResponse {

  @Schema(description = "タスクID", example = "1")
  private final int id;

  @Schema(description = "タスク名", example = "API設計")
  private final String title;

  @Schema(description = "タスク完了日時", example = "2026-08-10T10:00:00+09:00")
  private final LocalDateTime finishedAt;

  @Schema(description = "実績作業時間（分）", example = "90", nullable = true)
  private final Integer actualMinutesCached;

  @Schema(description = "実績と見積もりの差（分）", example = "30", nullable = true)
  private final Integer gapMinutesCached;

  @Schema(description = "実績と見積もりの差率（%）", example = "50.0", nullable = true)
  private final Double gapRateCached;

  @Schema(description = "登録済みの振り返り。未入力の場合はnull", nullable = true)
  private final ReflectionResponse reflection;

  public ReflectionTaskResponse(
      int id,
      String title,
      LocalDateTime finishedAt,
      Integer actualMinutesCached,
      Integer gapMinutesCached,
      Double gapRateCached,
      ReflectionResponse reflection) {
    this.id = id;
    this.title = title;
    this.finishedAt = finishedAt;
    this.actualMinutesCached = actualMinutesCached;
    this.gapMinutesCached = gapMinutesCached;
    this.gapRateCached = gapRateCached;
    this.reflection = reflection;
  }
}
