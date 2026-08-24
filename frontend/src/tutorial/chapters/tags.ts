import type { TutorialChapter } from '@/tutorial/types'

// 要件 §9.5 の内容を、§9.8.2 の執筆規約(指示語を使わず単体で意味が通る文にする)へ
// 適合させて書き起こしたもの。「伝えること」列の骨子をそのまま転記していない。
//
// ステップ6(assign)のアンカーは .tag-add-button で、タスク詳細画面(TaskDetailView.vue)
// 側にあり .tag-management-view の配下にない。タグ管理画面からのスコープ再生では
// 自動的に対象外になるが、これは意図どおりの挙動(実装計画 §フェーズF5)。
// タグの付与そのものの説明は「タスク管理」章のステップ13(tag)が既に担っている。
//
// 「見積もりを立てるその瞬間にタグ別の実績を参照できる」という説明は書かない。
// タスク作成フォームに過去実績を表示する機能は実装されていないため(要件 §9.5)。
export const tagsChapter: TutorialChapter = {
  id: 'tags',
  title: 'タグ管理',
  summary: 'タグの役割、プリセットの扱い、上限、タグの付け方とアーカイブを確認できます。',
  entryRoute: '/tags',
  replayable: true,
  steps: [
    {
      id: 'purpose',
      title: 'タグの役割',
      body: 'タスクの中には「計画」や「調査」など同じ性質のものがプロジェクトを横断して現れると思います。それらの特定の性質に限定した見積もりのずれの傾向を分析するために、タスクにタグを付与することができます。',
      targets: ['.tag-management-view h1'],
    },
    {
      id: 'cross-project',
      title: 'プロジェクトをまたいで使う',
      body: 'タグはプロジェクトに属さず、ユーザーに属します。複数のプロジェクトに同じタグが現れて初めて、傾向を比較する価値が生まれます。',
      targets: ['.page-header'],
    },
    {
      id: 'rename',
      title: '好きな名前に書き換えられる',
      body: '「名前を変更」から、タグの名前を変更できます。初期タグも全く別の名前に書き換えてOKです。ただし、すでにタスクに付与済みの場合は、それらのタスク間で一貫性を保てる名前が良いでしょう。',
      targets: ['.tag-row .row-actions'],
    },
    {
      id: 'create',
      title: '新しいタグを作る',
      body: '新しいタグを作成できます。',
      targets: ['.create-section'],
    },
    {
      id: 'limit',
      title: '有効なタグの上限は50件',
      body: '有効なタグの上限は50件です。増やしすぎると1タグあたりの件数が減り、傾向が読みにくくなります。',
      targets: ['.active-count'],
    },
    {
      id: 'assign',
      title: 'タグを付ける',
      body: 'タグはタスク詳細画面から付けます。1つのタスクに複数のタグを付けられます。',
      targets: ['.tag-add-button'],
    },
    {
      id: 'archive',
      title: '使わなくなったらアーカイブする',
      body: '使わなくなったタグはアーカイブします。削除ではないため、アーカイブを解除したら再び分析機能で絞り込みの対象に選ぶことができます',
      targets: ['.archive-toggle'],
    },
  ],
}
