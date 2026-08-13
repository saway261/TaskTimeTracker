package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

  private final PasswordPolicy sut = new PasswordPolicy();

  @Test
  void 検証成功_12文字かつ72バイト以内なら許可すること() {
    assertThat(sut.isValid("123456789012", "user@example.com")).isTrue();
  }

  @Test
  void 検証失敗_12文字未満なら拒否すること() {
    assertThat(sut.isValid("12345678901", "user@example.com")).isFalse();
  }

  @Test
  void 検証失敗_UTF8で72バイトを超えるなら拒否すること() {
    assertThat(sut.isValid("あ".repeat(25), "user@example.com")).isFalse();
  }

  @Test
  void 検証成功_UTF8で72バイトちょうどなら許可すること() {
    assertThat(sut.isValid("あ".repeat(24), "user@example.com")).isTrue();
  }

  @Test
  void 検証失敗_正規化済みメールと同一なら拒否すること() {
    assertThat(sut.isValid("user@example.com", "user@example.com")).isFalse();
  }
}
