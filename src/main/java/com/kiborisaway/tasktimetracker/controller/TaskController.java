package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.entity.Task;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorResponse;
import com.kiborisaway.tasktimetracker.service.TaskService;
import com.kiborisaway.tasktimetracker.validation.CreateGroup;
import com.kiborisaway.tasktimetracker.validation.UpdateGroup;
import com.kiborisaway.tasktimetracker.validation.ValidUpdateParentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.NotNull;
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
                  array = @ArraySchema(schema = @Schema(implementation = Task.class))
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
  public List<Task> getAllInProject(
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
                  array = @ArraySchema(schema = @Schema(implementation = Task.class))
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
  public List<Task> getAllInTaskGroup(
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
                  schema = @Schema(implementation = Task.class)
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
  public Task getById(@PathVariable @Positive int taskId) {
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
          content = @Content(schema = @Schema(implementation = Task.class))
      ),
      responses = {
          @ApiResponse(
              responseCode = "201", description = "登録成功",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = Task.class)
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
  public ResponseEntity<Task> createInProject(
      @PathVariable @Positive int pId,
      @RequestBody @Validated(CreateGroup.class) Task request) {
    request.setProjectId(pId);
    request.setTaskGroupId(null);//タスクグループIDは明示的にnullにする
    Task response = service.register(request);
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
          content = @Content(schema = @Schema(implementation = Task.class))
      ),
      responses = {
          @ApiResponse(
              responseCode = "201", description = "登録成功",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = Task.class)
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
  public ResponseEntity<Task> createInTaskGroup(
      @PathVariable @Positive int tgId,
      @RequestBody @Validated(CreateGroup.class) Task request) {
    request.setTaskGroupId(tgId);
    request.setProjectId(null);//プロジェクトIDは明示的にnullにする
    Task response = service.register(request);
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
          content = @Content(schema = @Schema(implementation = Task.class))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "更新成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(type = "string", example = "タスク名と説明を更新しました"))
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
  public ResponseEntity<String> updateProperty(
      @PathVariable @Positive int taskId,
      @RequestBody @Validated(UpdateGroup.class) Task request) {
    request.setId(taskId);
    service.updateProperty(request);
    return ResponseEntity.ok("タスク名と説明を更新しました");
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
          content = @Content(schema = @Schema(type = "integer", format = "int32", example = "60"))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "更新成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(type = "string", example = "見積作業時間を更新しました"))
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
  public ResponseEntity<String> updateEstimatedMinutes(
      @PathVariable @Positive int taskId,
      @RequestBody @Positive Integer estimatedMinutes) {
    service.updateEstimateMinutes(taskId, estimatedMinutes);
    return ResponseEntity.ok("見積作業時間を更新しました");
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
          content = @Content(schema = @Schema(implementation = UpdateParentRequest.class))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "更新成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(type = "string", example = "タスクの所属を更新しました"))
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
  public ResponseEntity<String> updateParent(
      @PathVariable @Positive int taskId,
      @RequestBody @Validated UpdateParentRequest request) {
    service.updateParent(taskId, request.projectId(), request.taskGroupId());
    return ResponseEntity.ok("タスクの所属を更新しました");
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
          content = @Content(schema = @Schema(implementation = TaskFinishedUpdateRequest.class))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "更新成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(type = "string", example = "タスクの完了状態を更新しました"))
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
  public ResponseEntity<String> updateFinished(
      @PathVariable @Positive int taskId,
      @RequestBody @Validated TaskFinishedUpdateRequest request) {
    service.updateFinished(taskId, request.isFinished());
    return ResponseEntity.ok("タスクの完了状態を更新しました");
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
          @ApiResponse(responseCode = "200", description = "削除成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(type = "string", example = "タスクを削除しました"))
          ),
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
  public ResponseEntity<String> delete(@PathVariable @Positive int taskId) {
    service.deleteById(taskId);
    return ResponseEntity.ok("タスクを削除しました");
  }

  /**
   * updateFinished に isFinished だけをリクエストボディとして渡すためのrecord
   *
   * @param isFinished trueの場合完了状態にする / falseの場合は作業中状態にする
   */
  @Schema(description = "タスク完了状態更新リクエスト")
  public record TaskFinishedUpdateRequest(
      @Schema(description = "完了フラグ。trueの場合完了状態、falseの場合は作業中状態にする", example = "true")
      @NotNull Boolean isFinished) {

  }

  /**
   * updateParent に移動先の親IDをリクエストボディとして渡すためのrecord
   *
   * @param projectId   移動先プロジェクトID
   * @param taskGroupId 移動先タスクグループID
   */
  @Schema(description = "タスク所属変更リクエスト。projectId と taskGroupId のどちらか片方のみ指定すること")
  @ValidUpdateParentRequest
  public record UpdateParentRequest(
      @Schema(description = "移動先プロジェクトID", example = "1")
      @Positive Integer projectId,
      @Schema(description = "移動先タスクグループID", example = "1")
      @Positive Integer taskGroupId) {

  }
}
