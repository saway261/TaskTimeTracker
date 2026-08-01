package com.kiborisaway.tasktimetracker.data;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
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
  private WorkSessionType type;

}
