package com.kiborisaway.tasktimetracker.data.dto.reflection;

import com.kiborisaway.tasktimetracker.data.entity.ReflectionCauseCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "振り返りレスポンス内の原因カテゴリ要素")
@Getter
@EqualsAndHashCode
public class ReflectionCauseCategorySummaryResponse {

  @Schema(description = "原因カテゴリコード", example = "TASK_BREAKDOWN")
  private final String code;

  @Schema(description = "表示ラベル", example = "作業の洗い出しが足りなかった")
  private final String label;

  public ReflectionCauseCategorySummaryResponse(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public ReflectionCauseCategorySummaryResponse(ReflectionCauseCategory category) {
    this(category.getCode(), category.getLabel());
  }
}
