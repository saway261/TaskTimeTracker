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

  public void sendEmailVerification(String to, String rawToken) {
    String url = buildFrontendUrl("/verify-email", rawToken);
    String subject = "[Task Time Tracker] メールアドレスの確認";
    String html = "<p>以下のURLからメールアドレスの確認を行ってください（有効期限24時間）。</p>"
        + "<p><a href=\"" + url + "\">" + url + "</a></p>"
        + "<p>心当たりがない場合は、このメールを無視してください。</p>";
    String text = "以下のURLからメールアドレスの確認を行ってください（有効期限24時間）。\n"
        + url + "\n\n"
        + "心当たりがない場合は、このメールを無視してください。";
    send(new MailMessage(to, subject, html, text));
  }

  public void sendEmailChangeConfirmation(String to, String rawToken) {
    String url = buildFrontendUrl("/verify-email-change", rawToken);
    String subject = "[Task Time Tracker] 新しいメールアドレスの確認";
    String html = "<p>以下のURLからメールアドレス変更の確定を行ってください（有効期限24時間）。</p>"
        + "<p><a href=\"" + url + "\">" + url + "</a></p>"
        + "<p>心当たりがない場合は、このメールを無視してください。</p>";
    String text = "以下のURLからメールアドレス変更の確定を行ってください（有効期限24時間）。\n"
        + url + "\n\n"
        + "心当たりがない場合は、このメールを無視してください。";
    send(new MailMessage(to, subject, html, text));
  }

  public void sendEmailChangeNotification(String to, String newEmail) {
    String maskedNewEmail = mask(newEmail);
    String subject = "[Task Time Tracker] メールアドレス変更の要求";
    String html = "<p>このアカウントのメールアドレスを " + maskedNewEmail + " へ変更する要求を受け付けました。</p>"
        + "<p>心当たりがない場合は、パスワードを変更してください。</p>";
    String text = "このアカウントのメールアドレスを " + maskedNewEmail + " へ変更する要求を受け付けました。\n\n"
        + "心当たりがない場合は、パスワードを変更してください。";
    send(new MailMessage(to, subject, html, text));
  }

  private String mask(String email) {
    int atIndex = email.indexOf('@');
    if (atIndex <= 0) {
      return "***";
    }
    return email.charAt(0) + "***" + email.substring(atIndex);
  }
}
