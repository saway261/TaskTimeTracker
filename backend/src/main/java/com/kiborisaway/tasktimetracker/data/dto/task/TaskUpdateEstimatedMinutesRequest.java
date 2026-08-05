package com.kiborisaway.tasktimetracker.data.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "見積作業時間更新リクエスト")
@Getter
@Setter
public class TaskUpdateEstimatedMinutesRequest {

  @Schema(description = "見積作業時間(分)", example = "60")
  @NotNull
  @Positive
  private Integer estimatedMinutes;

}
