package com.kiborisaway.tasktimetracker.data.dto.item_order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * プロジェクト直下の並び替えリクエストに含める項目1件分。
 *
 * <p>{@code id} が指すのはタスクの公開IDかタスクグループの公開IDかは {@code type} 次第で変わる
 * （公開IDは種別ごとに別のアルファベットを使うため、種別を跨いだ自動変換ができない）。
 * このためJacksonでは型付きIDへ自動変換せず、生の文字列のまま受け取り、
 * {@code type} が判明するService層（{@code ProjectItemOrderService}）で解決する。
 *
 * @param type 項目種別
 * @param id   タスクまたはタスクグループの公開ID
 */
@Schema(description = "プロジェクト直下の並び順項目指定")
public record ProjectItemOrderItemRequest(
    @Schema(description = "項目種別", example = "TASK")
    @NotNull ItemType type,
    @Schema(description = "タスクまたはタスクグループの公開ID", example = "Xr9mQ2vKp3")
    @NotNull String id) {

}
