package com.kiborisaway.tasktimetracker.repository;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 複数タスクへのタグ付与の一括取得クエリの結果を受け取るフラット行。
 */
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class TaskTagRow {

  private Integer taskId;

  private Integer tagId;

  private String name;
}
