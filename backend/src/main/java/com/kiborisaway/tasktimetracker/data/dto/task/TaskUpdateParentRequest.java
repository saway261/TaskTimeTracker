package com.kiborisaway.tasktimetracker.data.dto.task;

import com.kiborisaway.tasktimetracker.publicid.id.ProjectId;
import com.kiborisaway.tasktimetracker.publicid.id.TaskGroupId;
import com.kiborisaway.tasktimetracker.validation.ValidUpdateParentRequest;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * updateParent に移動先の親IDをリクエストボディとして渡すためのrecord
 *
 * @param projectId   移動先プロジェクトID
 * @param taskGroupId 移動先タスクグループID
 */
@Schema(description = "タスク所属変更リクエスト。projectId と taskGroupId のどちらか片方のみ指定すること")
@ValidUpdateParentRequest
public record TaskUpdateParentRequest(
    @Schema(description = "移動先プロジェクトID", example = "Xr9mQ2vKp3")
    ProjectId projectId,
    @Schema(description = "移動先タスクグループID", example = "null")
    TaskGroupId taskGroupId) {

}
