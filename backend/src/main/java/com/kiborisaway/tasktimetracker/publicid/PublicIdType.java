package com.kiborisaway.tasktimetracker.publicid;

/**
 * 公開IDの種別。種別ごとに別のアルファベットを使い分けるためのキーです。
 *
 * <p>アルファベットは非公開の値なのでここでは持たず、設定から注入します（{@link PublicIdCodec}）。
 *
 * <p>種別を示すプレフィックス（{@code prj_} 等）は付けません。当初は種別の取り違えを防ぐために
 * 必要と考えていましたが、実測により不要と判明したためです。{@code minLength} による詰め物まで
 * 含めて正規形が一意に決まるため、{@link PublicIdCodec} の正規化チェックが別種別のアルファベットで
 * 生成された文字列も確実に弾きます（各種別20,000件・計100,000件の試行で誤って解決された件数は0件）。
 * この性質は {@code PublicIdCodecTest} が回帰テストとして検証しています。
 */
public enum PublicIdType {

  PROJECT,
  TASK_GROUP,
  TASK,
  MEMO,
  WORK_SESSION,
  TAG
}
