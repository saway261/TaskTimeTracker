package com.kiborisaway.tasktimetracker.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.data.dto.analytics.EstimationAccuracyResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.ExcludedTaskCountResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.AccuracySummaryResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.MetricAvailabilityResponse;
import com.kiborisaway.tasktimetracker.data.dto.analytics.OutcomeBreakdownResponse;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorDetailsBuilder;
import com.kiborisaway.tasktimetracker.security.JsonAuthenticationEntryPoint;
import com.kiborisaway.tasktimetracker.service.AnalyticsService;
import com.kiborisaway.tasktimetracker.support.WebMvcTestSecuritySupportConfig;
import com.kiborisaway.tasktimetracker.support.WithMockAuthenticatedUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(AnalyticsController.class)
@WithMockAuthenticatedUser
@Import({WebMvcTestSecuritySupportConfig.class, JsonAuthenticationEntryPoint.class})
class AnalyticsControllerTest {

  private static final int USER_ID = 1;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AnalyticsService service;

  @MockitoBean
  private ErrorDetailsBuilder errorDetailsBuilder;

  @Test
  void 取得成功_200と集計結果を返すこと() throws Exception {
    when(service.getEstimationAccuracy(eq(USER_ID), any())).thenReturn(response());

    mockMvc.perform(MockMvcRequestBuilders.get("/analytics/estimation-accuracy"))
        .andExpect(status().isOk());

    verify(service).getEstimationAccuracy(anyInt(), any());
  }

  @Test
  void 取得失敗_fromがtoより後の場合は400を返すこと() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/analytics/estimation-accuracy")
            .param("from", "2026-02-01T00:00:00")
            .param("to", "2026-01-01T00:00:00"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 取得失敗_projectIdが負値の場合は400を返すこと() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/analytics/estimation-accuracy")
            .param("projectId", "-1"))
        .andExpect(status().isBadRequest());
  }

  private static EstimationAccuracyResponse response() {
    return new EstimationAccuracyResponse(
        10.0,
        0,
        new ExcludedTaskCountResponse(0, 0, 0),
        new AccuracySummaryResponse(
            new MetricAvailabilityResponse(false, 5, 0),
            new OutcomeBreakdownResponse(0, 0, 0),
            null, null, null, null, null, null, null, null),
        null,
        List.of(),
        false,
        List.of(),
        List.of(),
        new MetricAvailabilityResponse(false, 20, 0));
  }
}
