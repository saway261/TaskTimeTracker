package com.kiborisaway.tasktimetracker.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.data.dto.task_group.TaskGroupCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.task_group.TaskGroupResponse;
import com.kiborisaway.tasktimetracker.data.dto.task_group.TaskGroupUpdateRequest;
import com.kiborisaway.tasktimetracker.data.entity.TaskGroup;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.exception.TaskGroupFinishNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorDetailsBuilder;
import com.kiborisaway.tasktimetracker.security.JsonAuthenticationEntryPoint;
import com.kiborisaway.tasktimetracker.service.TaskGroupService;
import com.kiborisaway.tasktimetracker.support.TestPublicIds;
import com.kiborisaway.tasktimetracker.support.WebMvcTestSecuritySupportConfig;
import com.kiborisaway.tasktimetracker.support.WithMockAuthenticatedUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(TaskGroupController.class)
@WithMockAuthenticatedUser
@Import({WebMvcTestSecuritySupportConfig.class, JsonAuthenticationEntryPoint.class})
class TaskGroupControllerTest {

  private static final int USER_ID = 1;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TaskGroupService service;

  @MockitoBean
  private ErrorDetailsBuilder errorDetailsBuilder;

  @Test
  void タスクグループ一覧検索成功_条件未指定でサービスを呼び出し200を返すこと() throws Exception {
    // Act & Assert
    int pId = 1;
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/{pId}/task-groups", TestPublicIds.project(pId)))
        .andExpect(status().isOk());

    verify(service).findAllByCondition(USER_ID, pId, null);
  }

  @Test
  void タスクグループ一覧検索成功_isFinishedがfalseでサービスを呼び出し200を返すこと()
      throws Exception {
    // Act & Assert
    int pId = 1;
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/{pId}/task-groups", TestPublicIds.project(pId))
            .param("isFinished", "false"))
        .andExpect(status().isOk());

