package com.kiborisaway.tasktimetracker.config;

import com.kiborisaway.tasktimetracker.publicid.id.MemoId;
import com.kiborisaway.tasktimetracker.publicid.id.ProjectId;
import com.kiborisaway.tasktimetracker.publicid.id.TagId;
import com.kiborisaway.tasktimetracker.publicid.id.TaskGroupId;
import com.kiborisaway.tasktimetracker.publicid.id.TaskId;
import com.kiborisaway.tasktimetracker.publicid.id.WorkSessionId;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

/**
 * HTTP境界で公開IDとして扱う型のOpenAPI表現を登録します。
 *
 * <p>型付きIDはJavaのrecordなので、springdocの既定動作では
 * {@code {"value": 1}} 形式のオブジェクトとして解釈されます。実際のJSON・URLでは
 * Sqidsでエンコードした文字列を使用するため、OpenAPI上でも文字列へ置き換えます。
 */
@Configuration
public class OpenApiConfig {

  static {
    SpringDocUtils.getConfig()
        .replaceWithClass(ProjectId.class, String.class)
        .replaceWithClass(TaskGroupId.class, String.class)
        .replaceWithClass(TaskId.class, String.class)
        .replaceWithClass(MemoId.class, String.class)
        .replaceWithClass(WorkSessionId.class, String.class)
        .replaceWithClass(TagId.class, String.class);
  }
}
