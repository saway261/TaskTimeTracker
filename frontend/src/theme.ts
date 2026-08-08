export const THEME_STORAGE_KEY = 'ttt-theme'

export type Theme = 'light' | 'dark'

/**
 * 決定順: localStorageの明示選択 → prefers-color-scheme → light。
 * ThemeToggle実装（フェーズ7）はこの関数を呼び直して切り替える想定。
 */
export function applyStoredTheme(): Theme {
  const stored = localStorage.getItem(THEME_STORAGE_KEY)
  const theme: Theme =
    stored === 'dark' || stored === 'light'
      ? stored
      : window.matchMedia('(prefers-color-scheme: dark)').matches
        ? 'dark'
        : 'light'

  document.documentElement.dataset.theme = theme
  return theme
}
