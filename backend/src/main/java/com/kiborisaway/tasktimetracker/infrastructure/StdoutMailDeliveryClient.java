package com.kiborisaway.tasktimetracker.infrastructure;

import java.io.PrintStream;

/**
 * ローカル開発用の実装。ログフレームワークを経由すると秘密情報（本文中のトークン等）が
 * ログファイルへ残るため、標準出力へ直接書き出す。
 */
public class StdoutMailDeliveryClient implements MailDeliveryClient {

  private final PrintStream standardOutput;

  public StdoutMailDeliveryClient(PrintStream standardOutput) {
    this.standardOutput = standardOutput;
  }

  @Override
  public void send(MailMessage message) {
    standardOutput.println("===== Mail (stdout delivery) =====");
    standardOutput.println("To: " + message.to());
    standardOutput.println("Subject: " + message.subject());
    standardOutput.println("----- text body -----");
    standardOutput.println(message.textBody());
    standardOutput.println("----- html body -----");
    standardOutput.println(message.htmlBody());
    standardOutput.println("===================================");
    standardOutput.flush();
  }
}
