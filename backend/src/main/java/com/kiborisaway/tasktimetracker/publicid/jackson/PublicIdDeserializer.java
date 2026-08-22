package com.kiborisaway.tasktimetracker.publicid.jackson;

import com.kiborisaway.tasktimetracker.publicid.PublicIdCodec;
import com.kiborisaway.tasktimetracker.publicid.PublicIdType;
import com.kiborisaway.tasktimetracker.publicid.id.PublicId;
import java.util.function.IntFunction;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * リクエストボディに含まれる公開ID文字列を型付きIDへ戻します。
 *
 * <p>デシリアライザは具象型ごとに登録する必要がある（Jacksonが生成する型を知る必要がある）ため、
 * 種別と生成関数を受け取る形にして {@link PublicIdJacksonConfig} から6種を登録します。
 *
 * @param <T> 生成する型付きID
 */
public class PublicIdDeserializer<T extends PublicId> extends ValueDeserializer<T> {

  private final PublicIdCodec codec;
  private final PublicIdType type;
  private final IntFunction<T> factory;

  public PublicIdDeserializer(PublicIdCodec codec, PublicIdType type, IntFunction<T> factory) {
    this.codec = codec;
    this.type = type;
    this.factory = factory;
  }

  @Override
  public T deserialize(JsonParser parser, DeserializationContext context)
      throws JacksonException {
    String value = parser.getValueAsString();
    if (value == null) {
      return null;
    }
    return factory.apply(codec.decode(type, value));
  }
}
