import { readdirSync, readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import type { Page } from '@playwright/test';

const require = createRequire(import.meta.url);
export const AXE_SOURCE = readFileSync(require.resolve('axe-core/axe.min.js'), 'utf8');

export type Theme = 'light' | 'dark' | 'hc-light' | 'hc-dark';

/** The site's base path (DOCS_BASE at build time): `/` locally, `/monarch/` in CI. */
const BASE = (() => {
  const b = process.env.DOCS_BASE ?? '/';
  return b.endsWith('/') ? b : `${b}/`;
})();

/**
 * The address of a route: `/components/button` → `<base>components/button/`.
 * The screenshot routes (`/__gallery`, `/__marketing`) live in the fragment.
 */
export function pageUrl(route: string, anchor?: string): string {
  if (route.startsWith('/__')) return `${BASE}#${route}`;
  const url = route === '/' ? BASE : `${BASE}${route.slice(1)}/`;
  return anchor ? `${url}#${anchor}` : url;
}

/**
 * Live previews mount when they near the viewport. Scrolls each one into view,
 * so every example is on the page before it is scanned, then back to the top.
 */
export async function mountPreviews(page: Page): Promise<void> {
  // Brings the next unmounted preview into view until none is left.
  await page.waitForFunction(
    () => {
      const next = document.querySelector('[data-pending]');
      next?.scrollIntoView({ block: 'center' });
      return next === null;
    },
    undefined,
    { polling: 'raf' },
  );
  await page.evaluate(() => window.scrollTo(0, 0));
}

/**
 * Console messages that are expected: the EmbeddedAppFrame and ChatDock
 * examples deliberately sandbox a cross-origin frame with `allow-scripts` and
 * `allow-same-origin` (documented in their threat model), which browsers warn
 * about. Everything else on the console fails the test.
 */
const EXPECTED_CONSOLE = [
  /allow-scripts and allow-same-origin/,
  // Firefox, when the test itself (axe, scrolling) forces layout while the
  // EmbeddedAppFrame example's blob: frame document is still loading.
  /Layout was forced before the page was fully loaded.*\{file: "(blob:|debugger eval code)/,
];

/** Collects console errors and warnings (and uncaught errors) from now on. */
export function watchConsole(page: Page): () => string[] {
  const messages: string[] = [];
  page.on('console', (m) => {
    if (m.type() !== 'error' && m.type() !== 'warning') return;
    const text = m.text();
    if (!EXPECTED_CONSOLE.some((re) => re.test(text))) messages.push(`${m.type()}: ${text}`);
  });
  page.on('pageerror', (e) => messages.push(`uncaught: ${e.message}`));
  return () => messages;
}

/** Sets the same cookies Rook apps use, so the pre-paint script applies the theme. */
/** `product`: an id applies that accent, `null` clears it, `undefined` leaves storage alone. */
export async function setAppearance(
  page: Page,
  theme: Theme,
  product?: string | null,
): Promise<void> {
  const dark = theme.endsWith('dark');
  const more = theme.startsWith('hc');
  await page.context().addCookies([
    { name: 'rook_theme', value: dark ? 'dark' : 'light', domain: '127.0.0.1', path: '/' },
    { name: 'mn_contrast', value: more ? 'more' : 'standard', domain: '127.0.0.1', path: '/' },
  ]);
  if (product === undefined) return;
  await page.addInitScript((p: string | null) => {
    try {
      if (p) localStorage.setItem('mn_docs_product', p);
      else localStorage.removeItem('mn_docs_product');
    } catch {
      // ignore
    }
  }, product);
}

export async function waitForFonts(page: Page): Promise<void> {
  await page.evaluate(() => document.fonts.ready);
}

export interface AxeViolation {
  id: string;
  impact: string | null;
  help: string;
  nodes: { target: string[]; failureSummary?: string }[];
}

/**
 * `exclude`: selectors left out of the scan. The tokens page uses it for swatch
 * sample text, which deliberately shows every token value including the
 * sub-AA ones (disabled text); the ratio is printed in the caption instead.
 */
export async function runAxe(page: Page, exclude: string[] = []): Promise<AxeViolation[]> {
  await page.addScriptTag({ content: AXE_SOURCE });
  return page.evaluate(async (excluded: string[]) => {
    const axe = (
      window as unknown as {
        axe: { run: (ctx: object, opts: object) => Promise<{ violations: AxeViolation[] }> };
      }
    ).axe;
    const context = excluded.length
      ? { include: [['html']], exclude: excluded.map((s) => [s]) }
      : document;
    const r = await axe.run(context, { resultTypes: ['violations'] });
    return r.violations.map((v) => ({
      id: v.id,
      impact: v.impact,
      help: v.help,
      nodes: v.nodes
        .slice(0, 5)
        .map((n) => ({ target: n.target, failureSummary: n.failureSummary })),
    }));
  }, exclude);
}

/**
 * `/components/<id>` for every component folder's docs file
 * (packages/react/src/{components,patterns}/<family>/<id>/<id>.docs.tsx).
 */
export function componentRoutes(): string[] {
  const src = fileURLToPath(new URL('../../../packages/react/src', import.meta.url));
  return readdirSync(src, { recursive: true, encoding: 'utf8' })
    .map((f) => /(?:^|\/)([a-z0-9-]+)\.docs\.tsx$/.exec(f.replaceAll('\\', '/'))?.[1])
    .filter((id): id is string => Boolean(id))
    .sort()
    .map((id) => `/components/${id}`);
}

/**
 * Waits for entrance animations to finish (the home hero rises in), so contrast
 * is measured on the final colours. Infinite decorative loops are ignored.
 */
export async function settle(page: Page): Promise<void> {
  await page.waitForFunction(() =>
    document
      .getAnimations()
      .every(
        (a) => a.playState !== 'running' || a.effect?.getComputedTiming().iterations === Infinity,
      ),
  );
}
