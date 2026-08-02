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

  @Schema(
      description = """
          タスクID
          登録時は自動採番を行うため不要、更新時にはパスパラメータで指定するため不要なので、リクエストボディとしては常に不要
          """,
      example = "1")
  private Integer id;

  @Schema(
      description = """
          作業セッションが紐づくタスクID
          登録時はパスパラメータで指定し、更新不可なので、リクエストボディとしては常に不要
          """,
      example = "1")
  private Integer taskId;

  @Schema(description = """
      作業セッションの実績作業時間 終了状態のセッションにおいてはtypeがTIMERでもMANUALでも値を持つ
      """, example = "15")
  private Integer minutes;

  @Schema(description = "作業セッション開始日時 typeがTIMERなら必ず値を持つ", example = "2026-01-01 09:00:00")
  private LocalDateTime startedAt;

  @Schema(description = "作業セッション終了日時 typeがTIMERで、作業中ならnullとなる", example = "2026-01-01 09:15:00")
  private LocalDateTime endedAt;

  @Schema(description = "作業セッション登録日時 DBにINSERTされる際に現在時刻がセットされる", example = "2026-01-01 09:00:00")
  private LocalDateTime createdAt;

  @Schema(description = "作業セッション更新日時 DBがUPDATEされる度に現在時刻がセットされる", example = "2026-01-01 09:00:00")
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
