package com.kiborisaway.tasktimetracker.publicid;

import lombok.Getter;

/**
 * 公開ID文字列が不正である（形式が誤っている、別種別のIDである、正規形でない）ことを表します。
 *
 * <p>公開IDは利用者にとって不透明な文字列であり、「形式が不正」と「存在しない」を
 * 区別する意味がありません。区別して返すと、文字列が正しい形式かどうかを外部から判別でき、
 * 総当たりの手がかりを与えてしまいます。そのためハンドラでは404として扱います。
 *
 * <p>例外メッセージには受け取った文字列を含めません。ログへ利用者入力をそのまま残さないためです。
 */
@Getter
public class PublicIdInvalidException extends RuntimeException {

  /** 期待していた種別。 */
  private final transient PublicIdType type;

  public PublicIdInvalidException(PublicIdType type) {
    super("invalid public id for " + type);
    this.type = type;
  }
}
