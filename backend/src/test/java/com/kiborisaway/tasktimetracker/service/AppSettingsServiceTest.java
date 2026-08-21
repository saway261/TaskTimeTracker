package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.config.AnalyticsThresholdProperties;
import com.kiborisaway.tasktimetracker.data.dto.settings.AppSettingsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppSettingsServiceTest {

  @Mock
  private AnalyticsThresholdProperties thresholdProperties;

  @InjectMocks
  private AppSettingsService sut;

  @Test
  void 設定取得成功_しきい値プロパティの値をそのまま返すこと() {
    // Arrange
    when(thresholdProperties.getOnTimePercent()).thenReturn(15.0);

    // Act
    AppSettingsResponse actual = sut.getSettings();

    // Assert
    assertThat(actual.getOnTimeThresholdPercent()).isEqualTo(15.0);
  }
}
