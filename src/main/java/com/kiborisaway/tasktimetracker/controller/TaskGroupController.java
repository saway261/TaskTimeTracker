package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.TaskGroup;
import com.kiborisaway.tasktimetracker.service.TaskGroupService;
import com.kiborisaway.tasktimetracker.validation.CreateGroup;
import com.kiborisaway.tasktimetracker.validation.UpdateGroup;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class TaskGroupController {

  private TaskGroupService service;

  @Autowired
  public TaskGroupController(TaskGroupService service) {
    this.service = service;
  }

  @GetMapping("/projects/{pId}/task-groups")
  public List<TaskGroup> getAll(
      @PathVariable @Positive int pId,
      @RequestParam(required = false) Boolean isFinished
  ) {
    return service.findAllByCondition(pId, isFinished);
  }

  @GetMapping("/task-groups/{tgId}")
  public TaskGroup getById(@PathVariable @Positive int tgId) {
    return service.findById(tgId);
  }

  @PostMapping("/projects/{pId}/task-groups")
  public ResponseEntity<TaskGroup> create(
      @PathVariable @Positive int pId,
      @RequestBody @Validated(CreateGroup.class) TaskGroup request
  ) {
    TaskGroup response = service.register(pId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PutMapping("/task-groups/{tgId}")
  public ResponseEntity<String> update(
      @PathVariable @Positive int tgId,
      @RequestBody @Validated(UpdateGroup.class) TaskGroup request) {
    request.setId(tgId);
    service.update(request);
    return ResponseEntity.ok("更新成功");
  }
}
