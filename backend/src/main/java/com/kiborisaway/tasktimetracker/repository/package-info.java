/**
 * 所有者スコープ（Repositoryにおけるuser_id条件付与）の規約。
 *
 * <ul>
 *   <li>外部（Controller）から直接渡されたIDを受け取るメソッドは、所有者条件を必須とする。
 *       例：{@code existsByIdAndUserId(id, userId)}、{@code findById(id, userId)}。
 *       更新・削除SQL自体にも所有者条件を含め、対象外レコードは更新件数0件として扱う（404の根拠にする）。</li>
 *   <li>同一トランザクション内で所有権検証済みのIDのみを使う内部カスケード
 *       （例：Task削除に伴う {@code deleteAllByTaskId}）は、所有者条件を省略してよい。
 *       省略するメソッドのJavadocへ「呼び出し前に所有権検証済みであること」を前提条件として明記する。</li>
 *   <li>親スコープ済みIDの一括取得（例：{@code findAllInProjects(projectIds)}）も、
 *       呼び出し元が所有者条件付きで取得したIDのみを渡す前提のため省略可。</li>
 * </ul>
 */
package com.kiborisaway.tasktimetracker.repository;
