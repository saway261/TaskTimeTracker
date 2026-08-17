package com.kiborisaway.tasktimetracker.operation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.repository.EmailChangeRequestRepository;
import com.kiborisaway.tasktimetracker.repository.EmailVerificationTokenRepository;
import com.kiborisaway.tasktimetracker.repository.PasswordResetTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ExpiredTokenCleanupJobTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-16T03:00:00Z"), ZoneOffset.UTC);

  @Test
  void 実行成功_24時間より前の期限切れ_使用済みレコードを3テーブルから削除すること() {
    EmailVerificationTokenRepository emailVerificationTokenRepository =
        mock(EmailVerificationTokenRepository.class);
    EmailChangeRequestRepository emailChangeRequestRepository =
        mock(EmailChangeRequestRepository.class);
    PasswordResetTokenRepository passwordResetTokenRepository =
        mock(PasswordResetTokenRepository.class);
    LocalDateTime expectedThreshold = LocalDateTime.now(CLOCK).minusHours(24);
    when(emailVerificationTokenRepository.deleteExpiredOrUsed(expectedThreshold)).thenReturn(2);
    when(emailChangeRequestRepository.deleteExpiredOrUsed(expectedThreshold)).thenReturn(1);
    when(passwordResetTokenRepository.deleteExpiredOrUsed(expectedThreshold)).thenReturn(3);
    ExpiredTokenCleanupJob sut = new ExpiredTokenCleanupJob(
        emailVerificationTokenRepository, emailChangeRequestRepository,
        passwordResetTokenRepository, CLOCK);

    sut.run();

    verify(emailVerificationTokenRepository).deleteExpiredOrUsed(expectedThreshold);
    verify(emailChangeRequestRepository).deleteExpiredOrUsed(expectedThreshold);
    verify(passwordResetTokenRepository).deleteExpiredOrUsed(expectedThreshold);
  }
}
