package com.kiborisaway.tasktimetracker.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kiborisaway.tasktimetracker.validation.CreateGroup;
import com.kiborisaway.tasktimetracker.validation.UpdateGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class WorkSession {

  private Integer id;
  private Integer taskId;
  private Integer minutes;

  /**
   * 手動登録の場合はnull。
   */
  private LocalDateTime startedAt;

  /**
   * 手動登録またはタイマー実行中の場合はnull。
   */
  private LocalDateTime endedAt;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @Schema(description = "作業セッションの記録タイプ", example = "TIMER")
  @NotNull(groups = {CreateGroup.class, UpdateGroup.class})
  private WorkSessionType type;

  @JsonIgnore
  @AssertTrue(message = "TIMERの場合、登録時はstartedAtが必須です", groups = CreateGroup.class)
  public boolean isValidCreateTimer() {
    if (type == null || type != WorkSessionType.TIMER) {
      return true;
    }
    return startedAt != null;
  }

  @JsonIgnore
  @AssertTrue(message = "MANUALの場合、登録時はminutesが必須です", groups = CreateGroup.class)
  public boolean isValidCreateManual() {
    if (type == null || type != WorkSessionType.MANUAL) {
      return true;
    }
    return minutes != null;
  }

  @JsonIgnore
  @AssertTrue(message = "TIMERの場合、更新時はstartedAtとendedAtが必須です", groups = UpdateGroup.class)
  public boolean isValidUpdateTimer() {
    if (type == null || type != WorkSessionType.TIMER) {
      return true;
    }
    return startedAt != null && endedAt != null;
  }

  @JsonIgnore
  @AssertTrue(message = "MANUALの場合、更新時はminutesが必須です", groups = UpdateGroup.class)
  public boolean isValidUpdateManual() {
    if (type == null || type != WorkSessionType.MANUAL) {
      return true;
    }
    return minutes != null;
  }

}
