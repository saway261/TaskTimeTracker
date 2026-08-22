package com.kiborisaway.tasktimetracker.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * ユーザーAのセッションでユーザーBが所有するリソースIDを指定した場合、
 * 全エンドポイントが404を返すことを検証する所有者分離の結合テストです（仕様書19章）。
 * ユーザーBの所有物一式（プロジェクト・タスクグループ・タスク・作業セッション・メモ）を
 * 実際のAPIで作成したうえで、ユーザーAのセッションから同一IDへアクセスします。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OwnershipIsolationIntegrationTest {

  private static final String USER_A_EMAIL = "user-a@example.com";
  private static final String USER_A_PASSWORD = "TestPasswordA123!";
  private static final String USER_B_EMAIL = "user-b@example.com";
  private static final String USER_B_PASSWORD = "TestPasswordB123!";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private Cookie userA;
  private Cookie userB;

  private String projectBId;
  private String taskGroupBId;
  private String taskB1Id; // プロジェクト直下タスク
  private String taskB2Id; // タスクグループ配下タスク
  private String workSessionBId;
  private String memoProjectBId;

  @BeforeEach
  void setUp() throws Exception {
    AppUser appUserB = userRepository.findById(2);
    userRepository.updatePassword(
        appUserB.getId(), passwordEncoder.encode(USER_B_PASSWORD), LocalDateTime.now());

    userA = login(USER_A_EMAIL, USER_A_PASSWORD);
    userB = login(USER_B_EMAIL, USER_B_PASSWORD);

    projectBId = createAndGetId("/projects", userB, """
        {"title":"userBプロジェクト","description":"分離テスト用"}
        """);
    taskGroupBId = createAndGetId("/projects/" + projectBId + "/task-groups", userB, """
        {"title":"userBタスクグループ","description":"分離テスト用"}
        """);
    taskB1Id = createAndGetId("/projects/" + projectBId + "/tasks", userB, """
        {"title":"userBタスク1","description":"直下","estimatedMinutes":60}
        """);
    taskB2Id = createAndGetId("/task-groups/" + taskGroupBId + "/tasks", userB, """
        {"title":"userBタスク2","description":"配下","estimatedMinutes":60}
        """);
    workSessionBId = createWorkSessionAndGetId("/tasks/" + taskB1Id + "/work-sessions", userB, """
        {"type":"MANUAL","minutes":30}
        """);
    memoProjectBId = createAndGetId("/projects/" + projectBId + "/memo", userB, """
        {"comment":"userBメモ"}
        """);
    // タスクグループ・タスク配下のメモも作成し、メモ操作系エンドポイントの前提を整える
    createAndGetId("/task-groups/" + taskGroupBId + "/memo", userB, """
        {"comment":"userBメモtg"}
        """);
    createAndGetId("/tasks/" + taskB1Id + "/memo", userB, """
        {"comment":"userBメモtask"}
        """);
  }

  @Test
  void プロジェクト関連エンドポイント_userAはuserBのプロジェクトへ404となること() throws Exception {
    expectNotFound(get("/projects/" + projectBId));
    expectNotFound(put("/projects/" + projectBId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"title":"改ざん","description":"改ざん","isFinished":true}
            """));
  }

  @Test
  void タスクグループ関連エンドポイント_userAはuserBのタスクグループへ404となること() throws Exception {
    expectNotFound(get("/projects/" + projectBId + "/task-groups"));
    expectNotFound(get("/task-groups/" + taskGroupBId));
    expectNotFound(post("/projects/" + projectBId + "/task-groups")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"title":"新規","description":"新規"}
            """));
    expectNotFound(put("/task-groups/" + taskGroupBId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"title":"改ざん","description":"改ざん","isFinished":true}
            """));
  }

  @Test
  void タスク関連エンドポイント_userAはuserBのタスクへ404となること() throws Exception {
    expectNotFound(get("/projects/" + projectBId + "/tasks"));
    expectNotFound(get("/task-groups/" + taskGroupBId + "/tasks"));
    expectNotFound(get("/tasks/" + taskB1Id));
    expectNotFound(post("/projects/" + projectBId + "/tasks")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"title":"新規","description":"新規","estimatedMinutes":30}
            """));
    expectNotFound(post("/task-groups/" + taskGroupBId + "/tasks")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"title":"新規","description":"新規","estimatedMinutes":30}
            """));
    expectNotFound(patch("/tasks/" + taskB1Id)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"title":"改ざん","description":"改ざん"}
            """));
    expectNotFound(patch("/tasks/" + taskB1Id + "/estimated-minutes")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"estimatedMinutes":999}
            """));
    expectNotFound(patch("/tasks/" + taskB1Id + "/parent")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"taskGroupId":"%s"}
            """.formatted(taskGroupBId)));
    expectNotFound(patch("/tasks/" + taskB1Id + "/finished")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"isFinished":true}
            """));
    expectNotFound(delete("/tasks/" + taskB2Id));
  }

  @Test
  void 作業セッション関連エンドポイント_userAはuserBの作業セッションへ404となること() throws Exception {
    // total-minutes系とタスク内一覧は所有者条件付きの集計・一覧SQLで
    // 非所有リソースを「0件/空リスト」として扱う設計のため、404ではなくその結果を確認する。
    expectValue(get("/tasks/" + taskB1Id + "/work-sessions/total-minutes"), "0");
    expectValue(get("/task-groups/" + taskGroupBId + "/work-sessions/total-minutes"), "0");
    expectValue(get("/projects/" + projectBId + "/work-sessions/total-minutes"), "0");
    expectEmptyArray(get("/tasks/" + taskB1Id + "/work-sessions"));
    expectNotFound(get("/work-sessions/" + workSessionBId));
    expectNotFound(post("/tasks/" + taskB1Id + "/work-sessions")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"type":"MANUAL","minutes":15}
            """));
    expectNotFound(post("/work-sessions/" + workSessionBId + "/end"));
    expectNotFound(patch("/work-sessions/" + workSessionBId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"type":"MANUAL","minutes":45}
            """));
    expectNotFound(delete("/work-sessions/" + workSessionBId));
  }

  @Test
  void メモ関連エンドポイント_userAはuserBのメモへ404となること() throws Exception {
    expectNotFound(post("/projects/" + projectBId + "/memo")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"comment":"侵入メモ"}
            """));
    expectNotFound(post("/task-groups/" + taskGroupBId + "/memo")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"comment":"侵入メモ"}
            """));
    expectNotFound(post("/tasks/" + taskB1Id + "/memo")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"comment":"侵入メモ"}
            """));
    expectNotFound(patch("/memo/" + memoProjectBId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"comment":"改ざん"}
            """));
    expectNotFound(delete("/memo/" + memoProjectBId));
  }

  @Test
  void 並び順関連エンドポイント_userAはuserBの並び順へ404となること() throws Exception {
    expectNotFound(get("/projects/" + projectBId + "/item-order"));
    expectNotFound(put("/projects/" + projectBId + "/item-order")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"items":[{"type":"TASK","id":"%s"}]}
            """.formatted(taskB1Id)));
    expectNotFound(get("/task-groups/" + taskGroupBId + "/item-order"));
    expectNotFound(put("/task-groups/" + taskGroupBId + "/item-order")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"items":[{"id":"%s"}]}
            """.formatted(taskB2Id)));
  }

  private void expectNotFound(MockHttpServletRequestBuilder request) throws Exception {
    mockMvc.perform(request.cookie(userA).with(csrf()))
        .andExpect(status().isNotFound());
  }

  private void expectValue(MockHttpServletRequestBuilder request, String expected) throws Exception {
    mockMvc.perform(request.cookie(userA).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
            .string(expected));
  }

  private void expectEmptyArray(MockHttpServletRequestBuilder request) throws Exception {
    mockMvc.perform(request.cookie(userA).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
            .jsonPath("$").isEmpty());
  }

  private Cookie login(String email, String password) throws Exception {
    MvcResult result = mockMvc.perform(post("/auth/login")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
        .andExpect(status().isOk())
        .andReturn();
    return result.getResponse().getCookie("JSESSIONID");
  }

  private String createAndGetId(String url, Cookie cookie, String body) throws Exception {
    MvcResult result = mockMvc.perform(post(url)
            .cookie(cookie)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }

  private String createWorkSessionAndGetId(String url, Cookie cookie, String body) throws Exception {
    MvcResult result = mockMvc.perform(post(url)
            .cookie(cookie)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }
}
