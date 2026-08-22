package com.kiborisaway.tasktimetracker.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.data.dto.work_session.WorkSessionCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.work_session.WorkSessionUpdateRequest;
import com.kiborisaway.tasktimetracker.data.dto.work_session.ActiveTimerResponse;
import com.kiborisaway.tasktimetracker.data.entity.WorkSession;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.exception.WorkSessionEndNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.WorkSessionOperationNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorDetailsBuilder;
import com.kiborisaway.tasktimetracker.publicid.id.ProjectId;
import com.kiborisaway.tasktimetracker.publicid.id.TaskId;
import com.kiborisaway.tasktimetracker.publicid.id.WorkSessionId;
import com.kiborisaway.tasktimetracker.security.JsonAuthenticationEntryPoint;
import com.kiborisaway.tasktimetracker.service.WorkSessionService;
import com.kiborisaway.tasktimetracker.support.TestPublicIds;
import com.kiborisaway.tasktimetracker.support.WebMvcTestSecuritySupportConfig;
import com.kiborisaway.tasktimetracker.support.WithMockAuthenticatedUser;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(WorkSessionController.class)
@WithMockAuthenticatedUser
@Import({WebMvcTestSecuritySupportConfig.class, JsonAuthenticationEntryPoint.class})
class WorkSessionControllerTest {

  private static final int USER_ID = 1;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private WorkSessionService service;

  @MockitoBean
  private ErrorDetailsBuilder errorDetailsBuilder;

  @Test
  void 稼働中タイマー一覧取得成功_200とタスク情報を返すこと() throws Exception {
    ActiveTimerResponse timer = new ActiveTimerResponse(
        new WorkSessionId(10), new TaskId(4), "画面設計", new ProjectId(1), null,
        LocalDateTime.of(2026, 8, 18, 9, 0));
    when(service.getAllActive(USER_ID)).thenReturn(List.of(timer));

    mockMvc.perform(MockMvcRequestBuilders.get("/work-sessions/active"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].sessionId").value(TestPublicIds.workSession(10)))
        .andExpect(jsonPath("$[0].taskId").value(TestPublicIds.task(4)))
        .andExpect(jsonPath("$[0].taskTitle").value("画面設計"))
        .andExpect(jsonPath("$[0].projectId").value(TestPublicIds.project(1)))
        .andExpect(jsonPath("$[0].taskGroupId").isEmpty())
        .andExpect(jsonPath("$[0].startedAt").exists());

    verify(service).getAllActive(USER_ID);
  }

  @Test
  void タスク作業時間合計取得成功_200を返しサービスを呼び出すこと() throws Exception {
    // Arrange
    int taskId = 1;
    when(service.getTaskActualTotalTime(USER_ID, taskId)).thenReturn(75);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/tasks/{taskId}/work-sessions/total-minutes",
            TestPublicIds.task(taskId)))
        .andExpect(status().isOk());

