package com.kiborisaway.tasktimetracker.publicid;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.sqids.Sqids;

/**
 * 内部の連番IDと、外部へ公開する不透明な文字列との相互変換を行います。
 *
 * <p>DBの{@code id}は変更せず、HTTPの境界（Controllerの入力・JSONの出力）でのみ変換します。
 * Service層・Repository層は内部IDのまま扱います。
 *
 * <p>アルファベットは種別ごとに別のものを設定から注入します。値が公開されると、第三者が
 * 同じSqidsで連番を再現できてしまい本施策の目的が失われるため、ソースへ直接書いてはいけません。
 */
@Component
public class PublicIdCodec {

  /** 生成する文字列の最小長。アルファベットと同様、変更すると既存IDを再現できなくなります。 */
  private static final int MIN_LENGTH = 10;

  /** アルファベットに使う文字数（英小文字26＋英大文字26＋数字10）。 */
  private static final int ALPHABET_LENGTH = 62;

  private final Map<PublicIdType, Sqids> sqidsByType;

  /**
   * 設定から種別ごとのアルファベットを受け取り、Sqidsを組み立てます。
   *
   * <p>プロパティにデフォルト値を置いていないため、未設定の場合はここへ到達する前に起動が失敗します。
   *
   * @param projectAlphabet     プロジェクト用アルファベット
   * @param taskGroupAlphabet   タスクグループ用アルファベット
   * @param taskAlphabet        タスク用アルファベット
   * @param memoAlphabet        メモ用アルファベット
   * @param workSessionAlphabet 作業セッション用アルファベット
   * @param tagAlphabet         タグ用アルファベット
   */
  public PublicIdCodec(
      @Value("${app.public-id.project-alphabet}") String projectAlphabet,
      @Value("${app.public-id.task-group-alphabet}") String taskGroupAlphabet,
      @Value("${app.public-id.task-alphabet}") String taskAlphabet,
      @Value("${app.public-id.memo-alphabet}") String memoAlphabet,
      @Value("${app.public-id.work-session-alphabet}") String workSessionAlphabet,
      @Value("${app.public-id.tag-alphabet}") String tagAlphabet) {

    Map<PublicIdType, String> alphabets = new EnumMap<>(PublicIdType.class);
    alphabets.put(PublicIdType.PROJECT, projectAlphabet);
    alphabets.put(PublicIdType.TASK_GROUP, taskGroupAlphabet);
    alphabets.put(PublicIdType.TASK, taskAlphabet);
    alphabets.put(PublicIdType.MEMO, memoAlphabet);
    alphabets.put(PublicIdType.WORK_SESSION, workSessionAlphabet);
    alphabets.put(PublicIdType.TAG, tagAlphabet);

    if (alphabets.size() != PublicIdType.values().length) {
      throw new IllegalStateException("公開IDのアルファベットが設定されていない種別があります");
    }
    alphabets.forEach(PublicIdCodec::validateAlphabet);
    if (new HashSet<>(alphabets.values()).size() != alphabets.size()) {
      throw new IllegalStateException("公開IDのアルファベットが種別間で重複しています");
    }

    Map<PublicIdType, Sqids> built = new EnumMap<>(PublicIdType.class);
    alphabets.forEach((type, alphabet) -> built.put(type,
        Sqids.builder().alphabet(alphabet).minLength(MIN_LENGTH).build()));
    this.sqidsByType = Map.copyOf(built);
  }

  /**
   * 内部IDを公開ID文字列へ変換します。
   *
   * <p>同じ種別・同じIDに対しては常に同じ文字列を返します。
   *
   * @param type 種別
   * @param id   内部ID（正の値）
   * @return 公開ID文字列（例: {@code Xr9mQ2vKp3}）
   */
  public String encode(PublicIdType type, int id) {
    if (id <= 0) {
      throw new IllegalArgumentException("公開IDへ変換できるのは正の内部IDのみです");
    }
    return sqidsByType.get(type).encode(List.of((long) id));
  }

  /**
   * 公開ID文字列を内部IDへ戻します。
   *
   * <p>種別を示すプレフィックスは持たないため、別種別のIDを渡された場合は下の正規化チェックで
   * 弾きます。実測では誤って解決された件数は0件でした（{@link PublicIdType} のJavadoc参照）。
   *
   * @param type     期待する種別
   * @param publicId 公開ID文字列
   * @return 内部ID
   * @throws PublicIdInvalidException 形式が不正、別種別、または正規形でない場合
   */
  public int decode(PublicIdType type, String publicId) {
    if (publicId == null || publicId.isEmpty()) {
      throw new PublicIdInvalidException(type);
    }

    Sqids sqids = sqidsByType.get(type);
    List<Long> decoded = sqids.decode(publicId);

    // Sqidsは仕様上、複数の文字列が同じ数値列へデコードされうる（公式READMEが明記）。
    // 再エンコードして一致を確認しないと、でたらめな文字列や別種別のIDが
    // 実在レコードに当たってしまう。このチェックが種別の取り違えも防いでいる。
    if (decoded.size() != 1 || !sqids.encode(decoded).equals(publicId)) {
      throw new PublicIdInvalidException(type);
    }

    long value = decoded.get(0);
    if (value <= 0 || value > Integer.MAX_VALUE) {
      throw new PublicIdInvalidException(type);
    }
    return (int) value;
  }

  /**
   * 設定ミスを起動時に検知します。環境変数で管理するため、静かに壊れるのを防ぎます。
   *
   * <p>ただし「別の正しい形式の値へ差し替わった」事故はここでは検知できません。
   * その場合は既存の公開URLが全て無効になるため、設定変更は運用ルールで抑止します。
   */
  private static void validateAlphabet(PublicIdType type, String alphabet) {
    if (alphabet == null || alphabet.length() != ALPHABET_LENGTH) {
      throw new IllegalStateException(
          "公開IDのアルファベットは" + ALPHABET_LENGTH + "文字である必要があります: " + type);
    }
    Set<Integer> distinct = new HashSet<>();
    alphabet.chars().forEach(distinct::add);
    if (distinct.size() != ALPHABET_LENGTH) {
      throw new IllegalStateException(
          "公開IDのアルファベットに重複した文字があります: " + type);
    }
  }
}
