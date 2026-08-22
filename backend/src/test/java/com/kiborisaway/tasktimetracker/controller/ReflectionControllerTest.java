package com.kiborisaway.tasktimetracker.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.data.dto.reflection.ProjectReflectionOverviewResponse;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionCauseCategorySummaryResponse;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionRequest;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionResponse;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionTaskGroupResponse;
import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionTaskResponse;
import com.kiborisaway.tasktimetracker.exception.ReflectionAlreadyExistsException;
import com.kiborisaway.tasktimetracker.exception.ReflectionCauseCategoryInvalidException;
import com.kiborisaway.tasktimetracker.exception.ReflectionCauseRequiredException;
import com.kiborisaway.tasktimetracker.exception.ReflectionOperationNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorDetailsBuilder;
import com.kiborisaway.tasktimetracker.security.JsonAuthenticationEntryPoint;
import com.kiborisaway.tasktimetracker.service.ReflectionService;
import com.kiborisaway.tasktimetracker.support.TestPublicIds;
import com.kiborisaway.tasktimetracker.support.WebMvcTestSecuritySupportConfig;
import com.kiborisaway.tasktimetracker.support.WithMockAuthenticatedUser;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(ReflectionController.class)
@WithMockAuthenticatedUser
@Import({WebMvcTestSecuritySupportConfig.class, JsonAuthenticationEntryPoint.class})
class ReflectionControllerTest {

  private static final int USER_ID = 1;
  private static final int TASK_ID = 10;
  private static final String VALID_REQUEST = """
      {
        "causeCategoryCodes": ["TASK_BREAKDOWN"],
        "cause": "着手前の調査が不足していた",
        "nextAction": "類似タスクの実績を確認する"
      }
      """;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ReflectionService service;

  @MockitoBean
  private ErrorDetailsBuilder errorDetailsBuilder;

  @Test
  void 一覧取得成功_200とプロジェクト直下およびグループ配下のタスクを返すこと() throws Exception {
    ReflectionResponse reflection = new ReflectionResponse(
        20,
        6,
        List.of(new ReflectionCauseCategorySummaryResponse(
            "TASK_BREAKDOWN", "作業の洗い出しが足りなかった")),
        "原因",
        "改善する",
        LocalDateTime.of(2026, 8, 10, 10, 5),
        LocalDateTime.of(2026, 8, 10, 10, 5));
    ReflectionTaskResponse directTask = new ReflectionTaskResponse(
        6,
        "直下タスク",
        LocalDateTime.of(2026, 8, 10, 10, 0),
        90,
        30,
        50.0,
        reflection);
    ReflectionTaskResponse groupedTask = new ReflectionTaskResponse(
        9,
        "配下タスク",
        LocalDateTime.of(2026, 8, 11, 11, 0),
        null,
        null,
        null,
        null);
    ProjectReflectionOverviewResponse response = new ProjectReflectionOverviewResponse(
        3,
        "プロジェクトX",
        List.of(directTask),
        List.of(new ReflectionTaskGroupResponse(4, "グループA", List.of(groupedTask))));
    when(service.getOverview(USER_ID, 3)).thenReturn(response);

    mockMvc.perform(MockMvcRequestBuilders.get("/projects/{pId}/reflections", TestPublicIds.project(3)))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.projectId").value(TestPublicIds.project(3)))
        .andExpect(jsonPath("$.projectTitle").value("プロジェクトX"))
        .andExpect(jsonPath("$.tasks[0].id").value(TestPublicIds.task(6)))
        .andExpect(jsonPath("$.tasks[0].gapRateCached").value(50.0))
        .andExpect(jsonPath("$.tasks[0].reflection.id").value(20))
        .andExpect(jsonPath("$.tasks[0].reflection.causeCategories.length()").value(1))
        .andExpect(jsonPath("$.tasks[0].reflection.causeCategories[0].code")
            .value("TASK_BREAKDOWN"))
        .andExpect(jsonPath("$.tasks[0].reflection.causeCategories[0].label")
            .value("作業の洗い出しが足りなかった"))
        .andExpect(jsonPath("$.taskGroups[0].id").value(TestPublicIds.taskGroup(4)))
        .andExpect(jsonPath("$.taskGroups[0].tasks[0].id").value(TestPublicIds.task(9)))
        .andExpect(jsonPath("$.taskGroups[0].tasks[0].actualMinutesCached").isEmpty())
        .andExpect(jsonPath("$.taskGroups[0].tasks[0].reflection").isEmpty());

    verify(service).getOverview(USER_ID, 3);
  }

