package com.kiborisaway.tasktimetracker.data.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "分析対象タスクのプロジェクト別件数 タグ絞り込み時の交絡（プロジェクトへの偏り）を"
    + "確認するための材料。件数のみでプロジェクトごとの統計量は算出しない")
@Getter
@EqualsAndHashCode
public class ProjectBreakdownItemResponse {

  @Schema(description = "プロジェクトID", example = "1")
  private final int projectId;

  @Schema(description = "プロジェクト名", example = "タスク管理アプリ開発")
  private final String projectTitle;

  @Schema(description = "分析対象件数", example = "18")
  private final int count;

  public ProjectBreakdownItemResponse(int projectId, String projectTitle, int count) {
    this.projectId = projectId;
    this.projectTitle = projectTitle;
    this.count = count;
  }
}
