package com.kiborisaway.tasktimetracker.repository;

import com.kiborisaway.tasktimetracker.data.entity.ReflectionCauseCategory;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReflectionCauseCategoryRepository {

  /**
   * 有効な原因カテゴリを表示順で取得します。
   *
   * @return 表示順に並んだ有効な原因カテゴリ一覧
   */
  @Select("""
      SELECT * FROM reflection_cause_categories
      WHERE is_active = TRUE
      ORDER BY display_order, id
      """)
  List<ReflectionCauseCategory> findAllActive();

  /**
   * コードを指定して有効な原因カテゴリを取得します。無効化されたカテゴリは対象外です。
   *
   * @param code 原因カテゴリコード
   * @return 原因カテゴリ。存在しない、または無効な場合はnull
   */
  @Select("""
      SELECT * FROM reflection_cause_categories
      WHERE code = #{code} AND is_active = TRUE
      """)
  ReflectionCauseCategory findActiveByCode(String code);
}
