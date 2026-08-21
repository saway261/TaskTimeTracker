package com.kiborisaway.tasktimetracker.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.data.dto.settings.AppSettingsResponse;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorDetailsBuilder;
import com.kiborisaway.tasktimetracker.security.JsonAuthenticationEntryPoint;
import com.kiborisaway.tasktimetracker.service.AppSettingsService;
import com.kiborisaway.tasktimetracker.support.WebMvcTestSecuritySupportConfig;
import com.kiborisaway.tasktimetracker.support.WithMockAuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(AppSettingsController.class)
@WithMockAuthenticatedUser
@Import({WebMvcTestSecuritySupportConfig.class, JsonAuthenticationEntryPoint.class})
class AppSettingsControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AppSettingsService service;

  @MockitoBean
  private ErrorDetailsBuilder errorDetailsBuilder;

  @Test
  void 設定取得成功_200としきい値を返すこと() throws Exception {
    when(service.getSettings()).thenReturn(new AppSettingsResponse(10.0));

    mockMvc.perform(MockMvcRequestBuilders.get("/app-settings"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.onTimeThresholdPercent").value(10.0));

    verify(service).getSettings();
  }

  @Test
  void 設定取得成功_環境変数変更後のしきい値をそのまま返すこと() throws Exception {
    when(service.getSettings()).thenReturn(new AppSettingsResponse(15.0));

    mockMvc.perform(MockMvcRequestBuilders.get("/app-settings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.onTimeThresholdPercent").value(15.0));
  }
}
