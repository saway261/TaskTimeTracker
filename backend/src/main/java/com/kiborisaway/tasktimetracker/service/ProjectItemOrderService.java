package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.dto.item_order.ItemType;
import com.kiborisaway.tasktimetracker.data.dto.item_order.ProjectItemOrderItemRequest;
import com.kiborisaway.tasktimetracker.data.dto.item_order.ProjectItemOrderResponse;
import com.kiborisaway.tasktimetracker.data.entity.ProjectItemOrder;
import com.kiborisaway.tasktimetracker.exception.InvalidItemOrderException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.ProjectItemOrderRepository;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectItemOrderService {

  /**
   * 並び替え時、position更新を一意制約に違反せず行うための一時オフセット。
   * 対象件数がこの値を超えることは実運用上想定しない。
   */
  private static final int TEMP_POSITION_OFFSET = 1_000_000;

  private ProjectItemOrderRepository orderRepository;
  private ProjectRepository projectRepository;

  @Autowired
  public ProjectItemOrderService(
      ProjectItemOrderRepository orderRepository,
      ProjectRepository projectRepository) {
    this.orderRepository = orderRepository;
    this.projectRepository = projectRepository;
  }

  /**
   * プロジェクト直下の並び順一覧をposition昇順で取得します。
   *
   * @param projectId プロジェクトID
   * @return 並び順一覧
   */
  public List<ProjectItemOrderResponse> findAllInProject(int projectId) {
    if (!projectRepository.existsById(projectId)) {
      throw new TargetNotFoundException("project.id",
          "指定したIDのプロジェクトは見つかりませんでした");
    }
    return toResponses(orderRepository.findAllInProjectOrdered(projectId));
  }

  /**
   * プロジェクト直下の並び順を、リクエストで渡した配列の順序へ全置換します。
   * リクエストの項目は、プロジェクト直下の現在の項目と過不足なく一致している必要があります。
   *
   * @param projectId プロジェクトID
   * @param items     並び替え後の順序どおりに並べた項目一覧
   * @return 更新後の並び順一覧
   */
  @Transactional
  public List<ProjectItemOrderResponse> replaceOrder(int projectId,
      List<ProjectItemOrderItemRequest> items) {
    if (!projectRepository.existsById(projectId)) {
      throw new TargetNotFoundException("project.id",
          "指定したIDのプロジェクトは見つかりませんでした");
    }

    List<ProjectItemOrder> current = orderRepository.findAllInProjectOrdered(projectId);
    validateSameItemSet(current, items);

    // 一意制約(project_id, position)に一時的にも違反しないよう、大きなオフセット値へ退避してから最終値を書き込む
    for (int i = 0; i < items.size(); i++) {
      updatePosition(items.get(i), TEMP_POSITION_OFFSET + i);
    }
    for (int i = 0; i < items.size(); i++) {
      updatePosition(items.get(i), i);
    }

    return toResponses(orderRepository.findAllInProjectOrdered(projectId));
  }

  private void updatePosition(ProjectItemOrderItemRequest item, int position) {
    if (item.type() == ItemType.TASK) {
      orderRepository.updatePositionByTaskId(item.id(), position);
    } else {
      orderRepository.updatePositionByTaskGroupId(item.id(), position);
    }
  }

  private void validateSameItemSet(List<ProjectItemOrder> current,
      List<ProjectItemOrderItemRequest> items) {
    Set<String> currentKeys = current.stream()
        .map(order -> order.getTaskId() != null
            ? ItemType.TASK + ":" + order.getTaskId()
            : ItemType.TASK_GROUP + ":" + order.getTaskGroupId())
        .collect(Collectors.toSet());
    Set<String> requestedKeys = items.stream()
        .map(item -> item.type() + ":" + item.id())
        .collect(Collectors.toSet());

    if (requestedKeys.size() != items.size()) {
      throw new InvalidItemOrderException("items", "並び順の項目が重複しています");
    }
    if (!requestedKeys.equals(currentKeys)) {
      throw new InvalidItemOrderException("items",
          "指定した項目がプロジェクト直下の現在の項目と一致しません");
    }
  }

  private List<ProjectItemOrderResponse> toResponses(List<ProjectItemOrder> orders) {
    return orders.stream().map(ProjectItemOrderResponse::new).toList();
  }
}
