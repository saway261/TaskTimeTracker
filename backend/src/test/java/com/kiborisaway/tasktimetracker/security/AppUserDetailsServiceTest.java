package com.kiborisaway.tasktimetracker.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-13T00:00:00Z"), ZoneOffset.UTC);

  @Mock
  private UserRepository userRepository;

  @Test
  void ユーザー検索_メールを正規化して内部IDをPrincipal名に設定すること() {
    AppUser user = new AppUser(
        7, "user@example.com", "{bcrypt}hash", true, false, null,
        LocalDateTime.now(), LocalDateTime.now());
    when(userRepository.findByEmail("user@example.com")).thenReturn(user);
    AppUserDetailsService sut = new AppUserDetailsService(userRepository, CLOCK);

    AuthenticatedUser actual = (AuthenticatedUser) sut.loadUserByUsername(" User@Example.com ");

    assertThat(actual.getUserId()).isEqualTo(7);
    assertThat(actual.getUsername()).isEqualTo("7");
    assertThat(actual.getEmail()).isEqualTo("user@example.com");
    assertThat(actual.getPassword()).isEqualTo("{bcrypt}hash");
    assertThat(actual.isEnabled()).isTrue();
    assertThat(actual.isPasswordChangeRequired()).isFalse();
    verify(userRepository).findByEmail("user@example.com");
  }

  @Test
  void ユーザー検索_存在しないメールならUsernameNotFoundExceptionになること() {
    when(userRepository.findByEmail("missing@example.com")).thenReturn(null);
    AppUserDetailsService sut = new AppUserDetailsService(userRepository, CLOCK);

    assertThatThrownBy(() -> sut.loadUserByUsername("missing@example.com"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessage("user not found");
  }

  @Test
  void ユーザー検索_一時パスワードが期限切れなら存在しないユーザーと同じ例外になること() {
    AppUser user = new AppUser(
        7, "user@example.com", "{bcrypt}hash", true, true,
        LocalDateTime.of(2026, 8, 13, 0, 0),
        LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK));
    when(userRepository.findByEmail("user@example.com")).thenReturn(user);
    AppUserDetailsService sut = new AppUserDetailsService(userRepository, CLOCK);

    assertThatThrownBy(() -> sut.loadUserByUsername("user@example.com"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessage("user not found");
  }
}
