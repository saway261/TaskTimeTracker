package com.kiborisaway.tasktimetracker.publicid.converter;

import com.kiborisaway.tasktimetracker.publicid.PublicIdCodec;
import com.kiborisaway.tasktimetracker.publicid.PublicIdType;
import com.kiborisaway.tasktimetracker.publicid.id.TaskId;
import org.springframework.stereotype.Component;

/** タスクの公開ID文字列を {@link TaskId} へ変換します。 */
@Component
public class StringToTaskIdConverter extends AbstractPublicIdConverter<TaskId> {

  public StringToTaskIdConverter(PublicIdCodec codec) {
    super(codec, PublicIdType.TASK, TaskId::new);
  }
}
