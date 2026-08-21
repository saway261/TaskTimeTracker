package com.kiborisaway.tasktimetracker.service;

import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionCauseCategoryResponse;
import com.kiborisaway.tasktimetracker.repository.ReflectionCauseCategoryRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReflectionCauseCategoryService {

  private ReflectionCauseCategoryRepository repository;

  @Autowired
  public ReflectionCauseCategoryService(ReflectionCauseCategoryRepository repository) {
    this.repository = repository;
  }

  /**
   * 有効な原因カテゴリを表示順で取得します。
   *
   * @return 表示順に並んだ有効な原因カテゴリ一覧
   */
  @Transactional(readOnly = true)
  public List<ReflectionCauseCategoryResponse> findAllActive() {
    return repository.findAllActive().stream()
        .map(ReflectionCauseCategoryResponse::new)
        .toList();
  }
}
