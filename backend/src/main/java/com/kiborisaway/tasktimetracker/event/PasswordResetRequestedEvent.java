package com.kiborisaway.tasktimetracker.event;

/**
 * パスワードリセットトークンが発行されたことを表すイベントです。登録ユーザーが見つかった場合にのみ発行します
 * （未登録メールでは発行しないことが、仕様書8.7のユーザー列挙対策そのものです）。
 */
public record PasswordResetRequestedEvent(int userId, String email, String rawToken) {
}
