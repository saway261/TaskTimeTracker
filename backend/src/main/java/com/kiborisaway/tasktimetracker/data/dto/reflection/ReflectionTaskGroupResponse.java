package com.kiborisaway.tasktimetracker.data.dto.reflection;

import com.kiborisaway.tasktimetracker.publicid.id.TaskGroupId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "振り返り一覧内のタスクグループ")
@Getter
@EqualsAndHashCode
public class ReflectionTaskGroupResponse {

  @Schema(description = "タスクグループID", example = "Xr9mQ2vKp3")
  private final TaskGroupId id;

  @Schema(description = "タスクグループ名", example = "バックエンド開発")
  private final String title;

  @Schema(description = "タスクグループ配下の完了タスク")
  private final List<ReflectionTaskResponse> tasks;

  public ReflectionTaskGroupResponse(
      int id, String title, List<ReflectionTaskResponse> tasks) {
    this.id = new TaskGroupId(id);
    this.title = title;
    this.tasks = tasks;
  }
}
