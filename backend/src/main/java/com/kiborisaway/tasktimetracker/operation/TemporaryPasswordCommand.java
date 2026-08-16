package com.kiborisaway.tasktimetracker.operation;

import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.repository.EmailChangeRequestRepository;
import com.kiborisaway.tasktimetracker.repository.UserRepository;
import com.kiborisaway.tasktimetracker.service.SessionInvalidationService;
import com.kiborisaway.tasktimetracker.service.UserService;
import java.io.PrintStream;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionOperations;

@Component
@Profile("ops")
public class TemporaryPasswordCommand implements ApplicationRunner {

  private static final Logger logger =
      LoggerFactory.getLogger(TemporaryPasswordCommand.class);
  private static final String PASSWORD_ALPHABET =
      "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
  private static final int PASSWORD_LENGTH = 20;
  private static final long EXPIRATION_HOURS = 72;

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final SessionInvalidationService sessionInvalidationService;
  private final EmailChangeRequestRepository emailChangeRequestRepository;
  private final Clock clock;
  private final TransactionOperations transactionOperations;
  private final SecureRandom secureRandom;
  private final PrintStream standardOutput;
  private final ConfigurableApplicationContext applicationContext;
  private final String targetEmail;

  @Autowired
  public TemporaryPasswordCommand(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      SessionInvalidationService sessionInvalidationService,
      EmailChangeRequestRepository emailChangeRequestRepository,
      Clock clock,
      PlatformTransactionManager transactionManager,
      ConfigurableApplicationContext applicationContext,
      @Value("${app.ops.temporary-password.email:}") String targetEmail) {
    this(
        userRepository,
        passwordEncoder,
        sessionInvalidationService,
        emailChangeRequestRepository,
        clock,
        new TransactionTemplate(transactionManager),
        new SecureRandom(),
        System.out,
        applicationContext,
        targetEmail);
  }

  TemporaryPasswordCommand(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      SessionInvalidationService sessionInvalidationService,
      EmailChangeRequestRepository emailChangeRequestRepository,
      Clock clock,
      TransactionOperations transactionOperations,
      SecureRandom secureRandom,
      PrintStream standardOutput,
      ConfigurableApplicationContext applicationContext,
      String targetEmail) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.sessionInvalidationService = sessionInvalidationService;
    this.emailChangeRequestRepository = emailChangeRequestRepository;
    this.clock = clock;
    this.transactionOperations = transactionOperations;
    this.secureRandom = secureRandom;
    this.standardOutput = standardOutput;
    this.applicationContext = applicationContext;
    this.targetEmail = targetEmail;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (targetEmail == null || targetEmail.isBlank()) {
      throw new IllegalArgumentException(
          "app.ops.temporary-password.email must be specified");
    }

    String normalizedEmail = UserService.normalizeEmail(targetEmail);
    AppUser user = userRepository.findByEmail(normalizedEmail);
    if (user == null) {
      logger.warn("Temporary password issuance failed: target user was not found");
      throw new IllegalArgumentException("target user was not found");
    }

    String temporaryPassword = generateTemporaryPassword();
    String passwordHash = passwordEncoder.encode(temporaryPassword);
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDateTime expiresAt = now.plusHours(EXPIRATION_HOURS);

    transactionOperations.executeWithoutResult(status -> {
      int updatedRows = userRepository.updateTemporaryPassword(
          user.getId(), passwordHash, expiresAt, now);
      if (updatedRows != 1) {
        throw new IllegalStateException("temporary password update failed");
      }
      emailChangeRequestRepository.invalidateAllForUser(user.getId(), now);
      sessionInvalidationService.invalidateAll(user.getId());
    });

    logger.info("Temporary password issued: userId={}", user.getId());
    standardOutput.println("Temporary password: " + temporaryPassword);
    standardOutput.flush();
    applicationContext.close();
  }

  String generateTemporaryPassword() {
    StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
    for (int index = 0; index < PASSWORD_LENGTH; index++) {
      password.append(PASSWORD_ALPHABET.charAt(
          secureRandom.nextInt(PASSWORD_ALPHABET.length())));
    }
    return password.toString();
  }
}
