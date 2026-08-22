package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.dto.item_order.TaskGroupItemOrderItemRequest;
import com.kiborisaway.tasktimetracker.data.dto.item_order.TaskGroupItemOrderResponse;
import com.kiborisaway.tasktimetracker.data.entity.TaskGroupItemOrder;
import com.kiborisaway.tasktimetracker.exception.InvalidItemOrderException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.TaskGroupItemOrderRepository;
import com.kiborisaway.tasktimetracker.repository.TaskGroupRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskGroupItemOrderService {

  /**
   * 並び替え時、position更新を一意制約に違反せず行うための一時オフセット。
   * 対象件数がこの値を超えることは実運用上想定しない。
   */
  private static final int TEMP_POSITION_OFFSET = 1_000_000;

  private TaskGroupItemOrderRepository orderRepository;
  private TaskGroupRepository taskGroupRepository;

  @Autowired
  public TaskGroupItemOrderService(
      TaskGroupItemOrderRepository orderRepository,
      TaskGroupRepository taskGroupRepository) {
    this.orderRepository = orderRepository;
    this.taskGroupRepository = taskGroupRepository;
  }

  /**
   * タスクグループ直下の並び順一覧をposition昇順で取得します。
   *
   * @param userId      認証ユーザーのID
   * @param taskGroupId タスクグループID
   * @return 並び順一覧
   */
  public List<TaskGroupItemOrderResponse> findAllInTaskGroup(int userId, int taskGroupId) {
    if (!taskGroupRepository.existsByIdAndUserId(taskGroupId, userId)) {
      throw new TargetNotFoundException("taskGroup.id",
          "指定したIDのタスクグループは見つかりませんでした");
    }
    return toResponses(orderRepository.findAllInTaskGroupOrdered(taskGroupId, userId));
  }

  /**
   * タスクグループ直下の並び順を、リクエストで渡した配列の順序へ全置換します。
   * リクエストの項目は、タスクグループ直下の現在の項目と過不足なく一致している必要があります。
   *
   * @param userId      認証ユーザーのID
   * @param taskGroupId タスクグループID
   * @param items       並び替え後の順序どおりに並べた項目一覧
   * @return 更新後の並び順一覧
   */
  @Transactional
  public List<TaskGroupItemOrderResponse> replaceOrder(int userId, int taskGroupId,
      List<TaskGroupItemOrderItemRequest> items) {
    if (!taskGroupRepository.existsByIdAndUserId(taskGroupId, userId)) {
      throw new TargetNotFoundException("taskGroup.id",
          "指定したIDのタスクグループは見つかりませんでした");
    }

    List<TaskGroupItemOrder> current =
        orderRepository.findAllInTaskGroupOrdered(taskGroupId, userId);
    validateSameItemSet(current, items);

    for (int i = 0; i < items.size(); i++) {
      orderRepository.updatePositionByTaskId(
          items.get(i).id().value(), TEMP_POSITION_OFFSET + i, userId);
    }
    for (int i = 0; i < items.size(); i++) {
      orderRepository.updatePositionByTaskId(items.get(i).id().value(), i, userId);
    }

    return toResponses(orderRepository.findAllInTaskGroupOrdered(taskGroupId, userId));
  }

  private void validateSameItemSet(List<TaskGroupItemOrder> current,
      List<TaskGroupItemOrderItemRequest> items) {
    Set<Integer> currentIds = current.stream()
        .map(TaskGroupItemOrder::getTaskId)
        .collect(Collectors.toSet());
    Set<Integer> requestedIds = items.stream()
        .map(item -> item.id().value())
        .collect(Collectors.toSet());

    if (requestedIds.size() != items.size()) {
      throw new InvalidItemOrderException("items", "並び順の項目が重複しています");
    }
    if (!requestedIds.equals(currentIds)) {
      throw new InvalidItemOrderException("items",
          "指定した項目がタスクグループ直下の現在の項目と一致しません");
    }
  }

  private List<TaskGroupItemOrderResponse> toResponses(List<TaskGroupItemOrder> orders) {
    return orders.stream().map(TaskGroupItemOrderResponse::new).toList();
  }
}
