package com.kiborisaway.tasktimetracker.data.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * updateFinished に isFinished だけをリクエストボディとして渡すためのrecord
 *
 * @param isFinished trueの場合完了状態にする / falseの場合は作業中状態にする
 */
@Schema(description = "タスク完了状態更新リクエスト")
public record TaskUpdateFinishedRequest(
    @Schema(description = "完了フラグ。trueの場合完了状態、falseの場合は作業中状態にする", example = "true")
    @NotNull Boolean isFinished) {

}
