package com.kiborisaway.tasktimetracker.data.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "振り返りの原因カテゴリ マスタデータ")
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class ReflectionCauseCategory {

  @Schema(description = "原因カテゴリID", example = "1")
  private Integer id;

  @Schema(description = "原因カテゴリコード", example = "TASK_BREAKDOWN")
  private String code;

  @Schema(description = "表示ラベル", example = "作業の洗い出しが足りなかった")
  private String label;

  @Schema(description = "方向 超過側・短縮側・共通のいずれか", example = "OVER")
  private CauseDirection direction;

  @Schema(description = "次のアクションのヒント 省略可", example = "着手前に手順を書き出す")
  private String nextActionHint;

  @Schema(description = "このカテゴリを選んだ場合に原因の自由記述を必須とするか", example = "false")
  private Boolean requiresCause;

  @Schema(description = "表示順", example = "10")
  private Integer displayOrder;

  @Schema(description = "有効フラグ 無効なカテゴリは新規選択肢に出さない", example = "true")
  private Boolean isActive;
}
