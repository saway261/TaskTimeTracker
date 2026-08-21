INSERT INTO app_users(
  email, password_hash, is_enabled, password_change_required,
  temporary_password_expires_at, created_at, updated_at, email_verified_at
) VALUES (
  'user-a@example.com',
  '{bcrypt}$2a$12$eXVmZkrzhJ4W18gvvloeBOtdTxZafS3hoF0JPQgGeoaJn4100Ss7u',
  TRUE, FALSE, NULL, '2026-08-13 00:00:00', '2026-08-13 00:00:00', '2026-08-13 00:00:00'
);
INSERT INTO app_users(
  email, password_hash, is_enabled, password_change_required,
  temporary_password_expires_at, created_at, updated_at, email_verified_at
) VALUES (
  'user-b@example.com',
  '{bcrypt}$2a$12$CcqglFlqf6yLLjmvKVE05uZ7XiphBYiNIw7eQ3Nr.H2ne0ehZSS0W',
  TRUE, FALSE, NULL, '2026-08-13 00:00:00', '2026-08-13 00:00:00', '2026-08-13 00:00:00'
);

INSERT INTO projects(user_id, title, description, is_finished) VALUES (1, 'タスク管理アプリ開発','A社から受託した開発', false);
INSERT INTO projects(user_id, title, description, is_finished) VALUES (1, 'Java Silver勉強',null, false);
INSERT INTO projects(user_id, title, description, is_finished) VALUES (2, 'プロジェクトX','社外秘', true);

INSERT INTO task_groups(project_id, title, description, is_finished) VALUES (1,'バックエンド開発',null,true);
INSERT INTO task_groups(project_id, title, description, is_finished) VALUES (1,'フロントエンド開発','Reactで',false);
INSERT INTO task_groups(project_id, title, description, is_finished) VALUES (2,'参考書','テキスト通読',true);
INSERT INTO task_groups(project_id, title, description, is_finished) VALUES (3,'振り返りグループA',null,false);
INSERT INTO task_groups(project_id, title, description, is_finished) VALUES (3,'振り返りグループB',null,false);

INSERT INTO tasks (task_group_id, title, description, estimated_minutes, created_at)
VALUES (1, 'カスタム例外作成', 'カスタム例外クラスの作成とハンドリングを行う', 60, NOW());--id=1
INSERT INTO tasks (task_group_id, title, description, estimated_minutes, created_at)
VALUES (1, 'バリデーション実装', 'バリデーションの実装とバリデーション違反のハンドリングを行う', 120, NOW());-- id=2
INSERT INTO tasks (task_group_id, title, description, estimated_minutes, created_at, finished_at, actual_minutes_cached, gap_minutes_cached, gap_rate_cached)
VALUES (2, 'コンポーネント作成', '共通UIコンポーネントを作成する', 90, NOW(), NOW(), 100, 10, 11.1111111111);-- id=3
INSERT INTO tasks (project_id, title, description, estimated_minutes, created_at)
VALUES (1, '画面設計', 'ワイヤーフレームを作成する', 180, NOW());-- id=4
INSERT INTO tasks (task_group_id, title, description, estimated_minutes, created_at)
VALUES (3, '章末問題', 'Java Silverの章末問題を解く', 150, NOW());-- id=5
INSERT INTO tasks (project_id, title, description, estimated_minutes, created_at, finished_at, actual_minutes_cached, gap_minutes_cached, gap_rate_cached)
VALUES (3, '振り返り直下A', 'Reflectionありの完了タスク', 60, NOW(), '2026-08-10 10:00:00', 90, 30, 50.0);-- id=6
INSERT INTO tasks (project_id, title, description, estimated_minutes, created_at, finished_at, actual_minutes_cached, gap_minutes_cached, gap_rate_cached)
VALUES (3, '振り返り直下B', 'Reflectionなしの完了タスク', 120, NOW(), '2026-08-11 11:00:00', 100, -20, -16.6666666667);-- id=7
INSERT INTO tasks (project_id, title, description, estimated_minutes, created_at)
VALUES (3, '振り返り直下未完了', '一覧対象外の未完了タスク', 30, NOW());-- id=8
INSERT INTO tasks (task_group_id, title, description, estimated_minutes, created_at, finished_at, actual_minutes_cached, gap_minutes_cached, gap_rate_cached)
VALUES (4, '振り返り配下A', 'Reflectionありの完了タスク', 45, NOW(), '2026-08-12 12:00:00', 60, 15, 33.3333333333);-- id=9
INSERT INTO tasks (task_group_id, title, description, estimated_minutes, created_at, finished_at, actual_minutes_cached, gap_minutes_cached, gap_rate_cached)
VALUES (4, '振り返り配下B', 'Reflectionなしの完了タスク', 90, NOW(), '2026-08-12 13:00:00', 90, 0, 0.0);-- id=10
INSERT INTO tasks (task_group_id, title, description, estimated_minutes, created_at)
VALUES (4, '振り返り配下未完了', '一覧対象外の未完了タスク', 30, NOW());-- id=11
INSERT INTO tasks (task_group_id, title, description, estimated_minutes, created_at, finished_at, actual_minutes_cached, gap_minutes_cached, gap_rate_cached)
VALUES (5, '振り返り別配下', 'グループ順確認用の完了タスク', 30, NOW(), '2026-08-12 14:00:00', 25, -5, -16.6666666667);-- id=12

