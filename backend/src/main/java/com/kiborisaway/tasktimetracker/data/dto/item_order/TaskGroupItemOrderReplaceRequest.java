package com.kiborisaway.tasktimetracker.data.dto.item_order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "タスクグループ直下の並び順置換リクエスト")
@Getter
@Setter
public class TaskGroupItemOrderReplaceRequest {

  @Schema(description = "並び替え後の順序どおりに並べた項目一覧。タスクグループ直下の現在の項目を過不足なく含む必要がある")
  @NotEmpty
  @Valid
  private List<TaskGroupItemOrderItemRequest> items;
}
