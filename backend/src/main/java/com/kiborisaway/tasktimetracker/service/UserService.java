package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.dto.auth.RegisterRequest;
import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.exception.EmailUnavailableException;
import com.kiborisaway.tasktimetracker.exception.PasswordPolicyViolationException;
import com.kiborisaway.tasktimetracker.repository.UserRepository;
import com.kiborisaway.tasktimetracker.security.AuthenticatedUser;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final PasswordPolicy passwordPolicy;
  private final EmailVerificationService emailVerificationService;
  private final TagService tagService;
  private final Clock clock;

  public UserService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      PasswordPolicy passwordPolicy,
      EmailVerificationService emailVerificationService,
      TagService tagService,
      Clock clock) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.passwordPolicy = passwordPolicy;
    this.emailVerificationService = emailVerificationService;
    this.tagService = tagService;
    this.clock = clock;
  }

  @Transactional
  public AuthenticatedUser register(RegisterRequest request) {
    String email = normalizeEmail(request.email());
    if (!passwordPolicy.isValid(request.password(), email)) {
      throw new PasswordPolicyViolationException();
    }
    if (userRepository.existsByEmail(email)) {
      throw new EmailUnavailableException();
    }

    LocalDateTime now = LocalDateTime.now(clock);
    AppUser user = new AppUser(
        null,
        email,
        passwordEncoder.encode(request.password()),
        true,
        false,
        null,
        now,
        now,
        null,
        false);
    try {
      userRepository.insert(user);
    } catch (DataIntegrityViolationException ex) {
      throw new EmailUnavailableException();
    }
    tagService.createPresetTags(user.getId());
    emailVerificationService.issueForRegistration(user.getId(), email);
    return new AuthenticatedUser(user);
  }

  public AppUser findById(int userId) {
    return userRepository.findById(userId);
  }

  public void completeOnboarding(int userId) {
    userRepository.updateOnboardingCompleted(userId, LocalDateTime.now(clock));
  }

  public static String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
