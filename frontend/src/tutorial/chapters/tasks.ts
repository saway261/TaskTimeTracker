import { TUTORIAL_SCOPES } from '@/tutorial/scopes'
import type { TutorialChapter } from '@/tutorial/types'

// 要件 §9.2 の内容を、§9.8.2 の執筆規約(指示語を使わず単体で意味が通る文にする)へ
// 適合させて書き起こしたもの。「伝えること」列の骨子をそのまま転記していない。
//
// アンカーは実装計画 §フェーズF4 での検証にもとづき、要件書の記載から2箇所補正している。
//   - row-menu/finish: `.task-row .menu-button` は実際のDOMでは存在しない
//     (menu-buttonはtask-rowの兄弟要素であり子要素ではない)。`.task-row-wrapper .menu-button` に補正。
//   - create-project/reorder: `.header-actions`/`.drag-handle` は他画面・他コンポーネント
//     (AppHeader、TaskGroupListItem)にも同名クラスがあり、絞り込まないと誤ったアンカーへ
//     一致しうる。それぞれ `.project-list-view` / `.task-row-wrapper` で絞り込んだ。
//   - finish: `.finished-checkbox` はProject/TaskGroup/Taskの3箇所で使われる共通コンポーネントの
//     クラスのため、`.task-detail-view` で絞り込んだ。
//
// 各ステップの targets には、タスク詳細ページ側のクラスに加えて
// TaskQuickActionModal 側の同等要素(`.metrics-section` / `.timer-section` /
// `.manual-record-section` / `.task-state`)も候補として並べている。モーダルから
// ヘルプボタンで再生したときに、モーダル内の実物を指せるようにするため(実装計画 §0-2-19)。
export const tasksChapter: TutorialChapter = {
  id: 'tasks',
  title: 'タスク管理',
  summary:
    'プロジェクト・タスクの作成から、見積もり・作業記録・完了までの一連の操作を確認できます。',
  entryRoute: '/projects',
  replayable: true,
  steps: [
    {
      id: 'structure',
      title: '3つの階層',
      body: 'プロジェクトの下にタスクグループ、タスクグループの下にタスクがあります。タスクグループは任意で、使わなくても構いません。',
      targets: ['.project-list-view h1'],
    },
    {
      id: 'create-project',
      title: 'プロジェクトを作る',
      body: 'プロジェクト一覧の「＋ 新規プロジェクト」から作成します。すべてのタスクは、いずれかのプロジェクトに属します。',
      targets: ['.project-list-view .header-actions'],
    },
    {
      id: 'add-task',
      title: 'タスクを追加する',
      body: 'プロジェクト詳細画面とタスクグループ詳細画面で、タスクを追加できます。',
      targets: ['.task-list-section .section-header'],
    },
    {
      id: 'estimate-first',
      title: '見積時間を入力する',
      body: 'タスクを作るときは見積時間の入力が必須です。入力した見積もりと、実際にかかった時間との差が、あとで見積もり精度として分析されます。',
      targets: ['.task-form .estimate-field'],
    },
    {
      id: 'estimate-lock',
      title: '見積もりは着手すると固定される',
      body: '見積時間は、タスク詳細画面の「編集」ボタンで変更できます(記録モーダルからは不可)。ただし、着手すると変更できなくなります。',
      targets: ['.estimation-section', '.metrics-section'],
    },
    {
      id: 'row-menu',
      title: '縦3点のメニュー',
      body: '一覧の各行にある縦3点のボタンから「タスクの操作」を開けます。完了にする、詳細画面へ移動する、上下へ並べ替える、別のタスクグループやプロジェクト直下へ移動する、の4つができます。',
      targets: ['.task-row-wrapper .menu-button'],
    },
    {
      id: 'quick-actions',
      title: '行を選んでその場で操作する',
      body: 'タスクの行を選ぶと、詳細画面へ移動せずに、タスク記録モーダルでタイマー・手動記録・完了・メモが使えます。',
      targets: ['.task-row'],
    },
    {
      id: 'reorder',
      title: 'ドラッグで並べ替える',
      body: '一覧の行はドラッグでも並べ替えられます（PCのみ）。モバイルでは縦3点のボタンから並べ替えます。',
      targets: ['.task-row-wrapper .drag-handle'],
      onMissing: 'skip',
    },
    {
      id: 'timer-start',
      title: '作業セッションを記録する',
      body: 'タスク詳細画面とタスク記録モーダルの作業セッションから、タイマーを開始・停止できます。開始した時点でサーバーに記録されるため、ブラウザを閉じても実績は失われません。',
      targets: ['.work-section', '.timer-section'],
    },
    {
      id: 'timer-manual',
      title: '計測し忘れた分を手入力する',
      body: 'タイマーを使い忘れた作業時間は、分数を手入力して記録することもできます。',
      targets: ['.work-section', '.manual-record-section'],
    },
    {
      id: 'active-timer',
      title: '稼働中のタイマーを確認する',
      body: 'ヘッダーのタイマーメニューから、稼働中のタイマーを一覧で確認できます。稼働中のタイマー一覧を見れば、作業の止め忘れに気づけます。',
      targets: ['.timer-menu-trigger'],
      // ヘッダーは全画面に常駐するためアンカーはどこでも解決できるが、
      // 作業中のタスクを扱う画面でだけ意味がある説明なので限定する。
      scopes: [TUTORIAL_SCOPES.taskDetail, TUTORIAL_SCOPES.taskQuickActions],
    },
    {
      id: 'memo',
      title: 'メモを残す',
      body: 'タスクにメモを残せます。次にやること、作業中の気づき、判断に迷った点など、内容は問いません。気負って書く必要はなく、不要になれば「このメモを削除する」から消せます。',
      targets: ['.memo-list .add-memo'],
    },
    {
      id: 'tag',
      title: 'タグを付ける',
      body: 'タスクにはタグを付けられます。作成時に付けることも、あとからタスク詳細画面で付け外しすることもできます。1つのタスクに複数のタグを付けられます。タグの使い道は、タグ管理の章で説明します。',
      targets: ['.tag-add-button', '.task-form .tag-select'],
    },
    {
      id: 'finish',
      title: '完了にする',
      body: 'タスクを完了にすると、実績時間と誤差が確定し、振り返りの入力画面が自動で開きます。',
      targets: [
        '.task-detail-view .finished-checkbox',
        '.task-state .finished-checkbox',
        '.task-row-wrapper .menu-button',
      ],
    },
  ],
}
