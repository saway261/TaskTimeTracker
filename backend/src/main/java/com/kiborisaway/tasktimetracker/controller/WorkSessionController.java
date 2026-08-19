package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.dto.work_session.WorkSessionCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.work_session.WorkSessionUpdateRequest;
import com.kiborisaway.tasktimetracker.data.dto.work_session.ActiveTimerResponse;
import com.kiborisaway.tasktimetracker.data.entity.WorkSession;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorResponse;
import com.kiborisaway.tasktimetracker.security.AuthenticatedUser;
import com.kiborisaway.tasktimetracker.service.WorkSessionService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class WorkSessionController {

  private WorkSessionService service;

  @Autowired
  public WorkSessionController(WorkSessionService service) {
    this.service = service;
  }

  @Operation(
      summary = "タスクの実作業時間合計取得",
      description = "タスクIDを指定して、タスクに紐づく全作業セッションの実作業時間の合計（分）を返します。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "taskId", required = true,
              description = "タスクID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      responses = {
          @ApiResponse(
              responseCode = "200", description = "取得成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(type = "integer", format = "int32", example = "120"))
          ),
          @ApiResponse(
              responseCode = "400", description = "タスクIDの形式が不正であったときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "404", description = "指定されたタスクIDが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @GetMapping("/tasks/{taskId}/work-sessions/total-minutes")
  public int getTaskActualTotalTime(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable @Positive int taskId) {
    return service.getTaskActualTotalTime(user.getUserId(), taskId);
  }

  @Operation(
      summary = "タスクグループの実作業時間合計取得",
      description = "タスクグループIDを指定して、タスクグループ内の全タスクの作業セッションの実作業時間合計（分）を返します。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "tgId", required = true,
              description = "タスクグループID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      responses = {
          @ApiResponse(
              responseCode = "200", description = "取得成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(type = "integer", format = "int32", example = "300"))
          ),
          @ApiResponse(
              responseCode = "400", description = "タスクグループIDの形式が不正であったときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "404", description = "指定されたタスクグループIDが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @GetMapping("/task-groups/{tgId}/work-sessions/total-minutes")
  public int getTaskGroupActualTotalTime(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable @Positive int tgId) {
    return service.getTaskGroupActualTotalTime(user.getUserId(), tgId);
  }

  @Operation(
      summary = "プロジェクトの実作業時間合計取得",
      description = "プロジェクトIDを指定して、プロジェクト内の全タスクの作業セッションの実作業時間合計（分）を返します。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "pId", required = true,
              description = "プロジェクトID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      responses = {
          @ApiResponse(
              responseCode = "200", description = "取得成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(type = "integer", format = "int32", example = "900"))
          ),
          @ApiResponse(
              responseCode = "400", description = "プロジェクトIDの形式が不正であったときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "404", description = "指定されたプロジェクトIDが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @GetMapping("/projects/{pId}/work-sessions/total-minutes")
  public int getProjectActualTotalTime(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable @Positive int pId) {
    return service.getProjectActualTotalTime(user.getUserId(), pId);
  }

  @Operation(
      summary = "タスク内作業セッション一覧取得",
      description = "タスクIDを指定して、タスクに紐づく全作業セッションの詳細一覧を返します。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "taskId", required = true,
              description = "タスクID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      responses = {
          @ApiResponse(
              responseCode = "200", description = "取得成功",
              content = @Content(mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = WorkSession.class)))
          ),
          @ApiResponse(
              responseCode = "400", description = "タスクIDの形式が不正であったときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @GetMapping("/tasks/{taskId}/work-sessions")
  public List<WorkSession> getAllInTask(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable @Positive int taskId) {
    return service.getAllInTask(user.getUserId(), taskId);
  }

  @Operation(
      summary = "稼働中タイマー一覧取得",
      description = "ログインユーザーが所有する全タスクから、終了していないタイマーを返します。",
      responses = {
          @ApiResponse(
              responseCode = "200", description = "取得成功",
              content = @Content(mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = ActiveTimerResponse.class)))
          )
      }
  )
  @GetMapping("/work-sessions/active")
  public List<ActiveTimerResponse> getAllActive(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
    return service.getAllActive(user.getUserId());
  }

  @Operation(
      summary = "作業セッションID検索",
      description = "IDを指定して単一の作業セッションの詳細を返します。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "wsId", required = true,
              description = "作業セッションID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      responses = {
          @ApiResponse(
              responseCode = "200", description = "取得成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = WorkSession.class))
          ),
          @ApiResponse(
              responseCode = "400", description = "作業セッションIDの形式が不正であったときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "404", description = "指定された作業セッションIDが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @GetMapping("/work-sessions/{wsId}")
  public WorkSession get(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable @Positive int wsId) {
    return service.get(user.getUserId(), wsId);
  }

  @Operation(
      summary = "作業セッション登録",
      description = """
          タスクIDを指定して作業セッションを登録します。type が TIMER・MANUAL どちらでもこのAPIを使用します。
          - MANUAL: type と minutes のみ必要です。
          - TIMER: type と startedAt のみ必要です（終了時刻は /end エンドポイントでセットします）。
          """,
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "taskId", required = true,
              description = "タスクID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "登録する作業セッションの詳細",
          required = true,
          content = @Content(schema = @Schema(implementation = WorkSessionCreateRequest.class))
      ),
      responses = {
          @ApiResponse(
              responseCode = "200", description = "登録成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = WorkSession.class))
          ),
          @ApiResponse(
              responseCode = "400", description = "入力値のバリデーションエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @PostMapping("/tasks/{taskId}/work-sessions")
  public ResponseEntity<WorkSession> create(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable @Positive int taskId,
      @RequestBody @Validated WorkSessionCreateRequest request) {
    return ResponseEntity.ok(service.create(user.getUserId(), taskId, request));
  }

  @Operation(
      summary = "作業セッション終了",
      description = """
          作業セッションIDを指定して、現在時刻を終了時刻としてセットします。
          type が TIMER で startedAt を持つ作業セッションに対して使用します。
          """,
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "wsId", required = true,
              description = "作業セッションID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      responses = {
          @ApiResponse(
              responseCode = "200", description = "終了処理成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = WorkSession.class))
          ),
          @ApiResponse(
              responseCode = "400", description = "作業セッションIDの形式が不正、またはTIMER以外のセッションへの操作など",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "404", description = "指定された作業セッションIDが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @PostMapping("/work-sessions/{wsId}/end")
  public ResponseEntity<WorkSession> setEnd(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable @Positive int wsId) {
    return ResponseEntity.ok(service.setEnd(user.getUserId(), wsId));
  }

  @Operation(
      summary = "作業セッション更新",
      description = """
          作業セッションIDを指定して作業セッションを更新します。type は変更できません。
          - MANUAL: minutes を更新できます。
          - TIMER: startedAt と endedAt をセットで更新します（セッションが終了済みであることが前提）。
          """,
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "wsId", required = true,
              description = "作業セッションID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "更新する作業セッションの詳細",
          required = true,
          content = @Content(schema = @Schema(implementation = WorkSessionUpdateRequest.class))
      ),
      responses = {
          @ApiResponse(
              responseCode = "200", description = "更新成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = WorkSession.class))
          ),
          @ApiResponse(
              responseCode = "400", description = "入力値のバリデーションエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "404", description = "指定された作業セッションIDが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @PatchMapping("/work-sessions/{wsId}")
  public ResponseEntity<WorkSession> update(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable @Positive int wsId,
      @RequestBody @Validated WorkSessionUpdateRequest request
  ) {
    return ResponseEntity.ok(service.update(user.getUserId(), wsId, request));
  }

  @Operation(
      summary = "作業セッション削除",
      description = "IDを指定して作業セッションを削除します。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "workSessionId", required = true,
              description = "作業セッションID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      responses = {
          @ApiResponse(
              responseCode = "204", description = "削除成功"
          ),
          @ApiResponse(
              responseCode = "400", description = "作業セッションIDの形式が不正であったときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "404", description = "指定された作業セッションIDが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @DeleteMapping("/work-sessions/{workSessionId}")
  public ResponseEntity<Void> delete(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable("workSessionId") @Positive int wsId) {
    service.delete(user.getUserId(), wsId);
    return ResponseEntity.noContent().build();
  }
}
