package com.kiborisaway.tasktimetracker.data.dto.item_order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * プロジェクト直下の並び替えリクエストに含める項目1件分。
 *
 * @param type 項目種別
 * @param id   タスクまたはタスクグループのID
 */
@Schema(description = "プロジェクト直下の並び順項目指定")
public record ProjectItemOrderItemRequest(
    @Schema(description = "項目種別", example = "TASK")
    @NotNull ItemType type,
    @Schema(description = "タスクまたはタスクグループのID", example = "3")
    @NotNull @Positive Integer id) {

}
