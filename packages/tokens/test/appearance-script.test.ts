// @vitest-environment jsdom
import { describe, expect, it, beforeEach } from 'vitest';
import { appearanceScript } from '../src/appearance.js';

function run(cookies: Record<string, string>, media: Record<string, boolean> = {}, opts = {}) {
  const html = document.documentElement;
  for (const a of ['data-mn-theme', 'data-mn-contrast', 'data-mn-product']) html.removeAttribute(a);
  html.classList.remove('dark');
  Object.defineProperty(document, 'cookie', {
    configurable: true,
    get: () =>
      Object.entries(cookies)
        .map(([k, v]) => `${k}=${v}`)
        .join('; '),
  });
  (window as unknown as { matchMedia: (q: string) => { matches: boolean } }).matchMedia = (
    q: string,
  ) => ({ matches: media[q] ?? false });
  new Function(appearanceScript(opts))();
  return {
    theme: html.getAttribute('data-mn-theme'),
    contrast: html.getAttribute('data-mn-contrast'),
    dark: html.classList.contains('dark'),
    product: html.getAttribute('data-mn-product'),
  };
}

describe('appearanceScript in a DOM', () => {
  beforeEach(() => {
    document.documentElement.removeAttribute('data-rook-theme-routes');
    window.history.replaceState(null, '', '/');
  });
  it('keeps public routes light under the Rook route scope', () => {
    document.documentElement.setAttribute('data-rook-theme-routes', '^/(w|home)(/|$)');
    window.history.replaceState(null, '', '/solutions/tables');
    expect(run({ rook_theme: 'dark' })).toMatchObject({ theme: 'light', dark: false });
  });
  it('applies dark on routes inside the Rook route scope', () => {
    document.documentElement.setAttribute('data-rook-theme-routes', '^/(w|home)(/|$)');
    window.history.replaceState(null, '', '/w/acme/documents');
    expect(run({ rook_theme: 'dark' })).toMatchObject({ theme: 'dark', dark: true });
  });
  it('defaults to light / standard', () => {
    expect(run({})).toEqual({ theme: 'light', contrast: 'standard', dark: false, product: null });
  });
  it('reads rook_theme=dark', () => {
    expect(run({ rook_theme: 'dark', other: 'x' })).toMatchObject({ theme: 'dark', dark: true });
  });
  it('resolves system from media queries', () => {
    expect(
      run(
        { rook_theme: 'system', mn_contrast: 'system' },
        { '(prefers-color-scheme: dark)': true, '(prefers-contrast: more)': true },
      ),
    ).toMatchObject({ theme: 'dark', contrast: 'more' });
  });
  it('rejects garbage cookie values', () => {
    expect(run({ rook_theme: 'blue', mn_contrast: 'lots' })).toMatchObject({
      theme: 'light',
      contrast: 'standard',
    });
  });
  it('sets product and honours defaultTheme', () => {
    expect(run({}, {}, { product: 'tickets', defaultTheme: 'dark' })).toMatchObject({
      product: 'tickets',
      theme: 'dark',
    });
  });
  it('does not match a cookie by suffix', () => {
    expect(run({ xrook_theme: 'dark' })).toMatchObject({ theme: 'light' });
  });
  it('handles url-encoded values', () => {
    expect(run({ rook_theme: encodeURIComponent('dark') })).toMatchObject({ theme: 'dark' });
  });
});
