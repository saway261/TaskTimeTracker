package com.kiborisaway.tasktimetracker.publicid.converter;

import com.kiborisaway.tasktimetracker.publicid.PublicIdCodec;
import com.kiborisaway.tasktimetracker.publicid.PublicIdType;
import com.kiborisaway.tasktimetracker.publicid.id.TagId;
import org.springframework.stereotype.Component;

/** タグの公開ID文字列を {@link TagId} へ変換します。 */
@Component
public class StringToTagIdConverter extends AbstractPublicIdConverter<TagId> {

  public StringToTagIdConverter(PublicIdCodec codec) {
    super(codec, PublicIdType.TAG, TagId::new);
  }
}
