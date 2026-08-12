package com.kiborisaway.tasktimetracker.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.data.dto.item_order.ItemType;
import com.kiborisaway.tasktimetracker.data.dto.item_order.ProjectItemOrderItemRequest;
import com.kiborisaway.tasktimetracker.data.dto.item_order.ProjectItemOrderResponse;
import com.kiborisaway.tasktimetracker.data.entity.ProjectItemOrder;
import com.kiborisaway.tasktimetracker.exception.InvalidItemOrderException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorDetailsBuilder;
import com.kiborisaway.tasktimetracker.service.ProjectItemOrderService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(ProjectItemOrderController.class)
class ProjectItemOrderControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ProjectItemOrderService service;

  @MockitoBean
  private ErrorDetailsBuilder errorDetailsBuilder;

  @Test
  void 並び順一覧取得成功_200と並び順一覧を返すこと() throws Exception {
    // Arrange
    int pId = 1;
    when(service.findAllInProject(pId)).thenReturn(List.of(
        new ProjectItemOrderResponse(new ProjectItemOrder(1, pId, 4, null, 0)),
        new ProjectItemOrderResponse(new ProjectItemOrder(2, pId, null, 1, 1))));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/{pId}/item-order", pId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].type").value("TASK"))
        .andExpect(jsonPath("$[0].id").value(4))
        .andExpect(jsonPath("$[1].type").value("TASK_GROUP"))
        .andExpect(jsonPath("$[1].id").value(1));

    verify(service).findAllInProject(pId);
  }

  @Test
  void 並び順一覧取得失敗_パス変数が0以下なら400を返すこと() throws Exception {
    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/0/item-order"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void 並び順一覧取得失敗_存在しないプロジェクトIDを指定した場合は404を返すこと() throws Exception {
    // Arrange
    int pId = 999;
    when(service.findAllInProject(pId)).thenThrow(
        new TargetNotFoundException("project.id", "指定したIDのプロジェクトは見つかりませんでした"));

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.get("/projects/{pId}/item-order", pId))
        .andExpect(status().isNotFound());
  }

  @Test
  void 並び替え成功_200とリクエストどおりの並び順でサービスを呼び出すこと() throws Exception {
    // Arrange
    int pId = 1;
    when(service.replaceOrder(eq(pId), any())).thenReturn(List.of(
        new ProjectItemOrderResponse(new ProjectItemOrder(2, pId, null, 1, 0)),
        new ProjectItemOrderResponse(new ProjectItemOrder(1, pId, 4, null, 1))));
    String validRequest = """
        {
          "items": [
            { "type": "TASK_GROUP", "id": 1 },
            { "type": "TASK", "id": 4 }
          ]
        }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/projects/{pId}/item-order", pId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[1].id").value(4));

    verify(service).replaceOrder(eq(pId), eq(List.of(
        new ProjectItemOrderItemRequest(ItemType.TASK_GROUP, 1),
        new ProjectItemOrderItemRequest(ItemType.TASK, 4))));
  }

  @Test
  void 並び替え失敗_itemsが空なら400を返すこと() throws Exception {
    // Arrange
    int pId = 1;
    String invalidRequest = """
        { "items": [] }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/projects/{pId}/item-order", pId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void 並び替え失敗_存在しないプロジェクトIDを指定した場合は404を返すこと() throws Exception {
    // Arrange
    int pId = 999;
    when(service.replaceOrder(eq(pId), any())).thenThrow(
        new TargetNotFoundException("project.id", "指定したIDのプロジェクトは見つかりませんでした"));
    String validRequest = """
        { "items": [ { "type": "TASK", "id": 4 } ] }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/projects/{pId}/item-order", pId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isNotFound());
  }

  @Test
  void 並び替え失敗_項目の過不足がある場合は400を返すこと() throws Exception {
    // Arrange
    int pId = 1;
    when(service.replaceOrder(eq(pId), any())).thenThrow(
        new InvalidItemOrderException("items", "指定した項目がプロジェクト直下の現在の項目と一致しません"));
    String validRequest = """
        { "items": [ { "type": "TASK", "id": 4 } ] }
        """;

    // Act & Assert
    mockMvc.perform(MockMvcRequestBuilders.put("/projects/{pId}/item-order", pId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isBadRequest());
  }

}
