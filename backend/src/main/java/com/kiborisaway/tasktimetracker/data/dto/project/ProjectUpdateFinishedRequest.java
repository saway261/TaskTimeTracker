package com.kiborisaway.tasktimetracker.data.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * updateFinished に isFinished だけをリクエストボディとして渡すためのrecord
 *
 * @param isFinished trueの場合完了状態にする / falseの場合は未完了状態にする
 */
@Schema(description = "プロジェクト完了状態更新リクエスト")
public record ProjectUpdateFinishedRequest(
    @Schema(description = "完了フラグ。trueの場合完了状態、falseの場合は未完了状態にする", example = "true")
    @NotNull Boolean isFinished) {

}
