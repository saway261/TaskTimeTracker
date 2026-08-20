package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.dto.task_group.TaskGroupCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.task_group.TaskGroupResponse;
import com.kiborisaway.tasktimetracker.data.dto.task_group.TaskGroupUpdateFinishedRequest;
import com.kiborisaway.tasktimetracker.data.dto.task_group.TaskGroupUpdateRequest;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorResponse;
import com.kiborisaway.tasktimetracker.security.AuthenticatedUser;
import com.kiborisaway.tasktimetracker.service.TaskGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class TaskGroupController {

  private TaskGroupService service;

  @Autowired
  public TaskGroupController(TaskGroupService service) {
    this.service = service;
  }

  @Operation(
      summary = "プロジェクト内タスクグループ全件取得（完了フラグ指定可）",
      description = """
          完了フラグを指定して、指定プロジェクト内の完了済み・未完了のタスクグループをリストで取得します。
          例: /api/projects/1/task-groups?isFinished=false
          クエリパラメータを省略した場合は、プロジェクト内のタスクグループを全件取得します。
          """,
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "pId", required = true,
              description = "プロジェクトID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "検索成功",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = TaskGroupResponse.class))
              )
          ),
          @ApiResponse(
              responseCode = "400",
              description = "パスパラメータまたはクエリパラメータの形式が不正であったときのエラー",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class)
              )
          )
      }
  )
  @GetMapping("/projects/{pId}/task-groups")
  public List<TaskGroupResponse> getAll(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable @Positive int pId,
      @RequestParam(required = false) Boolean isFinished
  ) {
    return service.findAllByCondition(user.getUserId(), pId, isFinished);
  }

  @Operation(
      summary = "タスクグループID検索",
      description = "タスクグループの全件からIDが一致するタスクグループを取得します。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "tgId", required = true,
              description = "タスクグループID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      responses = {
          @ApiResponse(
              responseCode = "200", description = "ok",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = TaskGroupResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "404", description = "指定されたタスクグループIDが存在しなかったときのエラー",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "400", description = "タスクグループIDの形式が不正であったときのエラー",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class)
              )
          )
      }
  )
  @GetMapping("/task-groups/{tgId}")
  public TaskGroupResponse getById(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable @Positive int tgId) {
    return service.findById(user.getUserId(), tgId);
  }

  @Operation(
      summary = "タスクグループ新規登録",
      description = "指定したプロジェクトにタスクグループを登録します。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "pId", required = true,
              description = "親となるプロジェクトID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "新規に登録したいタスクグループの詳細",
          required = true,
          content = @Content(schema = @Schema(implementation = TaskGroupCreateRequest.class))
      ),
      responses = {
          @ApiResponse(
              responseCode = "201", description = "登録成功",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = TaskGroupResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "400", description = "入力値のバリデーションエラー",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class)
              )
          )
      }
  )
  @PostMapping("/projects/{pId}/task-groups")
  public ResponseEntity<TaskGroupResponse> create(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable @Positive int pId,
      @RequestBody @Validated TaskGroupCreateRequest request
  ) {
    TaskGroupResponse response = service.register(user.getUserId(), pId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(
      summary = "タスクグループ更新",
      description = "タスクグループの更新を行います。タスクグループ名・説明の変更を行います。タスクグループIDが存在しない場合はエラーを返します。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "tgId", required = true,
              description = "タスクグループID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "更新したいタスクグループ",
          required = true,
          content = @Content(schema = @Schema(implementation = TaskGroupUpdateRequest.class))
      ),
      responses = {
          @ApiResponse(
              responseCode = "200", description = "更新成功",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = TaskGroupResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "400", description = "入力値のバリデーションエラー",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "404", description = "指定されたタスクグループIDが存在しないときのエラー",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class)
              )
          )
      }
  )
  @PutMapping("/task-groups/{tgId}")
  public ResponseEntity<TaskGroupResponse> update(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable @Positive int tgId,
      @RequestBody @Validated TaskGroupUpdateRequest request) {
    return ResponseEntity.ok(service.update(user.getUserId(), tgId, request));
  }

  @Operation(
      summary = "タスクグループの完了状態更新",
      description = """
          タスクグループの完了状態を更新します。未完了のタスクが1件でも存在する場合は完了状態にできません。
          タスクグループIDが存在しない場合はエラーを返します。
          """,
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "tgId", required = true,
              description = "タスクグループID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "完了状態フラグ（true: 完了、false: 未完了）",
          required = true,
          content = @Content(schema = @Schema(implementation = TaskGroupUpdateFinishedRequest.class))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "更新成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = TaskGroupResponse.class))
          ),
          @ApiResponse(responseCode = "400",
              description = "入力値のバリデーションエラー、または未完了のタスクが存在する場合",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          ),
          @ApiResponse(responseCode = "404", description = "指定されたタスクグループIDが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @PatchMapping("/task-groups/{tgId}/finished")
  public ResponseEntity<TaskGroupResponse> updateFinished(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable @Positive int tgId,
      @RequestBody @Validated TaskGroupUpdateFinishedRequest request) {
    return ResponseEntity.ok(
        service.updateFinished(user.getUserId(), tgId, request.isFinished()));
  }
}
