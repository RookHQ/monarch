import { expect, test, type Page } from '@playwright/test';
import {
  componentPageHydrated,
  componentRoutes,
  mountPreviews,
  pageUrl,
  settle,
  stubThirdPartyFrames,
  waitForFonts,
  watchConsole,
} from './helpers';

/**
 * Every component page on a small phone, a phone and a tablet, upright and on its side. For each
 * example:
 *
 * - nothing sticks out of its box sideways (unless a scroller holds it) and the page never
 *   scrolls sideways (WCAG 1.4.10 Reflow);
 * - every control has a tap area of at least 24 by 24 CSS pixels (WCAG 2.5.8 Target Size).
 *   The area is hit-tested, so an invisible `::after`, a wrapping label or a stretched link
 *   counts. Links inside a sentence are exempt, as the criterion says;
 * - the page logs no errors.
 *
 * A control drawn smaller than 24px gets its tap area from the "touch target floor" in
 * packages/react/src/styles/foundation.css; add it there (or grow its own box when it clips).
 */
const DEVICES = [
  // The smallest phones in use (320 CSS px). Also absorbs font differences: Linux and Android
  // fonts run wider than macOS ones, so a layout with no slack at 360px fails here first.
  { name: 'small-phone', width: 320, height: 640 },
  { name: 'phone-portrait', width: 360, height: 740 },
  { name: 'phone-landscape', width: 740, height: 360 },
  { name: 'tablet-portrait', width: 768, height: 1024 },
  { name: 'tablet-landscape', width: 1024, height: 768 },
] as const;
type Device = (typeof DEVICES)[number]['name'];

/**
 * Findings that are the example's doing, not the component's. Each needs a reason; `devices`
 * limits it to some shapes. `match` is tested against the finding text.
 */
const ALLOWED: Record<string, { match: RegExp; devices?: Device[]; why: string }[]> = {
  '/components/rook-bar': [
    {
      match: /^overflow: .*mn-rook-bar__/,
      devices: ['tablet-landscape'],
      why: 'A site header spans the screen; the preview is narrower than the screen its media queries read.',
    },
  ],
  '/components/event-chip': [
    {
      match: /^overflow: /,
      devices: ['small-phone', 'phone-portrait'],
      why: 'The example draws chips in fixed 160px calendar cells to show truncation.',
    },
  ],
  '/components/ai-launcher': [
    {
      match: /^small target: input /,
      why: 'A plain text field in the example page, not part of the launcher.',
    },
  ],
  '/components/skip-link': [
    {
      match: /^small target: input /,
      why: 'A plain text field in the example page, not part of SkipLink.',
    },
  ],
  '/components/app-shell': [
    {
      match: /^small target: a in \.mn-app-shell__main/,
      why: 'Example page content inside the shell.',
    },
    {
      match: /^small target: a\.mn-link /,
      why: 'Example page content inside the shell: the links of the sample inbox list.',
    },
  ],
};

interface Findings {
  pageOverflow: number;
  problems: string[];
}

