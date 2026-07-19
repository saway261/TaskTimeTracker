package com.kiborisaway.tasktimetracker.data;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class Task {

  private Integer id;
  private Integer projectId;//taskGroupIdを持つならここは持たない
  private Integer taskGroupId;
  private String title;
  private String description;
  private Integer estimatedMinutes;
  private Integer displayOrder;
  private LocalDateTime createdAt;
  private LocalDateTime completedAt;
  private Integer actualMinutesCached;
  private Integer gapMinutesCached;
  private Double gapRateCached;


}
