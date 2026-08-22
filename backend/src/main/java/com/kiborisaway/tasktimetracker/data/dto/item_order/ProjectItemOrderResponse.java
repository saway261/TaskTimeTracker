package com.kiborisaway.tasktimetracker.data.dto.item_order;

import com.kiborisaway.tasktimetracker.data.entity.ProjectItemOrder;
import com.kiborisaway.tasktimetracker.publicid.PublicIdCodec;
import com.kiborisaway.tasktimetracker.publicid.PublicIdType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "プロジェクト直下の並び順項目")
@Getter
@EqualsAndHashCode
public class ProjectItemOrderResponse {

  @Schema(description = "項目種別", example = "TASK")
  private final ItemType type;

  @Schema(description = "タスクまたはタスクグループの公開ID", example = "Xr9mQ2vKp3")
  private final String id;

  @Schema(description = "プロジェクト内の並び順（0始まり。削除により欠番が発生することがある）", example = "1")
  private final Integer position;

  /**
   * {@code id} が指す公開IDの種別（タスクかタスクグループか）は {@code type} によって変わるため、
   * 単純なフィールド型の付け替えではなく、この場でどちらのアルファベットでエンコードするか
   * を判定する必要がある。そのため codec を直接受け取る。
   */
  public ProjectItemOrderResponse(ProjectItemOrder order, PublicIdCodec codec) {
    this.type = order.getTaskId() != null ? ItemType.TASK : ItemType.TASK_GROUP;
    this.id = order.getTaskId() != null
        ? codec.encode(PublicIdType.TASK, order.getTaskId())
        : codec.encode(PublicIdType.TASK_GROUP, order.getTaskGroupId());
    this.position = order.getPosition();
  }
}
