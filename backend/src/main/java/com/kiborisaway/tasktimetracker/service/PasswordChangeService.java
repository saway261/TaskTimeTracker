package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.dto.auth.PasswordChangeRequest;
import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.exception.PasswordChangeNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.PasswordPolicyViolationException;
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
  private final Clock clock;

  public PasswordChangeService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      PasswordPolicy passwordPolicy,
      SessionInvalidationService sessionInvalidationService,
      Clock clock) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.passwordPolicy = passwordPolicy;
    this.sessionInvalidationService = sessionInvalidationService;
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

    userRepository.updatePassword(
        userId,
        passwordEncoder.encode(request.newPassword()),
        LocalDateTime.now(clock));
    sessionInvalidationService.invalidateAll(userId);
  }
}
