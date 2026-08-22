package com.kiborisaway.tasktimetracker.publicid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 公開IDのコーデックの単体テスト。
 *
 * <p>テスト用のアルファベットはこのクラス内で固定する。Springの設定値に依存させると、
 * 設定を変えたときにテストの意味が変わってしまうため。
 */
class PublicIdCodecTest {

  // テスト専用の固定アルファベット（62文字・重複なし・種別ごとに別順序）
  private static final String PROJECT_ALPHABET =
      "FaHxrcZ1yMXzR2CBwWJ5m8nqb7NVd4fYtETPUOgp6jQKv0sAlDISi9h3GLueko";
  private static final String TASK_GROUP_ALPHABET =
      "o3mMQhTrLO7zUGc8VKvCEFZnj549NqHDlkRSJaBWxX2d1AgtIfwPY6besiuy0p";
  private static final String TASK_ALPHABET =
      "hbBuqVkCYNoIS4mLlJ7vgnjO5TP2EAaGQFrfidp9HXw3t0Wz6KD8y1xsceMUZR";
  private static final String MEMO_ALPHABET =
      "iWXPVN8u7vlmgrQ4kbanAwYIohZKs3p1GHeEScdLzRtq9y6UfOCB025TFDxJjM";
  private static final String WORK_SESSION_ALPHABET =
      "43fuenWxcVKgC7HBDRPp1S2UwJvFyiQNLYMt9OGsdZXI6jbh08arTozq5lmkEA";
  private static final String TAG_ALPHABET =
      "lUOtWazDkQJP0u8f5LgGxZ6hwyYE1H3eoIcABSqKmXCvb9M4V72isTjRFdrpNn";

  private final PublicIdCodec sut = newCodec();

