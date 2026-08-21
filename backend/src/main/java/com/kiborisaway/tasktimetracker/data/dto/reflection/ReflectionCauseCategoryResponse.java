package com.kiborisaway.tasktimetracker.data.dto.reflection;

import com.kiborisaway.tasktimetracker.data.entity.CauseDirection;
import com.kiborisaway.tasktimetracker.data.entity.ReflectionCauseCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "原因カテゴリレスポンス")
@Getter
@EqualsAndHashCode
public class ReflectionCauseCategoryResponse {

  @Schema(description = "原因カテゴリコード", example = "TASK_BREAKDOWN")
  private final String code;

  @Schema(description = "表示ラベル", example = "作業の洗い出しが足りなかった")
  private final String label;

  @Schema(description = "方向 超過側・短縮側・共通のいずれか", example = "OVER")
  private final CauseDirection direction;

  @Schema(description = "次のアクションのヒント 省略可", example = "着手前に手順を書き出す")
  private final String nextActionHint;

  @Schema(description = "このカテゴリを選んだ場合に原因の自由記述を必須とするか", example = "false")
  private final boolean requiresCause;

  public ReflectionCauseCategoryResponse(ReflectionCauseCategory category) {
    this.code = category.getCode();
    this.label = category.getLabel();
    this.direction = category.getDirection();
    this.nextActionHint = category.getNextActionHint();
    this.requiresCause = Boolean.TRUE.equals(category.getRequiresCause());
  }
}
