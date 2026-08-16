package com.kiborisaway.tasktimetracker.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.repository.EmailChangeRequestRepository;
import com.kiborisaway.tasktimetracker.repository.UserRepository;
import com.kiborisaway.tasktimetracker.service.SessionInvalidationService;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionOperations;

class TemporaryPasswordCommandTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC);

  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private SessionInvalidationService sessionInvalidationService;
  private EmailChangeRequestRepository emailChangeRequestRepository;
  private TransactionOperations transactionOperations;
  private SecureRandom secureRandom;
  private ByteArrayOutputStream output;
  private ConfigurableApplicationContext applicationContext;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    sessionInvalidationService = mock(SessionInvalidationService.class);
    emailChangeRequestRepository = mock(EmailChangeRequestRepository.class);
    transactionOperations = mock(TransactionOperations.class);
    secureRandom = mock(SecureRandom.class);
    output = new ByteArrayOutputStream();
    applicationContext = mock(ConfigurableApplicationContext.class);
    doAnswer(invocation -> {
      Consumer<TransactionStatus> callback = invocation.getArgument(0);
      callback.accept(mock(TransactionStatus.class));
      return null;
    }).when(transactionOperations).executeWithoutResult(any());
  }

  @Test
  void 発行成功_メールを正規化してハッシュと期限を保存しセッションを失効すること() {
    AppUser user = new AppUser();
    user.setId(7);
    user.setEmail("user@example.com");
    when(userRepository.findByEmail("user@example.com")).thenReturn(user);
    when(secureRandom.nextInt(anyInt())).thenReturn(0);
    when(passwordEncoder.encode("AAAAAAAAAAAAAAAAAAAA")).thenReturn("{bcrypt}hash");
    when(userRepository.updateTemporaryPassword(anyInt(), any(), any(), any()))
        .thenReturn(1);
    TemporaryPasswordCommand sut = command(" User@Example.com ");

    sut.run(new DefaultApplicationArguments());

    verify(userRepository).updateTemporaryPassword(
        7,
        "{bcrypt}hash",
        LocalDateTime.of(2026, 8, 17, 0, 0),
        LocalDateTime.of(2026, 8, 14, 0, 0));
    verify(emailChangeRequestRepository).invalidateAllForUser(7, LocalDateTime.of(2026, 8, 14, 0, 0));
    verify(sessionInvalidationService).invalidateAll(7);
    verify(applicationContext).close();
    assertThat(output.toString(StandardCharsets.UTF_8))
        .isEqualTo("Temporary password: AAAAAAAAAAAAAAAAAAAA" + System.lineSeparator())
        .doesNotContain("{bcrypt}hash");
  }

  @Test
  void 生成成功_紛らわしい文字を含まない20文字を生成すること() {
    TemporaryPasswordCommand sut = command("user@example.com");

    for (int attempt = 0; attempt < 100; attempt++) {
      assertThat(sut.generateTemporaryPassword())
          .hasSize(20)
          .matches("[A-HJ-NP-Za-km-z2-9]{20}")
          .doesNotContain("0", "O", "1", "l", "I");
    }
  }

  @Test
  void 発行失敗_対象ユーザーが存在しなければ更新も出力もしないこと() {
    TemporaryPasswordCommand sut = command("unknown@example.com");

    assertThatThrownBy(() -> sut.run(new DefaultApplicationArguments()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("target user was not found");
    verify(passwordEncoder, never()).encode(any());
    verify(sessionInvalidationService, never()).invalidateAll(anyInt());
    verify(applicationContext, never()).close();
    assertThat(output.toString(StandardCharsets.UTF_8)).isEmpty();
  }

  @Test
  void 発行失敗_メール指定がなければ非正常終了用の例外を送出すること() {
    TemporaryPasswordCommand sut = command(" ");

    assertThatThrownBy(() -> sut.run(new DefaultApplicationArguments()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("app.ops.temporary-password.email must be specified");
    verify(userRepository, never()).findByEmail(any());
    assertThat(output.toString(StandardCharsets.UTF_8)).isEmpty();
  }

  private TemporaryPasswordCommand command(String targetEmail) {
    return new TemporaryPasswordCommand(
        userRepository,
        passwordEncoder,
        sessionInvalidationService,
        emailChangeRequestRepository,
        CLOCK,
        transactionOperations,
        secureRandom,
        new PrintStream(output, true, StandardCharsets.UTF_8),
        applicationContext,
        targetEmail);
  }
}
