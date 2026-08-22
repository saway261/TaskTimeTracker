package com.kiborisaway.tasktimetracker.data.dto.analytics;

import com.kiborisaway.tasktimetracker.data.dto.tag.TagSummaryResponse;
import com.kiborisaway.tasktimetracker.publicid.id.TaskId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "散布図の1点")
@Getter
@EqualsAndHashCode
public class ScatterPointResponse {

  @Schema(description = "タスクID", example = "Xr9mQ2vKp3")
  private final TaskId taskId;

  @Schema(description = "タスク名", example = "画面設計")
  private final String taskTitle;

  @Schema(description = "見積時間（分）", example = "60")
  private final int estimatedMinutes;

  @Schema(description = "実績時間（分）", example = "90")
  private final int actualMinutes;

  @Schema(description = "誤差率（百分率）", example = "50.0")
  private final double gapRate;

  @Schema(description = "判定区分", example = "LATE", allowableValues = {"LATE", "ON_TIME", "EARLY"})
  private final String outcome;

  @Schema(description = "付与されたタグ 名前の昇順。アーカイブ済みのタグも含む。未付与の場合は空配列")
  private final List<TagSummaryResponse> tags;

  public ScatterPointResponse(
      int taskId, String taskTitle, int estimatedMinutes, int actualMinutes, double gapRate,
      String outcome, List<TagSummaryResponse> tags) {
    this.taskId = new TaskId(taskId);
    this.taskTitle = taskTitle;
    this.estimatedMinutes = estimatedMinutes;
    this.actualMinutes = actualMinutes;
    this.gapRate = gapRate;
    this.outcome = outcome;
    this.tags = tags;
  }
}
