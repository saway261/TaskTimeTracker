package com.kiborisaway.tasktimetracker.infrastructure;

/**
 * メール送信事業者に依存しない送信インターフェース。実装はResend用と、
 * ローカル開発用の標準出力書き出しの2種類を持つ（config.MailConfigで切り替える）。
 */
public interface MailDeliveryClient {

  void send(MailMessage message);
}