function inspect(): Findings {
  const problems: string[] = [];
  const viewport = document.documentElement.clientWidth;
  const describe = (el: Element): string => {
    const own = String(el.getAttribute('class') ?? '')
      .split(/\s+/)
      .find((c) => c.startsWith('mn-'));
    const tag = el.tagName.toLowerCase();
    if (own) return `${tag}.${own}`;
    const up = el.parentElement?.closest("[class*='mn-']");
    const parent = up
      ? String(up.getAttribute('class') ?? '')
          .split(/\s+/)
          .find((c) => c.startsWith('mn-'))
      : undefined;
    return parent ? `${tag} in .${parent}` : tag;
  };
  const CONTROLS =
    'button, [role=button], input:not([type=hidden]), select, textarea, [role=checkbox], [role=switch], [role=radio], [role=tab], [role=menuitem], a[href]';
  const CONTAINERS =
    'div, section, main, body, html, ul, ol, li, table, tr, td, th, p, form, fieldset, article, header, footer, nav, aside';

  document.querySelectorAll('.docs-example__preview').forEach((preview, example) => {
    const box = preview.getBoundingClientRect();
    const seen = new Set<string>();
    const report = (text: string) => {
      if (seen.size < 6 && !seen.has(text)) {
        seen.add(text);
        problems.push(`${text} (example ${example + 1})`);
      }
    };
    /** True when a sideways scroller or a clipping box between `el` and the preview holds it. */
    const held = (el: Element): boolean => {
      for (let n = el.parentElement; n && n !== preview.parentElement; n = n.parentElement) {
        if (getComputedStyle(n).overflowX !== 'visible') return true;
      }
      return false;
    };

    preview.querySelectorAll<HTMLElement>('*').forEach((el) => {
      const style = getComputedStyle(el);
      if (
        style.display === 'none' ||
        style.visibility === 'hidden' ||
        style.position === 'fixed' ||
        style.opacity === '0' ||
        el.closest('.mn-visually-hidden, [aria-hidden="true"]')
      )
        return;
      const rect = el.getBoundingClientRect();
      if (rect.width === 0 || rect.height === 0) return;

      const out = Math.max(rect.right - box.right, box.left - rect.left);
      if (out > 1.5 && !held(el)) report(`overflow: ${describe(el)} by ${Math.round(out)}px`);

      if (!el.matches(CONTROLS) || (el as HTMLButtonElement).disabled) return;
      if (style.pointerEvents === 'none') return;
      // A link in running text is exempt (2.5.8, "inline").
      if (
        el.matches('a[href]') &&
        style.display.startsWith('inline') &&
        el.parentElement &&
        el.parentElement.textContent?.trim() !== el.textContent?.trim()
      )
        return;
      const label = el.closest('label');
      const labelRect = label ? label.getBoundingClientRect() : rect;
      const width = Math.max(rect.width, labelRect.width);
      const height = Math.max(rect.height, labelRect.height);
      if (width >= 23.5 && height >= 23.5) return;

      // A stretched link: its ::after covers the whole card or row.
      const after = getComputedStyle(el, '::after');
      if (
        after.position === 'absolute' &&
        parseFloat(after.width) >= 24 &&
        parseFloat(after.height) >= 24
      )
        return;

      el.scrollIntoView({ block: 'center', inline: 'nearest' });
      const r = el.getBoundingClientRect();
      const cx = r.left + r.width / 2;
      const cy = r.top + r.height / 2;
      const mine = (hit: Element | null): boolean => {
        if (!hit) return false;
        if (hit === el || el.contains(hit)) return true;
        if (hit.contains(el)) return !hit.matches(CONTAINERS);
        const l = hit.closest('label');
        return !!l && (l.control === el || l.contains(el));
      };
      /** A point outside a scrolling or clipping ancestor cannot be tapped until scrolled to. */
      const offstage = (x: number, y: number): boolean => {
        if (x < 0 || y < 0 || x > window.innerWidth || y > window.innerHeight) return true;
        for (let n = el.parentElement; n && n !== document.body; n = n.parentElement) {
          const s = getComputedStyle(n);
          if (s.overflowX === 'visible' && s.overflowY === 'visible') continue;
          const b = n.getBoundingClientRect();
          if (x < b.left || x > b.right || y < b.top || y > b.bottom) return true;
        }
        return false;
      };
      const points: [number, number][] = [
        [cx - 11.5, cy],
        [cx + 11.5, cy],
        [cx, cy - 11.5],
        [cx, cy + 11.5],
      ];
      if (!points.every(([x, y]) => offstage(x, y) || mine(document.elementFromPoint(x, y))))
        report(`small target: ${describe(el)} ${Math.round(width)}x${Math.round(height)}`);
    });
  });

  return {
    pageOverflow: Math.max(0, document.documentElement.scrollWidth - viewport),
    problems,
  };
}

async function open(page: Page, route: string): Promise<() => string[]> {
  await stubThirdPartyFrames(page);
  const consoleMessages = watchConsole(page);
  await page.goto(pageUrl(route));
  await waitForFonts(page);
  // Five shapes of 500+ pages run in parallel: give a page as long to appear as the axe test does.
  await expect(page.locator('h1').first()).toBeAttached({ timeout: 15_000 });
  // The platform tabs replace their prerendered copy once the page has hydrated: measure after.
  await componentPageHydrated(page);
  await mountPreviews(page);
  // Components that measure themselves (a ResizeObserver choosing a compact layout) re-render
  // after mounting: wait until the examples keep the same size for several frames in a row.
  await page.evaluate(
    () =>
      new Promise<void>((resolve) => {
        const signature = () => {
          let sum = document.documentElement.scrollHeight;
          document
            .querySelectorAll(
              '.docs-example__preview, .docs-example__preview :is(input, textarea, button)',
            )
            .forEach((el) => {
              const r = el.getBoundingClientRect();
              sum += r.width * 7 + r.height;
            });
          return Math.round(sum);
        };
        let last = signature();
        let same = 0;
        let frames = 0;
        const tick = () => {
          const now = signature();
          same = now === last ? same + 1 : 0;
          last = now;
          frames += 1;
          if (same >= 6 || frames > 120) resolve();
          else requestAnimationFrame(tick);
        };
        requestAnimationFrame(tick);
      }),
  );
  await settle(page);
  return consoleMessages;
}

for (const device of DEVICES) {
  test.describe(`devices · ${device.name}`, () => {
    test.use({
      viewport: { width: device.width, height: device.height },
      hasTouch: true,
      // Firefox has no mobile emulation (Playwright rejects `isMobile` there): it runs the
      // same viewport with touch input, which is what reflow and tap-target checks need.
      isMobile: async ({ browserName }, use) => {
        await use(browserName !== 'firefox' && device.width < 800);
      },
      deviceScaleFactor: 1,
    });
    for (const route of componentRoutes()) {
      test(`${route} fits and is tappable`, async ({ page }) => {
        const consoleMessages = await open(page, route);
        const found = await page.evaluate(inspect);
        const allowed = (ALLOWED[route] ?? []).filter(
          (a) => !a.devices || a.devices.includes(device.name),
        );
        const problems = found.problems.filter((p) => !allowed.some((a) => a.match.test(p)));
        if (found.pageOverflow > 1)
          problems.unshift(`the page scrolls sideways by ${found.pageOverflow}px`);
        expect(problems).toEqual([]);
        expect(consoleMessages()).toEqual([]);
      });
    }
  });
}
