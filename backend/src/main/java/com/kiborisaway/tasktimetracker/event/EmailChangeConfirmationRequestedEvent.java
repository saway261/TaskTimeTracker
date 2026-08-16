package com.kiborisaway.tasktimetracker.event;

/**
 * メールアドレス変更の確定を求めるメールを、新しいメールアドレスへ送るべきことを表すイベントです。
 */
public record EmailChangeConfirmationRequestedEvent(String newEmail, String rawToken) {
}
