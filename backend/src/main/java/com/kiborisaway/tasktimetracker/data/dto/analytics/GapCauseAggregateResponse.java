package com.kiborisaway.tasktimetracker.data.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "原因カテゴリ別集計")
@Getter
@EqualsAndHashCode
public class GapCauseAggregateResponse {

  @Schema(description = "分析対象件数 付与率の分母", example = "27")
  private final int analyzedTaskCount;

  @Schema(description = "全グループの延べ件数の合計 1タスクが複数カテゴリを持つため分析対象件数を上回りうる",
      example = "41")
  private final int totalLinkCount;

  @Schema(description = "タスクの判定区分（超過・おおむね見積どおり・短縮）の3グループ")
  private final List<GapCauseGroupResponse> groups;

  public GapCauseAggregateResponse(
      int analyzedTaskCount, int totalLinkCount, List<GapCauseGroupResponse> groups) {
    this.analyzedTaskCount = analyzedTaskCount;
    this.totalLinkCount = totalLinkCount;
    this.groups = groups;
  }
}
