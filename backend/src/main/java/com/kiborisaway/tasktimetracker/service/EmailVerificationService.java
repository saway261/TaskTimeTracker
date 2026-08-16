package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.data.entity.EmailVerificationToken;
import com.kiborisaway.tasktimetracker.event.EmailVerificationRequestedEvent;
import com.kiborisaway.tasktimetracker.exception.EmailVerificationInvalidException;
import com.kiborisaway.tasktimetracker.repository.EmailVerificationTokenRepository;
import com.kiborisaway.tasktimetracker.repository.UserRepository;
import com.kiborisaway.tasktimetracker.security.TokenGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {

  private final UserRepository userRepository;
  private final EmailVerificationTokenRepository tokenRepository;
  private final TokenGenerator tokenGenerator;
  private final ApplicationEventPublisher eventPublisher;
  private final SessionInvalidationService sessionInvalidationService;
  private final Clock clock;
  private final Duration tokenTtl;

  public EmailVerificationService(
      UserRepository userRepository,
      EmailVerificationTokenRepository tokenRepository,
      TokenGenerator tokenGenerator,
      ApplicationEventPublisher eventPublisher,
      SessionInvalidationService sessionInvalidationService,
      Clock clock,
      @Value("${app.auth.email-verification.token-ttl:24h}") Duration tokenTtl) {
    this.userRepository = userRepository;
    this.tokenRepository = tokenRepository;
    this.tokenGenerator = tokenGenerator;
    this.eventPublisher = eventPublisher;
    this.sessionInvalidationService = sessionInvalidationService;
    this.clock = clock;
    this.tokenTtl = tokenTtl;
  }

  @Transactional
  public void issueForRegistration(int userId, String email) {
    issueToken(userId, email);
  }

  @Transactional
  public void resend(int userId) {
    AppUser user = userRepository.findById(userId);
    if (user == null || user.getEmailVerifiedAt() != null) {
      return;
    }
    LocalDateTime now = LocalDateTime.now(clock);
    tokenRepository.invalidateAllForUser(userId, now);
    issueToken(userId, user.getEmail());
  }

  @Transactional
  public void confirm(String rawToken) {
    LocalDateTime now = LocalDateTime.now(clock);
    EmailVerificationToken token =
        tokenRepository.findByTokenHashForUpdate(tokenGenerator.hash(rawToken));
    if (token == null) {
      throw new EmailVerificationInvalidException();
    }

    AppUser user = userRepository.findById(token.getUserId());
    if (user != null && user.getEmailVerifiedAt() != null) {
      // 既に確認済みのユーザーのトークン再提示。メールクライアントの先読みで初回アクセスが消費される
      // 事故に備えた冪等性のため、この場合も何もせず成功として扱う（仕様書8.10.2）。
      return;
    }
    if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) {
      throw new EmailVerificationInvalidException();
    }

    userRepository.updateEmailVerified(token.getUserId(), now, now);
    tokenRepository.invalidateAllForUser(token.getUserId(), now);
    sessionInvalidationService.invalidateAll(token.getUserId());
  }

  private void issueToken(int userId, String email) {
    String rawToken = tokenGenerator.generateRawToken();
    LocalDateTime now = LocalDateTime.now(clock);
    EmailVerificationToken token = new EmailVerificationToken(
        null, userId, tokenGenerator.hash(rawToken), now.plus(tokenTtl), null, now);
    tokenRepository.insert(token);
    eventPublisher.publishEvent(new EmailVerificationRequestedEvent(userId, email, rawToken));
  }
}
