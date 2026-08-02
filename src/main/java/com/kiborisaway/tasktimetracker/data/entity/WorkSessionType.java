package com.kiborisaway.tasktimetracker.data.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "作業セッションの記録タイプ タイマーか手動かを選べます")
public enum WorkSessionType {
  TIMER,
  MANUAL
}
