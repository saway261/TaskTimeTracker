package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.dto.auth.EmailChangeRequestBody;
import com.kiborisaway.tasktimetracker.data.dto.auth.PendingEmailResponse;
import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.data.entity.EmailChangeRequest;
import com.kiborisaway.tasktimetracker.event.EmailChangeConfirmationRequestedEvent;
import com.kiborisaway.tasktimetracker.event.EmailChangeNotificationRequestedEvent;
import com.kiborisaway.tasktimetracker.exception.EmailChangeNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.EmailChangeRequestInvalidException;
import com.kiborisaway.tasktimetracker.exception.EmailUnavailableException;
import com.kiborisaway.tasktimetracker.repository.EmailChangeRequestRepository;
import com.kiborisaway.tasktimetracker.repository.EmailVerificationTokenRepository;
import com.kiborisaway.tasktimetracker.repository.UserRepository;
import com.kiborisaway.tasktimetracker.security.TokenGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailChangeService {

  private final UserRepository userRepository;
  private final EmailChangeRequestRepository requestRepository;
  private final EmailVerificationTokenRepository verificationTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenGenerator tokenGenerator;
  private final ApplicationEventPublisher eventPublisher;
  private final SessionInvalidationService sessionInvalidationService;
  private final Clock clock;
  private final Duration tokenTtl;

  public EmailChangeService(
      UserRepository userRepository,
      EmailChangeRequestRepository requestRepository,
      EmailVerificationTokenRepository verificationTokenRepository,
      PasswordEncoder passwordEncoder,
      TokenGenerator tokenGenerator,
      ApplicationEventPublisher eventPublisher,
      SessionInvalidationService sessionInvalidationService,
      Clock clock,
      @Value("${app.auth.email-change.token-ttl:24h}") Duration tokenTtl) {
    this.userRepository = userRepository;
    this.requestRepository = requestRepository;
    this.verificationTokenRepository = verificationTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenGenerator = tokenGenerator;
    this.eventPublisher = eventPublisher;
    this.sessionInvalidationService = sessionInvalidationService;
    this.clock = clock;
    this.tokenTtl = tokenTtl;
  }

  @Transactional
  public PendingEmailResponse requestChange(int userId, EmailChangeRequestBody body) {
    AppUser user = userRepository.findById(userId);
    if (user == null
        || !passwordEncoder.matches(body.currentPassword(), user.getPasswordHash())) {
      throw EmailChangeNotAllowedException.currentPasswordIncorrect();
    }
    String newEmail = body.newEmail();
    if (newEmail.equals(user.getEmail())) {
      throw EmailChangeNotAllowedException.sameAsCurrentEmail();
    }
    if (userRepository.existsByEmail(newEmail)) {
      throw new EmailUnavailableException();
    }

    LocalDateTime now = LocalDateTime.now(clock);
    requestRepository.invalidateAllForUser(userId, now);
    String rawToken = tokenGenerator.generateRawToken();
    EmailChangeRequest request = new EmailChangeRequest(
        null, userId, newEmail, tokenGenerator.hash(rawToken), now.plus(tokenTtl), null, now);
    requestRepository.insert(request);

    eventPublisher.publishEvent(new EmailChangeConfirmationRequestedEvent(newEmail, rawToken));
    if (user.getEmailVerifiedAt() != null) {
      eventPublisher.publishEvent(
          new EmailChangeNotificationRequestedEvent(user.getEmail(), newEmail));
    }
    return new PendingEmailResponse(newEmail);
  }

  @Transactional
  public void confirm(String rawToken) {
    LocalDateTime now = LocalDateTime.now(clock);
    EmailChangeRequest request =
        requestRepository.findValidForUpdate(tokenGenerator.hash(rawToken), now);
    if (request == null) {
      throw new EmailChangeRequestInvalidException();
    }

    if (userRepository.existsByEmail(request.getNewEmail())) {
      requestRepository.markUsed(request.getId(), now);
      throw new EmailUnavailableException();
    }

    try {
      userRepository.updateEmail(request.getUserId(), request.getNewEmail(), now, now);
    } catch (DataIntegrityViolationException ex) {
      requestRepository.markUsed(request.getId(), now);
      throw new EmailUnavailableException();
    }

    // password_reset_tokensはBE13で追加されるテーブルのため、その無効化はBE13が担う。
    requestRepository.invalidateAllForUser(request.getUserId(), now);
    verificationTokenRepository.invalidateAllForUser(request.getUserId(), now);
    sessionInvalidationService.invalidateAll(request.getUserId());
  }
}
