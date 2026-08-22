package com.kiborisaway.tasktimetracker.data.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "タスク新規登録リクエスト")
@Getter
@Setter
public class TaskCreateRequest {

  @Schema(description = "タスク名", example = "docker-compose.yml作成")
  @NotBlank
  @Size(max = 20)
  private String title;

  @Schema(description = "タスクの説明　省略可", example = "")
  @Size(max = 200)
  private String description;

  @Schema(description = "見積作業時間(分)", example = "60")
  @NotNull
  @Positive
  private Integer estimatedMinutes;

  @Schema(description = "付与するタグのIDリスト 省略可。省略・空配列はタグなしとして扱う。件数の上限はない",
      example = "[1, 2]")
  private List<Integer> tagIds;

}
