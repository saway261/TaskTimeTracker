package com.kiborisaway.tasktimetracker.data.dto.item_order;

import com.kiborisaway.tasktimetracker.publicid.id.TaskId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * タスクグループ直下の並び替えリクエストに含める項目1件分。
 *
 * @param id タスクID
 */
@Schema(description = "タスクグループ直下の並び順項目指定")
public record TaskGroupItemOrderItemRequest(
    @Schema(description = "タスクID", example = "Xr9mQ2vKp3")
    @NotNull TaskId id) {

}
