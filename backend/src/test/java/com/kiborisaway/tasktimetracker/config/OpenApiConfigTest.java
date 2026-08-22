package com.kiborisaway.tasktimetracker.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.support.AuthenticatedUserTestFactory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiConfigTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void 公開IDをオブジェクトではなく文字列として出力すること() throws Exception {
    String response = mockMvc.perform(get("/v3/api-docs")
            .with(user(AuthenticatedUserTestFactory.create(1))))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    JsonNode schemas = objectMapper.readTree(response).path("components").path("schemas");

    assertStringProperty(schemas, "ProjectResponse", "id");
    assertStringProperty(schemas, "TaskGroupResponse", "id");
    assertStringProperty(schemas, "TaskResponse", "id");
    assertStringProperty(schemas, "MemoResponse", "id");
    assertStringProperty(schemas, "WorkSession", "id");
    assertStringProperty(schemas, "WorkSession", "taskId");
    assertStringProperty(schemas, "TagResponse", "id");
    assertStringProperty(schemas, "AnalyticsQueryCondition", "projectId");
    assertStringProperty(schemas, "AnalyticsQueryCondition", "tagId");
    assertStringProperty(schemas, "ReflectionTimelineQueryCondition", "projectId");
    assertStringProperty(schemas, "ReflectionTimelineQueryCondition", "tagId");

    for (String publicIdSchema : List.of(
        "ProjectId", "TaskGroupId", "TaskId", "MemoId", "WorkSessionId", "TagId")) {
      assertThat(schemas.has(publicIdSchema)).isFalse();
    }
  }

  private void assertStringProperty(JsonNode schemas, String schemaName, String propertyName) {
    assertThat(schemas.path(schemaName).path("properties").path(propertyName).path("type").asText())
        .as("%s.%s", schemaName, propertyName)
        .isEqualTo("string");
  }
}
