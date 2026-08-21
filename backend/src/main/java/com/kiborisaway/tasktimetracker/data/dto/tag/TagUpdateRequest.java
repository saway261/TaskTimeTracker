package com.kiborisaway.tasktimetracker.data.dto.tag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "タグ名変更リクエスト")
@Getter
@Setter
public class TagUpdateRequest {

  @Schema(description = "新しいタグ名 前後の空白はトリムして保存する", example = "リサーチ")
  @NotBlank
  @Size(max = 20)
  private String name;

}
