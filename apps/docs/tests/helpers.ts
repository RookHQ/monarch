import { existsSync, readdirSync, readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import { expect, type Page } from '@playwright/test';

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
      // The docs' own marker: components use `data-pending` for their saving state too.
      const next = document.querySelector('.docs-example__preview[data-pending]');
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
  // WebKit, on the heaviest example pages: the two preloaded Poppins files are
  // fetched at once, but the first paint with them can land after WebKit's
  // few-second preload heuristic. The fonts do load; this is only the warning.
  /assets\/poppins-(400|600)-latin-[\w-]+\.woff2 was preloaded using link preload but not used/,
  // WebKit, on pages whose example mounts slowly under load (PreJoinLobby asks for
  // devices): a page chunk is used after the same few-second heuristic. It is
  // used; a chunk that is never used fails the docs budget's preload check.
  /assets\/[\w.-]+\.js was preloaded using link preload but not used within a few seconds/,
  // EmbedBlock's video frames: Chromium supports both and the players use them
  // (picture in picture, casting), Firefox and WebKit report the token they skip.
  /Feature Policy: Skipping unsupported feature name “picture-in-picture”/,
  // ChatDock's frame asks for calls, autoplay and the clipboard (DEFAULT_CHAT_ALLOW): Chromium
  // supports each, Firefox reports the tokens it does not know and applies the rest.
  /Feature Policy: Skipping unsupported feature name “(autoplay|clipboard-read|clipboard-write)”/,
  // Firefox, on pages whose first example is a staged popover or menu (overlay stage): Radix
  // positions popper content from scroll events, and the stage pins it in the flow with
  // !important styles, so nothing moves; Firefox still reports the scroll-driven style write.
  /This site appears to use a scroll-linked positioning effect/,
  /'sandbox' attribute: 'allow-presentation' is an invalid sandbox flag/,
  // AiArtifactCanvas's HTML preview is a `srcdoc` frame with an empty `sandbox`,
  // so no script runs in it: Chromium reports the example's own <script> (which
  // shows exactly that) and the trace recorder's snapshot script as blocked.
  /Blocked script execution in 'about:srcdoc' because the document's frame is sandboxed and the 'allow-scripts' permission is not set/,
  // The same frame has an opaque origin, so each engine reports axe's message to
  // it (axe messages every frame for its scan) as refused. The page posts nothing.
  /postMessage.*DOMWindow.*does not match the recipient window.s origin \(.null.\)/,
  /Unable to post message to .*\. Recipient has origin null\./,
];

/**
 * Third-party frames (EmbedBlock's Vimeo example) load an empty page instead.
 * The scan is of the docs' own markup; a live player's console output (or a
 * bot check against a headless browser, which Firefox reports on the page's
 * console) is neither the page's to fix nor something a test should depend on.
 */
export async function stubThirdPartyFrames(page: Page): Promise<void> {
  await page.route(
    (url) => url.protocol === 'https:',
    (route) => {
      const request = route.request();
      if (request.resourceType() === 'document' && request.frame() !== page.mainFrame()) {
        return route.fulfill({ status: 200, contentType: 'text/html', body: '<!doctype html>' });
      }
      return route.fallback();
    },
  );
}

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
    // axe is injected into the page only, so it cannot scan inside frames (the
    // frames themselves, e.g. their titles, are still checked). `iframes: false`
    // stops it posting to cross-origin frames that cannot answer, which every
    // engine logs as a target-origin mismatch.
    const r = await axe.run(context, { resultTypes: ['violations'], iframes: false });
    return r.violations.map((v) => ({
      id: v.id,
      impact: v.impact,
      help: v.help,
      nodes: v.nodes.slice(0, 5).map((n) => {
        // Contrast failures carry the measured colours: put them in the target so
        // a CI log shows what the engine saw.
        const d = (n as { any?: { data?: Record<string, unknown> }[] }).any?.[0]?.data;
        let extra: string[] = [];
        if (d && 'fgColor' in d) {
          const el = document.querySelector(n.target.join(' '));
          const box = el?.closest('button, [role], .mn-input') ?? el?.parentElement;
          const cs = box ? getComputedStyle(box) : null;
          extra = [
            `fg ${d.fgColor} on ${d.bgColor} = ${d.contrastRatio}`,
            `forced-colors ${matchMedia('(forced-colors: active)').matches}, scheme ${matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'}, box ${box?.tagName} bg ${cs?.backgroundColor} color ${cs?.color} appearance ${cs?.getPropertyValue('appearance')}`,
          ];
        }
        return { target: [...n.target, ...extra], failureSummary: n.failureSummary };
      }),
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
/**
 * Waits until a component page has hydrated. The prerendered HTML already has
 * the headline example's controls, and a click or key press on them before
 * hydration does nothing, which made interaction tests fail under load. The
 * platform tabs go live only after hydration (pages/components/native-slot.tsx),
 * so they are the signal.
 */
export async function componentPageHydrated(page: Page): Promise<void> {
  await expect(page.locator('[role="tablist"][data-live]')).toBeAttached({ timeout: 15_000 });
}

export async function settle(page: Page): Promise<void> {
  await page.waitForFunction(() =>
    document
      .getAnimations()
      .every(
        (a) => a.playState !== 'running' || a.effect?.getComputedTiming().iterations === Infinity,
      ),
  );
}

/**
 * `/native/<id>` for every native guide whose Markdown file exists: a guide
 * that is not written yet has no page (apps/docs/plugins/native.ts).
 */
export function nativeGuideRoutes(): string[] {
  const guides: Record<string, string> = {
    'adaptive-layout': 'docs/guides/native-adaptive-layout.md',
    accessibility: 'docs/guides/native-accessibility-and-testing.md',
    compatibility: 'docs/architecture/NATIVE-COMPATIBILITY.md',
    contributing: 'docs/guides/native-contributing.md',
  };
  return Object.entries(guides)
    .filter(([, file]) => existsSync(fileURLToPath(new URL(`../../../${file}`, import.meta.url))))
    .map(([id]) => `/native/${id}`);
}
