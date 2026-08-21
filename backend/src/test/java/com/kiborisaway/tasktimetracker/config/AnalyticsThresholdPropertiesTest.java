package com.kiborisaway.tasktimetracker.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AnalyticsThresholdPropertiesTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(AnalyticsThresholdProperties.class);

  @Test
  void 環境変数未設定_既定値10を保持すること() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(AnalyticsThresholdProperties.class);
      assertThat(context.getBean(AnalyticsThresholdProperties.class).getOnTimePercent())
          .isEqualTo(10.0);
    });
  }

  @Test
  void 環境変数設定_指定値を保持すること() {
    contextRunner.withPropertyValues("app.analytics.on-time-threshold-percent=15")
        .run(context -> assertThat(
            context.getBean(AnalyticsThresholdProperties.class).getOnTimePercent())
            .isEqualTo(15.0));
  }

  @Test
  void 値が0_起動に失敗すること() {
    contextRunner.withPropertyValues("app.analytics.on-time-threshold-percent=0")
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure())
              .rootCause()
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("app.analytics.on-time-threshold-percent");
        });
  }

  @Test
  void 値が負_起動に失敗すること() {
    contextRunner.withPropertyValues("app.analytics.on-time-threshold-percent=-5")
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure())
              .rootCause()
              .isInstanceOf(IllegalArgumentException.class);
        });
  }

  @Test
  void 値が100超過_起動に失敗すること() {
    contextRunner.withPropertyValues("app.analytics.on-time-threshold-percent=120")
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure())
              .rootCause()
              .isInstanceOf(IllegalArgumentException.class);
        });
  }

  @Test
  void 値が数値として解釈できない_起動に失敗すること() {
    contextRunner.withPropertyValues("app.analytics.on-time-threshold-percent=abc")
        .run(context -> assertThat(context).hasFailed());
  }
}
