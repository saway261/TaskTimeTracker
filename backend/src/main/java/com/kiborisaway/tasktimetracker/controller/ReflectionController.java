package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.dto.reflection.ProjectReflectionOverviewResponse;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionRequest;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionResponse;
import com.kiborisaway.tasktimetracker.data.entity.Reflection;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorResponse;
import com.kiborisaway.tasktimetracker.security.AuthenticatedUser;
import com.kiborisaway.tasktimetracker.service.ReflectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class ReflectionController {

  private ReflectionService service;

  @Autowired
  public ReflectionController(ReflectionService service) {
    this.service = service;
  }

  @Operation(
      summary = "プロジェクトの振り返り対象一覧取得",
      description = "プロジェクト内の完了タスクを階層と表示順を維持して返します。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "pId", required = true,
              description = "対象プロジェクトID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      responses = {
          @ApiResponse(responseCode = "200", description = "取得成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ProjectReflectionOverviewResponse.class))),
          @ApiResponse(responseCode = "400", description = "プロジェクトIDの形式が不正なときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))),
          @ApiResponse(responseCode = "404", description = "指定されたプロジェクトが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class)))
      }
  )
  @GetMapping("/projects/{pId}/reflections")
  public ProjectReflectionOverviewResponse getOverview(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable @Positive int pId) {
    return service.getOverview(user.getUserId(), pId);
  }

  @Operation(
      summary = "タスクの振り返り新規登録",
      description = "完了済みタスクへ振り返りを登録します。1タスクにつき1件だけ登録できます。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "taskId", required = true,
              description = "対象タスクID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "登録する振り返り",
          required = true,
          content = @Content(schema = @Schema(implementation = ReflectionRequest.class))
      ),
      responses = {
          @ApiResponse(responseCode = "201", description = "登録成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ReflectionResponse.class))),
          @ApiResponse(responseCode = "400", description = "入力値のバリデーションエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))),
          @ApiResponse(responseCode = "404", description = "指定されたタスクが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))),
          @ApiResponse(responseCode = "409", description = "未完了または振り返り登録済みのときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class)))
      }
  )
  @PostMapping("/tasks/{taskId}/reflection")
  public ResponseEntity<ReflectionResponse> create(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable @Positive int taskId,
      @RequestBody @Validated ReflectionRequest request) {
    Reflection reflection = service.register(user.getUserId(), taskId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(new ReflectionResponse(reflection));
  }

  @Operation(
      summary = "タスクの振り返り更新",
      description = "完了済みタスクに登録されている振り返りを更新します。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "taskId", required = true,
              description = "対象タスクID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "更新する振り返り",
          required = true,
          content = @Content(schema = @Schema(implementation = ReflectionRequest.class))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "更新成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ReflectionResponse.class))),
          @ApiResponse(responseCode = "400", description = "入力値のバリデーションエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))),
          @ApiResponse(responseCode = "404", description = "タスクまたは振り返りが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))),
          @ApiResponse(responseCode = "409", description = "対象タスクが未完了のときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class)))
      }
  )
  @PutMapping("/tasks/{taskId}/reflection")
  public ResponseEntity<ReflectionResponse> update(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable @Positive int taskId,
      @RequestBody @Validated ReflectionRequest request) {
    Reflection reflection = service.update(user.getUserId(), taskId, request);
    return ResponseEntity.ok(new ReflectionResponse(reflection));
  }
}
