package com.kiborisaway.tasktimetracker.publicid.converter;

import com.kiborisaway.tasktimetracker.publicid.PublicIdCodec;
import com.kiborisaway.tasktimetracker.publicid.PublicIdType;
import com.kiborisaway.tasktimetracker.publicid.id.WorkSessionId;
import org.springframework.stereotype.Component;

/** 作業セッションの公開ID文字列を {@link WorkSessionId} へ変換します。 */
@Component
public class StringToWorkSessionIdConverter extends AbstractPublicIdConverter<WorkSessionId> {

  public StringToWorkSessionIdConverter(PublicIdCodec codec) {
    super(codec, PublicIdType.WORK_SESSION, WorkSessionId::new);
  }
}
