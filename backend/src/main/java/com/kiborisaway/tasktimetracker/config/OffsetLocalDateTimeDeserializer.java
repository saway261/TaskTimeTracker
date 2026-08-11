package com.kiborisaway.tasktimetracker.config;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/** オフセット付きまたはオフセットなしのISO 8601日時をLocalDateTimeへ変換します。 */
@JacksonComponent
public class OffsetLocalDateTimeDeserializer extends ValueDeserializer<LocalDateTime> {

  private final ZoneId databaseTimeZone;

  public OffsetLocalDateTimeDeserializer(
      @Value("${app.database-time-zone:Asia/Tokyo}") String databaseTimeZone) {
    this.databaseTimeZone = ZoneId.of(databaseTimeZone);
  }

  @Override
  public LocalDateTime deserialize(JsonParser parser, DeserializationContext context)
      throws JacksonException {
    String value = parser.getValueAsString();
    if (value == null) {
      return (LocalDateTime) context.handleUnexpectedToken(LocalDateTime.class, parser);
    }

    try {
      return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
          .atZoneSameInstant(databaseTimeZone)
          .toLocalDateTime();
    } catch (DateTimeParseException offsetDateTimeParseException) {
      try {
        return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      } catch (DateTimeParseException localDateTimeParseException) {
        return (LocalDateTime) context.handleWeirdStringValue(
            LocalDateTime.class,
            value,
            "オフセット付きまたはオフセットなしのISO 8601日時として解釈できません");
      }
    }
  }

  @Override
  public Class<LocalDateTime> handledType() {
    return LocalDateTime.class;
  }
}
