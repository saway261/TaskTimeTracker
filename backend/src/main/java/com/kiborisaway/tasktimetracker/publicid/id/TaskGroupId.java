package com.kiborisaway.tasktimetracker.publicid.id;

import com.kiborisaway.tasktimetracker.publicid.PublicIdType;

/**
 * タスクグループの識別子。
 *
 * @param value 内部ID
 */
public record TaskGroupId(int value) implements PublicId {

  @Override
  public PublicIdType type() {
    return PublicIdType.TASK_GROUP;
  }
}
