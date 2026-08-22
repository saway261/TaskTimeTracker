package com.kiborisaway.tasktimetracker.publicid.converter;

import com.kiborisaway.tasktimetracker.publicid.PublicIdCodec;
import com.kiborisaway.tasktimetracker.publicid.PublicIdType;
import com.kiborisaway.tasktimetracker.publicid.id.TaskGroupId;
import org.springframework.stereotype.Component;

/** タスクグループの公開ID文字列を {@link TaskGroupId} へ変換します。 */
@Component
public class StringToTaskGroupIdConverter extends AbstractPublicIdConverter<TaskGroupId> {

  public StringToTaskGroupIdConverter(PublicIdCodec codec) {
    super(codec, PublicIdType.TASK_GROUP, TaskGroupId::new);
  }
}