INSERT INTO work_sessions (task_id, started_at, type, created_at, updated_at)
VALUES (1,'2026-01-01 09:00:00', 'TIMER', '2026-01-01 09:00:00', '2026-01-01 09:00:00')
;-- task_id=1のduration_secondsはnull
INSERT INTO work_sessions (task_id, duration_seconds, started_at, ended_at, type, created_at, updated_at)
VALUES (4, 1800, '2026-01-02 09:00:00', '2026-01-02 09:30:00', 'TIMER', '2026-01-02 09:00:00', '2026-01-02 09:30:00');
INSERT INTO work_sessions (task_id, duration_seconds, started_at, ended_at, type, created_at, updated_at)
VALUES (4, 2700, '2026-01-02 10:00:00', '2026-01-02 10:45:00', 'TIMER', '2026-01-02 10:00:00', '2026-01-02 10:45:00');
--id =2のワークセッションは存在しない

INSERT INTO memos (project_id, comment)
VALUES (1, 'プロジェクト方針を確認する');-- id=1
INSERT INTO memos (task_group_id, comment)
VALUES (1, 'バックエンド優先で進める');-- id=2
INSERT INTO memos (task_id, comment)
VALUES (1, '例外メッセージを見直す');-- id=3

-- 原因カテゴリマスタ（本番と同じ定義。docs/sql/001_reflection_cause_categories.sql 参照）
INSERT INTO reflection_cause_categories (code, label, direction, next_action_hint, requires_cause, display_order, is_active) VALUES
  ('TASK_BREAKDOWN', '作業の洗い出しが足りなかった', 'OVER', '着手前に手順を書き出す', FALSE, 10, TRUE),
  ('UNEXPECTED_PROBLEM', '想定外の問題・エラーに対応した', 'OVER', '調査時間をバッファとして見込む', FALSE, 20, TRUE),
  ('KNOWLEDGE_GAP', '知識・技術が足りず調べながら進めた', 'OVER', '事前調査を別タスクに切り出す', FALSE, 30, TRUE),
  ('UNCLEAR_GOAL', 'ゴール・完了条件が曖昧だった', 'OVER', '着手前に完了条件を書き出す', FALSE, 40, TRUE),
  ('REWORK', '手戻り・やり直しが発生した', 'OVER', '早い段階で方針を確認する', FALSE, 50, TRUE),
  ('SCOPE_CREEP', '予定外の作業を追加した', 'OVER', 'タスクの範囲を決めて守る', FALSE, 60, TRUE),
  ('INTERRUPTION', '中断・割り込みが入った', 'OVER', '作業する時間帯や環境を見直す', FALSE, 70, TRUE),
  ('FATIGUE', '疲れ・体調不良で本来の速度が出なかった', 'OVER', '体調と時間帯を考慮して着手日を決める', FALSE, 80, TRUE),
  ('ESTIMATE_TOO_SHORT', '見積もりが根拠のない勘で、短すぎた', 'OVER', '過去の類似タスクの実績を参照する', FALSE, 90, TRUE),
  ('BUFFER_TOO_LARGE', '不安から見積もりに余裕を持たせすぎた', 'UNDER', 'バッファを見積もり本体と分けて置く', FALSE, 110, TRUE),
  ('WORK_UNNECESSARY', '必要だと思っていた作業が不要だった', 'UNDER', '着手前に本当に必要な作業か確かめる', FALSE, 120, TRUE),
  ('REUSE', '既存の資産や過去の成果を再利用できた', 'UNDER', '見積もり時に再利用できるものを洗い出す', FALSE, 130, TRUE),
  ('SKILL_ABOVE_EXPECTATION', '想像していたより自分が習熟していた', 'UNDER', '得意な領域の見積もりを下げる', FALSE, 140, TRUE),
  ('GOOD_CONDITION', '集中できた・体調や環境が良かった', 'UNDER', 'その条件が何だったかを記録して再現する', FALSE, 150, TRUE),
  ('QUALITY_TRADEOFF', '品質や範囲を落として早く終えた', 'UNDER', '完了条件を満たしているか見直す', FALSE, 160, TRUE),
  ('TASK_TOO_SMALL', 'タスクが想定より小さかった／一部が既に終わっていた', 'UNDER', '着手時点の状態をタスクに反映する', FALSE, 170, TRUE),
  ('AS_PLANNED', 'おおむね見積もりどおりに進んだ', 'BOTH', '何が良かったかを記録して再現する', FALSE, 210, TRUE),
  ('OTHER', 'その他', 'BOTH', NULL, TRUE, 220, TRUE);
