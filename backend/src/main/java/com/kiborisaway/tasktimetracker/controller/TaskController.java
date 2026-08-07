package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.dto.task.TaskCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.task.TaskResponse;
import com.kiborisaway.tasktimetracker.data.dto.task.TaskUpdateEstimatedMinutesRequest;
import com.kiborisaway.tasktimetracker.data.dto.task.TaskUpdateFinishedRequest;
import com.kiborisaway.tasktimetracker.data.dto.task.TaskUpdateParentRequest;
import com.kiborisaway.tasktimetracker.data.dto.task.TaskUpdatePropertyRequest;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorResponse;
import com.kiborisaway.tasktimetracker.service.TaskService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class TaskController {

  private TaskService service;

  @Autowired
  public TaskController(TaskService service) {
    this.service = service;
  }

  @Operation(
      summary = "プロジェクト内タスク全件取得（完了フラグ指定可）",
      description = """
          完了フラグを指定して、指定プロジェクト内の完了済み・未完了のタスクをリストで取得します。
          例: /api/projects/1/tasks?isFinished=false
          クエリパラメータを省略した場合は、プロジェクト内のタスクを全件取得します。
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
                  array = @ArraySchema(schema = @Schema(implementation = TaskResponse.class))
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
  @GetMapping("/projects/{pId}/tasks")
  public List<TaskResponse> getAllInProject(
      @PathVariable @Positive int pId,
      @RequestParam(required = false) Boolean isFinished
  ) {
    return service.findAllInProjectByCondition(pId, isFinished);
  }

  @Operation(
      summary = "タスクグループ内タスク全件取得（完了フラグ指定可）",
      description = """
          完了フラグを指定して、指定タスクグループ内の完了済み・未完了のタスクをリストで取得します。
          例: /api/task-groups/1/tasks?isFinished=true
          クエリパラメータを省略した場合は、タスクグループ内のタスクを全件取得します。
          """,
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "tgId", required = true,
              description = "タスクグループID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "検索成功",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = TaskResponse.class))
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
  @GetMapping("/task-groups/{tgId}/tasks")
  public List<TaskResponse> getAllInTaskGroup(
      @PathVariable @Positive int tgId,
      @RequestParam(required = false) Boolean isFinished
  ) {
    return service.findAllInTaskGroupByCondition(tgId, isFinished);
  }

  @Operation(
      summary = "タスクID検索",
      description = "タスクの全件からIDが一致するタスクを取得します。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "taskId", required = true,
              description = "タスクID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      responses = {
          @ApiResponse(
              responseCode = "200", description = "ok",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = TaskResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "404", description = "指定されたタスクIDが存在しなかったときのエラー",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "400", description = "タスクIDの形式が不正であったときのエラー",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class)
              )
          )
      }
  )
  @GetMapping("/tasks/{taskId}")
  public TaskResponse getById(@PathVariable @Positive int taskId) {
    return service.findById(taskId);
  }

  @Operation(
      summary = "プロジェクト内タスク新規登録",
      description = "指定したプロジェクトにタスクを登録します。タスクグループIDは自動的にnullになります。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "pId", required = true,
              description = "親となるプロジェクトID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "新規に登録したいタスクの詳細",
          required = true,
          content = @Content(schema = @Schema(implementation = TaskCreateRequest.class))
      ),
      responses = {
          @ApiResponse(
              responseCode = "201", description = "登録成功",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = TaskResponse.class)
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
  @PostMapping("/projects/{pId}/tasks")
  public ResponseEntity<TaskResponse> createInProject(
      @PathVariable @Positive int pId,
      @RequestBody @Validated TaskCreateRequest request) {
    TaskResponse response = service.register(pId, null, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(
      summary = "タスクグループ内タスク新規登録",
      description = "指定したタスクグループにタスクを登録します。プロジェクトIDは自動的にnullになります。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "tgId", required = true,
              description = "親となるタスクグループID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "新規に登録したいタスクの詳細",
          required = true,
          content = @Content(schema = @Schema(implementation = TaskCreateRequest.class))
      ),
      responses = {
          @ApiResponse(
              responseCode = "201", description = "登録成功",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = TaskResponse.class)
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
  @PostMapping("/task-groups/{tgId}/tasks")
  public ResponseEntity<TaskResponse> createInTaskGroup(
      @PathVariable @Positive int tgId,
      @RequestBody @Validated TaskCreateRequest request) {
    TaskResponse response = service.register(null, tgId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(
      summary = "タスク名・説明の更新",
      description = "タスクのタイトルと説明を更新します。タスクIDが存在しない場合はエラーを返します。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "taskId", required = true,
              description = "タスクID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "更新したいタスクの詳細",
          required = true,
          content = @Content(schema = @Schema(implementation = TaskUpdatePropertyRequest.class))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "更新成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = TaskResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "入力値のバリデーションエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          ),
          @ApiResponse(responseCode = "404", description = "指定されたタスクIDが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @PatchMapping("/tasks/{taskId}")
  public ResponseEntity<TaskResponse> updateProperty(
      @PathVariable @Positive int taskId,
      @RequestBody @Validated TaskUpdatePropertyRequest request) {
    return ResponseEntity.ok(service.updateProperty(taskId, request));
  }

  @Operation(
      summary = "見積作業時間の更新",
      description = "タスクの見積作業時間（分）を更新します。タスクIDが存在しない場合はエラーを返します。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "taskId", required = true,
              description = "タスクID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "更新したい見積作業時間（分）",
          required = true,
          content = @Content(schema = @Schema(implementation = TaskUpdateEstimatedMinutesRequest.class))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "更新成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = TaskResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "入力値のバリデーションエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          ),
          @ApiResponse(responseCode = "404", description = "指定されたタスクIDが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @PatchMapping("/tasks/{taskId}/estimated-minutes")
  public ResponseEntity<TaskResponse> updateEstimatedMinutes(
      @PathVariable @Positive int taskId,
      @RequestBody @Validated TaskUpdateEstimatedMinutesRequest request) {
    return ResponseEntity.ok(service.updateEstimateMinutes(taskId, request));
  }

  @Operation(
      summary = "タスクの所属（親）変更",
      description = """
          タスクの所属先をプロジェクトまたはタスクグループに変更します。
          projectId と taskGroupId のどちらか片方のみ指定してください。
          タスクIDが存在しない場合はエラーを返します。
          """,
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "taskId", required = true,
              description = "タスクID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "移動先の親ID（projectId と taskGroupId のどちらか片方のみ指定）",
          required = true,
          content = @Content(schema = @Schema(implementation = TaskUpdateParentRequest.class))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "更新成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = TaskResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "入力値のバリデーションエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          ),
          @ApiResponse(responseCode = "404", description = "指定されたタスクIDが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @PatchMapping("/tasks/{taskId}/parent")
  public ResponseEntity<TaskResponse> updateParent(
      @PathVariable @Positive int taskId,
      @RequestBody @Validated TaskUpdateParentRequest request) {
    return ResponseEntity.ok(
        service.updateParent(taskId, request.projectId(), request.taskGroupId()));
  }

  @Operation(
      summary = "タスクの完了状態更新",
      description = "タスクの完了状態を更新します。タスクIDが存在しない場合はエラーを返します。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "taskId", required = true,
              description = "タスクID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "完了状態フラグ（true: 完了、false: 作業中）",
          required = true,
          content = @Content(schema = @Schema(implementation = TaskUpdateFinishedRequest.class))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "更新成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = TaskResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "入力値のバリデーションエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          ),
          @ApiResponse(responseCode = "404", description = "指定されたタスクIDが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @PatchMapping("/tasks/{taskId}/finished")
  public ResponseEntity<TaskResponse> updateFinished(
      @PathVariable @Positive int taskId,
      @RequestBody @Validated TaskUpdateFinishedRequest request) {
    return ResponseEntity.ok(service.updateFinished(taskId, request.isFinished()));
  }

  @Operation(
      summary = "タスク削除",
      description = "指定したIDのタスクを削除します。タスクIDが存在しない場合はエラーを返します。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "taskId", required = true,
              description = "タスクID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      responses = {
          @ApiResponse(responseCode = "204", description = "削除成功"),
          @ApiResponse(responseCode = "400", description = "タスクIDの形式が不正であったときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          ),
          @ApiResponse(responseCode = "404", description = "指定されたタスクIDが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @DeleteMapping("/tasks/{taskId}")
  public ResponseEntity<Void> delete(@PathVariable @Positive int taskId) {
    service.deleteById(taskId);
    return ResponseEntity.noContent().build();
  }
}
