package com.kiborisaway.tasktimetracker.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.data.dto.tag.TagCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.tag.TagResponse;
import com.kiborisaway.tasktimetracker.data.dto.tag.TagUpdateRequest;
import com.kiborisaway.tasktimetracker.exception.TagLimitExceededException;
import com.kiborisaway.tasktimetracker.exception.TagNameDuplicateException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorDetailsBuilder;
import com.kiborisaway.tasktimetracker.security.JsonAuthenticationEntryPoint;
import com.kiborisaway.tasktimetracker.service.TagService;
import com.kiborisaway.tasktimetracker.support.TestPublicIds;
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

@WebMvcTest(TagController.class)
@WithMockAuthenticatedUser
@Import({WebMvcTestSecuritySupportConfig.class, JsonAuthenticationEntryPoint.class})
class TagControllerTest {

  private static final int USER_ID = 1;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TagService service;

  @MockitoBean
  private ErrorDetailsBuilder errorDetailsBuilder;

  @Test
  void 一覧取得成功_200とタグ一覧を返すこと() throws Exception {
    when(service.findAll(USER_ID, false)).thenReturn(List.of(
        new TagResponse(1, "調査", false, 3),
        new TagResponse(2, "設定", false, 0)));

    mockMvc.perform(MockMvcRequestBuilders.get("/tags"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value(TestPublicIds.tag(1)))
        .andExpect(jsonPath("$[0].name").value("調査"))
        .andExpect(jsonPath("$[0].isArchived").value(false))
        .andExpect(jsonPath("$[0].assignedTaskCount").value(3));

    verify(service).findAll(USER_ID, false);
  }

  @Test
  void 一覧取得成功_includeArchivedを省略するとfalseとしてサービスを呼び出すこと() throws Exception {
    when(service.findAll(USER_ID, false)).thenReturn(List.of());

    mockMvc.perform(MockMvcRequestBuilders.get("/tags"))
        .andExpect(status().isOk());

    verify(service).findAll(USER_ID, false);
  }

  @Test
  void 一覧取得成功_includeArchivedにtrueを指定するとアーカイブ済みを含めてサービスを呼び出すこと()
      throws Exception {
    when(service.findAll(USER_ID, true)).thenReturn(List.of());

    mockMvc.perform(MockMvcRequestBuilders.get("/tags").param("includeArchived", "true"))
        .andExpect(status().isOk());

    verify(service).findAll(USER_ID, true);
  }

  @Test
  void 一覧取得成功_タグが0件の場合は空配列を返すこと() throws Exception {
    when(service.findAll(USER_ID, false)).thenReturn(List.of());

    mockMvc.perform(MockMvcRequestBuilders.get("/tags"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void 新規作成成功_新規登録の場合は201を返すこと() throws Exception {
    when(service.create(eq(USER_ID), any(TagCreateRequest.class)))
        .thenReturn(new TagService.CreateResult(new TagResponse(10, "調査", false, 0), true));
    String validRequest = """
        {
          "name": "調査"
        }
        """;

    mockMvc.perform(MockMvcRequestBuilders.post("/tags")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(TestPublicIds.tag(10)))
        .andExpect(jsonPath("$.name").value("調査"));

    verify(service).create(eq(USER_ID), any(TagCreateRequest.class));
  }

  @Test
  void 新規作成成功_既存タグを再利用した場合は200を返すこと() throws Exception {
    when(service.create(eq(USER_ID), any(TagCreateRequest.class)))
        .thenReturn(new TagService.CreateResult(new TagResponse(10, "ＡＰＩ", false, 4), false));
    String validRequest = """
        {
          "name": "API"
        }
        """;

    mockMvc.perform(MockMvcRequestBuilders.post("/tags")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(TestPublicIds.tag(10)))
        .andExpect(jsonPath("$.name").value("ＡＰＩ"));
  }

  @Test
  void 新規作成失敗_名前が空白のみなら400を返すこと() throws Exception {
    String invalidRequest = """
        {
          "name": "  "
        }
        """;

    mockMvc.perform(MockMvcRequestBuilders.post("/tags")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 新規作成失敗_名前が20文字を超えるなら400を返すこと() throws Exception {
    String invalidRequest = """
        {
          "name": "123456789012345678901"
        }
        """;

    mockMvc.perform(MockMvcRequestBuilders.post("/tags")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 新規作成失敗_保有上限に達している場合は400を返すこと() throws Exception {
    when(service.create(eq(USER_ID), any(TagCreateRequest.class)))
        .thenThrow(new TagLimitExceededException("tagLimit", "tag limit exceeded"));
    String validRequest = """
        {
          "name": "調査"
        }
        """;

    mockMvc.perform(MockMvcRequestBuilders.post("/tags")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("tag limit exceeded"));
  }

  @Test
  void リネーム成功_200と更新後データを返すこと() throws Exception {
    when(service.update(eq(USER_ID), eq(10), any(TagUpdateRequest.class)))
        .thenReturn(new TagResponse(10, "リサーチ", false, 2));
    String validRequest = """
        {
          "name": "リサーチ"
        }
        """;

    mockMvc.perform(MockMvcRequestBuilders.put("/tags/" + TestPublicIds.tag(10))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("リサーチ"))
        .andExpect(jsonPath("$.assignedTaskCount").value(2));

    verify(service).update(eq(USER_ID), eq(10), any(TagUpdateRequest.class));
  }

  @Test
  void リネーム失敗_パス変数の形式が不正なら404を返すこと() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.put("/tags/invalid-id").with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void リネーム失敗_不正なリクエストボディなら400を返すこと() throws Exception {
    String invalidRequest = """
        {
          "name": ""
        }
        """;

    mockMvc.perform(MockMvcRequestBuilders.put("/tags/" + TestPublicIds.tag(10))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest());
  }

  @Test
  void リネーム失敗_名前が重複する場合は400を返すこと() throws Exception {
    doThrow(new TagNameDuplicateException("name", "同じ名前のタグが既に存在します"))
        .when(service).update(eq(USER_ID), eq(10), any(TagUpdateRequest.class));
    String validRequest = """
        {
          "name": "設定"
        }
        """;

    mockMvc.perform(MockMvcRequestBuilders.put("/tags/" + TestPublicIds.tag(10))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("tag name duplicate"));
  }

  @Test
  void リネーム失敗_対象が存在しない場合は404を返すこと() throws Exception {
    doThrow(new TargetNotFoundException("tag.id", "指定したIDのタグは見つかりませんでした"))
        .when(service).update(eq(USER_ID), eq(999), any(TagUpdateRequest.class));
    String validRequest = """
        {
          "name": "調査"
        }
        """;

    mockMvc.perform(MockMvcRequestBuilders.put("/tags/" + TestPublicIds.tag(999))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isNotFound());
  }

  @Test
  void アーカイブ状態更新成功_200と更新後データを返すこと() throws Exception {
    when(service.updateArchived(USER_ID, 10, true))
        .thenReturn(new TagResponse(10, "調査", true, 1));
    String validRequest = """
        {
          "isArchived": true
        }
        """;

    mockMvc.perform(MockMvcRequestBuilders.patch("/tags/" + TestPublicIds.tag(10) + "/archived")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isArchived").value(true));

    verify(service).updateArchived(USER_ID, 10, true);
  }

  @Test
  void アーカイブ状態更新失敗_リクエストボディにisArchivedがなければ400を返すこと() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.patch("/tags/" + TestPublicIds.tag(10) + "/archived")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void アーカイブ状態更新失敗_保有上限に達している場合は400を返すこと() throws Exception {
    when(service.updateArchived(USER_ID, 10, false))
        .thenThrow(new TagLimitExceededException("tagLimit", "tag limit exceeded"));
    String validRequest = """
        {
          "isArchived": false
        }
        """;

    mockMvc.perform(MockMvcRequestBuilders.patch("/tags/" + TestPublicIds.tag(10) + "/archived")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isBadRequest());
  }

  @Test
  void アーカイブ状態更新失敗_対象が存在しない場合は404を返すこと() throws Exception {
    doThrow(new TargetNotFoundException("tag.id", "指定したIDのタグは見つかりませんでした"))
        .when(service).updateArchived(eq(USER_ID), eq(999), anyBoolean());
    String validRequest = """
        {
          "isArchived": true
        }
        """;

    mockMvc.perform(MockMvcRequestBuilders.patch("/tags/" + TestPublicIds.tag(999) + "/archived")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
        .andExpect(status().isNotFound());
  }
}