-- テスト専用: is_active=FALSEの除外・取得不可を検証するためのフィクスチャ（本番マスタには含めない）
INSERT INTO reflection_cause_categories (code, label, direction, next_action_hint, requires_cause, display_order, is_active) VALUES
  ('TEST_INACTIVE', 'テスト用無効カテゴリ', 'BOTH', NULL, FALSE, 900, FALSE);

INSERT INTO reflections (task_id, cause, next_action, created_at, updated_at)
VALUES (6, '着手前の調査が不足していた', '類似タスクの実績を見積もり前に確認する', '2026-08-10 10:05:00', '2026-08-10 10:05:00');-- id=1
INSERT INTO reflections (task_id, cause, next_action, created_at, updated_at)
VALUES (9, 'レビュー観点に漏れがあった', NULL, '2026-08-12 12:05:00', '2026-08-12 12:05:00');-- id=2

INSERT INTO project_item_orders (project_id, task_id, task_group_id, position)
VALUES (3, 7, NULL, 0);
INSERT INTO project_item_orders (project_id, task_id, task_group_id, position)
VALUES (3, NULL, 5, 1);
INSERT INTO project_item_orders (project_id, task_id, task_group_id, position)
VALUES (3, 8, NULL, 2);
INSERT INTO project_item_orders (project_id, task_id, task_group_id, position)
VALUES (3, NULL, 4, 3);
INSERT INTO project_item_orders (project_id, task_id, task_group_id, position)
VALUES (3, 6, NULL, 4);

INSERT INTO task_group_item_orders (task_group_id, task_id, position)
VALUES (4, 10, 0);
INSERT INTO task_group_item_orders (task_group_id, task_id, position)
VALUES (4, 11, 1);
INSERT INTO task_group_item_orders (task_group_id, task_id, position)
VALUES (4, 9, 2);
INSERT INTO task_group_item_orders (task_group_id, task_id, position)
VALUES (5, 12, 0);
