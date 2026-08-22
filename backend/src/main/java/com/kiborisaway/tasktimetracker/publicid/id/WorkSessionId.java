package com.kiborisaway.tasktimetracker.publicid.id;

import com.kiborisaway.tasktimetracker.publicid.PublicIdType;

/**
 * 作業セッションの識別子。
 *
 * @param value 内部ID
 */
public record WorkSessionId(int value) implements PublicId {

  @Override
  public PublicIdType type() {
    return PublicIdType.WORK_SESSION;
  }
}
