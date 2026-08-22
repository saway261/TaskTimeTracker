package com.kiborisaway.tasktimetracker.publicid.converter;

import com.kiborisaway.tasktimetracker.publicid.PublicIdCodec;
import com.kiborisaway.tasktimetracker.publicid.PublicIdType;
import com.kiborisaway.tasktimetracker.publicid.id.MemoId;
import org.springframework.stereotype.Component;

/** メモの公開ID文字列を {@link MemoId} へ変換します。 */
@Component
public class StringToMemoIdConverter extends AbstractPublicIdConverter<MemoId> {

  public StringToMemoIdConverter(PublicIdCodec codec) {
    super(codec, PublicIdType.MEMO, MemoId::new);
  }
}
