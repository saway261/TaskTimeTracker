package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.Task;
import com.kiborisaway.tasktimetracker.service.TaskService;
import com.kiborisaway.tasktimetracker.validation.CreateGroup;
import com.kiborisaway.tasktimetracker.validation.UpdateGroup;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class TaskController {

  private TaskService service;

  @Autowired
  public TaskController(TaskService service) {
    this.service = service;
  }

  @GetMapping("/projects/{pId}/tasks")
  public List<Task> getAllInProject(
      @PathVariable @Positive int pId,
      @RequestParam(required = false) Boolean isFinished
  ) {
    return service.findAllInProjectByCondition(pId, isFinished);
  }

  @GetMapping("/task-groups/{tgId}/tasks")
  public List<Task> getAllInTaskGroup(
      @PathVariable @Positive int tgId,
      @RequestParam(required = false) Boolean isFinished
  ) {
    return service.findAllInTaskGroupByCondition(tgId, isFinished);
  }

  @GetMapping("/tasks/{taskId}")
  public Task getById(@PathVariable @Positive int taskId) {
    return service.findById(taskId);
  }

  @PostMapping("/projects/{pId}/tasks")
  public ResponseEntity<Task> createInProject(
      @PathVariable @Positive int pId,
      @RequestBody @Validated(CreateGroup.class) Task request) {
    request.setProjectId(pId);
    request.setTaskGroupId(null);//タスクグループIDは明示的にnullにする
    Task response = service.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/task-groups/{tgId}/tasks")
  public ResponseEntity<Task> createInTaskGroup(
      @PathVariable @Positive int tgId,
      @RequestBody @Validated(CreateGroup.class) Task request) {
    request.setTaskGroupId(tgId);
    request.setProjectId(null);//プロジェクトIDは明示的にnullにする
    Task response = service.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PatchMapping("/tasks/{taskId}")
  public ResponseEntity<String> updateProperty(
      @PathVariable @Positive int taskId,
      @RequestBody @Validated(UpdateGroup.class) Task request) {
    request.setId(taskId);
    service.updateProperty(request);
    return ResponseEntity.ok("タスク名と説明を更新しました");
  }

  @PatchMapping("/tasks/{taskId}/estimated-minutes")
  public ResponseEntity<String> updateEstimatedMinutes(
      @PathVariable @Positive int taskId,
      @RequestBody @Positive Integer estimatedMinutes) {
    service.updateEstimateMinutes(taskId, estimatedMinutes);
    return ResponseEntity.ok("見積作業時間を更新しました");
  }

  @PostMapping("/tasks/{taskId}/finished")
  public ResponseEntity<String> setFinished(@PathVariable @Positive int taskId) {
    service.setFinished(taskId);
    return ResponseEntity.ok("タスクを完了状態にしました");
  }

  @DeleteMapping("/tasks/{taskId}")
  public ResponseEntity<String> delete(@PathVariable @Positive int taskId) {
    service.deleteById(taskId);
    return ResponseEntity.ok("タスクを削除しました");
  }
}
