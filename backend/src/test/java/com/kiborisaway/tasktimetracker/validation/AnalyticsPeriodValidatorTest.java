package com.kiborisaway.tasktimetracker.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.kiborisaway.tasktimetracker.data.dto.analytics.AnalyticsQueryCondition;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AnalyticsPeriodValidatorTest {

  private final AnalyticsPeriodValidator sut = new AnalyticsPeriodValidator();

  @Test
  void 有効_fromがto以前なら有効であること() {
    AnalyticsQueryCondition condition = condition(
        LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 2, 1, 0, 0));

    assertThat(sut.isValid(condition, null)).isTrue();
  }

  @Test
  void 有効_fromとtoが同時刻なら有効であること() {
    LocalDateTime same = LocalDateTime.of(2026, 1, 1, 0, 0);
    AnalyticsQueryCondition condition = condition(same, same);

    assertThat(sut.isValid(condition, null)).isTrue();
  }

  @Test
  void 無効_fromがtoより後なら無効であること() {
    AnalyticsQueryCondition condition = condition(
        LocalDateTime.of(2026, 2, 1, 0, 0), LocalDateTime.of(2026, 1, 1, 0, 0));

    assertThat(sut.isValid(condition, null)).isFalse();
  }

  @Test
  void 有効_fromまたはtoが未指定なら有効であること() {
    assertThat(sut.isValid(condition(null, LocalDateTime.now()), null)).isTrue();
    assertThat(sut.isValid(condition(LocalDateTime.now(), null), null)).isTrue();
    assertThat(sut.isValid(condition(null, null), null)).isTrue();
  }

  @Test
  void 有効_値がnullなら有効であること() {
    assertThat(sut.isValid(null, null)).isTrue();
  }

  private static AnalyticsQueryCondition condition(LocalDateTime from, LocalDateTime to) {
    AnalyticsQueryCondition condition = new AnalyticsQueryCondition();
    condition.setFrom(from);
    condition.setTo(to);
    return condition;
  }
}
