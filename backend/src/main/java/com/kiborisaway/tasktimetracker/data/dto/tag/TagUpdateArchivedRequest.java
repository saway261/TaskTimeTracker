package com.kiborisaway.tasktimetracker.data.dto.tag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * updateArchived に isArchived だけをリクエストボディとして渡すためのrecord
 *
 * @param isArchived trueの場合アーカイブする / falseの場合はアーカイブを解除する
 */
@Schema(description = "タグのアーカイブ状態更新リクエスト")
public record TagUpdateArchivedRequest(
    @Schema(description = "アーカイブ済みフラグ。trueでアーカイブ、falseで解除する", example = "true")
    @NotNull Boolean isArchived) {

}
