package com.kiborisaway.tasktimetracker.event;

/**
 * メールアドレス確認トークンが発行されたことを表すイベントです。DBコミット後にメール送信するため
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} で購読します。
 */
public record EmailVerificationRequestedEvent(int userId, String email, String rawToken) {
}
