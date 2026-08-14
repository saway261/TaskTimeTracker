package com.kiborisaway.tasktimetracker.security;

import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;
  private final Clock clock;

  public AppUserDetailsService(UserRepository userRepository, Clock clock) {
    this.userRepository = userRepository;
    this.clock = clock;
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    String normalizedEmail = normalizeEmail(email);
    AppUser user = userRepository.findByEmail(normalizedEmail);
    if (user == null) {
      throw new UsernameNotFoundException("user not found");
    }
    if (Boolean.TRUE.equals(user.getPasswordChangeRequired())
        && !user.getTemporaryPasswordExpiresAt().isAfter(LocalDateTime.now(clock))) {
      throw new UsernameNotFoundException("user not found");
    }
    return new AuthenticatedUser(user);
  }

  static String normalizeEmail(String email) {
    if (email == null) {
      return "";
    }
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
