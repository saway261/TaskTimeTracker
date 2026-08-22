package com.kiborisaway.tasktimetracker.data.dto.work_session;

import com.kiborisaway.tasktimetracker.publicid.id.ProjectId;
import com.kiborisaway.tasktimetracker.publicid.id.TaskGroupId;
import com.kiborisaway.tasktimetracker.publicid.id.TaskId;
import com.kiborisaway.tasktimetracker.publicid.id.WorkSessionId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "ログインユーザーの稼働中タイマー")
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class ActiveTimerResponse {

  @Schema(description = "作業セッションID", example = "Xr9mQ2vKp3")
  private WorkSessionId sessionId;

  @Schema(description = "タスクID", example = "Xr9mQ2vKp3")
  private TaskId taskId;

  @Schema(description = "タスク名", example = "タイマー一覧を実装する")
  private String taskTitle;

  @Schema(description = "タスクが属するプロジェクトID", example = "Xr9mQ2vKp3")
  private ProjectId projectId;

  @Schema(description = "親タスクグループID。プロジェクト直下のタスクではnull", example = "Xr9mQ2vKp3", nullable = true)
  private TaskGroupId taskGroupId;

  @Schema(description = "タイマー開始日時", example = "2026-08-18T09:00:00+09:00")
  private LocalDateTime startedAt;
}
