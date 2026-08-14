package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.data.dto.item_order.ItemType;
import com.kiborisaway.tasktimetracker.data.dto.item_order.ProjectItemOrderItemRequest;
import com.kiborisaway.tasktimetracker.data.dto.item_order.ProjectItemOrderResponse;
import com.kiborisaway.tasktimetracker.data.entity.ProjectItemOrder;
import com.kiborisaway.tasktimetracker.exception.InvalidItemOrderException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.ProjectItemOrderRepository;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectItemOrderServiceTest {

  private static final int USER_ID = 1;

  @Mock
  private ProjectItemOrderRepository orderRepository;

  @Mock
  private ProjectRepository projectRepository;

  @InjectMocks
  private ProjectItemOrderService sut;

  @Test
  void 一覧取得成功_position昇順の並び順一覧を返すこと() {
    // Arrange
    int pId = 1;
    ProjectItemOrder taskOrder = new ProjectItemOrder(1, pId, 4, null, 0);
    ProjectItemOrder groupOrder = new ProjectItemOrder(2, pId, null, 1, 1);

    when(projectRepository.existsByIdAndUserId(pId, USER_ID)).thenReturn(true);
    when(orderRepository.findAllInProjectOrdered(pId, USER_ID)).thenReturn(List.of(taskOrder, groupOrder));

    // Act
    List<ProjectItemOrderResponse> actual = sut.findAllInProject(USER_ID, pId);

    // Assert
    assertThat(actual)
        .extracting(ProjectItemOrderResponse::getType, ProjectItemOrderResponse::getId,
            ProjectItemOrderResponse::getPosition)
        .containsExactly(
            tuple(ItemType.TASK, 4, 0),
            tuple(ItemType.TASK_GROUP, 1, 1)
        );
  }

  @Test
  void 一覧取得失敗_存在しないプロジェクトIDの場合は例外を投げること() {
    // Arrange
    int pId = 999;
    when(projectRepository.existsByIdAndUserId(pId, USER_ID)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.findAllInProject(USER_ID, pId))
        .isInstanceOf(TargetNotFoundException.class);
    verify(orderRepository, never()).findAllInProjectOrdered(anyInt(), anyInt());
  }

  @Test
  void 並び替え成功_リクエストの順序どおりに一時オフセットを経由してpositionを更新すること() {
    // Arrange
    int pId = 1;
    ProjectItemOrder taskOrder = new ProjectItemOrder(1, pId, 4, null, 0);
    ProjectItemOrder groupOrder = new ProjectItemOrder(2, pId, null, 1, 1);

    when(projectRepository.existsByIdAndUserId(pId, USER_ID)).thenReturn(true);
    when(orderRepository.findAllInProjectOrdered(pId, USER_ID))
        .thenReturn(List.of(taskOrder, groupOrder))
        .thenReturn(List.of(
            new ProjectItemOrder(2, pId, null, 1, 0),
            new ProjectItemOrder(1, pId, 4, null, 1)));

    List<ProjectItemOrderItemRequest> items = List.of(
        new ProjectItemOrderItemRequest(ItemType.TASK_GROUP, 1),
        new ProjectItemOrderItemRequest(ItemType.TASK, 4));

    // Act
    List<ProjectItemOrderResponse> actual = sut.replaceOrder(USER_ID, pId, items);

    // Assert
    assertThat(actual)
        .extracting(ProjectItemOrderResponse::getId, ProjectItemOrderResponse::getPosition)
        .containsExactly(tuple(1, 0), tuple(4, 1));
    // 一時オフセット退避 → 最終値 の2段階で更新されること
    verify(orderRepository, times(1)).updatePositionByTaskGroupId(1, 1_000_000, USER_ID);
    verify(orderRepository, times(1)).updatePositionByTaskId(4, 1_000_001, USER_ID);
    verify(orderRepository, times(1)).updatePositionByTaskGroupId(1, 0, USER_ID);
    verify(orderRepository, times(1)).updatePositionByTaskId(4, 1, USER_ID);
  }

  @Test
  void 並び替え失敗_存在しないプロジェクトIDの場合は例外を投げて以降の処理を呼び出さないこと() {
    // Arrange
    int pId = 999;
    when(projectRepository.existsByIdAndUserId(pId, USER_ID)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.replaceOrder(USER_ID, pId,
        List.of(new ProjectItemOrderItemRequest(ItemType.TASK, 1))))
        .isInstanceOf(TargetNotFoundException.class);
    verify(orderRepository, never()).findAllInProjectOrdered(anyInt(), anyInt());
  }

  @Test
  void 並び替え失敗_リクエストの項目に重複がある場合は例外を投げて更新処理を呼び出さないこと() {
    // Arrange
    int pId = 1;
    when(projectRepository.existsByIdAndUserId(pId, USER_ID)).thenReturn(true);
    when(orderRepository.findAllInProjectOrdered(pId, USER_ID)).thenReturn(List.of(
        new ProjectItemOrder(1, pId, 4, null, 0)));

    List<ProjectItemOrderItemRequest> items = List.of(
        new ProjectItemOrderItemRequest(ItemType.TASK, 4),
        new ProjectItemOrderItemRequest(ItemType.TASK, 4));

    // Act & Assert
    assertThatThrownBy(() -> sut.replaceOrder(USER_ID, pId, items))
        .isInstanceOf(InvalidItemOrderException.class);
    verify(orderRepository, never()).updatePositionByTaskId(anyInt(), anyInt(), anyInt());
  }

  @Test
  void 並び替え失敗_リクエストの項目が現在の項目と一致しない場合は例外を投げて更新処理を呼び出さないこと() {
    // Arrange
    int pId = 1;
    when(projectRepository.existsByIdAndUserId(pId, USER_ID)).thenReturn(true);
    when(orderRepository.findAllInProjectOrdered(pId, USER_ID)).thenReturn(List.of(
        new ProjectItemOrder(1, pId, 4, null, 0),
        new ProjectItemOrder(2, pId, null, 1, 1)));

    List<ProjectItemOrderItemRequest> items =
        List.of(new ProjectItemOrderItemRequest(ItemType.TASK, 4)); // タスクグループ1が不足

    // Act & Assert
    assertThatThrownBy(() -> sut.replaceOrder(USER_ID, pId, items))
        .isInstanceOf(InvalidItemOrderException.class);
    verify(orderRepository, never()).updatePositionByTaskId(anyInt(), anyInt(), anyInt());
    verify(orderRepository, never()).updatePositionByTaskGroupId(anyInt(), anyInt(), anyInt());
  }

}
