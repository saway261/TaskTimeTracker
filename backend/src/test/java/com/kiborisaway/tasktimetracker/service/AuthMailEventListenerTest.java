package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.kiborisaway.tasktimetracker.event.EmailChangeConfirmationRequestedEvent;
import com.kiborisaway.tasktimetracker.event.EmailChangeNotificationRequestedEvent;
import com.kiborisaway.tasktimetracker.event.EmailVerificationRequestedEvent;
import org.junit.jupiter.api.Test;

class AuthMailEventListenerTest {

  @Test
  void 確認メール発行イベント_MailServiceを呼び出すこと() {
    MailService mailService = mock(MailService.class);
    AuthMailEventListener sut = new AuthMailEventListener(mailService);

    sut.onEmailVerificationRequested(
        new EmailVerificationRequestedEvent(1, "user@example.com", "raw-token"));

    verify(mailService).sendEmailVerification("user@example.com", "raw-token");
  }

  @Test
  void 確認メール発行イベント_送信失敗しても例外を伝播しないこと() {
    MailService mailService = mock(MailService.class);
    doThrow(new RuntimeException("send failed"))
        .when(mailService).sendEmailVerification("user@example.com", "raw-token");
    AuthMailEventListener sut = new AuthMailEventListener(mailService);

    assertThatCode(() -> sut.onEmailVerificationRequested(
        new EmailVerificationRequestedEvent(1, "user@example.com", "raw-token")))
        .doesNotThrowAnyException();
  }

  @Test
  void 変更確定メールイベント_MailServiceを呼び出すこと() {
    MailService mailService = mock(MailService.class);
    AuthMailEventListener sut = new AuthMailEventListener(mailService);

    sut.onEmailChangeConfirmationRequested(
        new EmailChangeConfirmationRequestedEvent("new@example.com", "raw-token"));

    verify(mailService).sendEmailChangeConfirmation("new@example.com", "raw-token");
  }

  @Test
  void 変更通知メールイベント_MailServiceを呼び出すこと() {
    MailService mailService = mock(MailService.class);
    AuthMailEventListener sut = new AuthMailEventListener(mailService);

    sut.onEmailChangeNotificationRequested(
        new EmailChangeNotificationRequestedEvent("old@example.com", "new@example.com"));

    verify(mailService).sendEmailChangeNotification("old@example.com", "new@example.com");
  }
}
