package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.dto.item_order.ItemType;
import com.kiborisaway.tasktimetracker.data.dto.item_order.ProjectItemOrderItemRequest;
import com.kiborisaway.tasktimetracker.data.dto.item_order.ProjectItemOrderResponse;
import com.kiborisaway.tasktimetracker.data.entity.ProjectItemOrder;
import com.kiborisaway.tasktimetracker.exception.InvalidItemOrderException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.publicid.PublicIdCodec;
import com.kiborisaway.tasktimetracker.publicid.PublicIdType;
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

  /**
   * {@code ProjectItemOrderItemRequest} の公開ID文字列を内部IDへ解決した結果。
   * type によってタスク用アルファベットとタスクグループ用アルファベットのどちらでデコードするかが
   * 決まるため、Controller/Jacksonでの自動変換ではなくここで解決する。
   */
  private record ResolvedItem(ItemType type, int id) {

  }

  private ProjectItemOrderRepository orderRepository;
  private ProjectRepository projectRepository;
  private PublicIdCodec codec;

  @Autowired
  public ProjectItemOrderService(
      ProjectItemOrderRepository orderRepository,
      ProjectRepository projectRepository,
      PublicIdCodec codec) {
    this.orderRepository = orderRepository;
    this.projectRepository = projectRepository;
    this.codec = codec;
  }

  /**
   * プロジェクト直下の並び順一覧をposition昇順で取得します。
   *
   * @param userId    認証ユーザーのID
   * @param projectId プロジェクトID
   * @return 並び順一覧
   */
  public List<ProjectItemOrderResponse> findAllInProject(int userId, int projectId) {
    if (!projectRepository.existsByIdAndUserId(projectId, userId)) {
      throw new TargetNotFoundException("project.id",
          "指定したIDのプロジェクトは見つかりませんでした");
    }
    return toResponses(orderRepository.findAllInProjectOrdered(projectId, userId));
  }

  /**
   * プロジェクト直下の並び順を、リクエストで渡した配列の順序へ全置換します。
   * リクエストの項目は、プロジェクト直下の現在の項目と過不足なく一致している必要があります。
   *
   * @param userId    認証ユーザーのID
   * @param projectId プロジェクトID
   * @param items     並び替え後の順序どおりに並べた項目一覧
   * @return 更新後の並び順一覧
   */
  @Transactional
  public List<ProjectItemOrderResponse> replaceOrder(int userId, int projectId,
      List<ProjectItemOrderItemRequest> items) {
    if (!projectRepository.existsByIdAndUserId(projectId, userId)) {
      throw new TargetNotFoundException("project.id",
          "指定したIDのプロジェクトは見つかりませんでした");
    }

    List<ProjectItemOrder> current = orderRepository.findAllInProjectOrdered(projectId, userId);
    // 公開ID文字列の解決（decode）はここで一度だけ行う。type によってタスク用・タスクグループ用の
    // どちらのアルファベットでデコードするかが変わるため、Controller側の自動変換には委ねられない。
    List<ResolvedItem> resolvedItems = items.stream().map(this::resolve).toList();
    validateSameItemSet(current, resolvedItems);

    // 一意制約(project_id, position)に一時的にも違反しないよう、大きなオフセット値へ退避してから最終値を書き込む
    for (int i = 0; i < resolvedItems.size(); i++) {
      updatePosition(resolvedItems.get(i), TEMP_POSITION_OFFSET + i, userId);
    }
    for (int i = 0; i < resolvedItems.size(); i++) {
      updatePosition(resolvedItems.get(i), i, userId);
    }

    return toResponses(orderRepository.findAllInProjectOrdered(projectId, userId));
  }

  private ResolvedItem resolve(ProjectItemOrderItemRequest item) {
    PublicIdType type = item.type() == ItemType.TASK ? PublicIdType.TASK : PublicIdType.TASK_GROUP;
    return new ResolvedItem(item.type(), codec.decode(type, item.id()));
  }

  private void updatePosition(ResolvedItem item, int position, int userId) {
    if (item.type() == ItemType.TASK) {
      orderRepository.updatePositionByTaskId(item.id(), position, userId);
    } else {
      orderRepository.updatePositionByTaskGroupId(item.id(), position, userId);
    }
  }

  private void validateSameItemSet(List<ProjectItemOrder> current,
      List<ResolvedItem> items) {
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
    return orders.stream().map(order -> new ProjectItemOrderResponse(order, codec)).toList();
  }
}
