package com.kiborisaway.tasktimetracker.data.dto.reflection;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.UniqueElements;

@Schema(description = "振り返り登録・更新リクエスト")
@Getter
@Setter
public class ReflectionRequest {

  @Schema(description = "原因カテゴリコード 1件以上3件以下、重複不可",
      example = "[\"TASK_BREAKDOWN\"]")
  @NotNull
  @Size(min = 1, max = 3)
  @UniqueElements
  private List<@NotBlank @Size(max = 40) String> causeCategoryCodes;

  @Schema(description = "見積もりと実績に差が生じた原因 任意。ただし選択した原因カテゴリによっては必須",
      example = "着手前の調査が不足していた")
  @Size(max = 200)
  private String cause;

  @Schema(description = "次回に向けた改善アクション", example = "類似タスクの実績を見積もり前に確認する")
  @Size(max = 1000)
  private String nextAction;
}
