export const THEME_STORAGE_KEY = 'ttt-theme'

export type Theme = 'light' | 'dark'

/**
 * 決定順: localStorageの明示選択 → prefers-color-scheme → light。
 * 実際の初期適用は index.html のインラインスクリプトが Vue マウント前に行う（§7.1）。
 * この関数は同じロジックで現在値を読み直す用途（ThemeToggle の初期表示など）に使う。
 */
export function getInitialTheme(): Theme {
  const stored = localStorage.getItem(THEME_STORAGE_KEY)
  if (stored === 'dark' || stored === 'light') {
    return stored
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

/** 現在DOMに適用されているテーマを返す。 */
export function getTheme(): Theme {
  return document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light'
}

/** テーマを切り替え、localStorageへ明示選択として保存する。 */
export function setTheme(theme: Theme): void {
  document.documentElement.dataset.theme = theme
  localStorage.setItem(THEME_STORAGE_KEY, theme)
}
