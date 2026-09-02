import { ref } from 'vue'

export type ThemeMode = 'amber' | 'black' | 'white'

export const THEMES: ThemeMode[] = ['amber', 'black', 'white']

const STORAGE_KEY = 'app-theme'
const theme = ref<ThemeMode>('amber')

function apply(t: ThemeMode) {
  theme.value = t
  const root = document.documentElement
  if (t === 'amber') {
    root.removeAttribute('data-theme')
  } else {
    root.setAttribute('data-theme', t)
  }
  try {
    localStorage.setItem(STORAGE_KEY, t)
  } catch {
    // 忽略（隐私模式等）
  }
}

function initTheme() {
  let saved: ThemeMode = 'amber'
  try {
    const v = localStorage.getItem(STORAGE_KEY) as ThemeMode | null
    if (v && (THEMES as string[]).includes(v)) saved = v
  } catch {
    // 忽略
  }
  apply(saved)
}

function setTheme(t: ThemeMode) {
  apply(t)
}

function cycleTheme() {
  apply(THEMES[(THEMES.indexOf(theme.value) + 1) % THEMES.length])
}

export function useTheme() {
  return { theme, initTheme, setTheme, cycleTheme, THEMES }
}
