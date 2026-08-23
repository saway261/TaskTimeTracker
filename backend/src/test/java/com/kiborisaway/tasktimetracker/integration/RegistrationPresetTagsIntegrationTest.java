package com.kiborisaway.tasktimetracker.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.infrastructure.MailDeliveryClient;
import com.kiborisaway.tasktimetracker.repository.TagRepository;
import com.kiborisaway.tasktimetracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RegistrationPresetTagsIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TagRepository tagRepository;

  @MockitoBean
  private MailDeliveryClient mailDeliveryClient;

  @Test
  void ユーザー登録成功_新規ユーザーにプリセットタグ3件が作成されること() throws Exception {
    String email = "preset-tags@example.com";

    mockMvc.perform(post("/auth/register")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"register-passphrase\"}"))
        .andExpect(status().isCreated());

    AppUser user = userRepository.findByEmail(email);
    assertThat(user).isNotNull();
    assertThat(tagRepository.findAllByUserId(user.getId(), true))
        .allSatisfy(tag -> {
          assertThat(tag.getIsArchived()).isFalse();
          assertThat(tag.getAssignedTaskCount()).isZero();
        })
        .extracting(tag -> tag.getName())
        .containsExactlyInAnyOrder("調査・計画", "環境構築", "手作業");
  }
}
