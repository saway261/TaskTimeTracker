package com.kiborisaway.tasktimetracker.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionCauseCategoryResponse;
import com.kiborisaway.tasktimetracker.data.entity.CauseDirection;
import com.kiborisaway.tasktimetracker.data.entity.ReflectionCauseCategory;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorDetailsBuilder;
import com.kiborisaway.tasktimetracker.security.JsonAuthenticationEntryPoint;
import com.kiborisaway.tasktimetracker.service.ReflectionCauseCategoryService;
import com.kiborisaway.tasktimetracker.support.WebMvcTestSecuritySupportConfig;
import com.kiborisaway.tasktimetracker.support.WithMockAuthenticatedUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(ReflectionCauseCategoryController.class)
@WithMockAuthenticatedUser
@Import({WebMvcTestSecuritySupportConfig.class, JsonAuthenticationEntryPoint.class})
class ReflectionCauseCategoryControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ReflectionCauseCategoryService service;

  @MockitoBean
  private ErrorDetailsBuilder errorDetailsBuilder;

  @Test
  void 一覧取得成功_200と表示順の原因カテゴリ一覧を返すこと() throws Exception {
    when(service.findAllActive()).thenReturn(List.of(
        new ReflectionCauseCategoryResponse(category(
            "TASK_BREAKDOWN", "作業の洗い出しが足りなかった", CauseDirection.OVER,
            "着手前に手順を書き出す")),
        new ReflectionCauseCategoryResponse(category(
            "OTHER", "その他", CauseDirection.BOTH, null))));

    mockMvc.perform(MockMvcRequestBuilders.get("/reflection-cause-categories"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].code").value("TASK_BREAKDOWN"))
        .andExpect(jsonPath("$[0].label").value("作業の洗い出しが足りなかった"))
        .andExpect(jsonPath("$[0].direction").value("OVER"))
        .andExpect(jsonPath("$[0].nextActionHint").value("着手前に手順を書き出す"))
        .andExpect(jsonPath("$[1].code").value("OTHER"))
        .andExpect(jsonPath("$[1].direction").value("BOTH"))
        .andExpect(jsonPath("$[1].nextActionHint").isEmpty());

    verify(service).findAllActive();
  }

  @Test
  void 一覧取得成功_カテゴリが0件の場合は空配列を返すこと() throws Exception {
    when(service.findAllActive()).thenReturn(List.of());

    mockMvc.perform(MockMvcRequestBuilders.get("/reflection-cause-categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  private static ReflectionCauseCategory category(
      String code, String label, CauseDirection direction, String nextActionHint) {
    return new ReflectionCauseCategory(1, code, label, direction, nextActionHint, 10, true);
  }
}
