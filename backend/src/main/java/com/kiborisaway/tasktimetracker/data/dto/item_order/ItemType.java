package com.kiborisaway.tasktimetracker.data.dto.item_order;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "並び順項目の種別")
public enum ItemType {
  TASK,
  TASK_GROUP
}
