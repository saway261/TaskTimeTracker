package com.kiborisaway.tasktimetracker.data.dto.work_session;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kiborisaway.tasktimetracker.data.entity.WorkSessionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "作業セッション新規登録リクエスト")
@Getter
@Setter
public class WorkSessionCreateRequest {

  @Schema(description = "作業セッションの記録タイプ", example = "TIMER")
  @NotNull
  private WorkSessionType type;

  @Schema(description = "作業セッションの実績作業時間 MANUALの場合は必須", example = "15")
  private Integer minutes;

  @Schema(description = "作業セッション開始日時 TIMERの場合は必須", example = "2026-01-01 09:00:00")
  private LocalDateTime startedAt;

  @JsonIgnore
  @AssertTrue(message = "TIMERの場合、登録時はstartedAtが必須です")
  public boolean isValidTimer() {
    if (type == null || type != WorkSessionType.TIMER) {
      return true;
    }
    return startedAt != null;
  }

  @JsonIgnore
  @AssertTrue(message = "MANUALの場合、登録時はminutesが必須です")
  public boolean isValidManual() {
    if (type == null || type != WorkSessionType.MANUAL) {
      return true;
    }
    return minutes != null;
  }

}
