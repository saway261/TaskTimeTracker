package com.kiborisaway.tasktimetracker.publicid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kiborisaway.tasktimetracker.publicid.id.ProjectId;
import com.kiborisaway.tasktimetracker.publicid.id.TagId;
import com.kiborisaway.tasktimetracker.publicid.id.TaskGroupId;
import com.kiborisaway.tasktimetracker.publicid.id.TaskId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.ConversionService;
import tools.jackson.databind.ObjectMapper;

/**
 * 公開IDの基盤が実際にSpringへ配線されていることを確認する。
 *
 * <p>フェーズB2ではControllerとDTOをまとめて切り替えるため、その前に
 * 「Converterが ConversionService に登録されているか」「Jacksonモジュールが
 * ObjectMapper に効いているか」をここで確定させておく。
 */
@SpringBootTest
class PublicIdWiringIntegrationTest {

  /** JSONのフィールドとして公開IDを持つ入れ子の検証用。 */
  record Holder(ProjectId projectId, TaskId taskId) {

  }

  @Autowired
  private PublicIdCodec codec;

  @Autowired
  private ConversionService conversionService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void Converterが登録されていて公開ID文字列を型付きIDへ変換できること() {
    String publicId = codec.encode(PublicIdType.PROJECT, 7);

    ProjectId converted = conversionService.convert(publicId, ProjectId.class);

    assertThat(converted).isEqualTo(new ProjectId(7));
  }

  @Test
  void Converterが6種すべて登録されていること() {
    assertThat(conversionService.canConvert(String.class, ProjectId.class)).isTrue();
    assertThat(conversionService.canConvert(String.class, TaskGroupId.class)).isTrue();
    assertThat(conversionService.canConvert(String.class, TaskId.class)).isTrue();
    assertThat(conversionService.canConvert(String.class, TagId.class)).isTrue();
    assertThat(conversionService.canConvert(String.class,
        com.kiborisaway.tasktimetracker.publicid.id.MemoId.class)).isTrue();
    assertThat(conversionService.canConvert(String.class,
        com.kiborisaway.tasktimetracker.publicid.id.WorkSessionId.class)).isTrue();
  }

  @Test
  void Converterが別種別のIDを変換しようとすると例外になること() {
    String taskPublicId = codec.encode(PublicIdType.TASK, 7);

    assertThatThrownBy(() -> conversionService.convert(taskPublicId, ProjectId.class))
        .hasRootCauseInstanceOf(PublicIdInvalidException.class);
  }

  @Test
  void Jacksonが型付きIDを公開ID文字列として出力すること() {
    Holder holder = new Holder(new ProjectId(7), new TaskId(7));

    String json = objectMapper.writeValueAsString(holder);

    assertThat(json)
        .contains("\"" + codec.encode(PublicIdType.PROJECT, 7) + "\"")
        .contains("\"" + codec.encode(PublicIdType.TASK, 7) + "\"")
        // 内部の連番が漏れていないこと
        .doesNotContain(":7")
        .doesNotContain("\"7\"");
  }

  @Test
  void Jacksonが公開ID文字列を型付きIDへ戻せること() {
    String json = """
        {"projectId":"%s","taskId":"%s"}
        """.formatted(
        codec.encode(PublicIdType.PROJECT, 7),
        codec.encode(PublicIdType.TASK, 12));

    Holder holder = objectMapper.readValue(json, Holder.class);

    assertThat(holder).isEqualTo(new Holder(new ProjectId(7), new TaskId(12)));
  }

  @Test
  void Jacksonが別種別のIDを受け取ると例外になること() {
    String json = """
        {"projectId":"%s","taskId":null}
        """.formatted(codec.encode(PublicIdType.TASK, 7));

    assertThatThrownBy(() -> objectMapper.readValue(json, Holder.class))
        .rootCause()
        .isInstanceOf(PublicIdInvalidException.class);
  }
}
