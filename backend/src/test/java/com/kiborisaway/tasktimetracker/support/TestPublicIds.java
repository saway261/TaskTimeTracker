package com.kiborisaway.tasktimetracker.support;

import com.kiborisaway.tasktimetracker.publicid.PublicIdCodec;
import com.kiborisaway.tasktimetracker.publicid.PublicIdType;

/**
 * テストコードから、本番と同じ変換規則で公開ID文字列を組み立てるためのヘルパー。
 *
 * <p>URLやJSONの期待値にエンコード済み文字列を直接ハードコードすると、テスト用アルファベット
 * （{@code src/test/resources/application.properties}）を調整するたびに全テストを書き直す
 * ことになる。{@link PublicIdCodec} はテスト用アルファベットの値だけから決まる純粋関数なので、
 * ここで同じ値を使って独立したインスタンスを持てば、Springコンテキストを必要とする
 * {@code @WebMvcTest} だけでなく、Mockitoのみで完結する単体テストからも呼び出せる。
 *
 * <p>ここに書く6つのアルファベットは {@code src/test/resources/application.properties} の
 * {@code app.public-id.*-alphabet} と必ず一致させること。
 */
public final class TestPublicIds {

  private static final PublicIdCodec CODEC = new PublicIdCodec(
      "eupdScj9oT5Qb8KEGnBWD4x3rl6g0fVOmvyAit7Nazw2qhHsRCYLMU1ZJIXPkF",
      "nQMjRZWL9a7JBqNkv0xwgGYSUHeEPui4sbhX6rt2mpDVcA5zfd1oCT8KIO3yFl",
      "g8J7lVTjHbUu6nxfYD3FGshQzBr4PNtdZO0IeowLEKWyipmS51CRvM2ak9qcXA",
      "Sg9TFtApYhisDj1aK7QL02bOHyrocNeCq3v6dUlwnZMGfEuXxm54IBzPkJVWR8",
      "Eg8qs2PfUHXzOpbItcB05kJxu6i4F7WYyAnSCV1w9eGKhoZQMRaNmLjvdlT3Dr",
      "J1vFZmthnisO3lD7bjfukxVaodCwLY2IPGUTHA8zKgXQ5BEWScMpr94q0NRye6");

  private TestPublicIds() {
  }

  /**
   * 実物の {@link PublicIdCodec} が必要なテスト（Mockitoのみで完結する単体テストで、
   * Service側にCodecを注入する必要がある場合など）向けに、この静的ヘルパーが内部で使っている
   * インスタンスをそのまま渡す。
   */
  public static PublicIdCodec codec() {
    return CODEC;
  }

  public static String project(int id) {
    return CODEC.encode(PublicIdType.PROJECT, id);
  }

  public static String taskGroup(int id) {
    return CODEC.encode(PublicIdType.TASK_GROUP, id);
  }

  public static String task(int id) {
    return CODEC.encode(PublicIdType.TASK, id);
  }

  public static String memo(int id) {
    return CODEC.encode(PublicIdType.MEMO, id);
  }

  public static String workSession(int id) {
    return CODEC.encode(PublicIdType.WORK_SESSION, id);
  }

  public static String tag(int id) {
    return CODEC.encode(PublicIdType.TAG, id);
  }
}
