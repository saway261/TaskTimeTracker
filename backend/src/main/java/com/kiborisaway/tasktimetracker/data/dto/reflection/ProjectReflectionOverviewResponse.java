package com.kiborisaway.tasktimetracker.data.dto.reflection;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "プロジェクトの振り返り対象一覧")
@Getter
@EqualsAndHashCode
public class ProjectReflectionOverviewResponse {

  @Schema(description = "プロジェクトID", example = "1")
  private final int projectId;

  @Schema(description = "プロジェクト名", example = "タスク管理アプリ開発")
  private final String projectTitle;

  @Schema(description = "プロジェクト直下の完了タスク")
  private final List<ReflectionTaskResponse> tasks;

  @Schema(description = "完了タスクを含むタスクグループ")
  private final List<ReflectionTaskGroupResponse> taskGroups;

  public ProjectReflectionOverviewResponse(
      int projectId,
      String projectTitle,
      List<ReflectionTaskResponse> tasks,
      List<ReflectionTaskGroupResponse> taskGroups) {
    this.projectId = projectId;
    this.projectTitle = projectTitle;
    this.tasks = tasks;
    this.taskGroups = taskGroups;
  }
}
