package com.kiborisaway.tasktimetracker.data.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "振り返りタイムライン（ページング済み）")
@Getter
@EqualsAndHashCode
public class ReflectionTimelineResponse {

  @Schema(description = "本ページの振り返り一覧 完了日時降順")
  private final List<ReflectionTimelineItemResponse> items;

  @Schema(description = "ページ番号（0始まり）", example = "0")
  private final int page;

  @Schema(description = "1ページの件数", example = "20")
  private final int size;

  @Schema(description = "絞り込み条件に一致する総件数", example = "42")
  private final int totalCount;

  @Schema(description = "次のページが存在するか", example = "true")
  private final boolean hasNext;

  public ReflectionTimelineResponse(
      List<ReflectionTimelineItemResponse> items, int page, int size, int totalCount,
      boolean hasNext) {
    this.items = items;
    this.page = page;
    this.size = size;
    this.totalCount = totalCount;
    this.hasNext = hasNext;
  }
}
