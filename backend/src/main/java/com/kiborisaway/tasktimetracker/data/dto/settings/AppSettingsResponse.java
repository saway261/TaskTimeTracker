package com.kiborisaway.tasktimetracker.data.dto.settings;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "アプリ設定レスポンス")
@Getter
@EqualsAndHashCode
public class AppSettingsResponse {

  @Schema(description = "「おおむね見積もりどおり」と判定する誤差率の範囲（百分率）", example = "10")
  private final double onTimeThresholdPercent;

  public AppSettingsResponse(double onTimeThresholdPercent) {
    this.onTimeThresholdPercent = onTimeThresholdPercent;
  }
}
