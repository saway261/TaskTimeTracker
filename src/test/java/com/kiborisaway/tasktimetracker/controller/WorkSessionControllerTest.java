package com.kiborisaway.tasktimetracker.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.data.WorkSession;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.exception.WorkSessionCreateNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.WorkSessionEndNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorDetailsBuilder;
import com.kiborisaway.tasktimetracker.service.WorkSessionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(WorkSessionController.class)
class WorkSessionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private WorkSessionService service;

  @MockitoBean
  private ErrorDetailsBuilder errorDetailsBuilder;

  @Test
  void タスク作業時間合計取得成功_200を返しサービスを呼び出すこと() throws Exception {
    // Arrange
    int taskId = 1;
    when(service.getTaskActualTotalTime(taskId)).thenReturn(75);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/tasks/{taskId}/work-sessions/total-minutes",
            taskId))
        .andExpect(status().isOk());

    verify(service).getTaskActualTotalTime(taskId);
  }

  @Test
  void タスク作業時間合計取得失敗_パス変数が0以下なら400を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/tasks/0/work-sessions/total-minutes"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void タスクグループ作業時間合計取得成功_200を返しサービスを呼び出すこと() throws Exception {
    // Arrange
    int tgId = 1;
    when(service.getTaskGroupActualTotalTime(tgId)).thenReturn(120);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get(
            "/task-groups/{tgId}/work-sessions/total-minutes", tgId))
        .andExpect(status().isOk());

    verify(service).getTaskGroupActualTotalTime(tgId);
  }

  @Test
  void プロジェクト作業時間合計取得成功_200を返しサービスを呼び出すこと() throws Exception {
    // Arrange
    int pId = 1;
    when(service.getProjectActualTotalTime(pId)).thenReturn(180);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/{pId}/work-sessions/total-minutes",
            pId))
        .andExpect(status().isOk());

    verify(service).getProjectActualTotalTime(pId);
  }

  @Test
  void タスク内作業セッション一覧取得成功_200を返しサービスを呼び出すこと() throws Exception {
    // Arrange
    int taskId = 1;
    WorkSession workSession = new WorkSession();
    workSession.setId(1);
    workSession.setTaskId(taskId);
    when(service.getAllInTask(taskId)).thenReturn(List.of(workSession));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/tasks/{taskId}/work-sessions", taskId))
        .andExpect(status().isOk());

    verify(service).getAllInTask(taskId);
  }

  @Test
  void 作業セッション単体取得成功_200を返しサービスを呼び出すこと() throws Exception {
    // Arrange
    int wsId = 1;
    WorkSession workSession = new WorkSession();
    workSession.setId(wsId);
    when(service.get(wsId)).thenReturn(workSession);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/work-sessions/{wsId}", wsId))
        .andExpect(status().isOk());

    verify(service).get(wsId);
  }

  @Test
  void 作業セッション単体取得失敗_対象が存在しないなら404を返すこと() throws Exception {
    // Arrange
    int wsId = 999;
    when(service.get(wsId)).thenThrow(
        new TargetNotFoundException("workSession.id", "work session not found"));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/work-sessions/{wsId}", wsId))
        .andExpect(status().isNotFound());

    verify(service).get(wsId);
  }

  @Test
  void 作業セッション単体取得失敗_パス変数の型が不正なら400を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/work-sessions/abc"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void 作業セッション登録成功_200を返しタスクIDとリクエストをサービスに渡すこと() throws Exception {
    // Arrange
    int taskId = 1;
    WorkSession response = new WorkSession();
    response.setId(10);
    response.setTaskId(taskId);
    when(service.create(eq(taskId), any(WorkSession.class))).thenReturn(response);
    String validRequest = """
        {
          "type": "MANUAL",
          "minutes": 30
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/tasks/{taskId}/work-sessions", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk());

    verify(service).create(eq(taskId), any(WorkSession.class));
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
    when(service.create(eq(taskId), any(WorkSession.class))).thenThrow(
        new TargetNotFoundException("task.id", "task not found"));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/tasks/{taskId}/work-sessions", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isNotFound());

    verify(service).create(eq(taskId), any(WorkSession.class));
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
    when(service.create(eq(taskId), any(WorkSession.class))).thenThrow(
        new WorkSessionCreateNotAllowedException("task.id", "cannot create"));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/tasks/{taskId}/work-sessions", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isBadRequest());

    verify(service).create(eq(taskId), any(WorkSession.class));
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
    mockMvc.perform(MockMvcRequestBuilders.post("/tasks/{taskId}/work-sessions", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void 作業セッション終了成功_200とメッセージを返しサービスを呼び出すこと() throws Exception {
    // Arrange
    int wsId = 1;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/work-sessions/{wsId}/end", wsId))
        .andExpect(status().isOk())
        .andExpect(content().string("作業セッションを終了しました"));

    verify(service).setEnd(wsId);
  }

  @Test
  void 作業セッション終了失敗_終了できない状態なら400を返すこと() throws Exception {
    // Arrange
    int wsId = 1;
    doThrow(new WorkSessionEndNotAllowedException("workSession.id", "cannot end"))
        .when(service).setEnd(wsId);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/work-sessions/{wsId}/end", wsId))
        .andExpect(status().isBadRequest());

    verify(service).setEnd(wsId);
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

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/work-sessions/{wsId}", wsId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk())
        .andExpect(content().string("作業セッションを更新しました"));

    verify(service).update(eq(wsId), any(WorkSession.class));
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
        .when(service).update(eq(wsId), any(WorkSession.class));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/work-sessions/{wsId}", wsId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isNotFound());

    verify(service).update(eq(wsId), any(WorkSession.class));
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
    mockMvc.perform(MockMvcRequestBuilders.patch("/work-sessions/{wsId}", wsId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void 作業セッション削除成功_200とメッセージを返しサービスを呼び出すこと() throws Exception {
    // Arrange
    int wsId = 1;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.delete("/work-sessions/{wsId}", wsId))
        .andExpect(status().isOk())
        .andExpect(content().string("作業セッションを削除しました"));

    verify(service).delete(wsId);
  }

  @Test
  void 作業セッション削除失敗_対象が存在しないなら404を返すこと() throws Exception {
    // Arrange
    int wsId = 999;
    doThrow(new TargetNotFoundException("workSession.id", "work session not found"))
        .when(service).delete(wsId);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.delete("/work-sessions/{wsId}", wsId))
        .andExpect(status().isNotFound());

    verify(service).delete(wsId);
  }

  @Test
  void 作業セッション削除失敗_パス変数が0以下なら400を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.delete("/work-sessions/0"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

}
