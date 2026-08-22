package com.kiborisaway.tasktimetracker.publicid.jackson;

import com.kiborisaway.tasktimetracker.data.entity.WorkSession;
import com.kiborisaway.tasktimetracker.publicid.PublicIdCodec;
import com.kiborisaway.tasktimetracker.publicid.PublicIdType;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

/**
 * {@link WorkSession} の {@code id}・{@code taskId} を公開ID文字列として出力します。
 *
 * <p>{@link WorkSession} はMyBatisの自動結果マッピング対象であり、フィールド型を
 * {@code WorkSessionId}/{@code TaskId} のような型付きIDへ直接変更すると、対応するTypeHandlerが
 * 無いためDB結果のマッピングが失敗する（{@code docs/dto_introduction_plan.md} の方針で
 * レスポンスをEntityそのまま返す設計になっているため、Repository層のマッピングとJSON出力の
 * 両立が必要になる）。
 *
 * <p>そのためEntityのフィールド型は {@code Integer} のまま維持し、この
 * {@link ValueSerializerModifier} でJSON出力時にだけ {@code WorkSession} の
 * {@code id}・{@code taskId} プロパティの直列化方法を差し替える。
 */
public class WorkSessionIdSerializerModifier extends ValueSerializerModifier {

  private final PublicIdCodec codec;

  public WorkSessionIdSerializerModifier(PublicIdCodec codec) {
    this.codec = codec;
  }

  @Override
  public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
      BeanDescription.Supplier beanDesc, List<BeanPropertyWriter> beanProperties) {
    if (beanDesc.getBeanClass() != WorkSession.class) {
      return beanProperties;
    }
    for (BeanPropertyWriter writer : beanProperties) {
      if ("id".equals(writer.getName())) {
        writer.assignSerializer(integerAsPublicId(PublicIdType.WORK_SESSION));
      } else if ("taskId".equals(writer.getName())) {
        writer.assignSerializer(integerAsPublicId(PublicIdType.TASK));
      }
    }
    return beanProperties;
  }

  private ValueSerializer<Object> integerAsPublicId(PublicIdType type) {
    return new ValueSerializer<>() {
      @Override
      public void serialize(Object value, JsonGenerator generator, SerializationContext context)
          throws JacksonException {
        generator.writeString(codec.encode(type, (Integer) value));
      }
    };
  }
}
