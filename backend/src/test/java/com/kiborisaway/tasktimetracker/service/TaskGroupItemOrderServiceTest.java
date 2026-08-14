package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.data.dto.item_order.TaskGroupItemOrderItemRequest;
import com.kiborisaway.tasktimetracker.data.dto.item_order.TaskGroupItemOrderResponse;
import com.kiborisaway.tasktimetracker.data.entity.TaskGroupItemOrder;
import com.kiborisaway.tasktimetracker.exception.InvalidItemOrderException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.TaskGroupItemOrderRepository;
import com.kiborisaway.tasktimetracker.repository.TaskGroupRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskGroupItemOrderServiceTest {

  private static final int USER_ID = 1;

  @Mock
  private TaskGroupItemOrderRepository orderRepository;

  @Mock
  private TaskGroupRepository taskGroupRepository;

  @InjectMocks
  private TaskGroupItemOrderService sut;

  @Test
  void 一覧取得成功_position昇順の並び順一覧を返すこと() {
    // Arrange
    int tgId = 1;
    when(taskGroupRepository.existsByIdAndUserId(tgId, USER_ID)).thenReturn(true);
    when(orderRepository.findAllInTaskGroupOrdered(tgId, USER_ID)).thenReturn(List.of(
        new TaskGroupItemOrder(1, tgId, 1, 0),
        new TaskGroupItemOrder(2, tgId, 2, 1)));

    // Act
    List<TaskGroupItemOrderResponse> actual = sut.findAllInTaskGroup(USER_ID, tgId);

    // Assert
    assertThat(actual)
        .extracting(TaskGroupItemOrderResponse::getId, TaskGroupItemOrderResponse::getPosition)
        .containsExactly(tuple(1, 0), tuple(2, 1));
  }

  @Test
  void 一覧取得失敗_存在しないタスクグループIDの場合は例外を投げること() {
    // Arrange
    int tgId = 999;
    when(taskGroupRepository.existsByIdAndUserId(tgId, USER_ID)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.findAllInTaskGroup(USER_ID, tgId))
        .isInstanceOf(TargetNotFoundException.class);
    verify(orderRepository, never()).findAllInTaskGroupOrdered(anyInt(), anyInt());
  }

  @Test
  void 並び替え成功_リクエストの順序どおりに一時オフセットを経由してpositionを更新すること() {
    // Arrange
    int tgId = 1;
    when(taskGroupRepository.existsByIdAndUserId(tgId, USER_ID)).thenReturn(true);
    when(orderRepository.findAllInTaskGroupOrdered(tgId, USER_ID))
        .thenReturn(List.of(
            new TaskGroupItemOrder(1, tgId, 1, 0),
            new TaskGroupItemOrder(2, tgId, 2, 1)))
        .thenReturn(List.of(
            new TaskGroupItemOrder(2, tgId, 2, 0),
            new TaskGroupItemOrder(1, tgId, 1, 1)));

    List<TaskGroupItemOrderItemRequest> items = List.of(
        new TaskGroupItemOrderItemRequest(2),
        new TaskGroupItemOrderItemRequest(1));

    // Act
    List<TaskGroupItemOrderResponse> actual = sut.replaceOrder(USER_ID, tgId,items);

    // Assert
    assertThat(actual)
        .extracting(TaskGroupItemOrderResponse::getId, TaskGroupItemOrderResponse::getPosition)
        .containsExactly(tuple(2, 0), tuple(1, 1));
    verify(orderRepository, times(1)).updatePositionByTaskId(2, 1_000_000, USER_ID);
    verify(orderRepository, times(1)).updatePositionByTaskId(1, 1_000_001, USER_ID);
    verify(orderRepository, times(1)).updatePositionByTaskId(2, 0, USER_ID);
    verify(orderRepository, times(1)).updatePositionByTaskId(1, 1, USER_ID);
  }

  @Test
  void 並び替え失敗_存在しないタスクグループIDの場合は例外を投げて以降の処理を呼び出さないこと() {
    // Arrange
    int tgId = 999;
    when(taskGroupRepository.existsByIdAndUserId(tgId, USER_ID)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(
        () -> sut.replaceOrder(USER_ID, tgId,List.of(new TaskGroupItemOrderItemRequest(1))))
        .isInstanceOf(TargetNotFoundException.class);
    verify(orderRepository, never()).findAllInTaskGroupOrdered(anyInt(), anyInt());
  }

  @Test
  void 並び替え失敗_リクエストの項目に重複がある場合は例外を投げて更新処理を呼び出さないこと() {
    // Arrange
    int tgId = 1;
    when(taskGroupRepository.existsByIdAndUserId(tgId, USER_ID)).thenReturn(true);
    when(orderRepository.findAllInTaskGroupOrdered(tgId, USER_ID)).thenReturn(List.of(
        new TaskGroupItemOrder(1, tgId, 1, 0)));

    List<TaskGroupItemOrderItemRequest> items = List.of(
        new TaskGroupItemOrderItemRequest(1),
        new TaskGroupItemOrderItemRequest(1));

    // Act & Assert
    assertThatThrownBy(() -> sut.replaceOrder(USER_ID, tgId,items))
        .isInstanceOf(InvalidItemOrderException.class);
    verify(orderRepository, never()).updatePositionByTaskId(anyInt(), anyInt(), anyInt());
  }

  @Test
  void 並び替え失敗_リクエストの項目が現在の項目と一致しない場合は例外を投げて更新処理を呼び出さないこと() {
    // Arrange
    int tgId = 1;
    when(taskGroupRepository.existsByIdAndUserId(tgId, USER_ID)).thenReturn(true);
    when(orderRepository.findAllInTaskGroupOrdered(tgId, USER_ID)).thenReturn(List.of(
        new TaskGroupItemOrder(1, tgId, 1, 0),
        new TaskGroupItemOrder(2, tgId, 2, 1)));

    List<TaskGroupItemOrderItemRequest> items =
        List.of(new TaskGroupItemOrderItemRequest(1)); // タスク2が不足

    // Act & Assert
    assertThatThrownBy(() -> sut.replaceOrder(USER_ID, tgId,items))
        .isInstanceOf(InvalidItemOrderException.class);
    verify(orderRepository, never()).updatePositionByTaskId(anyInt(), anyInt(), anyInt());
  }

}
