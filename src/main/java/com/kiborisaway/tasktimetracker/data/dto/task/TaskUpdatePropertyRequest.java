package com.kiborisaway.tasktimetracker.data.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "タスク名・説明更新リクエスト")
@Getter
@Setter
public class TaskUpdatePropertyRequest {

  @Schema(description = "タスク名", example = "docker-compose.yml作成")
  @NotBlank
  @Size(max = 20)
  private String title;

  @Schema(description = "タスクの説明　省略可", example = "")
  @Size(max = 200)
  private String description;

}
