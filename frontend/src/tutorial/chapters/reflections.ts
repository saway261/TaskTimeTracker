import type { TutorialChapter } from '@/tutorial/types'

// 要件 §9.3 の内容を、§9.8.2 の執筆規約(指示語を使わず単体で意味が通る文にする)へ
// 適合させて書き起こしたもの。「伝えること」列の骨子をそのまま転記していない。
//
// ステップ5〜7のアンカーは ReflectionModal の内部にある(実装計画 §0-1-1)。
//   - cause-category: `.cause-category-select`(CauseCategorySelect.vue のfieldset)
//   - cause-detail: `.cause-field`(原因の自由記述欄。BaseTextareaを包む既存のラッパー)
//   - next-action: `.next-action-field`(改善アクション欄には専用クラスが無かったため、
//     BaseTextareaを<div class="next-action-field">で包んで新設した)
// gapのアンカーは振り返り詳細画面の行(.reflection-task-row .row-meta)と、
// ReflectionModal内の参考情報(.reference-info)の両方を候補に持つ。どちらの画面から
// 再生してもスコープ内の実物を指せるようにするため(要件 §7.2)。
// reopenはアンカーを持たない(要件 §9.3)。スコープ再生では表示されず、
// 章選択モーダルからの全体再生でのみセンターカードとして表示される。
export const reflectionsChapter: TutorialChapter = {
  id: 'reflections',
  title: '振り返り',
  summary: '完了したタスクの誤差を振り返り、原因と改善アクションを記録する流れを確認できます。',
  entryRoute: '/reflections',
  replayable: true,
  steps: [
    {
      id: 'why',
      title: '振り返りの目的',
      body: '振り返りは次の見積もりを改善するために書きます。誤差の原因と改善アクションを言葉にすることが、次回の精度につながります。',
      targets: ['.reflection-view h1'],
    },
    {
      id: 'project-select',
      title: 'プロジェクトを選ぶ',
      body: '選んだプロジェクトの中で、完了したタスクだけが振り返りの対象になります。',
      targets: ['.project-cards', '.reflection-view .empty'],
    },
    {
      id: 'target',
      title: '振り返りの対象',
      body: '一覧には完了したタスクだけが並びます。行の枠線が破線のものは、振り返りが未入力です。',
      targets: ['.reflection-task-row'],
    },
    {
      id: 'gap',
      title: '誤差はシステムが算出する',
      body: '誤差と誤差率は見積もりと実績から自動で算出されます。算出された数値をもとに、原因と改善アクションを考えましょう。',
      targets: ['.reflection-task-row .row-meta', '.reference-info'],
    },
    {
      id: 'cause-category',
      title: '原因をカテゴリで選ぶ',
      body: '振り返りの原因はカテゴリから選びます。超過のときと短縮のときで標準で表示される選択肢が異なりますが、「全てのカテゴリを表示」で標準外のカテゴリも出すことができます。',
      targets: ['.cause-category-select'],
    },
    {
      id: 'cause-detail',
      title: '原因自由記述',
      body: '原因カテゴリで「その他」を選んだときは、自由記述欄に具体的な内容を入力してください。何か関連した気づきがあれば、書き残しておくとよいでしょう。',
      targets: ['.cause-field'],
    },
    {
      id: 'next-action',
      title: '改善アクションを書く',
      body: '改善アクションには「次回どう見積もるか」を具体的に書きます。原因を選ぶだけでなく、どう変えるかまで言葉にすることで、見積もりの精度が上がります。',
      targets: ['.next-action-field'],
    },
    {
      id: 'reopen',
      title: '作業中に戻すと振り返りは破棄される',
      body: '完了したタスクを作業中の状態に戻すと、入力していた振り返りは破棄されます。元に戻せないため、戻す前に内容を書き留めておくと安心です。',
    },
  ],
}
