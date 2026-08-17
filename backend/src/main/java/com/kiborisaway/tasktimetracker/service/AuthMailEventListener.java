package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.event.EmailChangeConfirmationRequestedEvent;
import com.kiborisaway.tasktimetracker.event.EmailChangeNotificationRequestedEvent;
import com.kiborisaway.tasktimetracker.event.EmailVerificationRequestedEvent;
import com.kiborisaway.tasktimetracker.event.PasswordResetRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 認証関連メールの送信をDBコミット後まで遅延させるリスナーです（仕様書8.5.3）。
 *
 * <p>メール送信の失敗はAPIレスポンスへ影響させないため、ここで例外を捕捉しログへ記録するだけに留めます。
 */
@Component
public class AuthMailEventListener {

  private static final Logger logger = LoggerFactory.getLogger(AuthMailEventListener.class);

  private final MailService mailService;

  public AuthMailEventListener(MailService mailService) {
    this.mailService = mailService;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onEmailVerificationRequested(EmailVerificationRequestedEvent event) {
    try {
      mailService.sendEmailVerification(event.email(), event.rawToken());
    } catch (RuntimeException ex) {
      logger.error("Failed to send email verification mail: userId={}", event.userId(), ex);
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onEmailChangeConfirmationRequested(EmailChangeConfirmationRequestedEvent event) {
    try {
      mailService.sendEmailChangeConfirmation(event.newEmail(), event.rawToken());
    } catch (RuntimeException ex) {
      logger.error("Failed to send email change confirmation mail", ex);
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onEmailChangeNotificationRequested(EmailChangeNotificationRequestedEvent event) {
    try {
      mailService.sendEmailChangeNotification(event.oldEmail(), event.newEmail());
    } catch (RuntimeException ex) {
      logger.error("Failed to send email change notification mail", ex);
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
    try {
      mailService.sendPasswordReset(event.email(), event.rawToken());
    } catch (RuntimeException ex) {
      logger.error("Failed to send password reset mail: userId={}", event.userId(), ex);
    }
  }
}
