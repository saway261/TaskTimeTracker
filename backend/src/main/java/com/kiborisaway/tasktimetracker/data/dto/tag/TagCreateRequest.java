package com.kiborisaway.tasktimetracker.data.dto.tag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "タグ新規登録リクエスト")
@Getter
@Setter
public class TagCreateRequest {

  @Schema(description = "タグ名 前後の空白はトリムして保存する。既存タグと正規化後に一致する場合は新規作成せず既存タグを返す",
      example = "調査")
  @NotBlank
  @Size(max = 20)
  private String name;

}
