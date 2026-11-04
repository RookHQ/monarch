import { expect, test } from '@playwright/test';
import {
  componentRoutes,
  mountPreviews,
  pageUrl,
  runAxe,
  setAppearance,
  settle,
  waitForFonts,
  watchConsole,
} from './helpers';

const PAGES = [
  '/',
  '/introduction',
  '/colours',
  '/typography',
  '/tokens',
  '/theming',
  '/getting-started',
  '/migration',
  '/conformance',
  '/patterns',
  '/patterns/guide',
  '/components',
  '/components/button',
  '/components/field',
  '/components/select',
  '/components/dialog',
  '/components/dropdown-menu',
  '/components/tabs',
  '/products',
  '/governance',
  '/marketing',
  '/__gallery',
  '/__marketing',
];
/** Every component page is scanned too: one per <id>.docs.tsx, found on disk. */
const ROUTES = [...new Set([...PAGES, ...componentRoutes()])];
/** Routes that render every component at once (each docs file is its own lazy chunk), so they need longer to appear in the slowest engine. */
const HEAVY_ROUTES = new Set(['/__gallery']);

for (const theme of ['light', 'dark'] as const) {
  test.describe(`axe · ${theme}`, () => {
    // Pages are prerendered in the build machine's time zone and locale. A distant one
    // and German make any example that depends on either fail to hydrate (console error).
    test.use({ timezoneId: 'Pacific/Kiritimati', locale: 'de-DE' });
    for (const route of ROUTES) {
      test(`${route} has no axe violations`, async ({ page }) => {
        const heavy = HEAVY_ROUTES.has(route);
        if (heavy) test.setTimeout(120_000);
        await setAppearance(page, theme);
        const consoleMessages = watchConsole(page);
        await page.goto(pageUrl(route));
        await waitForFonts(page);
        await expect(page.locator('main')).toBeVisible({ timeout: heavy ? 45_000 : 5_000 });
        // Pages are lazy chunks: wait for the page itself, not the shell.
        await expect(page.locator('h1').first()).toBeAttached();
        // Examples mount near the viewport; scan all of them.
        if (route.startsWith('/components/')) await mountPreviews(page);
        await settle(page);
        const violations = await runAxe(page, route === '/colours' ? ['.docs-swatch__chip'] : []);
        expect(
          violations.map(
            (v) =>
              `${v.id} (${v.impact}): ${v.help}\n  ${v.nodes.map((n) => n.target.join(' ')).join('\n  ')}`,
          ),
        ).toEqual([]);
        // Prerendered pages hydrate without a mismatch, and nothing else logs either.
        // The screenshot routes are left out: they open from the prerendered home
        // page and replace it, so browsers rightly report its preloads as unused.
        if (!route.startsWith('/__')) expect(consoleMessages()).toEqual([]);
      });
    }
  });
}

test('pages are prerendered and hydrate in place', async ({ page }) => {
  await setAppearance(page, 'light');
  const consoleMessages = watchConsole(page);
  const response = await page.request.get(pageUrl('/components/button'));
  const html = await response.text();
  // The HTML already has the page, before any script runs.
  expect(html).toContain('data-route="/components/button"');
  expect(html).toMatch(/<h1[^>]*>Button<\/h1>/);
  expect(html).toContain('<link rel="canonical"');
  await page.goto(pageUrl('/components/button'));
  await expect(page.getByRole('heading', { level: 1, name: 'Button' })).toBeVisible();
  // Hydrated, not re-rendered: the route marker is still on #root.
  await expect(page.locator('#root')).toHaveAttribute('data-route', '/components/button');
  // Client-side navigation still works from a hydrated page.
  await page
    .getByRole('navigation', { name: 'Documentation' })
    .getByRole('link', { name: 'Colours' })
    .click();
  await expect(page).toHaveTitle('Colours · Monarch');
  expect(new URL(page.url()).pathname).toMatch(/\/colours\/$/);
  expect(consoleMessages()).toEqual([]);
});

test('old hash addresses move to the real page address', async ({ page }) => {
  await setAppearance(page, 'light');
  await page.goto(`${pageUrl('/')}#/components/tabs#keyboard`);
  await expect(page).toHaveTitle('Tabs · Monarch');
  const url = new URL(page.url());
  expect(url.pathname).toMatch(/\/components\/tabs\/$/);
  expect(url.hash).toBe('#keyboard');
  // A renamed page keeps working at its old hash address.
  await page.goto(`${pageUrl('/')}#/components/input`);
  expect(new URL(page.url()).pathname).toMatch(/\/components\/text-field\/$/);
});

