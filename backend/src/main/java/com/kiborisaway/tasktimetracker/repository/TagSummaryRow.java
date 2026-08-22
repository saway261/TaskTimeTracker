package com.kiborisaway.tasktimetracker.repository;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code TagSummaryResponse} のMyBatis結果マッピング用。
 *
 * <p>{@code TagSummaryResponse.id} は公開ID化により型付きIDになったが、MyBatisの自動結果
 * マッピングは素の {@code Integer} 列にしか対応できないため、SQL結果はいったんこの素朴な
 * Rowで受け、呼び出し元（{@code TaskService}）で {@code TagSummaryResponse} へ変換する。
 */
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class TagSummaryRow {

  private Integer id;

  private String name;
}