  @Test
  void 一覧取得失敗_プロジェクトが存在しない場合は404を返すこと() throws Exception {
    when(service.getOverview(USER_ID, 999))
        .thenThrow(new TargetNotFoundException("project.id", "project not found"));

    mockMvc.perform(MockMvcRequestBuilders.get("/projects/{pId}/reflections", TestPublicIds.project(999)))
        .andExpect(status().isNotFound());
  }

  @Test
  void 一覧取得失敗_プロジェクトIDの形式が不正なら404を返すこと() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/invalid-id/reflections"))
        .andExpect(status().isNotFound());
  }

  @Test
  void 登録成功_201と登録済み振り返りを返すこと() throws Exception {
    when(service.register(eq(USER_ID), eq(TASK_ID), any(ReflectionRequest.class)))
        .thenReturn(reflectionResponse());

    mockMvc.perform(MockMvcRequestBuilders.post("/tasks/{taskId}/reflection", TestPublicIds.task(TASK_ID))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_REQUEST))
        .andExpect(status().isCreated())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(20))
        .andExpect(jsonPath("$.taskId").value(TestPublicIds.task(TASK_ID)))
        .andExpect(jsonPath("$.causeCategories.length()").value(1))
        .andExpect(jsonPath("$.causeCategories[0].code").value("TASK_BREAKDOWN"))
        .andExpect(jsonPath("$.causeCategories[0].label").value("作業の洗い出しが足りなかった"))
        .andExpect(jsonPath("$.cause").value("着手前の調査が不足していた"))
        .andExpect(jsonPath("$.nextAction").value("類似タスクの実績を確認する"))
        .andExpect(jsonPath("$.createdAt").value("2026-08-10T10:05:00+09:00"))
        .andExpect(jsonPath("$.updatedAt").value("2026-08-10T10:05:00+09:00"));

    verify(service).register(eq(USER_ID), eq(TASK_ID), any(ReflectionRequest.class));
  }

  @Test
  void 更新成功_200と更新済み振り返りを返すこと() throws Exception {
    when(service.update(eq(USER_ID), eq(TASK_ID), any(ReflectionRequest.class)))
        .thenReturn(reflectionResponse());

    mockMvc.perform(MockMvcRequestBuilders.put("/tasks/{taskId}/reflection", TestPublicIds.task(TASK_ID))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_REQUEST))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(20))
        .andExpect(jsonPath("$.taskId").value(TestPublicIds.task(TASK_ID)))
        .andExpect(jsonPath("$.causeCategories[0].code").value("TASK_BREAKDOWN"));

    verify(service).update(eq(USER_ID), eq(TASK_ID), any(ReflectionRequest.class));
  }

  @Test
  void 登録成功_causeが空白文字だけでも201を返すこと() throws Exception {
    String request = """
        {
          "causeCategoryCodes": ["TASK_BREAKDOWN"],
          "cause": "   ",
          "nextAction": null
        }
        """;
    when(service.register(eq(USER_ID), eq(TASK_ID), any(ReflectionRequest.class)))
        .thenReturn(reflectionResponse());

    mockMvc.perform(MockMvcRequestBuilders.post("/tasks/{taskId}/reflection", TestPublicIds.task(TASK_ID))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
        .andExpect(status().isCreated());
  }

  @Test
  void 登録失敗_causeCategoryCodesが未指定の場合は400を返すこと() throws Exception {
    String invalidRequest = """
        {
          "cause": "着手前の調査が不足していた",
          "nextAction": null
        }
        """;

    mockMvc.perform(MockMvcRequestBuilders.post("/tasks/{taskId}/reflection", TestPublicIds.task(TASK_ID))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 登録失敗_causeCategoryCodesが空配列の場合は400を返すこと() throws Exception {
    String invalidRequest = """
        {
          "causeCategoryCodes": [],
          "cause": "原因",
          "nextAction": null
        }
        """;

    mockMvc.perform(MockMvcRequestBuilders.post("/tasks/{taskId}/reflection", TestPublicIds.task(TASK_ID))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 登録失敗_causeCategoryCodesが4件の場合は400を返すこと() throws Exception {
    String invalidRequest = """
        {
          "causeCategoryCodes": ["A", "B", "C", "D"],
          "cause": "原因",
          "nextAction": null
        }
        """;

    mockMvc.perform(MockMvcRequestBuilders.post("/tasks/{taskId}/reflection", TestPublicIds.task(TASK_ID))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 登録失敗_causeCategoryCodesに重複がある場合は400を返すこと() throws Exception {
    String invalidRequest = """
        {
          "causeCategoryCodes": ["TASK_BREAKDOWN", "TASK_BREAKDOWN"],
          "cause": "原因",
          "nextAction": null
        }
        """;

    mockMvc.perform(MockMvcRequestBuilders.post("/tasks/{taskId}/reflection", TestPublicIds.task(TASK_ID))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 登録失敗_原因カテゴリが存在しないまたは無効な場合は400を返すこと() throws Exception {
    when(service.register(eq(USER_ID), eq(TASK_ID), any(ReflectionRequest.class)))
        .thenThrow(new ReflectionCauseCategoryInvalidException(
            "reflection.causeCategoryCodes", "指定した原因カテゴリは選択できません"));

    mockMvc.perform(MockMvcRequestBuilders.post("/tasks/{taskId}/reflection", TestPublicIds.task(TASK_ID))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_REQUEST))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("reflection cause category invalid"));
  }

  @Test
  void 登録失敗_選択した原因カテゴリが原因の記述を必須とする場合は400を返すこと() throws Exception {
    when(service.register(eq(USER_ID), eq(TASK_ID), any(ReflectionRequest.class)))
        .thenThrow(new ReflectionCauseRequiredException(
            "reflection.cause", "選択した原因カテゴリでは、原因の記述が必要です"));

    mockMvc.perform(MockMvcRequestBuilders.post("/tasks/{taskId}/reflection", TestPublicIds.task(TASK_ID))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_REQUEST))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("reflection cause required"));
  }

  @Test
  void 登録失敗_タスクが存在しない場合は404を返すこと() throws Exception {
    when(service.register(eq(USER_ID), eq(TASK_ID), any(ReflectionRequest.class)))
        .thenThrow(new TargetNotFoundException("task.id", "task not found"));

    mockMvc.perform(MockMvcRequestBuilders.post("/tasks/{taskId}/reflection", TestPublicIds.task(TASK_ID))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_REQUEST))
        .andExpect(status().isNotFound());
  }

  @Test
  void 更新失敗_振り返りが存在しない場合は404を返すこと() throws Exception {
    when(service.update(eq(USER_ID), eq(TASK_ID), any(ReflectionRequest.class)))
        .thenThrow(new TargetNotFoundException("reflection.taskId", "reflection not found"));

    mockMvc.perform(MockMvcRequestBuilders.put("/tasks/{taskId}/reflection", TestPublicIds.task(TASK_ID))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_REQUEST))
        .andExpect(status().isNotFound());
  }

  @Test
  void 登録失敗_振り返りが既に存在する場合は409を返すこと() throws Exception {
    when(service.register(eq(USER_ID), eq(TASK_ID), any(ReflectionRequest.class)))
        .thenThrow(new ReflectionAlreadyExistsException(
            "reflection.taskId", "reflection already exists"));

    mockMvc.perform(MockMvcRequestBuilders.post("/tasks/{taskId}/reflection", TestPublicIds.task(TASK_ID))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_REQUEST))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("reflection already exists"));
  }

  @Test
  void 更新失敗_対象タスクが未完了の場合は409を返すこと() throws Exception {
    when(service.update(eq(USER_ID), eq(TASK_ID), any(ReflectionRequest.class)))
        .thenThrow(new ReflectionOperationNotAllowedException(
            "task.finishedAt", "task is unfinished"));

    mockMvc.perform(MockMvcRequestBuilders.put("/tasks/{taskId}/reflection", TestPublicIds.task(TASK_ID))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_REQUEST))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("reflection operation not allowed"));
  }

  private static ReflectionResponse reflectionResponse() {
    return new ReflectionResponse(
        20,
        TASK_ID,
        List.of(new ReflectionCauseCategorySummaryResponse(
            "TASK_BREAKDOWN", "作業の洗い出しが足りなかった")),
        "着手前の調査が不足していた",
        "類似タスクの実績を確認する",
        LocalDateTime.of(2026, 8, 10, 10, 5),
        LocalDateTime.of(2026, 8, 10, 10, 5));
  }
}
