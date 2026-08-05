package com.kiborisaway.tasktimetracker.data.dto.task_group;

import com.kiborisaway.tasktimetracker.data.dto.memo.MemoResponse;
import com.kiborisaway.tasktimetracker.data.entity.TaskGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "タスクグループ")
@Getter
@EqualsAndHashCode
public class TaskGroupResponse {

  @Schema(description = "プロジェクトID", example = "1")
  private final Integer id;

  @Schema(description = "親となるプロジェクトID", example = "1")
  private final Integer projectId;

  @Schema(description = "タスクグループ名", example = "環境構築")
  private final String title;

  @Schema(description = "タスクグループの説明", example = "Dockerを使う")
  private final String description;

  @Schema(description = "完了フラグ", example = "false")
  private final Boolean isFinished;

  @Schema(description = "メモリスト")
  private final List<MemoResponse> memos;

  public TaskGroupResponse(TaskGroup tg, List<MemoResponse> memos) {
    this.id = tg.getId();
    this.projectId = tg.getProjectId();
    this.title = tg.getTitle();
    this.description = tg.getDescription();
    this.isFinished = tg.getIsFinished();
    this.memos = memos;
  }
}
