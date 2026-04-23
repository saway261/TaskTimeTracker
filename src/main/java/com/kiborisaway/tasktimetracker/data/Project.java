package com.kiborisaway.tasktimetracker.data;

import com.kiborisaway.tasktimetracker.validation.CreateGroup;
import com.kiborisaway.tasktimetracker.validation.UpdateGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class Project {

  @NotNull(groups = UpdateGroup.class)
  private Integer id;

  @NotBlank(groups = {CreateGroup.class, UpdateGroup.class})
  @Size(max = 20, groups = {CreateGroup.class, UpdateGroup.class})
  private String title;

  @Size(max = 200, groups = {CreateGroup.class, UpdateGroup.class})
  private String description;

}
