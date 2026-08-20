package com.kiborisaway.tasktimetracker.config;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * クエリパラメータのLocalDateTimeを、オフセット付き・オフセットなしのISO 8601日時の両方から変換します。
 * リクエストボディの{@link OffsetLocalDateTimeDeserializer}と同じ解釈規則です
 * （Jacksonのデシリアライザはリクエストボディにしか適用されないため、クエリパラメータ側は別途Converterで揃えます）。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final ZoneId databaseTimeZone;

  public WebMvcConfig(@Value("${app.database-time-zone:Asia/Tokyo}") String databaseTimeZone) {
    this.databaseTimeZone = ZoneId.of(databaseTimeZone);
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(new StringToLocalDateTimeConverter(databaseTimeZone));
  }

  private record StringToLocalDateTimeConverter(ZoneId databaseTimeZone)
      implements Converter<String, LocalDateTime> {

    @Override
    public LocalDateTime convert(String source) {
      try {
        return OffsetDateTime.parse(source, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .atZoneSameInstant(databaseTimeZone)
            .toLocalDateTime();
      } catch (DateTimeParseException offsetParseException) {
        return LocalDateTime.parse(source, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      }
    }
  }
}
