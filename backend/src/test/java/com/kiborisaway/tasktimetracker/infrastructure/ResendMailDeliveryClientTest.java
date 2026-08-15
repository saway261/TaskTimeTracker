package com.kiborisaway.tasktimetracker.infrastructure;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ResendMailDeliveryClientTest {

  private static final String API_URL = "https://api.resend.com/emails";
  private static final MailMessage MESSAGE = new MailMessage(
      "user@example.com", "件名", "<p>html本文</p>", "text本文");

  @Test
  void 送信成功_認証ヘッダーと冪等キーを付与してJSONを送信すること() {
    RestClient.Builder builder = RestClient.builder().baseUrl(API_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    RestClient restClient = builder.build();
    ResendMailDeliveryClient sut =
        new ResendMailDeliveryClient(restClient, "test-api-key", "noreply@notify.kibori-saway.com");

    server.expect(requestTo(API_URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer test-api-key"))
        .andExpect(header("Idempotency-Key", org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString())))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(
            "{\"from\":\"noreply@notify.kibori-saway.com\","
                + "\"to\":[\"user@example.com\"],"
                + "\"subject\":\"件名\","
                + "\"html\":\"<p>html本文</p>\","
                + "\"text\":\"text本文\"}"))
        .andRespond(withSuccess("{\"id\":\"msg-1\"}", MediaType.APPLICATION_JSON));

    sut.send(MESSAGE);

    server.verify();
  }

  @Test
  void 送信失敗_Resendがエラーを返しても例外を送出しないこと() {
    RestClient.Builder builder = RestClient.builder().baseUrl(API_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    RestClient restClient = builder.build();
    ResendMailDeliveryClient sut =
        new ResendMailDeliveryClient(restClient, "test-api-key", "noreply@notify.kibori-saway.com");

    server.expect(requestTo(API_URL)).andRespond(withServerError());

    assertThatCode(() -> sut.send(MESSAGE)).doesNotThrowAnyException();
  }
}
