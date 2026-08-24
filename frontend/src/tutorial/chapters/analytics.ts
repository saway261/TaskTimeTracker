import type { TutorialChapter } from '@/tutorial/types'

// 要件 §9.4 の内容を、§9.8.2 の執筆規約(指示語を使わず単体で意味が通る文にする)へ
// 適合させて書き起こしたもの。「伝えること」列の骨子をそのまま転記していない。
//
// 全ステップのアンカーが .analytics-view の配下にあるため、他章と違いスコープの
// 問題は起きない(実装計画 §フェーズF5)。
// ステップ4・5はいずれも .accuracy-summary を指す。同じアンカーのままステップだけが
// 進む(スポットライトは動かない)。偏り(係数で直る)とばらつき(係数では直らない)を
// 分けて説明するための意図した構成であり、不具合ではない。
export const analyticsChapter: TutorialChapter = {
  id: 'analytics',
  title: '分析',
  summary: '見積もり精度の代表係数・ばらつき・診断・グラフを、どう読むかを確認できます。',
  entryRoute: '/analytics',
  replayable: true,
  steps: [
    {
      id: 'prerequisite',
      title: '分析はデータが貯まってから',
      body: '分析は完了したタスクが貯まってから意味を持ちます。データが少ないうちは、代表係数もばらつきも参考程度になります。焦らず記録を続けてください。',
      targets: ['.analytics-view h1', '.empty-state'],
    },
    {
      id: 'filter',
      title: '対象を絞り込む',
      body: 'プロジェクト・期間・タグで分析の対象を絞り込めます。特定のプロジェクトや特定の性質のタスク、直近の傾向だけを見たいときに便利です。',
      targets: ['.analytics-filter-bar'],
    },
    {
      id: 'excluded',
      title: '除外されるタスクがある',
      body: '実績が欠けたタスクは分析から除外されます。除外件数と理由は、絞り込みバーの数値欄で確認できます。',
      targets: ['.analytics-filter-bar .counts'],
    },
    {
      id: 'summary',
      title: '代表係数とばらつき',
      body: '代表係数は、見積もりを何倍すれば実績に近づくかを表します。あわせてばらつきの大きさも確認できます。',
      targets: ['.accuracy-summary'],
    },
    {
      id: 'bias-vs-variance',
      title: '偏りとばらつきは対処が違う',
      body: '偏りが大きくばらつきが小さい場合は、代表係数を掛ければ精度が上がります。ばらつきが大きい場合は係数では直らず、着手前の分解が足りていません。',
      targets: ['.accuracy-summary'],
    },
    {
      id: 'diagnosis',
      title: '診断を読む',
      body: '診断カードには、現在の状態が文章で要約されています。詳しいグラフを見る前に、まず診断カードの文章を読むと状況をつかめます。',
      targets: ['.diagnosis-card'],
    },
    {
      id: 'charts',
      title: 'グラフで掘り下げる',
      body: '推移・散布図・サイズ帯別・原因別のグラフで、見積もり精度がどこで崩れているかを掘り下げられます。',
      targets: ['.analytics-charts', '.chart-card'],
    },
  ],
}
