package com.kiborisaway.tasktimetracker.publicid.id;

import com.kiborisaway.tasktimetracker.publicid.PublicIdType;

/**
 * タグの識別子。
 *
 * @param value 内部ID
 */
public record TagId(int value) implements PublicId {

  @Override
  public PublicIdType type() {
    return PublicIdType.TAG;
  }
}
