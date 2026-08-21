package com.kiborisaway.tasktimetracker.data.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "偏りとばらつきの2軸に基づく4象限診断 分析対象10件未満の場合はレスポンス自体がnull")
@Getter
@EqualsAndHashCode
public class DiagnosisResponse {

  @Schema(description = "診断コード",
      example = "GOOD",
      allowableValues = {"GOOD", "UNSTABLE", "BIASED_LATE", "BIASED_EARLY", "UNSTABLE_BIASED"})
  private final String code;

  @Schema(description = "偏りの方向", example = "NONE", allowableValues = {"LATE", "EARLY", "NONE"})
  private final String biasDirection;

  @Schema(description = "診断の見出し", example = "精度良好")
  private final String title;

  @Schema(description = "診断の説明文", example = "見積もりは信頼できる水準です。現在の見積もり方を維持しましょう。")
  private final String message;

  public DiagnosisResponse(String code, String biasDirection, String title, String message) {
    this.code = code;
    this.biasDirection = biasDirection;
    this.title = title;
    this.message = message;
  }
}
