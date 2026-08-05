package com.kiborisaway.tasktimetracker.data.dto.task;

import com.kiborisaway.tasktimetracker.validation.ValidUpdateParentRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/**
 * updateParent に移動先の親IDをリクエストボディとして渡すためのrecord
 *
 * @param projectId   移動先プロジェクトID
 * @param taskGroupId 移動先タスクグループID
 */
@Schema(description = "タスク所属変更リクエスト。projectId と taskGroupId のどちらか片方のみ指定すること")
@ValidUpdateParentRequest
public record TaskUpdateParentRequest(
    @Schema(description = "移動先プロジェクトID", example = "1")
    @Positive Integer projectId,
    @Schema(description = "移動先タスクグループID", example = "null")
    @Positive Integer taskGroupId) {

}
