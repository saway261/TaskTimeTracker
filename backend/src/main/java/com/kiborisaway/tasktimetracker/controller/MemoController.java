package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.dto.memo.MemoRequest;
import com.kiborisaway.tasktimetracker.data.dto.memo.MemoResponse;
import com.kiborisaway.tasktimetracker.data.entity.Memo;
import com.kiborisaway.tasktimetracker.publicid.id.MemoId;
import com.kiborisaway.tasktimetracker.publicid.id.ProjectId;
import com.kiborisaway.tasktimetracker.publicid.id.TaskGroupId;
import com.kiborisaway.tasktimetracker.publicid.id.TaskId;
import com.kiborisaway.tasktimetracker.security.AuthenticatedUser;
import com.kiborisaway.tasktimetracker.service.MemoService;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable ProjectId pId,
      @RequestBody @Validated MemoRequest request) {
    Memo memo = service.registerInProject(user.getUserId(), pId.value(), request);
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
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable TaskGroupId tgId,
      @RequestBody @Validated MemoRequest request) {
    Memo memo = service.registerInTaskGroup(user.getUserId(), tgId.value(), request);
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
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable TaskId taskId,
      @RequestBody @Validated MemoRequest request) {
    Memo memo = service.registerInTask(user.getUserId(), taskId.value(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(new MemoResponse(memo));
  }

  /**
   * idを指定してメモの内容を更新します
   *
   * @param id      メモのID
   * @param request メモに書き込みたいコメント
   * @return 更新後のメモ
   */
  @PatchMapping("/memo/{id}")
  public ResponseEntity<MemoResponse> update(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable MemoId id,
      @RequestBody @Validated MemoRequest request) {

    Memo memo = service.update(user.getUserId(), id.value(), request);
    return ResponseEntity.ok(new MemoResponse(memo));
  }

  /**
   * idを指定してメモを破棄します
   *
   * @param id メモのID
   * @return レスポンスボディなし
   */
  @DeleteMapping("/memo/{id}")
  public ResponseEntity<Void> delete(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable MemoId id) {

    service.delete(user.getUserId(), id.value());
    return ResponseEntity.noContent().build();
  }


}
