package com.kiborisaway.tasktimetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@MybatisTest
class UserRepositoryTest {

  private static final String PASSWORD_HASH =
      "{bcrypt}$2a$12$eXVmZkrzhJ4W18gvvloeBOtdTxZafS3hoF0JPQgGeoaJn4100Ss7u";

  @Autowired
  private UserRepository sut;

  @Test
  void メール検索_正規化済みメールに一致するユーザーを取得できること() {
    AppUser actual = sut.findByEmail("user-a@example.com");

    assertThat(actual).isNotNull();
    assertThat(actual.getId()).isEqualTo(1);
    assertThat(actual.getEmail()).isEqualTo("user-a@example.com");
    assertThat(actual.getPasswordHash()).startsWith("{bcrypt}");
    assertThat(actual.getIsEnabled()).isTrue();
    assertThat(actual.getPasswordChangeRequired()).isFalse();
    assertThat(actual.getTemporaryPasswordExpiresAt()).isNull();
    assertThat(actual.getEmailVerifiedAt()).isNotNull();
    assertThat(actual.getOnboardingCompleted()).isFalse();
  }

  @Test
  void メール存在確認_存在するメールならtrueを返すこと() {
    assertThat(sut.existsByEmail("user-b@example.com")).isTrue();
  }

  @Test
  void メール存在確認_存在しないメールならfalseを返すこと() {
    assertThat(sut.existsByEmail("unknown@example.com")).isFalse();
  }

  @Test
  void 登録成功_ユーザーを登録して採番されたIDを設定できること() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 13, 12, 0);
    AppUser user = new AppUser(
        null, "new-user@example.com", PASSWORD_HASH, true, false, null, now, now, null, false);

    sut.insert(user);

    assertThat(user.getId()).isNotNull();
    assertThat(sut.findByEmail("new-user@example.com")).isEqualTo(user);
  }

  @Test
  void 登録失敗_正規化されていないメールなら制約違反になること() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 13, 12, 0);
    AppUser user = new AppUser(
        null, "User@Example.com", PASSWORD_HASH, true, false, null, now, now, null, false);

    assertThatThrownBy(() -> sut.insert(user))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void パスワード更新_通常パスワード状態へ更新できること() {
    LocalDateTime updatedAt = LocalDateTime.of(2026, 8, 14, 12, 0);

    int actual = sut.updatePassword(1, "{bcrypt}updated", updatedAt);

    AppUser updated = sut.findByEmail("user-a@example.com");
    assertThat(actual).isEqualTo(1);
    assertThat(updated.getPasswordHash()).isEqualTo("{bcrypt}updated");
    assertThat(updated.getPasswordChangeRequired()).isFalse();
    assertThat(updated.getTemporaryPasswordExpiresAt()).isNull();
    assertThat(updated.getUpdatedAt()).isEqualTo(updatedAt);
  }

  @Test
  void 一時パスワード更新_変更必須状態と有効期限を設定できること() {
    LocalDateTime updatedAt = LocalDateTime.of(2026, 8, 14, 12, 0);
    LocalDateTime expiresAt = updatedAt.plusHours(72);

    int actual = sut.updateTemporaryPassword(1, "{bcrypt}temporary", expiresAt, updatedAt);

    AppUser updated = sut.findByEmail("user-a@example.com");
    assertThat(actual).isEqualTo(1);
    assertThat(updated.getPasswordHash()).isEqualTo("{bcrypt}temporary");
    assertThat(updated.getPasswordChangeRequired()).isTrue();
    assertThat(updated.getTemporaryPasswordExpiresAt()).isEqualTo(expiresAt);
    assertThat(updated.getUpdatedAt()).isEqualTo(updatedAt);
  }

  @Test
  void メール確認状態更新_確認日時を設定できること() {
    LocalDateTime verifiedAt = LocalDateTime.of(2026, 8, 15, 9, 0);
    LocalDateTime updatedAt = LocalDateTime.of(2026, 8, 15, 9, 0);

    int actual = sut.updateEmailVerified(1, verifiedAt, updatedAt);

    AppUser updated = sut.findByEmail("user-a@example.com");
    assertThat(actual).isEqualTo(1);
    assertThat(updated.getEmailVerifiedAt()).isEqualTo(verifiedAt);
    assertThat(updated.getUpdatedAt()).isEqualTo(updatedAt);
  }

  @Test
  void メールアドレス更新_メールと確認日時を同時に更新できること() {
    LocalDateTime verifiedAt = LocalDateTime.of(2026, 8, 15, 9, 0);
    LocalDateTime updatedAt = LocalDateTime.of(2026, 8, 15, 9, 0);

    int actual = sut.updateEmail(1, "changed@example.com", verifiedAt, updatedAt);

    AppUser updated = sut.findByEmail("changed@example.com");
    assertThat(actual).isEqualTo(1);
    assertThat(updated).isNotNull();
    assertThat(updated.getEmailVerifiedAt()).isEqualTo(verifiedAt);
    assertThat(updated.getUpdatedAt()).isEqualTo(updatedAt);
  }
}
