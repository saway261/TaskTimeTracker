package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.Project;
import com.kiborisaway.tasktimetracker.service.ProjectService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/projects")
public class ProjectController {

  private ProjectService service;

  @Autowired
  public ProjectController(ProjectService service) {
    this.service = service;
  }

  @GetMapping
  public List<Project> getAll(@RequestParam(required = false) Boolean isFinished) {
    return service.findAllByCondition(isFinished);
  }

  @GetMapping("/{id}")
  public Project getById(@PathVariable @Positive int id) {
    return service.findById(id);
  }

  @PostMapping
  public ResponseEntity<Project> create(
      @RequestBody @Validated(CreateGroup.class) Project request) {
    Project response = service.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PutMapping("/{id}")
  public ResponseEntity<String> update(
      @PathVariable @Positive int id,
      @RequestBody @Validated(UpdateGroup.class) Project request) {
    request.setId(id);
    service.update(request);
    return ResponseEntity.ok("更新成功");
  }
}
