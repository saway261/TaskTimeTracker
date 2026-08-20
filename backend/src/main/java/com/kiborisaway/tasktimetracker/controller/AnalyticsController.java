package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.dto.analytics.AnalyticsQueryCondition;
import com.kiborisaway.tasktimetracker.data.dto.analytics.EstimationAccuracyResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.ReflectionTimelineQueryCondition;
import com.kiborisaway.tasktimetracker.data.dto.analytics.ReflectionTimelineResponse;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorResponse;
import com.kiborisaway.tasktimetracker.security.AuthenticatedUser;
import com.kiborisaway.tasktimetracker.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

  private AnalyticsService service;

  @Autowired
  public AnalyticsController(AnalyticsService service) {
    this.service = service;
  }

  @Operation(
      summary = "見積もり精度集計取得",
      description = "認証ユーザーの完了タスクから見積もり精度のサマリー・診断を集計して返します。"
          + "projectId未指定で全プロジェクト横断となります。",
      responses = {
          @ApiResponse(responseCode = "200", description = "取得成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = EstimationAccuracyResponse.class))),
          @ApiResponse(responseCode = "400", description = "fromがtoより後、または入力値が不正なときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))),
          @ApiResponse(responseCode = "404", description = "指定されたプロジェクトが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class)))
      }
  )
  @GetMapping("/estimation-accuracy")
  public EstimationAccuracyResponse getEstimationAccuracy(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @ModelAttribute AnalyticsQueryCondition condition) {
    return service.getEstimationAccuracy(user.getUserId(), condition);
  }

  @Operation(
      summary = "振り返りタイムライン取得",
      description = "認証ユーザーの振り返り済み完了タスクを完了日時降順でページング取得します。"
          + "実績時間が記録されていない完了タスクも含みます（除外ルールを適用しません）。",
      responses = {
          @ApiResponse(responseCode = "200", description = "取得成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ReflectionTimelineResponse.class))),
          @ApiResponse(responseCode = "400",
              description = "fromがtoより後、原因カテゴリコードが不正、または入力値が不正なときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))),
          @ApiResponse(responseCode = "404", description = "指定されたプロジェクトが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class)))
      }
  )
  @GetMapping("/reflections")
  public ReflectionTimelineResponse getReflectionTimeline(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @ModelAttribute ReflectionTimelineQueryCondition condition) {
    return service.getReflectionTimeline(user.getUserId(), condition);
  }
}
