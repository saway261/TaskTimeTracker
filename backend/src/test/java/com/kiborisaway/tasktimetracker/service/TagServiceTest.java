package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.data.dto.tag.TagCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.tag.TagResponse;
import com.kiborisaway.tasktimetracker.data.dto.tag.TagUpdateRequest;
import com.kiborisaway.tasktimetracker.data.entity.Tag;
import com.kiborisaway.tasktimetracker.exception.TagLimitExceededException;
import com.kiborisaway.tasktimetracker.exception.TagNameDuplicateException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.TagRepository;
import com.kiborisaway.tasktimetracker.repository.TagRow;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

  private static final int USER_ID = 1;
  private static final int MAX_ACTIVE_TAGS = 50;

  @Mock
  private TagRepository repository;

  @InjectMocks
  private TagService sut;

  @Test
  void 一覧検索_付与タスク数つきのレスポンスへ変換すること() {
    when(repository.findAllByUserId(USER_ID, true)).thenReturn(List.of(
        new TagRow(1, "調査", false, 3),
        new TagRow(2, "設定", true, 0)));

    List<TagResponse> actual = sut.findAll(USER_ID, true);

    assertThat(actual)
        .extracting(TagResponse::getId, TagResponse::getName, TagResponse::getIsArchived,
            TagResponse::getAssignedTaskCount)
        .containsExactly(
            tuple(1, "調査", false, 3),
            tuple(2, "設定", true, 0));
  }

  @Test
  void 新規作成成功_既存タグがなければ登録しcreatedがtrueで返ること() {
    stubInsertAssignsId(10);
    when(repository.findByUserIdAndNameNormalized(eq(USER_ID), anyString())).thenReturn(null);
    when(repository.countActiveByUserId(USER_ID)).thenReturn(0);

    TagCreateRequest request = new TagCreateRequest();
    request.setName("調査");

    TagService.CreateResult actual = sut.create(USER_ID, request);

    assertThat(actual.created()).isTrue();
    assertThat(actual.tag().getId()).isEqualTo(10);
    assertThat(actual.tag().getName()).isEqualTo("調査");
    assertThat(actual.tag().getAssignedTaskCount()).isZero();
    verify(repository).insert(any(Tag.class));
    verify(repository, never()).countAssignedTasks(anyInt());
  }

  @Test
  void 新規作成成功_前後の空白をトリムして保存すること() {
    stubInsertAssignsId(10);
    when(repository.findByUserIdAndNameNormalized(eq(USER_ID), anyString())).thenReturn(null);
    when(repository.countActiveByUserId(USER_ID)).thenReturn(0);

    TagCreateRequest request = new TagCreateRequest();
    request.setName("  調査  ");

    sut.create(USER_ID, request);

    ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
    verify(repository).insert(captor.capture());
    assertThat(captor.getValue().getName()).isEqualTo("調査");
  }

  @Test
  void 新規作成成功_全角半角の違いを吸収した正規化名で既存タグを検索すること() {
    stubInsertAssignsId(10);
    when(repository.findByUserIdAndNameNormalized(eq(USER_ID), anyString())).thenReturn(null);
    when(repository.countActiveByUserId(USER_ID)).thenReturn(0);

    TagCreateRequest request = new TagCreateRequest();
    request.setName("ＡＰＩ");

    sut.create(USER_ID, request);

    verify(repository).findByUserIdAndNameNormalized(USER_ID, "api");
  }

  @Test
  void 新規作成成功_半角カナと全角カナが同じ正規化名になること() {
    stubInsertAssignsId(10);
    when(repository.findByUserIdAndNameNormalized(eq(USER_ID), anyString())).thenReturn(null);
    when(repository.countActiveByUserId(USER_ID)).thenReturn(0);

    TagCreateRequest halfWidth = new TagCreateRequest();
    halfWidth.setName("ﾁｮｳｻ");
    sut.create(USER_ID, halfWidth);

    TagCreateRequest fullWidth = new TagCreateRequest();
    fullWidth.setName("チョウサ");
    sut.create(USER_ID, fullWidth);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(repository, times(2))
        .findByUserIdAndNameNormalized(eq(USER_ID), captor.capture());
    assertThat(captor.getAllValues().get(0)).isEqualTo(captor.getAllValues().get(1));
  }

  @Test
  void 新規作成成功_ひらがなとカタカナは異なる正規化名になること() {
    stubInsertAssignsId(10);
    when(repository.findByUserIdAndNameNormalized(eq(USER_ID), anyString())).thenReturn(null);
    when(repository.countActiveByUserId(USER_ID)).thenReturn(0);

    TagCreateRequest hiragana = new TagCreateRequest();
    hiragana.setName("ちょうさ");
    sut.create(USER_ID, hiragana);

    TagCreateRequest katakana = new TagCreateRequest();
    katakana.setName("チョウサ");
    sut.create(USER_ID, katakana);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(repository, times(2))
        .findByUserIdAndNameNormalized(eq(USER_ID), captor.capture());
    assertThat(captor.getAllValues().get(0)).isNotEqualTo(captor.getAllValues().get(1));
  }

  @Test
  void 新規作成成功_大文字小文字の違いを吸収した正規化名になること() {
    stubInsertAssignsId(10);
    when(repository.findByUserIdAndNameNormalized(eq(USER_ID), anyString())).thenReturn(null);
    when(repository.countActiveByUserId(USER_ID)).thenReturn(0);

    TagCreateRequest request = new TagCreateRequest();
    request.setName("Bug");

    sut.create(USER_ID, request);

    verify(repository).findByUserIdAndNameNormalized(USER_ID, "bug");
  }

  @Test
  void 新規作成成功_表示名にはNFKCを適用せず入力どおり保存すること() {
    stubInsertAssignsId(10);
    when(repository.findByUserIdAndNameNormalized(eq(USER_ID), anyString())).thenReturn(null);
    when(repository.countActiveByUserId(USER_ID)).thenReturn(0);

    TagCreateRequest request = new TagCreateRequest();
    request.setName("ＡＰＩ");

    sut.create(USER_ID, request);

    ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
    verify(repository).insert(captor.capture());
    assertThat(captor.getValue().getName()).isEqualTo("ＡＰＩ");
  }

  @Test
  void 新規作成成功_既存タグと正規化後に一致する場合は登録せず既存タグをcreatedはfalseで返すこと() {
    Tag existing = new Tag(10, USER_ID, "ＡＰＩ", "api", false, null);
    when(repository.findByUserIdAndNameNormalized(USER_ID, "api")).thenReturn(existing);
    when(repository.countAssignedTasks(10)).thenReturn(4);

    TagCreateRequest request = new TagCreateRequest();
    request.setName("API");

    TagService.CreateResult actual = sut.create(USER_ID, request);

    assertThat(actual.created()).isFalse();
    assertThat(actual.tag().getId()).isEqualTo(10);
    assertThat(actual.tag().getName()).isEqualTo("ＡＰＩ");
    assertThat(actual.tag().getAssignedTaskCount()).isEqualTo(4);
    verify(repository, never()).insert(any(Tag.class));
    verify(repository, never()).countActiveByUserId(anyInt());
  }

  @Test
  void 新規作成失敗_保有上限に達している場合はTagLimitExceededExceptionが発生すること() {
    when(repository.findByUserIdAndNameNormalized(eq(USER_ID), anyString())).thenReturn(null);
    when(repository.countActiveByUserId(USER_ID)).thenReturn(MAX_ACTIVE_TAGS);

    TagCreateRequest request = new TagCreateRequest();
    request.setName("調査");

    assertThatThrownBy(() -> sut.create(USER_ID, request))
        .isInstanceOf(TagLimitExceededException.class);
    verify(repository, never()).insert(any(Tag.class));
  }

  @Test
  void 更新成功_リネームできること() {
    when(repository.findByUserIdAndNameNormalized(eq(USER_ID), anyString())).thenReturn(null);
    when(repository.updateName(eq(10), eq(USER_ID), anyString(), anyString())).thenReturn(1);
    when(repository.findByIdAndUserId(10, USER_ID))
        .thenReturn(new Tag(10, USER_ID, "リサーチ", "リサーチ", false, null));
    when(repository.countAssignedTasks(10)).thenReturn(2);

    TagUpdateRequest request = new TagUpdateRequest();
    request.setName("リサーチ");

    TagResponse actual = sut.update(USER_ID, 10, request);

    assertThat(actual.getName()).isEqualTo("リサーチ");
    assertThat(actual.getAssignedTaskCount()).isEqualTo(2);
  }

  @Test
  void 更新失敗_自分自身以外の既存タグと正規化後に重複する場合はTagNameDuplicateExceptionが発生すること() {
    Tag other = new Tag(20, USER_ID, "設定", "設定", false, null);
    when(repository.findByUserIdAndNameNormalized(USER_ID, "設定")).thenReturn(other);

    TagUpdateRequest request = new TagUpdateRequest();
    request.setName("設定");

    assertThatThrownBy(() -> sut.update(USER_ID, 10, request))
        .isInstanceOf(TagNameDuplicateException.class);
    verify(repository, never()).updateName(anyInt(), anyInt(), anyString(), anyString());
  }

  @Test
  void 更新成功_一致する既存タグが自分自身であれば更新できること() {
    Tag self = new Tag(10, USER_ID, "調査", "調査", false, null);
    when(repository.findByUserIdAndNameNormalized(USER_ID, "調査")).thenReturn(self);
    when(repository.updateName(eq(10), eq(USER_ID), anyString(), anyString())).thenReturn(1);
    when(repository.findByIdAndUserId(10, USER_ID)).thenReturn(self);
    when(repository.countAssignedTasks(10)).thenReturn(0);

    TagUpdateRequest request = new TagUpdateRequest();
    request.setName("調査");

    TagResponse actual = sut.update(USER_ID, 10, request);

    assertThat(actual.getId()).isEqualTo(10);
  }

  @Test
  void 更新失敗_対象が存在しない場合はTargetNotFoundExceptionが発生すること() {
    when(repository.findByUserIdAndNameNormalized(eq(USER_ID), anyString())).thenReturn(null);
    when(repository.updateName(eq(99), eq(USER_ID), anyString(), anyString())).thenReturn(0);

    TagUpdateRequest request = new TagUpdateRequest();
    request.setName("調査");

    assertThatThrownBy(() -> sut.update(USER_ID, 99, request))
        .isInstanceOf(TargetNotFoundException.class);
  }

  @Test
  void アーカイブ状態更新成功_アーカイブできること() {
    Tag tag = new Tag(10, USER_ID, "調査", "調査", false, null);
    when(repository.findByIdAndUserId(10, USER_ID))
        .thenReturn(tag)
        .thenReturn(new Tag(10, USER_ID, "調査", "調査", true, null));
    when(repository.countAssignedTasks(10)).thenReturn(1);

    TagResponse actual = sut.updateArchived(USER_ID, 10, true);

    assertThat(actual.getIsArchived()).isTrue();
    verify(repository).updateArchived(10, USER_ID, true);
    verify(repository, never()).countActiveByUserId(anyInt());
  }

  @Test
  void アーカイブ状態更新成功_解除できること() {
    Tag archived = new Tag(10, USER_ID, "調査", "調査", true, null);
    when(repository.findByIdAndUserId(10, USER_ID))
        .thenReturn(archived)
        .thenReturn(new Tag(10, USER_ID, "調査", "調査", false, null));
    when(repository.countActiveByUserId(USER_ID)).thenReturn(0);
    when(repository.countAssignedTasks(10)).thenReturn(1);

    TagResponse actual = sut.updateArchived(USER_ID, 10, false);

    assertThat(actual.getIsArchived()).isFalse();
    verify(repository).updateArchived(10, USER_ID, false);
  }

  @Test
  void アーカイブ状態更新失敗_解除時に保有上限に達している場合はTagLimitExceededExceptionが発生すること() {
    Tag archived = new Tag(10, USER_ID, "調査", "調査", true, null);
    when(repository.findByIdAndUserId(10, USER_ID)).thenReturn(archived);
    when(repository.countActiveByUserId(USER_ID)).thenReturn(MAX_ACTIVE_TAGS);

    assertThatThrownBy(() -> sut.updateArchived(USER_ID, 10, false))
        .isInstanceOf(TagLimitExceededException.class);
    verify(repository, never()).updateArchived(anyInt(), anyInt(), anyBoolean());
  }

  @Test
  void アーカイブ状態更新成功_既にアクティブなタグへの解除要求は上限に関係なく成功すること() {
    Tag active = new Tag(10, USER_ID, "調査", "調査", false, null);
    when(repository.findByIdAndUserId(10, USER_ID))
        .thenReturn(active)
        .thenReturn(active);
    when(repository.countAssignedTasks(10)).thenReturn(0);

    TagResponse actual = sut.updateArchived(USER_ID, 10, false);

    assertThat(actual.getIsArchived()).isFalse();
    verify(repository, never()).countActiveByUserId(anyInt());
    verify(repository).updateArchived(10, USER_ID, false);
  }

  @Test
  void アーカイブ状態更新失敗_対象が存在しない場合はTargetNotFoundExceptionが発生すること() {
    when(repository.findByIdAndUserId(99, USER_ID)).thenReturn(null);

    assertThatThrownBy(() -> sut.updateArchived(USER_ID, 99, true))
        .isInstanceOf(TargetNotFoundException.class);
    verify(repository, never()).updateArchived(anyInt(), anyInt(), anyBoolean());
  }

  private void stubInsertAssignsId(int id) {
    doAnswer(invocation -> {
      Tag tag = invocation.getArgument(0);
      tag.setId(id);
      return null;
    }).when(repository).insert(any(Tag.class));
  }
}
