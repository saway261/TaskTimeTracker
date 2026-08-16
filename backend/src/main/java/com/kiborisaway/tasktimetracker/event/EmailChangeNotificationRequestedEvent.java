package com.kiborisaway.tasktimetracker.event;

/**
 * メールアドレス変更が要求された事実を、変更前のメールアドレスへ通知すべきことを表すイベントです。
 *
 * <p>変更前のメールアドレスが確認済みだった場合にのみ発行します（送信要否の判断はEmailChangeService側の責務）。
 */
public record EmailChangeNotificationRequestedEvent(String oldEmail, String newEmail) {
}