    verify(service).findAllByCondition(USER_ID, pId, false);
  }

  @Test
  void タスクグループ一覧検索成功_isFinishedがtrueでサービスを呼び出し200を返すこと()
      throws Exception {
    // Act & Assert
    int pId = 1;
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/{pId}/task-groups", TestPublicIds.project(pId))
            .param("isFinished", "true"))
        .andExpect(status().isOk());

    verify(service).findAllByCondition(USER_ID, pId, true);
  }

  @Test
  void タスクグループ一覧検索失敗_isFinishedが真偽値でなければ400を返すこと() throws Exception {
    // Act & Assert
    int pId = 1;
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/{pId}/task-groups", TestPublicIds.project(pId))
            .param("isFinished", "invalid"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void タスクグループ一覧検索失敗_存在しないプロジェクトIDを指定した場合は404を返すこと()
      throws Exception {
    // Arrange
    int pId = 999;
    when(service.findAllByCondition(eq(USER_ID), eq(pId), any())).thenThrow(
        new TargetNotFoundException("project.id",
            "指定したIDのプロジェクトは見つかりませんでした"));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/{pId}/task-groups", TestPublicIds.project(pId)))
        .andExpect(status().isNotFound());

  }

  @Test
  void タスクグループ単体取得成功_200と対象データを返すこと() throws Exception {
    // Arrange
    int tgId = 1;
    TaskGroup tg = new TaskGroup();
    tg.setId(tgId);
    tg.setProjectId(1);
    when(service.findById(USER_ID, tgId)).thenReturn(new TaskGroupResponse(tg, List.of()));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/task-groups/{tgId}", TestPublicIds.taskGroup(tgId)))
        .andExpect(status().isOk());

    verify(service).findById(USER_ID, tgId);
  }

  @Test
  void タスクグループ単体取得失敗_パス変数の形式が不正なら404を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/task-groups/invalid-id"))
        .andExpect(status().isNotFound());
  }

  @Test
  void タスクグループ単体取得失敗_対象が存在しないなら404を返すこと() throws Exception {
    // Arrange
    when(service.findById(USER_ID, 999)).thenThrow(
        new TargetNotFoundException("id", "project not found"));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/task-groups/{tgId}", TestPublicIds.taskGroup(999)))
        .andExpect(status().isNotFound());

    verify(service).findById(USER_ID, 999);
  }

  @Test
  void タスクグループ登録成功_201と登録済みデータを返すこと() throws Exception {
    // Arrange
    int pId = 1;
    TaskGroup response = new TaskGroup();
    response.setId(10);
    response.setProjectId(pId);
    response.setTitle("タスクグループ1");
    response.setDescription("説明");
    when(service.register(eq(USER_ID), eq(pId), any(TaskGroupCreateRequest.class)))
        .thenReturn(new TaskGroupResponse(response, List.of()));
    String validRequest = """
        {
            "title" : "タスクグループ1",
            "description" : "説明"
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/projects/{pId}/task-groups", TestPublicIds.project(pId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isCreated());

    verify(service).register(eq(USER_ID), eq(pId), any(TaskGroupCreateRequest.class));
  }

  @Test
  void タスクグループ登録失敗_存在しないプロジェクトを指定した場合404を返すこと() throws Exception {
    // Arrange
    int pId = 999;
    String validRequest = """
        {
          "title": "タイトル",
          "description": "説明"
        }
        """;
    when(service.register(eq(USER_ID), eq(pId), any(TaskGroupCreateRequest.class))).thenThrow(
        new TargetNotFoundException("project.id",
            "指定したIDのプロジェクトは見つかりませんでした"));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/projects/{pId}/task-groups", TestPublicIds.project(pId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isNotFound());

    verify(service).register(eq(USER_ID), eq(pId), any(TaskGroupCreateRequest.class));
  }

  @Test
  void タスクグループ登録失敗_不正なリクエストボディなら400を返すこと() throws Exception {
    // Arrange
    int pId = 1;
    String invalidRequest = """
        {
          "title": "",
          "description": "説明"
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/projects/{pId}/task-groups", TestPublicIds.project(pId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());
  }

  @Test
  void タスクグループ更新成功_200とメッセージを返すこと() throws Exception {
    // Arrange
    int tgId = 1;
    String validRequest = """
        {
            "title" : "タスク管理アプリ開発",
            "description" : "説明を更新"
        }
        """;

    TaskGroup updated = new TaskGroup(tgId, 1, "タスク管理アプリ開発", "説明を更新", true);
    when(service.update(eq(USER_ID), eq(tgId), any(TaskGroupUpdateRequest.class)))
        .thenReturn(new TaskGroupResponse(updated, List.of()));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/task-groups/{tgId}", TestPublicIds.taskGroup(tgId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(TestPublicIds.taskGroup(tgId)))
        .andExpect(jsonPath("$.projectId").value(TestPublicIds.project(1)))
        .andExpect(jsonPath("$.isFinished").value(true));

    verify(service).update(eq(USER_ID), eq(tgId), any(TaskGroupUpdateRequest.class));
  }

  @Test
  void タスクグループ更新失敗_パス変数を渡さないと404を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/task-groups"))
        .andExpect(status().isNotFound());
  }

  @Test
  void タスクグループ更新失敗_パス変数の形式が不正なら404を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/task-groups/invalid-id"))
        .andExpect(status().isNotFound());
  }

  @Test
  void タスクグループ更新失敗_対象が存在しないなら404を返すこと() throws Exception {
    // Arrange
    int tgId = 999;
    String validRequest = """
        {
          "title": "更新タイトル",
          "description": "更新説明"
        }
        """;
    doThrow(new TargetNotFoundException("id", "project not found"))
        .when(service).update(eq(USER_ID), eq(tgId), any(TaskGroupUpdateRequest.class));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/task-groups/" + TestPublicIds.taskGroup(tgId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isNotFound());

    verify(service).update(eq(USER_ID), eq(tgId), any(TaskGroupUpdateRequest.class));
  }

  @Test
  void タスクグループ更新失敗_不正なリクエストボディなら400を返すこと() throws Exception {
    // Arrange
    int tgId = 1;
    String invalidRequest = """
        {
          "description": "更新説明"
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/task-groups/" + TestPublicIds.taskGroup(tgId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());
  }

  @Test
  void タスクグループ完了状態更新成功_200と更新後データを返すこと() throws Exception {
    // Arrange
    int tgId = 1;
    String validRequest = """
        {
          "isFinished": true
        }
        """;
    TaskGroup updated = new TaskGroup(tgId, 1, "バックエンド開発", null, true);
    when(service.updateFinished(eq(USER_ID), eq(tgId), eq(true)))
        .thenReturn(new TaskGroupResponse(updated, List.of()));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/task-groups/{tgId}/finished", TestPublicIds.taskGroup(tgId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(TestPublicIds.taskGroup(tgId)))
        .andExpect(jsonPath("$.isFinished").value(true));

    verify(service).updateFinished(USER_ID, tgId, true);
  }

  @Test
  void タスクグループ完了状態更新失敗_未完了タスクが存在する場合は400を返すこと() throws Exception {
    // Arrange
    int tgId = 1;
    String validRequest = """
        {
          "isFinished": true
        }
        """;
    doThrow(new TaskGroupFinishNotAllowedException("taskGroup.id", "未完了のタスクがあります"))
        .when(service).updateFinished(USER_ID, tgId, true);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/task-groups/{tgId}/finished", TestPublicIds.taskGroup(tgId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isBadRequest());
  }

  @Test
  void タスクグループ完了状態更新失敗_対象が存在しないなら404を返すこと() throws Exception {
    // Arrange
    int tgId = 999;
    String validRequest = """
        {
          "isFinished": true
        }
        """;
    doThrow(new TargetNotFoundException("taskGroup.id", "task group not found"))
        .when(service).updateFinished(USER_ID, tgId, true);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/task-groups/{tgId}/finished", TestPublicIds.taskGroup(tgId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isNotFound());
  }

  @Test
  void タスクグループ完了状態更新失敗_不正なリクエストボディなら400を返すこと() throws Exception {
    // Arrange
    int tgId = 1;
    String invalidRequest = "{}";

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/task-groups/{tgId}/finished", TestPublicIds.taskGroup(tgId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());
  }

}
