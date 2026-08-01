package com.kiborisaway.tasktimetracker.data;

import com.kiborisaway.tasktimetracker.validation.CreateGroup;
import com.kiborisaway.tasktimetracker.validation.UpdateGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "タスク")
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class Task {

  @Schema(
      description = """
          タスクID
          登録時は自動採番を行うため不要、更新時にはパスパラメータで指定するため不要なので、リクエストボディとしては常に不要
          """,
      example = "1")
  private Integer id;

  @Schema(
      description = """
          親となるプロジェクトID
          登録時はパスパラメータで指定し、更新不可なので、リクエストボディとしては常に不要
          プロジェクトIDとタスクグループIDはどちらか片方のみ持つ
          """,
      example = "1")
  private Integer projectId;//taskGroupIdを持つならここは持たない

  @Schema(
      description = """
          親となるタスクグループID
          登録時はパスパラメータで指定し、更新不可なので、リクエストボディとしては常に不要
          プロジェクトIDとタスクグループIDはどちらか片方のみ持つ
          """,
      example = "1")
  private Integer taskGroupId;

  @Schema(description = "タスク名", example = "docker-compose.yml作成")
  @NotBlank(groups = {CreateGroup.class, UpdateGroup.class})
  @Size(max = 20, groups = {CreateGroup.class, UpdateGroup.class})
  private String title;

  @Schema(description = "タスクの説明　省略可", example = "")
  @Size(max = 20, groups = {CreateGroup.class, UpdateGroup.class})
  private String description;

  @Schema(description = "見積作業時間(分)", example = "60")
  @NotNull(groups = CreateGroup.class)
  @Positive(groups = {CreateGroup.class, UpdateGroup.class})
  private Integer estimatedMinutes;

  @Schema(description = "タスク作成日時", example = "2026-01-01 09:00:00")
  private LocalDateTime createdAt;

  @Schema(description = "タスク完了日時", example = "2026-01-10 21:00:00")
  private LocalDateTime finishedAt;

  @Schema(description = "実績作業時間(分)　WorkSessionを合計してキャッシュする", example = "70")
  private Integer actualMinutesCached;

  @Schema(description = "(実績-見積)作業時間(分)　estimatedMinutes と actualMinutesCached から計算してキャッシュする", example = "10")
  private Integer gapMinutesCached;
  
  @Schema(description = "(実績-見積)作業時間比率　estimatedMinutes と actualMinutesCached から計算してキャッシュする", example = "16.666")
  private Double gapRateCached;

  /**
   * タスク新規作成時にDBにINSERTするために満たされているべきフィールドのみでTaskインスタンスを生成するコンストラクタです。
   *
   * @param projectId        プロジェクトID
   * @param taskGroupId      タスクグループID
   * @param title            タイトル
   * @param description      説明
   * @param estimatedMinutes 見積もり作業時間
   */
  public Task(Integer projectId, Integer taskGroupId, String title, String description,
      Integer estimatedMinutes) {
    this.projectId = projectId;
    this.taskGroupId = taskGroupId;
    this.title = title;
    this.description = description;
    this.estimatedMinutes = estimatedMinutes;
  }
}
