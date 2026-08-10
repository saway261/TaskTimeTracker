package com.kiborisaway.tasktimetracker.config;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/** DBのタイムゾーンを持たない日時を、オフセット付きISO 8601形式でJSONに出力します。 */
@JacksonComponent
public class OffsetLocalDateTimeSerializer extends ValueSerializer<LocalDateTime> {

  private final ZoneId databaseTimeZone;

  public OffsetLocalDateTimeSerializer(
      @Value("${app.database-time-zone:Asia/Tokyo}") String databaseTimeZone) {
    this.databaseTimeZone = ZoneId.of(databaseTimeZone);
  }

  @Override
  public void serialize(
      LocalDateTime value, JsonGenerator generator, SerializationContext context)
      throws JacksonException {
    String offsetDateTime = value.atZone(databaseTimeZone)
        .toOffsetDateTime()
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    generator.writeString(offsetDateTime);
  }

  @Override
  public Class<LocalDateTime> handledType() {
    return LocalDateTime.class;
  }
}
