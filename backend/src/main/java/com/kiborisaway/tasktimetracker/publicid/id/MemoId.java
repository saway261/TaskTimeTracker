package com.kiborisaway.tasktimetracker.publicid.id;

import com.kiborisaway.tasktimetracker.publicid.PublicIdType;

/**
 * メモの識別子。
 *
 * @param value 内部ID
 */
public record MemoId(int value) implements PublicId {

  @Override
  public PublicIdType type() {
    return PublicIdType.MEMO;
  }
}