test('search: ⌘K / Ctrl K opens the palette and a result navigates', async ({ page }) => {
  await setAppearance(page, 'light');
  await page.goto(pageUrl('/introduction'));
  await expect(page.getByRole('heading', { level: 1, name: 'Introduction' })).toBeVisible();
  // The palette reads the platform the page reports (device emulation may differ from the host).
  const mod = await page.evaluate(() => {
    const nav = navigator as Navigator & { userAgentData?: { platform?: string } };
    return /mac|iphone|ipad|ipod/i.test(nav.userAgentData?.platform ?? nav.platform ?? '')
      ? 'Meta'
      : 'Control';
  });
  await page.keyboard.press(`${mod}+k`);
  const palette = page.getByRole('dialog', { name: 'Search the documentation' });
  await expect(palette).toBeVisible();
  await page.keyboard.type('data table');
  await page.keyboard.press('Enter');
  await expect(page).toHaveTitle('DataTable · Monarch');
  await expect(palette).toBeHidden();
});

test('component page: Show code reveals the extracted source', async ({ page }) => {
  await setAppearance(page, 'light');
  await page.goto(pageUrl('/components/button'));
  const show = page.getByRole('button', { name: 'Show code' }).first();
  await show.click();
  await expect(page.getByRole('button', { name: 'Hide code' }).first()).toHaveAttribute(
    'aria-expanded',
    'true',
  );
  await expect(page.getByRole('region', { name: 'Code: Variants' })).toContainText(
    "import { Button } from '@rookcom/monarch-react';",
  );
});

test('keyboard: skip link, nav and page title', async ({ page, browserName }) => {
  await setAppearance(page, 'light');
  await page.goto(pageUrl('/components/button'));
  await expect(page).toHaveTitle('Button · Monarch');
  // Safari's default (and WebKit on macOS) only tabs to form controls; links are
  // reached with Option+Tab. That is a browser preference, not a page bug.
  const tab = browserName === 'webkit' && process.platform === 'darwin' ? 'Alt+Tab' : 'Tab';
  await page.keyboard.press(tab);
  await expect(page.getByRole('link', { name: 'Skip to content' })).toBeFocused();
  await page.keyboard.press('Enter');
  await expect(page.locator('#main')).toBeFocused();
  await page
    .getByRole('navigation', { name: 'Documentation' })
    .getByRole('link', { name: 'Tokens' })
    .click();
  await expect(page).toHaveTitle('Tokens · Monarch');
  await expect(page.getByRole('link', { name: 'Tokens', exact: true })).toHaveAttribute(
    'aria-current',
    'page',
  );
});

test('theme and product controls change <html> attributes', async ({ page }) => {
  await setAppearance(page, 'light');
  await page.goto(pageUrl('/tokens'));
  // The top bar's compact switcher.
  await page
    .getByRole('banner')
    .getByRole('radiogroup', { name: 'Theme' })
    .getByRole('radio', { name: 'Dark' })
    .click();
  await expect(page.locator('html')).toHaveAttribute('data-mn-theme', 'dark');
  await expect(page.locator('html')).toHaveClass(/dark/);
  // Contrast and product accent live in the appearance popover.
  await page.getByTestId('appearance').click();
  const settings = page.getByRole('dialog', { name: 'Appearance' });
  await settings.getByRole('radio', { name: 'More' }).click();
  await expect(page.locator('html')).toHaveAttribute('data-mn-contrast', 'more');
  await settings.getByTestId('product').click();
  await page.getByRole('option', { name: 'Rook Base' }).click();
  await expect(page.locator('html')).toHaveAttribute('data-mn-product', 'base');
  // The accent variable now comes from the product file.
  const solid = await page.evaluate(() =>
    getComputedStyle(document.documentElement).getPropertyValue('--mn-color-accent-solid').trim(),
  );
  expect(solid).toMatch(/^hsl\(44 /);
  // Reload keeps it (cookie + storage), applied before paint.
  await page.reload();
  await expect(page.locator('html')).toHaveAttribute('data-mn-theme', 'dark');
  await expect(page.locator('html')).toHaveAttribute('data-mn-product', 'base');
});

for (const theme of ['light', 'dark'] as const) {
  test(`toast open (${theme}): no axe violations, focus stays, F8 reaches it`, async ({ page }) => {
    await setAppearance(page, theme);
    await page.goto(pageUrl('/components/toast'));
    await waitForFonts(page);
    const trigger = page.getByRole('button', { name: 'Archive ticket' }).first();
    // Keyboard activation: WebKit does not focus buttons on click.
    await trigger.focus();
    await page.keyboard.press('Enter');
    await expect(page.locator('.mn-toast').first()).toBeVisible();
    // Measure contrast after the enter animation, not mid-fade.
    await page.waitForFunction(() =>
      document.getAnimations().every((a) => a.playState !== 'running'),
    );
    // A toast never takes focus.
    await expect(trigger).toBeFocused();
    const violations = await runAxe(page, []);
    expect(
      violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`),
    ).toEqual([]);
    await page.keyboard.press('F8');
    const focusedInToast = await page.evaluate(
      // The docs page mounts one provider per example; each listens for F8.
      () => !!document.activeElement?.closest('[role="region"][aria-label$="(F8)"]'),
    );
    expect(focusedInToast).toBe(true);
  });
}
