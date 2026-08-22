package com.kiborisaway.tasktimetracker.publicid.jackson;

import com.kiborisaway.tasktimetracker.publicid.PublicIdCodec;
import com.kiborisaway.tasktimetracker.publicid.PublicIdType;
import com.kiborisaway.tasktimetracker.publicid.id.MemoId;
import com.kiborisaway.tasktimetracker.publicid.id.ProjectId;
import com.kiborisaway.tasktimetracker.publicid.id.PublicId;
import com.kiborisaway.tasktimetracker.publicid.id.TagId;
import com.kiborisaway.tasktimetracker.publicid.id.TaskGroupId;
import com.kiborisaway.tasktimetracker.publicid.id.TaskId;
import com.kiborisaway.tasktimetracker.publicid.id.WorkSessionId;
import java.util.function.IntFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.module.SimpleModule;

/**
 * 型付きIDのJSON入出力を公開ID文字列で行うための登録。
 *
 * <p>既存の日時変換は {@code @JacksonComponent} を使っていますが、公開IDは6種あり
 * シリアライザ・デシリアライザを個別クラスにすると12クラスになるため、
 * ここでモジュールとしてまとめて登録します。
 *
 * <p>{@code @Configuration} はテストスライスに自動では含まれないので、
 * {@code WebMvcTestSecuritySupportConfig} から明示的に取り込んでいます。
 */
@Configuration
public class PublicIdJacksonConfig {

  /**
   * 公開IDの変換モジュールを登録します。
   *
   * @param codec 公開IDのコーデック
   * @return Jacksonモジュール
   */
  @Bean
  public JacksonModule publicIdModule(PublicIdCodec codec) {
    SimpleModule module = new SimpleModule("PublicIdModule");

    // シリアライザはインタフェースに対して1つ登録すれば全実装に効く。
    module.addSerializer(PublicId.class, new PublicIdSerializer(codec));

    addDeserializer(module, codec, ProjectId.class, PublicIdType.PROJECT, ProjectId::new);
    addDeserializer(module, codec, TaskGroupId.class, PublicIdType.TASK_GROUP, TaskGroupId::new);
    addDeserializer(module, codec, TaskId.class, PublicIdType.TASK, TaskId::new);
    addDeserializer(module, codec, MemoId.class, PublicIdType.MEMO, MemoId::new);
    addDeserializer(module, codec, WorkSessionId.class, PublicIdType.WORK_SESSION,
        WorkSessionId::new);
    addDeserializer(module, codec, TagId.class, PublicIdType.TAG, TagId::new);

    return module;
  }

  private <T extends PublicId> void addDeserializer(SimpleModule module, PublicIdCodec codec,
      Class<T> targetType, PublicIdType type, IntFunction<T> factory) {
    module.addDeserializer(targetType, new PublicIdDeserializer<>(codec, type, factory));
  }
}
