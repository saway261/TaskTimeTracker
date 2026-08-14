package com.kiborisaway.tasktimetracker.data.dto.reflection;

import com.kiborisaway.tasktimetracker.data.entity.Reflection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "振り返りレスポンス")
@Getter
@EqualsAndHashCode
public class ReflectionResponse {

  @Schema(description = "振り返りID", example = "1")
  private final int id;

  @Schema(description = "対象タスクID", example = "1")
  private final int taskId;

  @Schema(description = "見積もりと実績に差が生じた原因", example = "着手前の調査が不足していた")
  private final String cause;

  @Schema(description = "次回に向けた改善アクション", example = "類似タスクの実績を見積もり前に確認する")
  private final String nextAction;

  @Schema(description = "登録日時", example = "2026-08-10T10:05:00+09:00")
  private final LocalDateTime createdAt;

  @Schema(description = "更新日時", example = "2026-08-10T10:05:00+09:00")
  private final LocalDateTime updatedAt;

  public ReflectionResponse(Reflection reflection) {
    this.id = reflection.getId();
    this.taskId = reflection.getTaskId();
    this.cause = reflection.getCause();
    this.nextAction = reflection.getNextAction();
    this.createdAt = reflection.getCreatedAt();
    this.updatedAt = reflection.getUpdatedAt();
  }
}
