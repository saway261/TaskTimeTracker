package com.kiborisaway.tasktimetracker.publicid.id;

import com.kiborisaway.tasktimetracker.publicid.PublicIdType;

/**
 * プロジェクトの識別子。
 *
 * @param value 内部ID
 */
public record ProjectId(int value) implements PublicId {

  @Override
  public PublicIdType type() {
    return PublicIdType.PROJECT;
  }
}
