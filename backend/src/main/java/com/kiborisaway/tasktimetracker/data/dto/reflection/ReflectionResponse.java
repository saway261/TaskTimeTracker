package com.kiborisaway.tasktimetracker.data.dto.reflection;

import com.kiborisaway.tasktimetracker.data.entity.Reflection;
import com.kiborisaway.tasktimetracker.data.entity.ReflectionCauseCategory;
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

  @Schema(description = "原因カテゴリコード カテゴリ未設定の場合はnull", example = "TASK_BREAKDOWN")
  private final String causeCategoryCode;

  @Schema(description = "原因カテゴリの表示ラベル カテゴリ未設定の場合はnull", example = "作業の洗い出しが足りなかった")
  private final String causeCategoryLabel;

  @Schema(description = "見積もりと実績に差が生じた原因", example = "着手前の調査が不足していた")
  private final String cause;

  @Schema(description = "次回に向けた改善アクション", example = "類似タスクの実績を見積もり前に確認する")
  private final String nextAction;

  @Schema(description = "登録日時", example = "2026-08-10T10:05:00+09:00")
  private final LocalDateTime createdAt;

  @Schema(description = "更新日時", example = "2026-08-10T10:05:00+09:00")
  private final LocalDateTime updatedAt;

  /**
   * カテゴリ未設定のReflection用。機能追加前に登録された行など、causeCategoryIdがnullの場合に使います。
   */
  public ReflectionResponse(Reflection reflection) {
    this(reflection, null);
  }

  public ReflectionResponse(Reflection reflection, ReflectionCauseCategory category) {
    this(
        reflection.getId(),
        reflection.getTaskId(),
        category == null ? null : category.getCode(),
        category == null ? null : category.getLabel(),
        reflection.getCause(),
        reflection.getNextAction(),
        reflection.getCreatedAt(),
        reflection.getUpdatedAt());
  }

  public ReflectionResponse(
      int id,
      int taskId,
      String causeCategoryCode,
      String causeCategoryLabel,
      String cause,
      String nextAction,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.taskId = taskId;
    this.causeCategoryCode = causeCategoryCode;
    this.causeCategoryLabel = causeCategoryLabel;
    this.cause = cause;
    this.nextAction = nextAction;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}
