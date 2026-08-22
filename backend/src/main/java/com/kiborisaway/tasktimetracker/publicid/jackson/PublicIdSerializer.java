package com.kiborisaway.tasktimetracker.publicid.jackson;

import com.kiborisaway.tasktimetracker.publicid.PublicIdCodec;
import com.kiborisaway.tasktimetracker.publicid.id.PublicId;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * 型付きIDを公開ID文字列としてJSONへ出力します。
 *
 * <p>{@link PublicId} インタフェースに対して登録するため、実装レコードを追加しても
 * シリアライザ側の変更は不要です。種別は識別子自身が持っています。
 */
public class PublicIdSerializer extends ValueSerializer<PublicId> {

  private final PublicIdCodec codec;

  public PublicIdSerializer(PublicIdCodec codec) {
    this.codec = codec;
  }

  @Override
  public void serialize(PublicId id, JsonGenerator generator, SerializationContext context)
      throws JacksonException {
    generator.writeString(codec.encode(id.type(), id.value()));
  }
}
