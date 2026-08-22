package com.kiborisaway.tasktimetracker.publicid.converter;

import com.kiborisaway.tasktimetracker.publicid.PublicIdCodec;
import com.kiborisaway.tasktimetracker.publicid.PublicIdType;
import java.beans.PropertyEditorSupport;

/**
 * 公開ID文字列を内部ID（{@code Integer}）へ変換する、フィールド限定の {@link java.beans.PropertyEditor}。
 *
 * <p>{@code AnalyticsQueryCondition}・{@code ReflectionTimelineQueryCondition} の
 * {@code projectId}・{@code tagId} のように、フィールドの型自体は {@code Integer} のまま
 * MyBatisへ渡す必要がある場合に使う。{@code @InitBinder} で
 * {@code WebDataBinder#registerCustomEditor(Class, String, PropertyEditor)} へ
 * フィールド名を指定して登録することで、そのプロパティだけ公開IDのデコードを挟める
 * （{@code Converter<String, Integer>} をグローバル登録すると、アプリ内の全ての
 * 整数バインディングを巻き込んで壊してしまうため採用しない）。
 *
 * <p>{@code setAsText} が例外を投げると、Springはそれを {@code TypeMismatchException} として
 * 捕捉し {@code BindingResult} のフィールドエラーへ積む。{@code @ModelAttribute} + {@code @Valid}
 * の組み合わせでは最終的に {@code MethodArgumentNotValidException}（400）になる。
 * これは本パラメータが元々「不正な形式は400」だった既存の挙動を維持するものであり、
 * パスパラメータ（{@code @PathVariable}）の404統一（0-2）とは意図的に扱いを分けている
 * （クエリの絞り込み条件と、リソースを一意に指す識別子とでは性質が異なるため）。
 */
public class PublicIdPropertyEditor extends PropertyEditorSupport {

  private final PublicIdCodec codec;
  private final PublicIdType type;

  public PublicIdPropertyEditor(PublicIdCodec codec, PublicIdType type) {
    this.codec = codec;
    this.type = type;
  }

  @Override
  public void setAsText(String text) {
    if (text == null || text.isEmpty()) {
      setValue(null);
      return;
    }
    setValue(codec.decode(type, text));
  }
}
