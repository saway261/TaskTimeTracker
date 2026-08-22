package com.kiborisaway.tasktimetracker.publicid.id;

import com.kiborisaway.tasktimetracker.publicid.PublicIdType;

/**
 * タスクの識別子。
 *
 * @param value 内部ID
 */
public record TaskId(int value) implements PublicId {

  @Override
  public PublicIdType type() {
    return PublicIdType.TASK;
  }
}
