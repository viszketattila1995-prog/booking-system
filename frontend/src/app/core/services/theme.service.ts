import { Injectable, effect, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'bs_theme';

// A kezdeti témát az index.html-be ágyazott kis boot script már beállította
// (data-theme attribútum a <html>-en) MÉG az Angular betöltése előtt, hogy ne
// villanjon fel a rossz téma egy pillanatra. Ez a service csak átveszi azt az
// állapotot, és onnantól a signal a forrás az igazságra.
@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly theme = signal<Theme>(readInitialTheme());

  constructor() {
    effect(() => {
      const theme = this.theme();
      document.documentElement.setAttribute('data-theme', theme);
      localStorage.setItem(STORAGE_KEY, theme);
    });
  }

  toggle(): void {
    this.theme.set(this.theme() === 'dark' ? 'light' : 'dark');
  }
}

function readInitialTheme(): Theme {
  const attr = document.documentElement.getAttribute('data-theme');
  return attr === 'dark' ? 'dark' : 'light';
}
