package com.kiborisaway.tasktimetracker.repository;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * プロジェクト単位で原因カテゴリのリンクを取得するクエリの結果を受け取るフラット行。
 */
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class ReflectionCauseCategoryLinkRow {

  private Integer reflectionId;

  private String causeCategoryCode;

  private String causeCategoryLabel;
}
