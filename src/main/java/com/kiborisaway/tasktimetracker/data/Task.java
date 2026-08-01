package com.kiborisaway.tasktimetracker.data;

import com.kiborisaway.tasktimetracker.validation.CreateGroup;
import com.kiborisaway.tasktimetracker.validation.UpdateGroup;
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

@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class Task {

  private Integer id;
  private Integer projectId;//taskGroupIdを持つならここは持たない
  private Integer taskGroupId;

  @NotBlank(groups = {CreateGroup.class, UpdateGroup.class})
  @Size(max = 20, groups = {CreateGroup.class, UpdateGroup.class})
  private String title;

  @Size(max = 20, groups = {CreateGroup.class, UpdateGroup.class})
  private String description;

  @NotNull(groups = CreateGroup.class)
  @Positive(groups = {CreateGroup.class, UpdateGroup.class})
  private Integer estimatedMinutes;
  private LocalDateTime createdAt;
  private LocalDateTime finishedAt;
  private Integer actualMinutesCached;
  private Integer gapMinutesCached;
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
