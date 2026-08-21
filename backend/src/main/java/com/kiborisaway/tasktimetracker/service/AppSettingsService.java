package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.config.AnalyticsThresholdProperties;
import com.kiborisaway.tasktimetracker.data.dto.settings.AppSettingsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AppSettingsService {

  private AnalyticsThresholdProperties thresholdProperties;

  @Autowired
  public AppSettingsService(AnalyticsThresholdProperties thresholdProperties) {
    this.thresholdProperties = thresholdProperties;
  }

  public AppSettingsResponse getSettings() {
    return new AppSettingsResponse(thresholdProperties.getOnTimePercent());
  }
}
