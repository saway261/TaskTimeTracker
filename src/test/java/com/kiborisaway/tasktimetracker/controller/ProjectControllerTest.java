package com.kiborisaway.tasktimetracker.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.data.Project;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorDetailsBuilder;
import com.kiborisaway.tasktimetracker.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

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

    verify(service).findAllByCondition(null);
  }

  @Test
  void プロジェクト一覧検索_isFinishedがfalseでサービスを呼び出し200を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/projects")
            .param("isFinished", "false"))
        .andExpect(status().isOk());

    verify(service).findAllByCondition(false);
  }

  @Test
  void プロジェクト一覧検索_isFinishedがtrueでサービスを呼び出し200を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/projects")
            .param("isFinished", "true"))
        .andExpect(status().isOk());

    verify(service).findAllByCondition(true);
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
    when(service.findById(id)).thenReturn(project);

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/{id}", id))
        .andExpect(status().isOk());

    verify(service).findById(id);
  }

  @Test
  void プロジェクト単体取得失敗_パス変数が0以下なら400を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/0"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void プロジェクト単体取得失敗_対象が存在しないなら404を返すこと() throws Exception {
    // Arrange
    when(service.findById(999)).thenThrow(
        new TargetNotFoundException("id", "project not found"));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/999"))
        .andExpect(status().isNotFound());

    verify(service).findById(999);
  }

  @Test
  void プロジェクト単体取得失敗_パス変数の型が不正なら400を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/abc"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void プロジェクト登録成功_201と登録済みデータを返すこと() throws Exception {
    // Arrange
    Project response = new Project();
    response.setId(10);
    response.setTitle("Spring学習");
    response.setDescription("REST APIを作る");
    when(service.register(any(Project.class))).thenReturn(response);
    String validRequest = """
        {
            "title" : "Spring学習",
            "description" : "REST APIを作る"
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.post("/projects")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isCreated());

    verify(service).register(any(Project.class));
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

    doNothing().when(service).update(any(Project.class));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/projects/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk())
        .andExpect(content().string("更新成功"));

    verify(service).update(any(Project.class));
  }

  @Test
  void プロジェクト更新失敗_パスパラメータを渡さないと405を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/projects"))
        .andExpect(status().isMethodNotAllowed());
  }

  @Test
  void プロジェクト更新失敗_パス変数が0以下なら400を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/projects/0"))
        .andExpect(status().isBadRequest());
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
        .when(service).update(any(Project.class));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/projects/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isNotFound());

    verify(service).update(any(Project.class));
  }

  @Test
  void プロジェクト更新失敗_パス変数の型が不正なら400を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/projects/abc"))
        .andExpect(status().isBadRequest());
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
    mockMvc.perform(MockMvcRequestBuilders.put("/projects/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());
  }
}