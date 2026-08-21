package com.kiborisaway.tasktimetracker.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class AnalyticsThresholdProperties {

  private final double onTimePercent;

  public AnalyticsThresholdProperties(
      @Value("${app.analytics.on-time-threshold-percent:10}") double onTimePercent) {
    if (!(onTimePercent > 0) || onTimePercent > 100) {
      throw new IllegalArgumentException(
          "app.analytics.on-time-threshold-percent must be in (0, 100]");
    }
    this.onTimePercent = onTimePercent;
  }
}
