package com.kiborisaway.tasktimetracker.data.dto.task_group;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "タスクグループ更新リクエスト")
@Getter
@Setter
public class TaskGroupUpdateRequest {

  @Schema(description = "タスクグループ名", example = "環境構築")
  @NotBlank
  @Size(max = 20)
  private String title;

  @Schema(description = "タスクグループの説明　省略可", example = "Dockerを使う")
  @Size(max = 200)
  private String description;

}
