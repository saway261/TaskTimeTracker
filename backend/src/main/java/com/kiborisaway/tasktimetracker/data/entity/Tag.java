package com.kiborisaway.tasktimetracker.data.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class Tag {

  private Integer id;

  private Integer userId;

  private String name;

  private String nameNormalized;

  private Boolean isArchived;

  private LocalDateTime createdAt;
}