    verify(service).getTaskActualTotalTime(USER_ID, taskId);
  }

  @Test
  void CORS許可済みオリジンからのリクエストなら許可ヘッダーを返すこと() throws Exception {
    // Arrange
    int taskId = 1;
    when(service.getTaskActualTotalTime(USER_ID, taskId)).thenReturn(75);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/tasks/{taskId}/work-sessions/total-minutes",
                TestPublicIds.task(taskId))
            .header("Origin", "http://localhost:5173"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
  }

  @Test
  void タスク作業時間合計取得失敗_パス変数の形式が不正なら404を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/tasks/invalid-id/work-sessions/total-minutes"))
        .andExpect(status().isNotFound());

    verifyNoInteractions(service);
  }

  @Test
  void タスクグループ作業時間合計取得成功_200を返しサービスを呼び出すこと() throws Exception {
    // Arrange
    int tgId = 1;
    when(service.getTaskGroupActualTotalTime(USER_ID, tgId)).thenReturn(120);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get(
            "/task-groups/{tgId}/work-sessions/total-minutes", TestPublicIds.taskGroup(tgId)))
        .andExpect(status().isOk());

    verify(service).getTaskGroupActualTotalTime(USER_ID, tgId);
  }

  @Test
  void プロジェクト作業時間合計取得成功_200を返しサービスを呼び出すこと() throws Exception {
    // Arrange
    int pId = 1;
    when(service.getProjectActualTotalTime(USER_ID, pId)).thenReturn(180);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/{pId}/work-sessions/total-minutes",
            TestPublicIds.project(pId)))
        .andExpect(status().isOk());

    verify(service).getProjectActualTotalTime(USER_ID, pId);
  }

  @Test
  void タスク内作業セッション一覧取得成功_200を返しサービスを呼び出すこと() throws Exception {
    // Arrange
    int taskId = 1;
    WorkSession workSession = new WorkSession();
    workSession.setId(1);
    workSession.setTaskId(taskId);
    when(service.getAllInTask(USER_ID, taskId)).thenReturn(List.of(workSession));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/tasks/{taskId}/work-sessions", TestPublicIds.task(taskId)))
        .andExpect(status().isOk());

    verify(service).getAllInTask(USER_ID, taskId);
  }

  @Test
  void 作業セッション単体取得成功_200を返しサービスを呼び出すこと() throws Exception {
    // Arrange
    int wsId = 1;
    WorkSession workSession = new WorkSession();
    workSession.setId(wsId);
    when(service.get(USER_ID, wsId)).thenReturn(workSession);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/work-sessions/{wsId}", TestPublicIds.workSession(wsId)))
        .andExpect(status().isOk());

    verify(service).get(USER_ID, wsId);
  }

  @Test
  void 作業セッション単体取得失敗_対象が存在しないなら404を返すこと() throws Exception {
    // Arrange
    int wsId = 999;
    when(service.get(USER_ID, wsId)).thenThrow(
        new TargetNotFoundException("workSession.id", "work session not found"));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/work-sessions/{wsId}", TestPublicIds.workSession(wsId)))
        .andExpect(status().isNotFound());

    verify(service).get(USER_ID, wsId);
  }

  @Test
  void 作業セッション単体取得失敗_パス変数の形式が不正なら404を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/work-sessions/invalid-id"))
        .andExpect(status().isNotFound());

    verifyNoInteractions(service);
  }

  @Test
  void 作業セッション登録成功_200を返しタスクIDとリクエストをサービスに渡すこと() throws Exception {
    // Arrange
    int taskId = 1;
    WorkSession response = new WorkSession();
    response.setId(10);
    response.setTaskId(taskId);
    when(service.create(eq(USER_ID), eq(taskId), any(WorkSessionCreateRequest.class))).thenReturn(response);
    String validRequest = """
        {
          "type": "MANUAL",
          "minutes": 30
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/tasks/{taskId}/work-sessions", TestPublicIds.task(taskId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk());

    verify(service).create(eq(USER_ID), eq(taskId), any(WorkSessionCreateRequest.class));
  }

  @Test
  void 作業セッション登録成功_オフセットなし日時をLocalDateTimeとしてサービスに渡すこと()
      throws Exception {
    // Arrange
    int taskId = 1;
    WorkSession response = new WorkSession();
    response.setId(10);
    when(service.create(eq(USER_ID), eq(taskId), any(WorkSessionCreateRequest.class))).thenReturn(response);
    String validRequest = """
        {
          "type": "TIMER",
          "startedAt": "2026-01-01T09:00:00"
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/tasks/{taskId}/work-sessions", TestPublicIds.task(taskId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk());

    verify(service).create(eq(USER_ID), eq(taskId), argThat(request ->
        LocalDateTime.of(2026, 1, 1, 9, 0).equals(request.getStartedAt())));
  }

  @Test
  void 作業セッション登録失敗_指定したタスクが存在しないなら404を返すこと() throws Exception {
    // Arrange
    int taskId = 999;
    String validRequest = """
        {
          "type": "MANUAL",
          "minutes": 30
        }
        """;
    when(service.create(eq(USER_ID), eq(taskId), any(WorkSessionCreateRequest.class))).thenThrow(
        new TargetNotFoundException("task.id", "task not found"));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/tasks/{taskId}/work-sessions", TestPublicIds.task(taskId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isNotFound());

    verify(service).create(eq(USER_ID), eq(taskId), any(WorkSessionCreateRequest.class));
  }

  @Test
  void 作業セッション登録失敗_指定したタスクが完了済みなら400を返すこと() throws Exception {
    // Arrange
    int taskId = 3;
    String validRequest = """
        {
          "type": "MANUAL",
          "minutes": 30
        }
        """;
    when(service.create(eq(USER_ID), eq(taskId), any(WorkSessionCreateRequest.class))).thenThrow(
        new WorkSessionOperationNotAllowedException("task.id", "cannot create"));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/tasks/{taskId}/work-sessions", TestPublicIds.task(taskId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isBadRequest());

    verify(service).create(eq(USER_ID), eq(taskId), any(WorkSessionCreateRequest.class));
  }

  @Test
  void 作業セッション登録失敗_不正なリクエストボディなら400を返すこと() throws Exception {
    // Arrange
    int taskId = 1;
    String invalidRequest = """
        {
          "type": "MANUAL"
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/tasks/{taskId}/work-sessions", TestPublicIds.task(taskId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void 作業セッション終了成功_200とメッセージを返しサービスを呼び出すこと() throws Exception {
    // Arrange
    int wsId = 1;
    WorkSession ended = new WorkSession();
    ended.setId(wsId);
    ended.setMinutes(30);
    ended.setStartedAt(LocalDateTime.of(2026, 1, 1, 9, 0));
    ended.setEndedAt(LocalDateTime.of(2026, 1, 1, 9, 30));
    when(service.setEnd(USER_ID, wsId)).thenReturn(ended);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/work-sessions/{wsId}/end", TestPublicIds.workSession(wsId)))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(TestPublicIds.workSession(wsId)))
        .andExpect(jsonPath("$.minutes").value(30))
        .andExpect(jsonPath("$.startedAt").value("2026-01-01T09:00:00+09:00"))
        .andExpect(jsonPath("$.endedAt").value("2026-01-01T09:30:00+09:00"));

    verify(service).setEnd(USER_ID, wsId);
  }

  @Test
  void 作業セッション終了失敗_終了できない状態なら400を返すこと() throws Exception {
    // Arrange
    int wsId = 1;
    doThrow(new WorkSessionEndNotAllowedException("workSession.id", "cannot end"))
        .when(service).setEnd(USER_ID, wsId);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/work-sessions/{wsId}/end", TestPublicIds.workSession(wsId)))
        .andExpect(status().isBadRequest());

    verify(service).setEnd(USER_ID, wsId);
  }

  @Test
  void 作業セッション終了失敗_親タスクが完了済みなら400を返すこと() throws Exception {
    // Arrange
    int wsId = 1;
    doThrow(new WorkSessionOperationNotAllowedException("task.id", "cannot end"))
        .when(service).setEnd(USER_ID, wsId);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/work-sessions/{wsId}/end", TestPublicIds.workSession(wsId)))
        .andExpect(status().isBadRequest());

    verify(service).setEnd(USER_ID, wsId);
  }

  @Test
  void 作業セッション更新成功_200とメッセージを返しサービスを呼び出すこと() throws Exception {
    // Arrange
    int wsId = 1;
    String validRequest = """
        {
          "type": "MANUAL",
          "minutes": 45
        }
        """;
    WorkSession updated = new WorkSession();
    updated.setId(wsId);
    updated.setMinutes(45);
    when(service.update(eq(USER_ID), eq(wsId), any(WorkSessionUpdateRequest.class))).thenReturn(updated);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/work-sessions/{wsId}", TestPublicIds.workSession(wsId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(TestPublicIds.workSession(wsId)))
        .andExpect(jsonPath("$.minutes").value(45));

    verify(service).update(eq(USER_ID), eq(wsId), any(WorkSessionUpdateRequest.class));
  }

  @Test
  void 作業セッション更新成功_レスポンスのオフセット付き日時をそのまま更新できること()
      throws Exception {
    // Arrange
    int wsId = 1;
    String validRequest = """
        {
          "type": "TIMER",
          "startedAt": "2026-08-08T21:57:21.691905+09:00",
          "endedAt": "2026-08-08T22:57:21.691905+09:00"
        }
        """;
    WorkSession updated = new WorkSession();
    updated.setId(wsId);
    updated.setStartedAt(LocalDateTime.of(2026, 8, 8, 21, 57, 21, 691_905_000));
    updated.setEndedAt(LocalDateTime.of(2026, 8, 8, 22, 57, 21, 691_905_000));
    when(service.update(eq(USER_ID), eq(wsId), any(WorkSessionUpdateRequest.class))).thenReturn(updated);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/work-sessions/{wsId}", TestPublicIds.workSession(wsId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.startedAt")
            .value("2026-08-08T21:57:21.691905+09:00"))
        .andExpect(jsonPath("$.endedAt")
            .value("2026-08-08T22:57:21.691905+09:00"));

    verify(service).update(eq(USER_ID), eq(wsId), argThat(request ->
        LocalDateTime.of(2026, 8, 8, 21, 57, 21, 691_905_000)
            .equals(request.getStartedAt())
            && LocalDateTime.of(2026, 8, 8, 22, 57, 21, 691_905_000)
            .equals(request.getEndedAt())));
  }

  @Test
  void 作業セッション更新成功_異なるオフセットの日時をDBタイムゾーンへ正規化すること()
      throws Exception {
    // Arrange
    int wsId = 1;
    String validRequest = """
        {
          "type": "TIMER",
          "startedAt": "2026-01-01T00:00:00Z",
          "endedAt": "2026-01-01T01:00:00Z"
        }
        """;
    WorkSession updated = new WorkSession();
    updated.setId(wsId);
    when(service.update(eq(USER_ID), eq(wsId), any(WorkSessionUpdateRequest.class))).thenReturn(updated);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/work-sessions/{wsId}", TestPublicIds.workSession(wsId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk());

    verify(service).update(eq(USER_ID), eq(wsId), argThat(request ->
        LocalDateTime.of(2026, 1, 1, 9, 0).equals(request.getStartedAt())
            && LocalDateTime.of(2026, 1, 1, 10, 0).equals(request.getEndedAt())));
  }

  @Test
  void 作業セッション更新失敗_対象が存在しないなら404を返すこと() throws Exception {
    // Arrange
    int wsId = 999;
    String validRequest = """
        {
          "type": "MANUAL",
          "minutes": 45
        }
        """;
    doThrow(new TargetNotFoundException("workSession.id", "work session not found"))
        .when(service).update(eq(USER_ID), eq(wsId), any(WorkSessionUpdateRequest.class));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/work-sessions/{wsId}", TestPublicIds.workSession(wsId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isNotFound());

    verify(service).update(eq(USER_ID), eq(wsId), any(WorkSessionUpdateRequest.class));
  }

  @Test
  void 作業セッション更新失敗_完了済みタスク配下なら400を返すこと() throws Exception {
    // Arrange
    int wsId = 1;
    String validRequest = """
        {
          "type": "MANUAL",
          "minutes": 45
        }
        """;
    doThrow(new WorkSessionOperationNotAllowedException("workSession.id", "cannot change"))
        .when(service).update(eq(USER_ID), eq(wsId), any(WorkSessionUpdateRequest.class));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/work-sessions/{wsId}", TestPublicIds.workSession(wsId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isBadRequest());

    verify(service).update(eq(USER_ID), eq(wsId), any(WorkSessionUpdateRequest.class));
  }

  @Test
  void 作業セッション更新失敗_DB登録済みのtypeと異なるなら400を返すこと() throws Exception {
    // Arrange
    int wsId = 1;
    String validRequest = """
        {
          "type": "MANUAL",
          "minutes": 45
        }
        """;
    doThrow(new WorkSessionOperationNotAllowedException("workSession.type",
        "作業セッションの記録タイプは変更できません"))
        .when(service).update(eq(USER_ID), eq(wsId), any(WorkSessionUpdateRequest.class));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/work-sessions/{wsId}", TestPublicIds.workSession(wsId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isBadRequest());

    verify(service).update(eq(USER_ID), eq(wsId), any(WorkSessionUpdateRequest.class));
  }

  @Test
  void 作業セッション更新失敗_不正なリクエストボディなら400を返すこと() throws Exception {
    // Arrange
    int wsId = 1;
    String invalidRequest = """
        {
          "type": "TIMER",
          "startedAt": "2026-01-01T09:00:00"
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/work-sessions/{wsId}", TestPublicIds.workSession(wsId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void 作業セッション更新失敗_終了日時が開始日時より前なら400を返すこと() throws Exception {
    // Arrange
    int wsId = 1;
    String invalidRequest = """
        {
          "type": "TIMER",
          "startedAt": "2026-01-01T10:00:00",
          "endedAt": "2026-01-01T09:00:00"
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/work-sessions/{wsId}", TestPublicIds.workSession(wsId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void 作業セッション更新失敗_日時形式が不正なら独自エラーレスポンスを返すこと()
      throws Exception {
    // Arrange
    int wsId = 1;
    String invalidRequest = """
        {
          "type": "TIMER",
          "startedAt": "invalid-date-time",
          "endedAt": "2026-01-01T10:00:00+09:00"
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/work-sessions/{wsId}", TestPublicIds.workSession(wsId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value("payload format error"))
        .andExpect(jsonPath("$.errors[0].field").value("startedAt"))
        .andExpect(jsonPath("$.errors[0].message").value("JSONの形式または値が不正です"));

    verifyNoInteractions(service);
  }

  @Test
  void 作業セッション更新失敗_未知のDB制約違反なら詳細を公開せず500を返すこと() throws Exception {
    // Arrange
    int wsId = 1;
    String validRequest = """
        {
          "type": "MANUAL",
          "minutes": 45
        }
        """;
    SQLException cause = new SQLException("sensitive database detail", "23514");
    doThrow(new DataIntegrityViolationException("database operation failed", cause))
        .when(service).update(eq(USER_ID), eq(wsId), any(WorkSessionUpdateRequest.class));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/work-sessions/{wsId}", TestPublicIds.workSession(wsId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message").value("internal server error"))
        .andExpect(jsonPath("$.errors").isEmpty())
        .andExpect(content().string(org.hamcrest.Matchers.not(
            org.hamcrest.Matchers.containsString("sensitive database detail"))));
  }

  @Test
  void 作業セッション削除成功_204と空のレスポンスボディを返しサービスを呼び出すこと()
      throws Exception {
    // Arrange
    int wsId = 1;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.delete("/work-sessions/{wsId}", TestPublicIds.workSession(wsId)))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    verify(service).delete(USER_ID, wsId);
  }

  @Test
  void 作業セッション削除失敗_対象が存在しないなら404を返すこと() throws Exception {
    // Arrange
    int wsId = 999;
    doThrow(new TargetNotFoundException("workSession.id", "work session not found"))
        .when(service).delete(USER_ID, wsId);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.delete("/work-sessions/{wsId}", TestPublicIds.workSession(wsId)))
        .andExpect(status().isNotFound());

    verify(service).delete(USER_ID, wsId);
  }

  @Test
  void 作業セッション削除失敗_完了済みタスク配下なら400を返すこと() throws Exception {
    // Arrange
    int wsId = 1;
    doThrow(new WorkSessionOperationNotAllowedException("workSession.id", "cannot change"))
        .when(service).delete(USER_ID, wsId);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.delete("/work-sessions/{wsId}", TestPublicIds.workSession(wsId)))
        .andExpect(status().isBadRequest());

    verify(service).delete(USER_ID, wsId);
  }

  @Test
  void 作業セッション削除失敗_パス変数の形式が不正なら404を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.delete("/work-sessions/invalid-id"))
        .andExpect(status().isNotFound());

    verifyNoInteractions(service);
  }

}
