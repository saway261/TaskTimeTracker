package com.kiborisaway.tasktimetracker.data.dto.project;

import com.kiborisaway.tasktimetracker.data.dto.memo.MemoResponse;
import com.kiborisaway.tasktimetracker.data.entity.Project;
import com.kiborisaway.tasktimetracker.publicid.id.ProjectId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "プロジェクトレスポンス")
@Getter
@EqualsAndHashCode
public class ProjectResponse {

  @Schema(description = "プロジェクトID", example = "Xr9mQ2vKp3")
  private final ProjectId id;

  @Schema(description = "プロジェクト名", example = "タスク管理アプリ開発")
  private final String title;

  @Schema(description = "プロジェクトの説明", example = "自主開発")
  private final String description;

  @Schema(description = "完了フラグ", example = "false")
  private final Boolean isFinished;

  @Schema(description = "メモリスト")
  private final List<MemoResponse> memos;

  public ProjectResponse(Project project, List<MemoResponse> memos) {
    this.id = new ProjectId(project.getId());
    this.title = project.getTitle();
    this.description = project.getDescription();
    this.isFinished = project.getIsFinished();
    this.memos = memos;
  }
}
