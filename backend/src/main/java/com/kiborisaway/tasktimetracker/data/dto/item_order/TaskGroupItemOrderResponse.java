package com.kiborisaway.tasktimetracker.data.dto.item_order;

import com.kiborisaway.tasktimetracker.data.entity.TaskGroupItemOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "タスクグループ直下の並び順項目")
@Getter
@EqualsAndHashCode
public class TaskGroupItemOrderResponse {

  @Schema(description = "タスクID", example = "3")
  private final Integer id;

  @Schema(description = "タスクグループ内の並び順（0始まり。削除により欠番が発生することがある）", example = "1")
  private final Integer position;

  public TaskGroupItemOrderResponse(TaskGroupItemOrder order) {
    this.id = order.getTaskId();
    this.position = order.getPosition();
  }
}
