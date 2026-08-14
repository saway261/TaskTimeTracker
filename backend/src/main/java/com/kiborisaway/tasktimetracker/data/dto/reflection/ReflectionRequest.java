package com.kiborisaway.tasktimetracker.data.dto.reflection;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "振り返り登録・更新リクエスト")
@Getter
@Setter
public class ReflectionRequest {

  @Schema(description = "見積もりと実績に差が生じた原因", example = "着手前の調査が不足していた")
  @NotBlank
  @Size(max = 200)
  private String cause;

  @Schema(description = "次回に向けた改善アクション", example = "類似タスクの実績を見積もり前に確認する")
  @Size(max = 1000)
  private String nextAction;
}
