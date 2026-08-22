package com.kiborisaway.tasktimetracker.publicid.converter;

import com.kiborisaway.tasktimetracker.publicid.PublicIdCodec;
import com.kiborisaway.tasktimetracker.publicid.PublicIdType;
import com.kiborisaway.tasktimetracker.publicid.id.PublicId;
import java.util.function.IntFunction;
import org.springframework.core.convert.converter.Converter;

/**
 * 公開ID文字列を型付きIDへ変換するConverterの共通実装。
 *
 * <p>サブクラスを具象クラスとして定義するのは、Spring が {@code Converter<S, T>} の総称型を
 * クラス階層から解決するためです。ラムダで登録すると総称型を解決できず登録に失敗します。
 *
 * <p>{@code @Component} として登録すれば、Spring Boot が MVC の ConversionService へ
 * 自動で追加します。これ1つで {@code @PathVariable}・{@code @RequestParam}・
 * {@code @ModelAttribute} のすべてに効きます。
 *
 * @param <T> 変換後の型付きID
 */
public abstract class AbstractPublicIdConverter<T extends PublicId> implements Converter<String, T> {

  private final PublicIdCodec codec;
  private final PublicIdType type;
  private final IntFunction<T> factory;

  protected AbstractPublicIdConverter(PublicIdCodec codec, PublicIdType type,
      IntFunction<T> factory) {
    this.codec = codec;
    this.type = type;
    this.factory = factory;
  }

  @Override
  public T convert(String source) {
    return factory.apply(codec.decode(type, source));
  }
}
