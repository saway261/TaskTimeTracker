// チュートリアルの再生範囲を表す画面ルートのCSSセレクタ。
// ヘルプボタンの scope prop と、ステップの scopes 指定の両方でこの定数を使う。
// 文字列リテラルを直接書くと、両者の食い違いに気づけないため。
export const TUTORIAL_SCOPES = {
  projectList: '.project-list-view',
  projectDetail: '.project-detail-view',
  taskGroupDetail: '.task-group-detail-view',
  taskDetail: '.task-detail-view',
  // タスク操作モーダル（一覧の行を選ぶと開く）
  taskQuickActions: '.task-quick-actions',
  // タスク作成モーダル。編集モーダルも同じ .task-form を持つが、見積時間欄とタグ欄は
  // 作成時のみ描画されるため、編集時はそれらのステップが自動的に対象外になる。
  taskForm: '.task-form',
  reflectionList: '.reflection-view',
  reflectionDetail: '.reflection-detail-view',
  // 振り返りモーダル（振り返り詳細画面の行から開く）
  reflectionModal: '.reflection-modal',
  analytics: '.analytics-view',
  tags: '.tag-management-view',
} as const

export type TutorialScope = (typeof TUTORIAL_SCOPES)[keyof typeof TUTORIAL_SCOPES]
