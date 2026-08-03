package com.kiborisaway.tasktimetracker.data.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "プロジェクト更新リクエスト")
@Getter
@Setter
public class ProjectUpdateRequest {

  @Schema(description = "プロジェクト名", example = "タスク管理アプリ開発")
  @NotBlank
  @Size(max = 20)
  private String title;

  @Schema(description = "プロジェクトの説明　省略可", example = "自主開発")
  @Size(max = 200)
  private String description;

  @Schema(description = "完了フラグ", example = "false")
  @NotNull
  private Boolean isFinished;

}
