package com.kiborisaway.tasktimetracker.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.data.dto.memo.MemoRequest;
import com.kiborisaway.tasktimetracker.data.entity.Memo;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorDetailsBuilder;
import com.kiborisaway.tasktimetracker.security.JsonAuthenticationEntryPoint;
import com.kiborisaway.tasktimetracker.service.MemoService;
import com.kiborisaway.tasktimetracker.support.TestPublicIds;
import com.kiborisaway.tasktimetracker.support.WebMvcTestSecuritySupportConfig;
import com.kiborisaway.tasktimetracker.support.WithMockAuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(MemoController.class)
@WithMockAuthenticatedUser
@Import({WebMvcTestSecuritySupportConfig.class, JsonAuthenticationEntryPoint.class})
class MemoControllerTest {

  private static final int USER_ID = 1;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private MemoService service;

  @MockitoBean
  private ErrorDetailsBuilder errorDetailsBuilder;

  private static final String VALID_REQUEST = """
      {
        "comment": "メモコメント"
      }
      """;

  @Test
  void プロジェクトメモ登録成功_201と登録済みデータを返すこと() throws Exception {
    // Arrange
    int projectId = 1;
    Memo response = new Memo(10, projectId, null, null, "メモコメント");
    org.mockito.Mockito.when(
            service.registerInProject(eq(USER_ID), eq(projectId), any(MemoRequest.class)))
        .thenReturn(response);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/projects/{pId}/memo", TestPublicIds.project(projectId))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_REQUEST))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(TestPublicIds.memo(10)))
        .andExpect(jsonPath("$.comment").value("メモコメント"))
        .andExpect(jsonPath("$.projectId").doesNotExist());

    verify(service).registerInProject(eq(USER_ID), eq(projectId), any(MemoRequest.class));
  }

  @Test
  void タスクグループメモ登録成功_201と登録済みデータを返すこと() throws Exception {
    // Arrange
    int taskGroupId = 1;
    Memo response = new Memo(10, null, taskGroupId, null, "メモコメント");
    org.mockito.Mockito.when(
            service.registerInTaskGroup(eq(USER_ID), eq(taskGroupId), any(MemoRequest.class)))
        .thenReturn(response);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/task-groups/{tgId}/memo", TestPublicIds.taskGroup(taskGroupId))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_REQUEST))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(TestPublicIds.memo(10)))
        .andExpect(jsonPath("$.comment").value("メモコメント"))
        .andExpect(jsonPath("$.taskGroupId").doesNotExist());

    verify(service).registerInTaskGroup(eq(USER_ID), eq(taskGroupId), any(MemoRequest.class));
  }

  @Test
  void タスクメモ登録成功_201と登録済みデータを返すこと() throws Exception {
    // Arrange
    int taskId = 1;
    Memo response = new Memo(10, null, null, taskId, "メモコメント");
    org.mockito.Mockito.when(
            service.registerInTask(eq(USER_ID), eq(taskId), any(MemoRequest.class)))
        .thenReturn(response);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/tasks/{taskId}/memo", TestPublicIds.task(taskId))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_REQUEST))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(TestPublicIds.memo(10)))
        .andExpect(jsonPath("$.comment").value("メモコメント"))
        .andExpect(jsonPath("$.taskId").doesNotExist());

    verify(service).registerInTask(eq(USER_ID), eq(taskId), any(MemoRequest.class));
  }

  @Test
  void メモ登録失敗_不正なリクエストボディなら400を返すこと() throws Exception {
    // Arrange
    String invalidRequest = """
        {
          "comment": ""
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/projects/" + TestPublicIds.project(1) + "/memo")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());
  }

  @Test
  void メモ登録失敗_親が存在しないなら404を返すこと() throws Exception {
    // Arrange
    int projectId = 999;
    org.mockito.Mockito.when(
            service.registerInProject(eq(USER_ID), eq(projectId), any(MemoRequest.class)))
        .thenThrow(new TargetNotFoundException("project.id", "project not found"));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/projects/{pId}/memo", TestPublicIds.project(projectId))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_REQUEST))
        .andExpect(status().isNotFound());

    verify(service).registerInProject(eq(USER_ID), eq(projectId), any(MemoRequest.class));
  }

  @Test
  void メモ更新成功_200とメッセージを返すこと() throws Exception {
    // Arrange
    int id = 1;
    org.mockito.Mockito.when(service.update(eq(USER_ID), eq(id), any(MemoRequest.class)))
        .thenReturn(new Memo(id, null, null, 1, "メモ"));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/memo/{id}", TestPublicIds.memo(id))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_REQUEST))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(TestPublicIds.memo(id)))
        .andExpect(jsonPath("$.comment").value("メモ"));

    verify(service).update(eq(USER_ID), eq(id), any(MemoRequest.class));
  }

  @Test
  void メモ更新失敗_対象が存在しないなら404を返すこと() throws Exception {
    // Arrange
    int id = 999;
    doThrow(new TargetNotFoundException("memo.id", "memo not found"))
        .when(service).update(eq(USER_ID), eq(id), any(MemoRequest.class));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/memo/{id}", TestPublicIds.memo(id))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_REQUEST))
        .andExpect(status().isNotFound());

    verify(service).update(eq(USER_ID), eq(id), any(MemoRequest.class));
  }

  @Test
  void メモ削除成功_204と空のレスポンスボディを返すこと() throws Exception {
    // Arrange
    int id = 1;
    doNothing().when(service).delete(USER_ID, id);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.delete("/memo/{id}", TestPublicIds.memo(id)).with(csrf()))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    verify(service).delete(USER_ID, id);
  }

  @Test
  void メモ削除失敗_対象が存在しないなら404を返すこと() throws Exception {
    // Arrange
    int id = 999;
    doThrow(new TargetNotFoundException("memo.id", "memo not found"))
        .when(service).delete(USER_ID, id);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.delete("/memo/{id}", TestPublicIds.memo(id)).with(csrf()))
        .andExpect(status().isNotFound());

    verify(service).delete(USER_ID, id);
  }
}
