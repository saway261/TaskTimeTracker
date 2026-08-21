package com.kiborisaway.tasktimetracker.data.dto.analytics;

import java.time.LocalDateTime;

/**
 * 期間絞り込み（from / to）を持つクエリ条件。{@link com.kiborisaway.tasktimetracker.validation.ValidAnalyticsPeriod}
 * が対応する各クエリ条件クラスに共通して適用できるようにするための型です。
 */
public interface AnalyticsPeriod {

  LocalDateTime getFrom();

  LocalDateTime getTo();
}
