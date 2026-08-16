package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.kiborisaway.tasktimetracker.infrastructure.MailDeliveryClient;
import com.kiborisaway.tasktimetracker.infrastructure.MailMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MailServiceTest {

  @Test
  void 送信成功_MailDeliveryClientへそのまま委譲すること() {
    MailDeliveryClient mailDeliveryClient = mock(MailDeliveryClient.class);
    MailService sut = new MailService(mailDeliveryClient, "https://app.example.com");
    MailMessage message = new MailMessage("user@example.com", "件名", "html", "text");

    sut.send(message);

    verify(mailDeliveryClient).send(message);
  }

  @Test
  void URL生成成功_フロントエンド基底URLへパスとトークンを付与すること() {
    MailDeliveryClient mailDeliveryClient = mock(MailDeliveryClient.class);
    MailService sut = new MailService(mailDeliveryClient, "https://app.example.com");

    String url = sut.buildFrontendUrl("/verify-email", "raw-token-value");

    assertThat(url).isEqualTo("https://app.example.com/verify-email?token=raw-token-value");
  }

  @Test
  void メール確認送信_確認URLを含めて送信すること() {
    MailDeliveryClient mailDeliveryClient = mock(MailDeliveryClient.class);
    MailService sut = new MailService(mailDeliveryClient, "https://app.example.com");

    sut.sendEmailVerification("user@example.com", "raw-token");

    ArgumentCaptor<MailMessage> captor = ArgumentCaptor.forClass(MailMessage.class);
    verify(mailDeliveryClient).send(captor.capture());
    MailMessage message = captor.getValue();
    assertThat(message.to()).isEqualTo("user@example.com");
    assertThat(message.htmlBody())
        .contains("https://app.example.com/verify-email?token=raw-token");
    assertThat(message.textBody())
        .contains("https://app.example.com/verify-email?token=raw-token");
  }

  @Test
  void 変更確定メール送信_確定URLを含めて送信すること() {
    MailDeliveryClient mailDeliveryClient = mock(MailDeliveryClient.class);
    MailService sut = new MailService(mailDeliveryClient, "https://app.example.com");

    sut.sendEmailChangeConfirmation("new@example.com", "raw-token");

    ArgumentCaptor<MailMessage> captor = ArgumentCaptor.forClass(MailMessage.class);
    verify(mailDeliveryClient).send(captor.capture());
    MailMessage message = captor.getValue();
    assertThat(message.to()).isEqualTo("new@example.com");
    assertThat(message.htmlBody())
        .contains("https://app.example.com/verify-email-change?token=raw-token");
  }

  @Test
  void 変更通知メール送信_URLを含めずローカル部を伏せて送信すること() {
    MailDeliveryClient mailDeliveryClient = mock(MailDeliveryClient.class);
    MailService sut = new MailService(mailDeliveryClient, "https://app.example.com");

    sut.sendEmailChangeNotification("old@example.com", "newaddress@example.com");

    ArgumentCaptor<MailMessage> captor = ArgumentCaptor.forClass(MailMessage.class);
    verify(mailDeliveryClient).send(captor.capture());
    MailMessage message = captor.getValue();
    assertThat(message.to()).isEqualTo("old@example.com");
    assertThat(message.htmlBody()).contains("n***@example.com");
    assertThat(message.htmlBody()).doesNotContain("newaddress@example.com");
    assertThat(message.htmlBody()).doesNotContain("http");
  }
}
