package com.kiborisaway.tasktimetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kiborisaway.tasktimetracker.data.entity.PasswordResetToken;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

@MybatisTest
class PasswordResetTokenRepositoryTest {

  private static final int USER_A = 1;
  private static final int USER_B = 2;
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 15, 9, 0);

  @Autowired
  private PasswordResetTokenRepository sut;

  @Test
  void 登録成功_採番されたIDを設定できること() {
    PasswordResetToken token = new PasswordResetToken(
        null, USER_A, "hash-1", NOW.plusMinutes(30), null, NOW);

    sut.insert(token);

    assertThat(token.getId()).isNotNull();
  }

  @Test
  void 有効トークン検索_未使用かつ期限内なら取得できること() {
    sut.insert(new PasswordResetToken(
        null, USER_A, "hash-valid", NOW.plusMinutes(30), null, NOW));

    PasswordResetToken actual = sut.findValidForUpdate("hash-valid", NOW);

    assertThat(actual).isNotNull();
    assertThat(actual.getUserId()).isEqualTo(USER_A);
  }

  @Test
  void 有効トークン検索_期限切れなら取得できないこと() {
    sut.insert(new PasswordResetToken(
        null, USER_A, "hash-expired", NOW.minusMinutes(1), null, NOW.minusMinutes(31)));

    PasswordResetToken actual = sut.findValidForUpdate("hash-expired", NOW);

    assertThat(actual).isNull();
  }

  @Test
  void 有効トークン検索_使用済みなら取得できないこと() {
    PasswordResetToken token = new PasswordResetToken(
        null, USER_A, "hash-used", NOW.plusMinutes(30), null, NOW);
    sut.insert(token);
    sut.invalidateAllForUser(USER_A, NOW);

    PasswordResetToken actual = sut.findValidForUpdate("hash-used", NOW);

    assertThat(actual).isNull();
  }

  @Test
  void 一括無効化_対象ユーザーの未使用トークンだけを無効化すること() {
    sut.insert(new PasswordResetToken(null, USER_A, "hash-a-1", NOW.plusMinutes(30), null, NOW));
    sut.insert(new PasswordResetToken(null, USER_A, "hash-a-2", NOW.plusMinutes(30), null, NOW));
    sut.insert(new PasswordResetToken(null, USER_B, "hash-b-1", NOW.plusMinutes(30), null, NOW));
    LocalDateTime usedAt = NOW.plusMinutes(5);

    int actual = sut.invalidateAllForUser(USER_A, usedAt);

    assertThat(actual).isEqualTo(2);
    assertThat(sut.findValidForUpdate("hash-a-1", NOW)).isNull();
    assertThat(sut.findValidForUpdate("hash-a-2", NOW)).isNull();
    assertThat(sut.findValidForUpdate("hash-b-1", NOW)).isNotNull();
  }

  @Test
  void 期限切れ削除_24時間以上前に期限切れ_使用済みのレコードだけを削除すること() {
    LocalDateTime threshold = NOW;
    sut.insert(new PasswordResetToken(
        null, USER_A, "hash-expired", threshold.minusMinutes(1), null, threshold.minusHours(25)));
    sut.insert(new PasswordResetToken(
        null, USER_A, "hash-used-long-ago", threshold.plusMinutes(30), null,
        threshold.minusHours(25)));
    sut.invalidateAllForUser(USER_A, threshold.minusHours(25));
    sut.insert(new PasswordResetToken(
        null, USER_B, "hash-still-valid", threshold.plusMinutes(30), null, threshold));

    int actual = sut.deleteExpiredOrUsed(threshold);

    assertThat(actual).isEqualTo(2);
    // 削除されていれば同じハッシュで再登録でき、UNIQUE制約に抵触しないことで確認する。
    sut.insert(new PasswordResetToken(
        null, USER_A, "hash-expired", threshold.plusMinutes(30), null, threshold));
    sut.insert(new PasswordResetToken(
        null, USER_A, "hash-used-long-ago", threshold.plusMinutes(30), null, threshold));
  }
}
