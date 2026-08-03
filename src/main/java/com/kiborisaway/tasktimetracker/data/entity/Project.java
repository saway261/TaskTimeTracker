package com.kiborisaway.tasktimetracker.data.entity;

import com.kiborisaway.tasktimetracker.data.dto.project.ProjectCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.project.ProjectUpdateRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "プロジェクト")
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class Project {

  @Schema(
      description = """
          プロジェクトID
          登録時は自動採番を行うため不要、更新時にはパスパラメータで指定するため不要なので、リクエストボディとしては常に不要
          """,
      example = "1")
  private Integer id;

  @Schema(description = "プロジェクト名", example = "タスク管理アプリ開発")
  private String title;

  @Schema(description = "プロジェクトの説明　省略可", example = "自主開発")
  private String description;

  @Schema(description = "完了フラグ", example = "false")
  private Boolean isFinished;

  // 新規登録用
  public Project(ProjectCreateRequest request) {
    this.title = request.getTitle();
    this.description = request.getDescription();
  }

  // 更新用
  public Project(int id, ProjectUpdateRequest request) {
    this.id = id;
    this.title = request.getTitle();
    this.description = request.getDescription();
    this.isFinished = request.getIsFinished();
  }

}
