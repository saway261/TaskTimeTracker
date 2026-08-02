package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.WorkSession;
import com.kiborisaway.tasktimetracker.service.WorkSessionService;
import com.kiborisaway.tasktimetracker.validation.CreateGroup;
import com.kiborisaway.tasktimetracker.validation.UpdateGroup;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class WorkSessionController {

  private WorkSessionService service;

  @Autowired
  public WorkSessionController(WorkSessionService service) {
    this.service = service;
  }

  /**
   * タスクIDを指定して、タスクの現在の作業セッションの実作業時間の合計を返します。
   *
   * @return
   */
  @GetMapping("/tasks/{taskId}/work-sessions/total-minutes")
  public int getTaskActualTotalTime(@PathVariable @Positive int taskId) {
    return service.getTaskActualTotalTime(taskId);
  }

  /**
   * タスクグループIDを指定して、タスクグループ全体の現在の作業セッションの実作業時間の合計を返します。 　【懸念】これはTaskGroupControllerに書くべき？
   *
   * @return
   */
  @GetMapping("/task-groups/{tgId}/work-sessions/total-minutes")
  public int getTaskGroupActualTotalTime(@PathVariable @Positive int tgId) {
    return service.getTaskGroupActualTotalTime(tgId);
  }

  /**
   * プロジェクトIDを指定して、プロジェクト全体の現在の作業セッションの実作業時間の合計を返します。 【懸念】これはProjectControllerにかくべき？
   *
   * @return
   */
  @GetMapping("/projects/{pId}/work-sessions/total-minutes")
  public int getProjectActualTotalTime(@PathVariable @Positive int pId) {
    return service.getProjectActualTotalTime(pId);
  }

  /**
   * タスクに紐づく全ての作業セッションの詳細一覧を返します。
   *
   * @param taskId タスクID
   * @return タスクに紐づく全ての作業セッションの詳細一覧
   */
  @GetMapping("/tasks/{taskId}/work-sessions")
  public List<WorkSession> getAllInTask(@PathVariable @Positive int taskId) {
    return service.getAllInTask(taskId);
  }

  /**
   * IDで指定した単一の作業セッションの詳細を返します
   *
   * @param wsId 作業セッションID
   * @return 作業セッションの詳細
   */
  @GetMapping("/work-sessions/{wsId}")
  public WorkSession get(@PathVariable @Positive int wsId) {
    return service.get(wsId);
  }

  /**
   * タスクIDを指定して作業セッションを登録します。 typeフィールドが'TIMER'でも'MANUAL'でもこのAPIから登録します。
   * 'MANUAL'のときはtypeとminutesだけを必要とします。'TIMER'のときはtypeとstartedAtのみを必要とします。
   *
   * @param request 作業セッション
   * @return
   */
  @PostMapping("/tasks/{taskId}/work-sessions")
  public ResponseEntity<WorkSession> create(
      @PathVariable @Positive int taskId,
      @RequestBody @Validated(CreateGroup.class) WorkSession request) {
    return ResponseEntity.ok(service.create(taskId, request));
  }

  /**
   * 作業セッションIDを指定して作業セッションに現在時刻で終了時刻をセットします。
   * typeフィールドが'TIMER'で'startedAt'を持っている作業セッションに対する操作を想定しています。（サービス層で検証します）
   *
   * @param wsId
   * @return
   */
  @PostMapping("/work-sessions/{wsId}/end")
  public ResponseEntity<String> setEnd(@PathVariable @Positive int wsId) {
    service.setEnd(wsId);
    return ResponseEntity.ok("作業セッションを終了しました");
  }

  /**
   * 作業セッションIDを指定して作業セッションの更新をします typeは変更できません。それぞれのtypeに適切なフィールドを変更できます。
   * typeが'TIMER'なら、作業セッションが終了していることを前提とし、開始時間と終了時間をセットで求めます。
   *
   * @param wsId
   * @param request
   * @return
   */
  @PatchMapping("/work-sessions/{wsId}")
  public ResponseEntity<String> update(
      @PathVariable @Positive int wsId,
      @RequestBody @Validated(UpdateGroup.class) WorkSession request
  ) {
    service.update(wsId, request);
    return ResponseEntity.ok("作業セッションを更新しました");
  }

  /**
   * IDで指定した作業セッションを削除します
   *
   * @param wsId
   * @return
   */
  @DeleteMapping("/work-sessions/{workSessionId}")
  public ResponseEntity<String> delete(@PathVariable("workSessionId") @Positive int wsId) {
    service.delete(wsId);
    return ResponseEntity.ok("作業セッションを削除しました");
  }
}
