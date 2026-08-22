package com.kiborisaway.tasktimetracker.data.dto.analytics;

import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionCauseCategorySummaryResponse;
import com.kiborisaway.tasktimetracker.data.dto.tag.TagSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "振り返りタイムラインの1件")
@Getter
@EqualsAndHashCode
public class ReflectionTimelineItemResponse {

  @Schema(description = "タスクID", example = "1")
  private final int taskId;

  @Schema(description = "タスク名", example = "画面設計")
  private final String taskTitle;

  @Schema(description = "プロジェクトID", example = "1")
  private final int projectId;

  @Schema(description = "プロジェクト名", example = "タスク管理アプリ開発")
  private final String projectTitle;

  @Schema(description = "完了日時", example = "2026-08-10T10:00:00+09:00")
  private final LocalDateTime finishedAt;

  @Schema(description = "見積時間（分）", example = "60")
  private final int estimatedMinutes;

  @Schema(description = "実績時間（分） 過去データ不整合時などはnull", example = "90")
  private final Integer actualMinutes;

  @Schema(description = "誤差（分） 過去データ不整合時などはnull", example = "30")
  private final Integer gapMinutes;

  @Schema(description = "誤差率（百分率） 算出できない場合はnull", example = "50.0")
  private final Double gapRate;

  @Schema(description = "判定区分 誤差率を算出できない場合はnull",
      example = "LATE", allowableValues = {"LATE", "ON_TIME", "EARLY"})
  private final String outcome;

  @Schema(description = "原因カテゴリ 表示順に並ぶ。未設定の場合は空配列")
  private final List<ReflectionCauseCategorySummaryResponse> causeCategories;

  @Schema(description = "見積もりと実績に差が生じた原因 任意項目のためnullの場合がある",
      example = "着手前の調査が不足していた")
  private final String cause;

  @Schema(description = "次回に向けた改善アクション", example = "類似タスクの実績を見積もり前に確認する")
  private final String nextAction;

  @Schema(description = "付与されたタグ 名前の昇順。アーカイブ済みのタグも含む。未付与の場合は空配列")
  private final List<TagSummaryResponse> tags;

  public ReflectionTimelineItemResponse(
      int taskId,
      String taskTitle,
      int projectId,
      String projectTitle,
      LocalDateTime finishedAt,
      int estimatedMinutes,
      Integer actualMinutes,
      Integer gapMinutes,
      Double gapRate,
      String outcome,
      List<ReflectionCauseCategorySummaryResponse> causeCategories,
      String cause,
      String nextAction,
      List<TagSummaryResponse> tags) {
    this.taskId = taskId;
    this.taskTitle = taskTitle;
    this.projectId = projectId;
    this.projectTitle = projectTitle;
    this.finishedAt = finishedAt;
    this.estimatedMinutes = estimatedMinutes;
    this.actualMinutes = actualMinutes;
    this.gapMinutes = gapMinutes;
    this.gapRate = gapRate;
    this.outcome = outcome;
    this.causeCategories = causeCategories;
    this.cause = cause;
    this.nextAction = nextAction;
    this.tags = tags;
  }
}