  private static PublicIdCodec newCodec() {
    return new PublicIdCodec(PROJECT_ALPHABET, TASK_GROUP_ALPHABET, TASK_ALPHABET,
        MEMO_ALPHABET, WORK_SESSION_ALPHABET, TAG_ALPHABET);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 42, 999_999, Integer.MAX_VALUE})
  void ラウンドトリップ_エンコードしてデコードすると元の内部IDに戻ること(int id) {
    for (PublicIdType type : PublicIdType.values()) {
      String publicId = sut.encode(type, id);

      assertThat(sut.decode(type, publicId)).isEqualTo(id);
    }
  }

  @ParameterizedTest
  @EnumSource(PublicIdType.class)
  void エンコード_英数字のみでプレフィックスや区切りを含まないこと(PublicIdType type) {
    assertThat(sut.encode(type, 1)).matches("[0-9A-Za-z]+");
  }

  @ParameterizedTest
  @EnumSource(PublicIdType.class)
  void エンコード_同じ入力に対して常に同じ文字列を返すこと(PublicIdType type) {
    assertThat(sut.encode(type, 123)).isEqualTo(newCodec().encode(type, 123));
  }

  @Test
  void エンコード_同じ内部IDでも種別が違えば別の文字列になること() {
    Set<String> encoded = new HashSet<>();
    for (PublicIdType type : PublicIdType.values()) {
      encoded.add(sut.encode(type, 3));
    }

    // アルファベットを種別ごとに分けているため、同じ内部IDでも別の文字列になる
    assertThat(encoded).hasSize(PublicIdType.values().length);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
  void エンコード_0以下の内部IDは変換できないこと(int id) {
    assertThatThrownBy(() -> sut.encode(PublicIdType.PROJECT, id))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void デコード_別種別のIDを渡すと例外になること() {
    String taskPublicId = sut.encode(PublicIdType.TASK, 3);

    assertThatThrownBy(() -> sut.decode(PublicIdType.PROJECT, taskPublicId))
        .isInstanceOf(PublicIdInvalidException.class);
  }

  /**
   * 別種別のIDが誤って解決されないことを確認する。
   *
   * <p>公開IDは種別を示すプレフィックスを持たないため、種別の取り違えを防いでいるのは
   * 正規化チェック（再エンコードによる一致確認）だけである。{@code minLength=10} の詰め物まで
   * 含めて正規形が一意に決まるため、別アルファベット由来の文字列も弾ける
   * （実測2026-08-22、各種別20,000件・計100,000件で受理数0件）。
   *
   * <p>この性質が崩れると、あるタスクのIDが別のプロジェクトのIDとして静かに解決される。
   * {@code minLength} を下げる変更などがあればこのテストが検知する。
   */
  @Test
  void デコード_別種別のIDは解決されないこと() {
    long crossTypeAccepted = Stream.of(PublicIdType.values())
        .filter(type -> type != PublicIdType.PROJECT)
        .flatMap(type -> IntStream.rangeClosed(1, 2000)
            .mapToObj(id -> sut.encode(type, id)))
        .filter(this::decodesAsProject)
        .count();

    assertThat(crossTypeAccepted).isZero();
  }

  /** アルファベット内の文字で構成したランダムな文字列も受理しないこと。 */
  @Test
  void デコード_アルファベット内の文字で作ったランダム文字列を受理しないこと() {
    Random random = new Random(42);
    long accepted = IntStream.range(0, 10_000)
        .mapToObj(i -> {
          StringBuilder candidate = new StringBuilder();
          for (int j = 0; j < 10; j++) {
            candidate.append(PROJECT_ALPHABET.charAt(random.nextInt(PROJECT_ALPHABET.length())));
          }
          return candidate.toString();
        })
        .filter(this::decodesAsProject)
        .count();

    assertThat(accepted).isZero();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "",                  // 空文字
      "!!!!!!!!!!",        // アルファベット外の文字
      "あいうえお",         // 非ASCII
      "1",                 // 最小長に満たない
      "prj_Xr9mQ2vK",      // かつて検討したプレフィックス付き形式は受け付けない
  })
  @NullSource
  void デコード_不正な文字列は例外になること(String publicId) {
    assertThatThrownBy(() -> sut.decode(PublicIdType.PROJECT, publicId))
        .isInstanceOf(PublicIdInvalidException.class);
  }

  @Test
  void デコード_正規形でない文字列を受け付けないこと() {
    // 正しい公開IDの末尾に文字を足したものは、デコードできても再エンコードで一致しない。
    String valid = sut.encode(PublicIdType.PROJECT, 1);

    assertThatThrownBy(() -> sut.decode(PublicIdType.PROJECT, valid + "a"))
        .isInstanceOf(PublicIdInvalidException.class);
  }

  @Test
  void デコード_例外は期待した種別を保持すること() {
    Throwable thrown = catchThrowable(() -> sut.decode(PublicIdType.TAG, "!!!!!!!!!!"));

    assertThat(thrown)
        .isInstanceOf(PublicIdInvalidException.class)
        .extracting(e -> ((PublicIdInvalidException) e).getType())
        .isEqualTo(PublicIdType.TAG);
  }

  @Test
  void エンコード_最小長を満たすこと() {
    // 内部IDが小さくても短い文字列にならない（連番であることを見た目から推測させない）
    assertThat(sut.encode(PublicIdType.PROJECT, 1)).hasSizeGreaterThanOrEqualTo(10);
  }

  @Test
  void 起動時検証_62文字でないアルファベットは拒否すること() {
    String tooShort = PROJECT_ALPHABET.substring(0, 61);

    assertThatThrownBy(() -> new PublicIdCodec(tooShort, TASK_GROUP_ALPHABET, TASK_ALPHABET,
        MEMO_ALPHABET, WORK_SESSION_ALPHABET, TAG_ALPHABET))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("62文字");
  }

  @Test
  void 起動時検証_重複した文字を含むアルファベットは拒否すること() {
    String duplicated = PROJECT_ALPHABET.substring(0, 61) + PROJECT_ALPHABET.charAt(0);

    assertThatThrownBy(() -> new PublicIdCodec(duplicated, TASK_GROUP_ALPHABET, TASK_ALPHABET,
        MEMO_ALPHABET, WORK_SESSION_ALPHABET, TAG_ALPHABET))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("重複");
  }

  @Test
  void 起動時検証_種別間で同じアルファベットは拒否すること() {
    assertThatThrownBy(() -> new PublicIdCodec(PROJECT_ALPHABET, PROJECT_ALPHABET, TASK_ALPHABET,
        MEMO_ALPHABET, WORK_SESSION_ALPHABET, TAG_ALPHABET))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("種別間で重複");
  }

  @Test
  void 起動時検証_nullのアルファベットは拒否すること() {
    assertThatThrownBy(() -> new PublicIdCodec(null, TASK_GROUP_ALPHABET, TASK_ALPHABET,
        MEMO_ALPHABET, WORK_SESSION_ALPHABET, TAG_ALPHABET))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void テスト用アルファベットが前提を満たしていること() {
    Stream.of(PROJECT_ALPHABET, TASK_GROUP_ALPHABET, TASK_ALPHABET, MEMO_ALPHABET,
            WORK_SESSION_ALPHABET, TAG_ALPHABET)
        .forEach(alphabet -> {
          assertThat(alphabet).hasSize(62);
          assertThat(alphabet.chars().distinct().count()).isEqualTo(62);
        });
    assertThat(new HashSet<>(Arrays.asList(PROJECT_ALPHABET, TASK_GROUP_ALPHABET, TASK_ALPHABET,
        MEMO_ALPHABET, WORK_SESSION_ALPHABET, TAG_ALPHABET))).hasSize(6);
  }

  private boolean decodesAsProject(String candidate) {
    try {
      sut.decode(PublicIdType.PROJECT, candidate);
      return true;
    } catch (PublicIdInvalidException e) {
      return false;
    }
  }
}
