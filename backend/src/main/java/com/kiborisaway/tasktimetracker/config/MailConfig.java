package com.kiborisaway.tasktimetracker.config;

import com.kiborisaway.tasktimetracker.infrastructure.MailDeliveryClient;
import com.kiborisaway.tasktimetracker.infrastructure.ResendMailDeliveryClient;
import com.kiborisaway.tasktimetracker.infrastructure.StdoutMailDeliveryClient;
import java.net.http.HttpClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class MailConfig {

  private static final Logger logger = LoggerFactory.getLogger(MailConfig.class);

  @Bean
  @ConditionalOnProperty(prefix = "app.mail", name = "delivery", havingValue = "resend")
  MailDeliveryClient resendMailDeliveryClient(
      @Value("${app.mail.api-url:https://api.resend.com/emails}") String apiUrl,
      @Value("${app.mail.api-key:}") String apiKey,
      @Value("${app.mail.from:Task Time Tracker <noreply@notify.kibori-saway.com>}") String from,
      @Value("${app.mail.connect-timeout:3s}") Duration connectTimeout,
      @Value("${app.mail.read-timeout:10s}") Duration readTimeout) {
    if (apiKey.isBlank()) {
      throw new IllegalStateException(
          "app.mail.api-key must be set when app.mail.delivery=resend");
    }

    HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(connectTimeout)
        .build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(readTimeout);
    RestClient restClient = RestClient.builder()
        .baseUrl(apiUrl)
        .requestFactory(requestFactory)
        .build();

    return new ResendMailDeliveryClient(restClient, apiKey, from);
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "app.mail", name = "delivery", havingValue = "stdout", matchIfMissing = true)
  MailDeliveryClient stdoutMailDeliveryClient() {
    logger.warn("Mail delivery is configured to use stdout; this must not be used in production");
    return new StdoutMailDeliveryClient(System.out);
  }
}
