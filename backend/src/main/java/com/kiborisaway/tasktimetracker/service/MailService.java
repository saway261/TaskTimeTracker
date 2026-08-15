package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.infrastructure.MailDeliveryClient;
import com.kiborisaway.tasktimetracker.infrastructure.MailMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class MailService {

  private final MailDeliveryClient mailDeliveryClient;
  private final String frontendBaseUrl;

  public MailService(
      MailDeliveryClient mailDeliveryClient,
      @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl) {
    this.mailDeliveryClient = mailDeliveryClient;
    this.frontendBaseUrl = frontendBaseUrl;
  }

  public void send(MailMessage message) {
    mailDeliveryClient.send(message);
  }

  /**
   * Hostヘッダーやユーザー入力ではなく、サーバー側の設定値からメール内リンクを組み立てる。
   */
  public String buildFrontendUrl(String path, String token) {
    return UriComponentsBuilder.fromUriString(frontendBaseUrl)
        .path(path)
        .queryParam("token", token)
        .toUriString();
  }
}
