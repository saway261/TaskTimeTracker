package com.kiborisaway.tasktimetracker.data.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "プロジェクト新規登録リクエスト")
@Getter
@Setter
public class ProjectCreateRequest {

  @Schema(description = "プロジェクト名", example = "タスク管理アプリ開発")
  @NotBlank
  @Size(max = 20)
  private String title;

  @Schema(description = "プロジェクトの説明　省略可", example = "自主開発")
  @Size(max = 200)
  private String description;

}
