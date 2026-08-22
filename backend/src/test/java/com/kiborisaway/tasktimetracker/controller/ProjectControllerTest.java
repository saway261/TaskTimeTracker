package com.kiborisaway.tasktimetracker.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.data.dto.project.ProjectCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.project.ProjectResponse;
import com.kiborisaway.tasktimetracker.data.dto.project.ProjectUpdateRequest;
import com.kiborisaway.tasktimetracker.data.entity.Project;
import com.kiborisaway.tasktimetracker.exception.ProjectFinishNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorDetailsBuilder;
import com.kiborisaway.tasktimetracker.security.JsonAuthenticationEntryPoint;
import com.kiborisaway.tasktimetracker.service.ProjectService;
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

@WebMvcTest(ProjectController.class)
@WithMockAuthenticatedUser
@Import({WebMvcTestSecuritySupportConfig.class, JsonAuthenticationEntryPoint.class})
class ProjectControllerTest {

  private static final int USER_ID = 1;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ProjectService service;

  @MockitoBean
  private ErrorDetailsBuilder errorDetailsBuilder;

  @Test
  void プロジェクト一覧検索_条件未指定でサービスを呼び出し200を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/projects"))
        .andExpect(status().isOk());

    verify(service).findAllByCondition(USER_ID, null);
  }

  @Test
  void プロジェクト一覧検索_isFinishedがfalseでサービスを呼び出し200を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/projects")
            .param("isFinished", "false"))
        .andExpect(status().isOk());

    verify(service).findAllByCondition(USER_ID, false);
  }

  @Test
  void プロジェクト一覧検索_isFinishedがtrueでサービスを呼び出し200を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/projects")
            .param("isFinished", "true"))
        .andExpect(status().isOk());

    verify(service).findAllByCondition(USER_ID, true);
  }

  @Test
  void プロジェクト一覧検索_isFinishedが真偽値でなければ400を返すこと() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/projects")
            .param("isFinished", "invalid"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void プロジェクト単体取得成功_200と対象データを返すこと() throws Exception {
    // Arrange
    int id = 1;
    Project project = new Project();
    project.setId(id);
    when(service.findById(USER_ID, id)).thenReturn(new ProjectResponse(project, List.of()));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/{id}", TestPublicIds.project(id)))
        .andExpect(status().isOk());

    verify(service).findById(USER_ID, id);
  }

  @Test
  void プロジェクト単体取得失敗_パス変数の形式が不正なら404を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/invalid-id"))
        .andExpect(status().isNotFound());
  }

  @Test
  void プロジェクト単体取得失敗_対象が存在しないなら404を返すこと() throws Exception {
    // Arrange
    when(service.findById(USER_ID, 999)).thenThrow(
        new TargetNotFoundException("id", "project not found"));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/{id}", TestPublicIds.project(999)))
        .andExpect(status().isNotFound());

    verify(service).findById(USER_ID, 999);
  }

  @Test
  void プロジェクト登録成功_201と登録済みデータを返すこと() throws Exception {
    // Arrange
    Project response = new Project();
    response.setId(10);
    response.setTitle("Spring学習");
    response.setDescription("REST APIを作る");
    when(service.register(eq(USER_ID), any(ProjectCreateRequest.class)))
        .thenReturn(new ProjectResponse(response, List.of()));
    String validRequest = """
        {
            "title" : "Spring学習",
            "description" : "REST APIを作る"
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/projects")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isCreated());

    verify(service).register(eq(USER_ID), any(ProjectCreateRequest.class));
  }

  @Test
  void プロジェクト登録失敗_不正なリクエストボディなら400を返すこと() throws Exception {
    // Arrange
    String invalidRequest = """
        {
          "title": "",
          "description": "説明"
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/projects")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());
  }

  @Test
  void プロジェクト更新成功_200とメッセージを返すこと() throws Exception {
    // Arrange
    int id = 1;
    String validRequest = """
        {
            "title" : "タスク管理アプリ開発",
            "description" : "説明を更新"
        }
        """;

    Project updated = new Project(id, "タスク管理アプリ開発", "説明を更新", false);
    when(service.update(eq(USER_ID), eq(id), any(ProjectUpdateRequest.class)))
        .thenReturn(new ProjectResponse(updated, List.of()));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/projects/" + TestPublicIds.project(id))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(TestPublicIds.project(id)))
        .andExpect(jsonPath("$.description").value("説明を更新"))
        .andExpect(jsonPath("$.memos").isEmpty());

    verify(service).update(eq(USER_ID), eq(id), any(ProjectUpdateRequest.class));
  }

  @Test
  void プロジェクト更新失敗_パスパラメータを渡さないと405を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/projects").with(csrf()))
        .andExpect(status().isMethodNotAllowed());
  }

  @Test
  void プロジェクト更新失敗_パス変数の形式が不正なら404を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/projects/invalid-id").with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void プロジェクト更新失敗_対象が存在しないなら404を返すこと() throws Exception {
    // Arrange
    int id = 999;
    String validRequest = """
        {
          "title": "更新タイトル",
          "description": "更新説明"
        }
        """;
    doThrow(new TargetNotFoundException("id", "project not found"))
        .when(service).update(eq(USER_ID), eq(id), any(ProjectUpdateRequest.class));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/projects/" + TestPublicIds.project(id))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isNotFound());

    verify(service).update(eq(USER_ID), eq(id), any(ProjectUpdateRequest.class));
  }

  @Test
  void プロジェクト更新失敗_不正なリクエストボディなら400を返すこと() throws Exception {
    // Arrange
    int id = 1;
    String invalidRequest = """
        {
          "description": "更新説明"
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/projects/" + TestPublicIds.project(id))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());
  }

  @Test
  void プロジェクト完了状態更新成功_200と更新後データを返すこと() throws Exception {
    // Arrange
    int id = 1;
    String validRequest = """
        {
          "isFinished": true
        }
        """;
    Project updated = new Project(id, "タスク管理アプリ開発", "A社から受託した開発", true);
    when(service.updateFinished(eq(USER_ID), eq(id), eq(true)))
        .thenReturn(new ProjectResponse(updated, List.of()));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/projects/{id}/finished", TestPublicIds.project(id))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(TestPublicIds.project(id)))
        .andExpect(jsonPath("$.isFinished").value(true));

    verify(service).updateFinished(USER_ID, id, true);
  }

  @Test
  void プロジェクト完了状態更新失敗_未完了タスクが存在する場合は400を返すこと() throws Exception {
    // Arrange
    int id = 1;
    String validRequest = """
        {
          "isFinished": true
        }
        """;
    doThrow(new ProjectFinishNotAllowedException("project.id", "未完了のタスクがあります"))
        .when(service).updateFinished(USER_ID, id, true);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/projects/{id}/finished", TestPublicIds.project(id))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isBadRequest());
  }

  @Test
  void プロジェクト完了状態更新失敗_対象が存在しないなら404を返すこと() throws Exception {
    // Arrange
    int id = 999;
    String validRequest = """
        {
          "isFinished": true
        }
        """;
    doThrow(new TargetNotFoundException("project.id", "project not found"))
        .when(service).updateFinished(USER_ID, id, true);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/projects/{id}/finished", TestPublicIds.project(id))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isNotFound());
  }

  @Test
  void プロジェクト完了状態更新失敗_不正なリクエストボディなら400を返すこと() throws Exception {
    // Arrange
    int id = 1;
    String invalidRequest = "{}";

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.patch("/projects/{id}/finished", TestPublicIds.project(id))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());
  }
}
