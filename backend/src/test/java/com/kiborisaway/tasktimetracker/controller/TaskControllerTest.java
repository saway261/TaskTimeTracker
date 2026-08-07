package com.kiborisaway.tasktimetracker.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.data.dto.task.TaskCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.task.TaskResponse;
import com.kiborisaway.tasktimetracker.data.dto.task.TaskUpdateEstimatedMinutesRequest;
import com.kiborisaway.tasktimetracker.data.dto.task.TaskUpdatePropertyRequest;
import com.kiborisaway.tasktimetracker.data.entity.Task;
import com.kiborisaway.tasktimetracker.exception.EstimateMinutesUpdateNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.exception.TaskFinishNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorDetailsBuilder;
import com.kiborisaway.tasktimetracker.service.TaskService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TaskService service;

  @MockitoBean
  private ErrorDetailsBuilder errorDetailsBuilder;

  @Test
  void プロジェクト内タスク一覧検索成功_条件未指定でサービスを呼び出し200を返すこと()
      throws Exception {
    // Act & Assert
    int pId = 1;
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/{pId}/tasks", pId))
        .andExpect(status().isOk());

    verify(service).findAllInProjectByCondition(pId, null);
  }

  @Test
  void プロジェクト内タスク一覧検索成功_isFinishedがtrueでサービスを呼び出し200を返すこと()
      throws Exception {
    // Act & Assert
    int pId = 1;
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/{pId}/tasks", pId)
            .param("isFinished", "true"))
        .andExpect(status().isOk());

    verify(service).findAllInProjectByCondition(pId, true);
  }

  @Test
  void プロジェクト内タスク一覧検索失敗_isFinishedが真偽値でなければ400を返すこと()
      throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/1/tasks")
            .param("isFinished", "invalid"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void プロジェクト内タスク一覧検索失敗_存在しないプロジェクトIDを指定した場合は404を返すこと()
      throws Exception {
    // Arrange
    int pId = 999;
    when(service.findAllInProjectByCondition(eq(pId), any())).thenThrow(
        new TargetNotFoundException("project.id",
            "指定したIDのプロジェクトは見つかりませんでした"));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/{pId}/tasks", pId))
        .andExpect(status().isNotFound());
  }

  @Test
  void タスクグループ内タスク一覧検索成功_条件未指定でサービスを呼び出し200を返すこと()
      throws Exception {
    // Act & Assert
    int tgId = 1;
    mockMvc.perform(MockMvcRequestBuilders.get("/task-groups/{tgId}/tasks", tgId))
        .andExpect(status().isOk());

    verify(service).findAllInTaskGroupByCondition(tgId, null);
  }

  @Test
  void タスクグループ内タスク一覧検索成功_isFinishedがfalseでサービスを呼び出し200を返すこと()
      throws Exception {
    // Act & Assert
    int tgId = 1;
    mockMvc.perform(MockMvcRequestBuilders.get("/task-groups/{tgId}/tasks", tgId)
            .param("isFinished", "false"))
        .andExpect(status().isOk());

    verify(service).findAllInTaskGroupByCondition(tgId, false);
  }

  @Test
  void タスクグループ内タスク一覧検索失敗_isFinishedが真偽値でなければ400を返すこと()
      throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/task-groups/1/tasks")
            .param("isFinished", "invalid"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void タスクグループ内タスク一覧検索失敗_存在しないタスクグループIDを指定した場合は404を返すこと()
      throws Exception {
    // Arrange
    int tgId = 999;
    when(service.findAllInTaskGroupByCondition(eq(tgId), any())).thenThrow(
        new TargetNotFoundException("taskGroup.id",
            "指定したIDのタスクグループは見つかりませんでした"));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/task-groups/{tgId}/tasks", tgId))
        .andExpect(status().isNotFound());
  }

  @Test
  void タスク単体取得成功_200と対象データを返すこと() throws Exception {
    // Arrange
    int taskId = 1;
    Task task = new Task();
    task.setId(taskId);
    when(service.findById(taskId)).thenReturn(new TaskResponse(task, List.of()));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/tasks/{taskId}", taskId))
        .andExpect(status().isOk());

    verify(service).findById(taskId);
  }

  @Test
  void タスク単体取得失敗_パス変数が0以下なら400を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/tasks/0"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void タスク単体取得失敗_対象が存在しないなら404を返すこと() throws Exception {
    // Arrange
    when(service.findById(999)).thenThrow(
        new TargetNotFoundException("task.id", "task not found"));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/tasks/999"))
        .andExpect(status().isNotFound());

    verify(service).findById(999);
  }

  @Test
  void タスク単体取得失敗_パス変数の型が不正なら400を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/tasks/abc"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void プロジェクト直下タスク登録成功_201を返しプロジェクトIDだけを設定してサービスを呼び出すこと()
      throws Exception {
    // Arrange
    int pId = 1;
    Task response = new Task();
    response.setId(10);
    response.setProjectId(pId);
    when(service.register(eq(pId), isNull(), any(TaskCreateRequest.class)))
        .thenReturn(new TaskResponse(response, List.of()));
    String validRequest = """
        {
          "taskGroupId": 999,
          "title": "画面設計",
          "description": "ワイヤーフレームを作成する",
          "estimatedMinutes": 180
        }
        """;
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/projects/{pId}/tasks", pId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isCreated());

    verify(service).register(eq(pId), isNull(), any(TaskCreateRequest.class));
  }

  @Test
  void タスクグループ内タスク登録成功_201を返しタスクグループIDだけを設定してサービスを呼び出すこと()
      throws Exception {
    // Arrange
    int tgId = 1;
    Task response = new Task();
    response.setId(10);
    response.setTaskGroupId(tgId);
    when(service.register(isNull(), eq(tgId), any(TaskCreateRequest.class)))
        .thenReturn(new TaskResponse(response, List.of()));
    String validRequest = """
        {
          "projectId": 999,
          "title": "API実装",
          "description": "タスクAPIを実装する",
          "estimatedMinutes": 120
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/task-groups/{tgId}/tasks", tgId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isCreated());

    verify(service).register(isNull(), eq(tgId), any(TaskCreateRequest.class));
  }

  @Test
  void タスク登録失敗_親が存在しないなら404を返すこと() throws Exception {
    // Arrange
    int pId = 999;
    String validRequest = """
        {
          "title": "タスク",
          "description": "説明",
          "estimatedMinutes": 60
        }
        """;
    when(service.register(eq(pId), isNull(), any(TaskCreateRequest.class))).thenThrow(
        new TargetNotFoundException("project.id",
            "指定したIDの親項目は見つかりませんでした"));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/projects/{pId}/tasks", pId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isNotFound());

    verify(service).register(eq(pId), isNull(), any(TaskCreateRequest.class));
  }

  @Test
  void タスク名説明更新成功_200とメッセージを返しIDを設定してサービスを呼び出すこと()
      throws Exception {
    // Arrange
    int taskId = 1;
    String validRequest = """
        {
          "title": "更新タイトル",
          "description": "更新説明"
        }
        """;
    Task updated = new Task(taskId, 1, null, "更新タイトル", "更新説明", 60,
        null, null, null, null, null);
    when(service.updateProperty(eq(taskId), any(TaskUpdatePropertyRequest.class)))
        .thenReturn(new TaskResponse(updated, List.of()));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/{taskId}", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(taskId))
        .andExpect(jsonPath("$.title").value("更新タイトル"));

    verify(service).updateProperty(eq(taskId), any(TaskUpdatePropertyRequest.class));
  }

  @Test
  void タスク名説明更新失敗_対象が存在しないなら404を返すこと() throws Exception {
    // Arrange
    int taskId = 999;
    String validRequest = """
        {
          "title": "更新タイトル",
          "description": "更新説明"
        }
        """;
    doThrow(new TargetNotFoundException("task.id", "task not found"))
        .when(service).updateProperty(eq(taskId), any(TaskUpdatePropertyRequest.class));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/{taskId}", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isNotFound());

    verify(service).updateProperty(eq(taskId), any(TaskUpdatePropertyRequest.class));
  }

  @Test
  void 見積もり作業時間更新成功_200とメッセージを返すこと() throws Exception {
    // Arrange
    int taskId = 1;
    String validRequest = """
        {
          "estimatedMinutes": 120
        }
        """;
    Task updated = new Task(taskId, 1, null, "タスク", "説明", 120,
        null, null, null, null, null);
    when(service.updateEstimateMinutes(eq(taskId), any(TaskUpdateEstimatedMinutesRequest.class)))
        .thenReturn(new TaskResponse(updated, List.of()));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/{taskId}/estimated-minutes", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(taskId))
        .andExpect(jsonPath("$.estimatedMinutes").value(120));

    verify(service).updateEstimateMinutes(eq(taskId), any(TaskUpdateEstimatedMinutesRequest.class));
  }

  @Test
  void 見積もり作業時間更新失敗_WorkSessionが存在するなら400を返すこと() throws Exception {
    // Arrange
    int taskId = 1;
    String validRequest = """
        {
          "estimatedMinutes": 120
        }
        """;
    doThrow(new EstimateMinutesUpdateNotAllowedException("task.id",
        "作業セッションが存在するタスクの見積もり作業時間は変更できません"))
        .when(service).updateEstimateMinutes(eq(taskId), any(TaskUpdateEstimatedMinutesRequest.class));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/{taskId}/estimated-minutes", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isBadRequest());

    verify(service).updateEstimateMinutes(eq(taskId), any(TaskUpdateEstimatedMinutesRequest.class));
  }

  @Test
  void 見積もり作業時間更新失敗_タスクが完了済みなら400を返すこと() throws Exception {
    // Arrange
    int taskId = 3;
    String validRequest = """
        {
          "estimatedMinutes": 120
        }
        """;
    doThrow(new EstimateMinutesUpdateNotAllowedException("task.id",
        "完了済みタスクの見積もり作業時間は変更できません"))
        .when(service).updateEstimateMinutes(eq(taskId), any(TaskUpdateEstimatedMinutesRequest.class));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/{taskId}/estimated-minutes", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isBadRequest());

    verify(service).updateEstimateMinutes(eq(taskId), any(TaskUpdateEstimatedMinutesRequest.class));
  }

  @Test
  void 見積もり作業時間更新失敗_対象が存在しないなら404を返すこと() throws Exception {
    // Arrange
    int taskId = 999;
    String validRequest = """
        {
          "estimatedMinutes": 120
        }
        """;
    doThrow(new TargetNotFoundException("task.id", "task not found"))
        .when(service).updateEstimateMinutes(eq(taskId), any(TaskUpdateEstimatedMinutesRequest.class));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/{taskId}/estimated-minutes", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isNotFound());

    verify(service).updateEstimateMinutes(eq(taskId), any(TaskUpdateEstimatedMinutesRequest.class));
  }

  @Test
  void 見積もり作業時間更新失敗_パス変数が0以下なら400を返すこと() throws Exception {
    // Arrange
    String validRequest = """
        {
          "estimatedMinutes": 120
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/0/estimated-minutes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void 見積もり作業時間更新失敗_不正なリクエストボディなら400を返すこと() throws Exception {
    // Arrange
    int taskId = 1;
    String invalidRequest = """
        {
          "estimatedMinutes": 0
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/{taskId}/estimated-minutes", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void 所属変更成功_200とメッセージを返すこと() throws Exception {
    // Arrange
    int taskId = 1;
    int taskGroupId = 2;
    String validRequest = """
        {
          "taskGroupId": 2
        }
        """;
    Task updated = new Task(taskId, null, taskGroupId, "タスク", "説明", 60,
        null, null, null, null, null);
    when(service.updateParent(taskId, null, taskGroupId))
        .thenReturn(new TaskResponse(updated, List.of()));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/{taskId}/parent", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.taskGroupId").value(taskGroupId))
        .andExpect(jsonPath("$.projectId").doesNotExist());

    verify(service).updateParent(taskId, null, taskGroupId);
  }

  @Test
  void 所属変更失敗_対象が存在しないなら404を返すこと() throws Exception {
    // Arrange
    int taskId = 999;
    int taskGroupId = 2;
    String validRequest = """
        {
          "taskGroupId": 2
        }
        """;
    doThrow(new TargetNotFoundException("task.id", "task not found"))
        .when(service).updateParent(taskId, null, taskGroupId);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/{taskId}/parent", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isNotFound());

    verify(service).updateParent(taskId, null, taskGroupId);
  }

  @Test
  void 所属変更失敗_taskGroupIdがnullなら400を返すこと() throws Exception {
    // Arrange
    int taskId = 1;
    String invalidRequest = """
        {
          "taskGroupId": null
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/{taskId}/parent", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void 所属変更失敗_taskGroupIdが0以下なら400を返すこと() throws Exception {
    // Arrange
    int taskId = 1;
    String invalidRequest = """
        {
          "taskGroupId": 0
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/{taskId}/parent", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void 所属変更失敗_パス変数が0以下なら400を返すこと() throws Exception {
    // Arrange
    String validRequest = """
        {
          "taskGroupId": 2
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/0/parent")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void プロジェクト直下への所属変更成功_200とメッセージを返すこと() throws Exception {
    // Arrange
    int taskId = 1;
    int projectId = 1;
    String validRequest = """
        {
          "projectId": 1
        }
        """;
    Task updated = new Task(taskId, projectId, null, "タスク", "説明", 60,
        null, null, null, null, null);
    when(service.updateParent(taskId, projectId, null))
        .thenReturn(new TaskResponse(updated, List.of()));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/{taskId}/parent", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.projectId").value(projectId))
        .andExpect(jsonPath("$.taskGroupId").doesNotExist());

    verify(service).updateParent(taskId, projectId, null);
  }

  @Test
  void プロジェクト直下への所属変更失敗_対象が存在しないなら404を返すこと() throws Exception {
    // Arrange
    int taskId = 999;
    int projectId = 1;
    String validRequest = """
        {
          "projectId": 1
        }
        """;
    doThrow(new TargetNotFoundException("task.id", "task not found"))
        .when(service).updateParent(taskId, projectId, null);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/{taskId}/parent", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isNotFound());

    verify(service).updateParent(taskId, projectId, null);
  }

  @Test
  void プロジェクト直下への所属変更失敗_projectIdがnullなら400を返すこと() throws Exception {
    // Arrange
    int taskId = 1;
    String invalidRequest = """
        {
          "projectId": null
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/{taskId}/parent", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void プロジェクト直下への所属変更失敗_projectIdが0以下なら400を返すこと() throws Exception {
    // Arrange
    int taskId = 1;
    String invalidRequest = """
        {
          "projectId": 0
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/{taskId}/parent", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void 所属変更失敗_projectIdとtaskGroupIdを両方指定したら400を返すこと() throws Exception {
    // Arrange
    int taskId = 1;
    String invalidRequest = """
        {
          "projectId": 1,
          "taskGroupId": 2
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/{taskId}/parent", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void 完了状態更新成功_isFinishedにtrueを指定すると200とメッセージを返すこと() throws Exception {
    // Arrange
    int taskId = 1;
    String validRequest = """
        {
          "isFinished": true
        }
        """;
    Task finished = new Task(taskId, 1, null, "タスク", "説明", 60,
        null, java.time.LocalDateTime.of(2026, 1, 1, 10, 0), 70, 10, 16.666);
    when(service.updateFinished(taskId, true))
        .thenReturn(new TaskResponse(finished, List.of()));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/{taskId}/finished", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(taskId))
        .andExpect(jsonPath("$.actualMinutesCached").value(70));

    verify(service).updateFinished(taskId, true);
  }

  @Test
  void 完了状態更新成功_isFinishedにfalseを指定すると200とメッセージを返すこと() throws Exception {
    // Arrange
    int taskId = 1;
    String validRequest = """
        {
          "isFinished": false
        }
        """;
    Task unfinished = new Task(taskId, 1, null, "タスク", "説明", 60,
        null, null, null, null, null);
    when(service.updateFinished(taskId, false))
        .thenReturn(new TaskResponse(unfinished, List.of()));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/{taskId}/finished", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(taskId))
        .andExpect(jsonPath("$.finishedAt").doesNotExist());

    verify(service).updateFinished(taskId, false);
  }

  @Test
  void 完了状態更新失敗_対象が存在しないなら404を返すこと() throws Exception {
    // Arrange
    int taskId = 999;
    String validRequest = """
        {
          "isFinished": true
        }
        """;
    doThrow(new TargetNotFoundException("task.id", "task not found"))
        .when(service).updateFinished(taskId, true);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/{taskId}/finished", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isNotFound());

    verify(service).updateFinished(taskId, true);
  }

  @Test
  void 完了状態更新失敗_未終了WorkSessionが存在するなら400を返すこと() throws Exception {
    // Arrange
    int taskId = 1;
    String validRequest = """
        {
          "isFinished": true
        }
        """;
    doThrow(new TaskFinishNotAllowedException("task.id", "cannot finish"))
        .when(service).updateFinished(taskId, true);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/{taskId}/finished", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isBadRequest());

    verify(service).updateFinished(taskId, true);
  }

  @Test
  void 完了状態更新失敗_isFinishedがnullなら400を返すこと() throws Exception {
    // Arrange
    int taskId = 1;
    String invalidRequest = """
        {
          "isFinished": null
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/{taskId}/finished", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void 完了状態更新失敗_パス変数が0以下なら400を返すこと() throws Exception {
    // Arrange
    String validRequest = """
        {
          "isFinished": true
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/tasks/0/finished")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void 削除成功_204と空のレスポンスボディを返すこと() throws Exception {
    // Arrange
    int taskId = 1;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.delete("/tasks/{taskId}", taskId))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    verify(service).deleteById(taskId);
  }

  @Test
  void 削除失敗_対象が存在しないなら404を返すこと() throws Exception {
    // Arrange
    int taskId = 999;
    doThrow(new TargetNotFoundException("task.id", "task not found"))
        .when(service).deleteById(taskId);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.delete("/tasks/{taskId}", taskId))
        .andExpect(status().isNotFound());

    verify(service).deleteById(taskId);
  }

  @Test
  void 削除失敗_パス変数の型が不正なら400を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.delete("/tasks/abc"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

}
