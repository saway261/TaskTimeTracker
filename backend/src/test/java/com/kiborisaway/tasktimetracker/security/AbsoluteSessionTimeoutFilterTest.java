package com.kiborisaway.tasktimetracker.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AbsoluteSessionTimeoutFilterTest {

  private static final Instant AUTHENTICATED_AT = Instant.parse("2026-07-15T00:00:00Z");

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void 認証から30日未満_リクエストを継続すること() throws Exception {
    TestRequest testRequest = requestAt(AUTHENTICATED_AT.plusSeconds(30L * 24 * 60 * 60 - 1));

    testRequest.filter().doFilter(
        testRequest.request(), testRequest.response(), testRequest.filterChain());

    verify(testRequest.filterChain()).doFilter(testRequest.request(), testRequest.response());
    verify(testRequest.entryPoint(), never()).commence(
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any());
  }

  @Test
  void 認証から30日ちょうど_セッションを破棄して401処理を呼ぶこと() throws Exception {
    TestRequest testRequest = requestAt(AUTHENTICATED_AT.plusSeconds(30L * 24 * 60 * 60));

    testRequest.filter().doFilter(
        testRequest.request(), testRequest.response(), testRequest.filterChain());

    verify(testRequest.filterChain(), never()).doFilter(
        testRequest.request(), testRequest.response());
    verify(testRequest.entryPoint()).commence(
        org.mockito.ArgumentMatchers.eq(testRequest.request()),
        org.mockito.ArgumentMatchers.eq(testRequest.response()),
        org.mockito.ArgumentMatchers.any());
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void 認証日時なし_認証済みセッションを破棄して401処理を呼ぶこと() throws Exception {
    TestRequest testRequest = requestAt(AUTHENTICATED_AT.plusSeconds(1));
    testRequest.session().removeAttribute(
        AbsoluteSessionTimeoutFilter.AUTHENTICATED_AT_SESSION_ATTRIBUTE);

    testRequest.filter().doFilter(
        testRequest.request(), testRequest.response(), testRequest.filterChain());

    verify(testRequest.entryPoint()).commence(
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any());
  }

  private TestRequest requestAt(Instant now) {
    JsonAuthenticationEntryPoint entryPoint = mock(JsonAuthenticationEntryPoint.class);
    AbsoluteSessionTimeoutFilter filter = new AbsoluteSessionTimeoutFilter(
        Clock.fixed(now, ZoneOffset.UTC), entryPoint);
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(
        AbsoluteSessionTimeoutFilter.AUTHENTICATED_AT_SESSION_ATTRIBUTE,
        AUTHENTICATED_AT);
    request.setSession(session);
    SecurityContextHolder.getContext().setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated("user", null, java.util.List.of()));
    return new TestRequest(
        filter, entryPoint, request, response, session, mock(FilterChain.class));
  }

  private record TestRequest(
      AbsoluteSessionTimeoutFilter filter,
      JsonAuthenticationEntryPoint entryPoint,
      MockHttpServletRequest request,
      MockHttpServletResponse response,
      MockHttpSession session,
      FilterChain filterChain) {
  }
}
