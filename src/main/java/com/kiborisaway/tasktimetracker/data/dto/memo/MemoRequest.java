package com.kiborisaway.tasktimetracker.data.dto.memo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "メモ作成更新リクエスト")
@Getter
@Setter
public class MemoRequest {

  @Schema(description = "メモコメント", example = "明日は早起きする")
  @NotBlank
  @Size(max = 1000)
  private String comment;

}
