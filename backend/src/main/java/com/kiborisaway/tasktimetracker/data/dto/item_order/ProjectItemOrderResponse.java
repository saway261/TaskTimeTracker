package com.kiborisaway.tasktimetracker.data.dto.item_order;

import com.kiborisaway.tasktimetracker.data.entity.ProjectItemOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "プロジェクト直下の並び順項目")
@Getter
@EqualsAndHashCode
public class ProjectItemOrderResponse {

  @Schema(description = "項目種別", example = "TASK")
  private final ItemType type;

  @Schema(description = "タスクまたはタスクグループのID", example = "3")
  private final Integer id;

  @Schema(description = "プロジェクト内の並び順（0始まり。削除により欠番が発生することがある）", example = "1")
  private final Integer position;

  public ProjectItemOrderResponse(ProjectItemOrder order) {
    this.type = order.getTaskId() != null ? ItemType.TASK : ItemType.TASK_GROUP;
    this.id = order.getTaskId() != null ? order.getTaskId() : order.getTaskGroupId();
    this.position = order.getPosition();
  }
}
