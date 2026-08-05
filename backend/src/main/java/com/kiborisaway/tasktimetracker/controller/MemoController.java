package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.dto.memo.MemoRequest;
import com.kiborisaway.tasktimetracker.data.dto.memo.MemoResponse;
import com.kiborisaway.tasktimetracker.data.entity.Memo;
import com.kiborisaway.tasktimetracker.service.MemoService;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * メモは親オブジェクトに付随するものとして取得し、単独ではGET APIを持たない また、カジュアルに作成し、削除できるものとするため、論理削除は設けず物理削除を行う。
 */
@Validated
@RestController
public class MemoController {

  private MemoService service;

  @Autowired
  public MemoController(MemoService service) {
    this.service = service;
  }

  /**
   * プロジェクトに対してメモを作成します
   *
   * @param pId     プロジェクトID
   * @param request メモに書き込みたいコメント
   * @return メモ
   */
  @PostMapping("/projects/{pId}/memo")
  public ResponseEntity<MemoResponse> createInProject(
      @PathVariable @Positive int pId,
      @RequestBody @Validated MemoRequest request) {
    Memo memo = service.registerInProject(pId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(new MemoResponse(memo));
  }

  /**
   * タスクグループに対してメモを作成します
   *
   * @param tgId    プロジェクトID
   * @param request メモに書き込みたいコメント
   * @return メモ
   */
  @PostMapping("/task-groups/{tgId}/memo")
  public ResponseEntity<MemoResponse> createInTaskGroup(
      @PathVariable @Positive int tgId,
      @RequestBody @Validated MemoRequest request) {
    Memo memo = service.registerInTaskGroup(tgId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(new MemoResponse(memo));
  }

  /**
   * タスクに対してメモを作成します
   *
   * @param taskId  プロジェクトID
   * @param request メモに書き込みたいコメント
   * @return メモ
   */
  @PostMapping("/tasks/{taskId}/memo")
  public ResponseEntity<MemoResponse> createInTask(
      @PathVariable @Positive int taskId,
      @RequestBody @Validated MemoRequest request) {
    Memo memo = service.registerInTask(taskId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(new MemoResponse(memo));
  }

  /**
   * idを指定してメモの内容を更新します
   *
   * @param id      メモのID
   * @param request メモに書き込みたいコメント
   * @return 更新成功メッセージ
   */
  @PatchMapping("/memo/{id}")
  public ResponseEntity<String> update(
      @PathVariable @Positive int id,
      @RequestBody @Validated MemoRequest request) {

    service.update(id, request);
    return ResponseEntity.ok("メモコメントを更新しました");
  }

  /**
   * idを指定してメモを破棄します
   *
   * @param id メモのID
   * @return 削除成功メッセージ
   */
  @DeleteMapping("/memo/{id}")
  public ResponseEntity<String> delete(@PathVariable @Positive int id) {

    service.delete(id);
    return ResponseEntity.ok("メモを削除しました");
  }


}
