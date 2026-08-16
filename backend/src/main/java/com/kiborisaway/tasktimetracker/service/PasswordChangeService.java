package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.dto.auth.PasswordChangeRequest;
import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.exception.PasswordChangeNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.PasswordPolicyViolationException;
import com.kiborisaway.tasktimetracker.repository.EmailChangeRequestRepository;
import com.kiborisaway.tasktimetracker.repository.PasswordResetTokenRepository;
import com.kiborisaway.tasktimetracker.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordChangeService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final PasswordPolicy passwordPolicy;
  private final SessionInvalidationService sessionInvalidationService;
  private final EmailChangeRequestRepository emailChangeRequestRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final Clock clock;

  public PasswordChangeService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      PasswordPolicy passwordPolicy,
      SessionInvalidationService sessionInvalidationService,
      EmailChangeRequestRepository emailChangeRequestRepository,
      PasswordResetTokenRepository passwordResetTokenRepository,
      Clock clock) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.passwordPolicy = passwordPolicy;
    this.sessionInvalidationService = sessionInvalidationService;
    this.emailChangeRequestRepository = emailChangeRequestRepository;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
    this.clock = clock;
  }

  @Transactional
  public void changePassword(int userId, PasswordChangeRequest request) {
    AppUser user = userRepository.findById(userId);
    if (user == null
        || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw PasswordChangeNotAllowedException.currentPasswordIncorrect();
    }
    if (!passwordPolicy.isValid(request.newPassword(), user.getEmail())) {
      throw new PasswordPolicyViolationException();
    }
    if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
      throw PasswordChangeNotAllowedException.newPasswordUnchanged();
    }

    LocalDateTime now = LocalDateTime.now(clock);
    userRepository.updatePassword(userId, passwordEncoder.encode(request.newPassword()), now);
    emailChangeRequestRepository.invalidateAllForUser(userId, now);
    passwordResetTokenRepository.invalidateAllForUser(userId, now);
    sessionInvalidationService.invalidateAll(userId);
  }
}
