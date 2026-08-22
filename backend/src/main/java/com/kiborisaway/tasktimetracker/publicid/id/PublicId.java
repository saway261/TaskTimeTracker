package com.kiborisaway.tasktimetracker.publicid.id;

import com.kiborisaway.tasktimetracker.publicid.PublicIdType;

/**
 * 公開IDとしてやり取りする識別子の共通型。
 *
 * <p>Controllerのパラメータやレスポンスの項目をこの型にすることで、公開IDへの変換を
 * 付け忘れて内部の連番IDがそのまま漏れることを型で防ぎます。
 *
 * <p>種別ごとに別の型を用意しているのは、{@code Converter<String, Integer>} を登録すると
 * アプリ内の全ての整数バインディング（{@code page}、{@code estimatedMinutes} 等）を
 * 巻き込んで壊してしまうためです。
 */
public sealed interface PublicId
    permits ProjectId, TaskGroupId, TaskId, MemoId, WorkSessionId, TagId {

  /** 内部ID。Service層・Repository層へはこの値を渡します。 */
  int value();

  /** この識別子の種別。 */
  PublicIdType type();
}
