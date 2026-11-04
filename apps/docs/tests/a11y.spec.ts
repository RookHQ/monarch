import { expect, test } from '@playwright/test';
import { componentRoutes, runAxe, setAppearance, waitForFonts } from './helpers';

const PAGES = [
  '/',
  '/getting-started',
  '/theming',
  '/migration',
  '/tokens',
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

for (const theme of ['light', 'dark'] as const) {
  test.describe(`axe · ${theme}`, () => {
    for (const route of ROUTES) {
      test(`${route} has no axe violations`, async ({ page }) => {
        await setAppearance(page, theme);
        await page.goto(`/#${route}`);
        await waitForFonts(page);
        await expect(page.locator('main')).toBeVisible();
        const violations = await runAxe(page, route === '/tokens' ? ['.docs-swatch__chip'] : []);
        expect(
          violations.map(
            (v) =>
              `${v.id} (${v.impact}): ${v.help}\n  ${v.nodes.map((n) => n.target.join(' ')).join('\n  ')}`,
          ),
        ).toEqual([]);
      });
    }
  });
}

test('keyboard: skip link, nav and page title', async ({ page, browserName }) => {
  await setAppearance(page, 'light');
  await page.goto('/#/components/button');
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
  await page.goto('/#/tokens');
  await page.getByTestId('theme').selectOption('dark');
  await expect(page.locator('html')).toHaveAttribute('data-mn-theme', 'dark');
  await expect(page.locator('html')).toHaveClass(/dark/);
  await page.getByTestId('contrast').selectOption('more');
  await expect(page.locator('html')).toHaveAttribute('data-mn-contrast', 'more');
  await page.getByTestId('product').selectOption('base');
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
    await page.goto('/#/components/toast');
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
