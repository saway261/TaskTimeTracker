package com.kiborisaway.tasktimetracker.data.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "原因カテゴリの方向グループ（超過側・短縮側・共通）")
@Getter
@EqualsAndHashCode
public class GapCauseGroupResponse {

  @Schema(description = "方向", example = "OVER", allowableValues = {"OVER", "UNDER", "BOTH"})
  private final String direction;

  @Schema(description = "表示ラベル", example = "超過側")
  private final String label;

  @Schema(description = "グループの延べ件数（items の taskCount 合計）", example = "18")
  private final int totalCount;

  @Schema(description = "分析対象件数に対する付与率（百分率） 合計が100を超えうる", example = "66.7")
  private final double sharePercent;

  @Schema(description = "件数降順（未分類は常に末尾）で並んだカテゴリ別集計")
  private final List<GapCauseItemResponse> items;

  public GapCauseGroupResponse(
      String direction, String label, int totalCount, double sharePercent,
      List<GapCauseItemResponse> items) {
    this.direction = direction;
    this.label = label;
    this.totalCount = totalCount;
    this.sharePercent = sharePercent;
    this.items = items;
  }
}
