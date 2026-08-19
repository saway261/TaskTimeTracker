package com.kiborisaway.tasktimetracker.data.dto.work_session;

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

  @Schema(description = "作業セッションID", example = "1")
  private Integer sessionId;

  @Schema(description = "タスクID", example = "1")
  private Integer taskId;

  @Schema(description = "タスク名", example = "タイマー一覧を実装する")
  private String taskTitle;

  @Schema(description = "タスクが属するプロジェクトID", example = "1")
  private Integer projectId;

  @Schema(description = "親タスクグループID。プロジェクト直下のタスクではnull", example = "1")
  private Integer taskGroupId;

  @Schema(description = "タイマー開始日時", example = "2026-08-18T09:00:00+09:00")
  private LocalDateTime startedAt;
}
