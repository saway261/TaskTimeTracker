package com.kiborisaway.tasktimetracker.repository;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * タグ一覧取得クエリ（付与タスク数つき）の結果を受け取るフラット行。
 */
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class TagRow {

  private Integer id;

  private String name;

  private Boolean isArchived;

  private Integer assignedTaskCount;
}
