package com.kiborisaway.tasktimetracker.data.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "原因カテゴリの方向 超過側か短縮側か、両方を示すか")
public enum CauseDirection {
  OVER,
  UNDER,
  BOTH
}
