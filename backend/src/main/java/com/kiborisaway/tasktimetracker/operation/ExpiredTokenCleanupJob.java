package com.kiborisaway.tasktimetracker.operation;

import com.kiborisaway.tasktimetracker.repository.EmailChangeRequestRepository;
import com.kiborisaway.tasktimetracker.repository.EmailVerificationTokenRepository;
import com.kiborisaway.tasktimetracker.repository.PasswordResetTokenRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 期限切れ・使用済みのトークン系テーブルを定期削除します（仕様書6.4）。
 *
 * <p>複数インスタンス化した場合は多重実行になり得るが、削除処理自体は冪等（同じ条件のレコードを
 * 削除するだけ）なので実害はない。
 */
@Component
public class ExpiredTokenCleanupJob {

  private static final Logger logger = LoggerFactory.getLogger(ExpiredTokenCleanupJob.class);
  private static final long RETENTION_HOURS = 24;

  private final EmailVerificationTokenRepository emailVerificationTokenRepository;
  private final EmailChangeRequestRepository emailChangeRequestRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final Clock clock;

  public ExpiredTokenCleanupJob(
      EmailVerificationTokenRepository emailVerificationTokenRepository,
      EmailChangeRequestRepository emailChangeRequestRepository,
      PasswordResetTokenRepository passwordResetTokenRepository,
      Clock clock) {
    this.emailVerificationTokenRepository = emailVerificationTokenRepository;
    this.emailChangeRequestRepository = emailChangeRequestRepository;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
    this.clock = clock;
  }

  @Scheduled(cron = "${app.auth.token-cleanup.cron:0 0 3 * * *}")
  public void run() {
    LocalDateTime threshold = LocalDateTime.now(clock).minusHours(RETENTION_HOURS);
    int verificationDeleted = emailVerificationTokenRepository.deleteExpiredOrUsed(threshold);
    int changeRequestDeleted = emailChangeRequestRepository.deleteExpiredOrUsed(threshold);
    int resetDeleted = passwordResetTokenRepository.deleteExpiredOrUsed(threshold);
    logger.info(
        "Expired token cleanup completed: emailVerificationTokens={}, "
            + "emailChangeRequests={}, passwordResetTokens={}",
        verificationDeleted, changeRequestDeleted, resetDeleted);
  }
}
