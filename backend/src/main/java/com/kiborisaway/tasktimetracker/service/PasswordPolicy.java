package com.kiborisaway.tasktimetracker.service;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

  public static final int MIN_LENGTH = 12;
  public static final int MAX_BYTES = 72;

  public boolean isValid(String password, String normalizedEmail) {
    return password != null
        && password.length() >= MIN_LENGTH
        && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES
        && !password.equals(normalizedEmail);
  }
}
