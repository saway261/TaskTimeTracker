package com.kiborisaway.tasktimetracker.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.data.dto.item_order.TaskGroupItemOrderItemRequest;
import com.kiborisaway.tasktimetracker.data.dto.item_order.TaskGroupItemOrderResponse;
import com.kiborisaway.tasktimetracker.data.entity.TaskGroupItemOrder;
import com.kiborisaway.tasktimetracker.exception.InvalidItemOrderException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorDetailsBuilder;
import com.kiborisaway.tasktimetracker.publicid.id.TaskId;
import com.kiborisaway.tasktimetracker.security.JsonAuthenticationEntryPoint;
import com.kiborisaway.tasktimetracker.service.TaskGroupItemOrderService;
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

@WebMvcTest(TaskGroupItemOrderController.class)
@WithMockAuthenticatedUser
@Import({WebMvcTestSecuritySupportConfig.class, JsonAuthenticationEntryPoint.class})
class TaskGroupItemOrderControllerTest {

  private static final int USER_ID = 1;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TaskGroupItemOrderService service;

  @MockitoBean
  private ErrorDetailsBuilder errorDetailsBuilder;

  @Test
  void 並び順一覧取得成功_200と並び順一覧を返すこと() throws Exception {
    // Arrange
    int tgId = 1;
    when(service.findAllInTaskGroup(USER_ID, tgId)).thenReturn(List.of(
        new TaskGroupItemOrderResponse(new TaskGroupItemOrder(1, tgId, 1, 0)),
        new TaskGroupItemOrderResponse(new TaskGroupItemOrder(2, tgId, 2, 1))));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/task-groups/{tgId}/item-order",
            TestPublicIds.taskGroup(tgId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(TestPublicIds.task(1)))
        .andExpect(jsonPath("$[1].id").value(TestPublicIds.task(2)));

    verify(service).findAllInTaskGroup(USER_ID, tgId);
  }

  @Test
  void 並び順一覧取得失敗_パス変数の形式が不正なら404を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/task-groups/invalid-id/item-order"))
        .andExpect(status().isNotFound());
    verifyNoInteractions(service);
  }

  @Test
  void 並び順一覧取得失敗_存在しないタスクグループIDを指定した場合は404を返すこと() throws Exception {
    // Arrange
    int tgId = 999;
    when(service.findAllInTaskGroup(USER_ID, tgId)).thenThrow(
        new TargetNotFoundException("taskGroup.id", "指定したIDのタスクグループは見つかりませんでした"));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/task-groups/{tgId}/item-order",
            TestPublicIds.taskGroup(tgId)))
        .andExpect(status().isNotFound());
  }

  @Test
  void 並び替え成功_200とリクエストどおりの並び順でサービスを呼び出すこと() throws Exception {
    // Arrange
    int tgId = 1;
    when(service.replaceOrder(eq(USER_ID), eq(tgId), any())).thenReturn(List.of(
        new TaskGroupItemOrderResponse(new TaskGroupItemOrder(2, tgId, 2, 0)),
        new TaskGroupItemOrderResponse(new TaskGroupItemOrder(1, tgId, 1, 1))));
    String validRequest = """
        { "items": [ { "id": "%s" }, { "id": "%s" } ] }
        """.formatted(TestPublicIds.task(2), TestPublicIds.task(1));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/task-groups/{tgId}/item-order",
            TestPublicIds.taskGroup(tgId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(TestPublicIds.task(2)))
        .andExpect(jsonPath("$[1].id").value(TestPublicIds.task(1)));

    verify(service).replaceOrder(eq(USER_ID), eq(tgId), eq(List.of(
        new TaskGroupItemOrderItemRequest(new TaskId(2)),
        new TaskGroupItemOrderItemRequest(new TaskId(1)))));
  }

  @Test
  void 並び替え失敗_itemsが空なら400を返すこと() throws Exception {
    // Arrange
    int tgId = 1;
    String invalidRequest = """
        { "items": [] }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/task-groups/{tgId}/item-order",
            TestPublicIds.taskGroup(tgId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void 並び替え失敗_存在しないタスクグループIDを指定した場合は404を返すこと() throws Exception {
    // Arrange
    int tgId = 999;
    when(service.replaceOrder(eq(USER_ID), eq(tgId), any())).thenThrow(
        new TargetNotFoundException("taskGroup.id", "指定したIDのタスクグループは見つかりませんでした"));
    String validRequest = """
        { "items": [ { "id": "%s" } ] }
        """.formatted(TestPublicIds.task(1));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/task-groups/{tgId}/item-order",
            TestPublicIds.taskGroup(tgId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isNotFound());
  }

  @Test
  void 並び替え失敗_項目の過不足がある場合は400を返すこと() throws Exception {
    // Arrange
    int tgId = 1;
    when(service.replaceOrder(eq(USER_ID), eq(tgId), any())).thenThrow(
        new InvalidItemOrderException("items", "指定した項目がタスクグループ直下の現在の項目と一致しません"));
    String validRequest = """
        { "items": [ { "id": "%s" } ] }
        """.formatted(TestPublicIds.task(1));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/task-groups/{tgId}/item-order",
            TestPublicIds.taskGroup(tgId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isBadRequest());
  }

}
