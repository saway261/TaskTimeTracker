package com.kiborisaway.tasktimetracker.data.dto.task;

import com.kiborisaway.tasktimetracker.data.dto.memo.MemoResponse;
import com.kiborisaway.tasktimetracker.data.dto.tag.TagSummaryResponse;
import com.kiborisaway.tasktimetracker.data.entity.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "タスク")
@Getter
@EqualsAndHashCode
public class TaskResponse {

  @Schema(description = "プロジェクトID", example = "1")
  private final Integer id;

  @Schema(description = "親となるプロジェクトID", example = "1")
  private final Integer projectId;//taskGroupIdを持つならここは持たない

  @Schema(description = "親となるタスクグループID", example = "1")
  private final Integer taskGroupId;

  @Schema(description = "タスク名", example = "docker-compose.yml作成")
  private final String title;

  @Schema(description = "タスクの説明", example = "省略")
  private final String description;

  @Schema(description = "見積作業時間(分)", example = "60")
  private final Integer estimatedMinutes;

  @Schema(description = "タスク作成日時", example = "2026-01-01 09:00:00")
  private final LocalDateTime createdAt;

  @Schema(description = "タスク完了日時", example = "2026-01-10 21:00:00")
  private final LocalDateTime finishedAt;

  @Schema(description = "実績作業時間(分)　WorkSessionを合計してキャッシュする", example = "70")
  private final Integer actualMinutesCached;

  @Schema(description = "(実績-見積)作業時間(分)　estimatedMinutes と actualMinutesCached から計算してキャッシュする", example = "10")
  private final Integer gapMinutesCached;

  @Schema(description = "(実績-見積)作業時間比率　estimatedMinutes と actualMinutesCached から計算してキャッシュする", example = "16.666")
  private final Double gapRateCached;

  @Schema(description = "メモリスト")
  private final List<MemoResponse> memos;

  @Schema(description = "付与されたタグ 名前の昇順。アーカイブ済みのタグも含む")
  private final List<TagSummaryResponse> tags;

  public TaskResponse(Task task, List<MemoResponse> memos, List<TagSummaryResponse> tags) {
    this.id = task.getId();
    this.projectId = task.getProjectId();
    this.taskGroupId = task.getTaskGroupId();
    this.title = task.getTitle();
    this.description = task.getDescription();
    this.estimatedMinutes = task.getEstimatedMinutes();
    this.createdAt = task.getCreatedAt();
    this.finishedAt = task.getFinishedAt();
    this.actualMinutesCached = task.getActualMinutesCached();
    this.gapMinutesCached = task.getGapMinutesCached();
    this.gapRateCached = task.getGapRateCached();
    this.memos = memos;
    this.tags = tags;
  }
}
