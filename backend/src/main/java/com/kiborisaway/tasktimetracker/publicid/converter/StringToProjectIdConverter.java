package com.kiborisaway.tasktimetracker.publicid.converter;

import com.kiborisaway.tasktimetracker.publicid.PublicIdCodec;
import com.kiborisaway.tasktimetracker.publicid.PublicIdType;
import com.kiborisaway.tasktimetracker.publicid.id.ProjectId;
import org.springframework.stereotype.Component;

/** プロジェクトの公開ID文字列を {@link ProjectId} へ変換します。 */
@Component
public class StringToProjectIdConverter extends AbstractPublicIdConverter<ProjectId> {

  public StringToProjectIdConverter(PublicIdCodec codec) {
    super(codec, PublicIdType.PROJECT, ProjectId::new);
  }
}
