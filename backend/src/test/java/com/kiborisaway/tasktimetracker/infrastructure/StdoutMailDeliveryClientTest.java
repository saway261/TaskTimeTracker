package com.kiborisaway.tasktimetracker.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class StdoutMailDeliveryClientTest {

  @Test
  void 送信成功_宛先件名本文を標準出力へ書き出すこと() {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    StdoutMailDeliveryClient sut =
        new StdoutMailDeliveryClient(new PrintStream(output, true, StandardCharsets.UTF_8));
    MailMessage message = new MailMessage(
        "user@example.com", "件名", "<p>html本文</p>", "text本文");

    sut.send(message);

    String printed = output.toString(StandardCharsets.UTF_8);
    assertThat(printed)
        .contains("user@example.com")
        .contains("件名")
        .contains("<p>html本文</p>")
        .contains("text本文");
  }
}
