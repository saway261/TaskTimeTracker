package com.kiborisaway.tasktimetracker.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.kiborisaway.tasktimetracker.infrastructure.MailDeliveryClient;
import com.kiborisaway.tasktimetracker.infrastructure.ResendMailDeliveryClient;
import com.kiborisaway.tasktimetracker.infrastructure.StdoutMailDeliveryClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.GenericApplicationContext;

class MailConfigTest {

  // ApplicationContextRunnerが作る素のコンテキストはBoot起動時のApplicationConversionServiceを
  // 持たないため、"3s"のようなDuration表記の@Valueを解決できるよう明示的に設定する。
  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withInitializer(context -> ((GenericApplicationContext) context)
          .getBeanFactory()
          .setConversionService(ApplicationConversionService.getSharedInstance()))
      .withUserConfiguration(MailConfig.class);

  @Test
  void 配信先未指定_標準出力実装を選択すること() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(MailDeliveryClient.class);
      assertThat(context.getBean(MailDeliveryClient.class))
          .isInstanceOf(StdoutMailDeliveryClient.class);
    });
  }

  @Test
  void 配信先stdout指定_標準出力実装を選択すること() {
    contextRunner.withPropertyValues("app.mail.delivery=stdout").run(context -> {
      assertThat(context.getBean(MailDeliveryClient.class))
          .isInstanceOf(StdoutMailDeliveryClient.class);
    });
  }

  @Test
  void 配信先resend指定_APIキー設定済みならResend実装を選択すること() {
    contextRunner.withPropertyValues(
        "app.mail.delivery=resend",
        "app.mail.api-key=test-key").run(context -> {
      assertThat(context.getBean(MailDeliveryClient.class))
          .isInstanceOf(ResendMailDeliveryClient.class);
    });
  }

  @Test
  void 配信先resend指定_APIキー未設定なら起動に失敗すること() {
    contextRunner.withPropertyValues("app.mail.delivery=resend").run(context -> {
      assertThat(context).hasFailed();
      assertThat(context.getStartupFailure())
          .rootCause()
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("app.mail.api-key");
    });
  }
}
