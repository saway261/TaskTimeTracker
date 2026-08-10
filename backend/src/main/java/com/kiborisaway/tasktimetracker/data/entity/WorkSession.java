package com.kiborisaway.tasktimetracker.data.entity;

import com.kiborisaway.tasktimetracker.data.dto.work_session.WorkSessionCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.work_session.WorkSessionUpdateRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class WorkSession {

  @Schema(
      description = """
          タスクID
          登録時は自動採番を行うため不要、更新時にはパスパラメータで指定するため不要なので、リクエストボディとしては常に不要
          """,
      example = "1")
  private Integer id;

  @Schema(
      description = """
          作業セッションが紐づくタスクID
          登録時はパスパラメータで指定し、更新不可なので、リクエストボディとしては常に不要
          """,
      example = "1")
  private Integer taskId;

  @Schema(description = """
      作業セッションの実績作業時間 終了状態のセッションにおいてはtypeがTIMERでもMANUALでも値を持つ
      """, example = "15")
  private Integer minutes;

  @Schema(description = "作業セッション開始日時 typeがTIMERなら必ず値を持つ", example = "2026-01-01T09:00:00+09:00")
  private LocalDateTime startedAt;

  @Schema(description = "作業セッション終了日時 typeがTIMERで、作業中ならnullとなる", example = "2026-01-01T09:15:00+09:00")
  private LocalDateTime endedAt;

  @Schema(description = "作業セッション登録日時 DBにINSERTされる際に現在時刻がセットされる", example = "2026-01-01T09:00:00+09:00")
  private LocalDateTime createdAt;

  @Schema(description = "作業セッション更新日時 DBがUPDATEされる度に現在時刻がセットされる", example = "2026-01-01T09:00:00+09:00")
  private LocalDateTime updatedAt;

  @Schema(description = "作業セッションの記録タイプ", example = "TIMER")
  private WorkSessionType type;

  // 新規登録用
  public WorkSession(WorkSessionCreateRequest request) {
    this.type = request.getType();
    this.minutes = request.getMinutes();
    this.startedAt = request.getStartedAt();
  }

  // 更新用
  public WorkSession(int id, WorkSessionUpdateRequest request) {
    this.id = id;
    this.type = request.getType();
    this.minutes = request.getMinutes();
    this.startedAt = request.getStartedAt();
    this.endedAt = request.getEndedAt();
  }

}
