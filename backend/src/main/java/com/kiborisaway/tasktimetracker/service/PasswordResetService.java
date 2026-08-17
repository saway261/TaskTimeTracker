package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.data.entity.PasswordResetToken;
import com.kiborisaway.tasktimetracker.event.PasswordResetRequestedEvent;
import com.kiborisaway.tasktimetracker.exception.PasswordPolicyViolationException;
import com.kiborisaway.tasktimetracker.exception.PasswordResetInvalidException;
import com.kiborisaway.tasktimetracker.repository.EmailChangeRequestRepository;
import com.kiborisaway.tasktimetracker.repository.EmailVerificationTokenRepository;
import com.kiborisaway.tasktimetracker.repository.PasswordResetTokenRepository;
import com.kiborisaway.tasktimetracker.repository.UserRepository;
import com.kiborisaway.tasktimetracker.security.TokenGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

  private final UserRepository userRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final EmailVerificationTokenRepository emailVerificationTokenRepository;
  private final EmailChangeRequestRepository emailChangeRequestRepository;
  private final PasswordEncoder passwordEncoder;
  private final PasswordPolicy passwordPolicy;
  private final TokenGenerator tokenGenerator;
  private final ApplicationEventPublisher eventPublisher;
  private final SessionInvalidationService sessionInvalidationService;
  private final Clock clock;
  private final Duration tokenTtl;

  public PasswordResetService(
      UserRepository userRepository,
      PasswordResetTokenRepository passwordResetTokenRepository,
      EmailVerificationTokenRepository emailVerificationTokenRepository,
      EmailChangeRequestRepository emailChangeRequestRepository,
      PasswordEncoder passwordEncoder,
      PasswordPolicy passwordPolicy,
      TokenGenerator tokenGenerator,
      ApplicationEventPublisher eventPublisher,
      SessionInvalidationService sessionInvalidationService,
      Clock clock,
      @Value("${app.auth.password-reset.token-ttl:30m}") Duration tokenTtl) {
    this.userRepository = userRepository;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
    this.emailVerificationTokenRepository = emailVerificationTokenRepository;
    this.emailChangeRequestRepository = emailChangeRequestRepository;
    this.passwordEncoder = passwordEncoder;
    this.passwordPolicy = passwordPolicy;
    this.tokenGenerator = tokenGenerator;
    this.eventPublisher = eventPublisher;
    this.sessionInvalidationService = sessionInvalidationService;
    this.clock = clock;
    this.tokenTtl = tokenTtl;
  }

  @Transactional
  public void requestReset(String email) {
    AppUser user = userRepository.findByEmail(email);
    if (user == null) {
      // 登録有無をクライアントへ開示しないため、未登録メールは何もせず正常終了する（仕様書8.7）。
      return;
    }

    LocalDateTime now = LocalDateTime.now(clock);
    passwordResetTokenRepository.invalidateAllForUser(user.getId(), now);
    String rawToken = tokenGenerator.generateRawToken();
    PasswordResetToken token = new PasswordResetToken(
        null, user.getId(), tokenGenerator.hash(rawToken), now.plus(tokenTtl), null, now);
    passwordResetTokenRepository.insert(token);
    eventPublisher.publishEvent(
        new PasswordResetRequestedEvent(user.getId(), user.getEmail(), rawToken));
  }

  @Transactional
  public void confirmReset(String rawToken, String newPassword) {
    LocalDateTime now = LocalDateTime.now(clock);
    PasswordResetToken token = passwordResetTokenRepository.findValidForUpdate(
        tokenGenerator.hash(rawToken), now);
    if (token == null) {
      throw new PasswordResetInvalidException();
    }

    AppUser user = userRepository.findById(token.getUserId());
    if (!passwordPolicy.isValid(newPassword, user.getEmail())) {
      throw new PasswordPolicyViolationException();
    }

    userRepository.updatePassword(user.getId(), passwordEncoder.encode(newPassword), now);
    if (user.getEmailVerifiedAt() == null) {
      userRepository.updateEmailVerified(user.getId(), now, now);
      emailVerificationTokenRepository.invalidateAllForUser(user.getId(), now);
    }
    passwordResetTokenRepository.invalidateAllForUser(user.getId(), now);
    // 仕様書8.8には明記されていないが、パスワードリセットは認証済み変更（8.6手順7）・
    // 一時パスワード発行（8.9手順5）と同様に保留中のメールアドレス変更要求も無効化する。
    emailChangeRequestRepository.invalidateAllForUser(user.getId(), now);
    sessionInvalidationService.invalidateAll(user.getId());
  }
}
