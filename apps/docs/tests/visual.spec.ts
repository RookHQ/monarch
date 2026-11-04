import { expect, test, type Page } from '@playwright/test';
import { GALLERY } from '../src/pages/gallery-ids';
import { setAppearance, waitForFonts, type Theme } from './helpers';

/**
 * Visual baselines: one screenshot per gallery component, Linux only (the CI
 * container is the single source of truth), so adding or changing a component
 * only adds or changes its own PNGs
 * (`__screenshots__/visual/gallery/<id>/<theme>-<accent>-<width>-linux.png`).
 *
 * Matrix, kept small on purpose (docs/guides/visual-testing.md):
 *   - every component: light, dark and hc-dark at 1280, light at 390
 *     (layout, both schemes, the strongest contrast mode, the phone layout);
 *   - product accents (base, calendar, chat) in light at 1280 for a small
 *     sample of accent-heavy components only. Accent contrast for every
 *     component and theme is enforced by the token contrast tests instead.
 */
const VARIANTS: Variant[] = [
  { theme: 'light', accent: null, width: 1280 },
  { theme: 'dark', accent: null, width: 1280 },
  { theme: 'hc-dark', accent: null, width: 1280 },
  { theme: 'light', accent: null, width: 390 },
];
/** base = gold (dark text); calendar = blue (darkened fill); chat = indigo. */
const ACCENTS = ['base', 'calendar', 'chat'] as const;
/** Components whose look depends most on the product accent. */
const ACCENT_SAMPLE = ['button', 'switch', 'tabs', 'progress-bar'] as const;

interface Variant {
  theme: Theme;
  accent: string | null;
  width: number;
}

test.skip(
  process.platform !== 'linux' && !process.env.MONARCH_VISUAL_LOCAL,
  'Visual baselines are Linux-only and checked in CI. Set MONARCH_VISUAL_LOCAL=1 to compare against your own local (git-ignored) baselines.',
);

async function openGallery(page: Page, { theme, accent, width }: Variant, path = '/#/__gallery') {
  await page.setViewportSize({ width, height: 900 });
  await setAppearance(page, theme, accent);
  await page.goto(path);
  await waitForFonts(page);
  await expect(page.getByTestId('gallery')).toBeVisible();
  await expect(page.locator('html')).toHaveAttribute(
    'data-mn-theme',
    theme.endsWith('dark') ? 'dark' : 'light',
  );
}

/**
 * Each component is rendered alone at `#/__gallery/<id>` (a hash change, no
 * reload), so a component's position never depends on the height of the
 * components before it: sub-pixel offsets would otherwise change text
 * rasterisation and rounded heights of everything below an edited component.
 */
async function shootGallery(page: Page, variant: Variant, ids: readonly string[]) {
  const suffix = `${variant.theme}-${variant.accent ?? 'default'}-${variant.width}`;
  await openGallery(page, variant, `/#/__gallery/${ids[0]}`);
  for (const id of ids) {
    await page.evaluate((hash) => {
      window.location.hash = hash;
    }, `/__gallery/${id}`);
    const section = page.locator(`[data-gallery-id="${id}"]`);
    await expect(section).toBeVisible();
    await page.evaluate(() => window.scrollTo(0, 0));
    await expect.soft(section, id).toHaveScreenshot(['gallery', id, `${suffix}.png`]);
  }
}

for (const variant of VARIANTS) {
  test(`gallery · ${variant.theme}-${variant.width}`, async ({ page }) => {
    test.setTimeout(240_000);
    await shootGallery(page, variant, GALLERY);
  });
}

for (const accent of ACCENTS) {
  test(`gallery accents · ${accent}`, async ({ page }) => {
    await shootGallery(page, { theme: 'light', accent, width: 1280 }, ACCENT_SAMPLE);
  });
}

/** Reference marketing page: both schemes, high contrast, the phone layout and one product accent. */
const MARKETING: { theme: Theme; accent: string | null; width: number }[] = [
  { theme: 'light', accent: null, width: 1280 },
  { theme: 'dark', accent: null, width: 1280 },
  { theme: 'hc-light', accent: null, width: 1280 },
  { theme: 'light', accent: null, width: 390 },
  { theme: 'light', accent: 'dossier', width: 1280 },
];
for (const { theme, accent, width } of MARKETING) {
  const name = `marketing-${theme}-${accent ?? 'default'}-${width}`;
  test(name, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    await setAppearance(page, theme, accent);
    await page.goto('/#/__marketing');
    await waitForFonts(page);
    await expect(page.getByTestId('marketing')).toBeVisible();
    await expect(page).toHaveScreenshot(`${name}.png`, { fullPage: true });
  });
}

test('open overlays · light · 1280', async ({ page }) => {
  const light: Variant = { theme: 'light', accent: null, width: 1280 };
  await openGallery(page, light, '/#/__gallery/dropdown-menu');
  await page.getByRole('button', { name: 'Actions', exact: true }).first().click();
  await expect(page.getByRole('menu')).toBeVisible();
  await expect(page).toHaveScreenshot(['overlays', 'menu-light.png']);
  await page.keyboard.press('Escape');

  await openGallery(page, light, '/#/__gallery/dialog');
  await page.getByRole('button', { name: 'Delete project' }).first().click();
  await expect(page.getByRole('dialog')).toBeVisible();
  await expect(page).toHaveScreenshot(['overlays', 'dialog-light.png']);
});

/** Opens the first trigger named `trigger` on a component's gallery page and shoots the page. */
async function shootOpen(
  page: Page,
  id: string,
  trigger: string,
  role: 'dialog' | 'alertdialog' | 'menu' | 'listbox',
  name: string,
  width = 1280,
  triggerRole: 'button' | 'combobox' = 'button',
) {
  // A fresh document each time: a hash-only navigation would keep the last overlay open.
  await page.goto('about:blank');
  await openGallery(page, { theme: 'light', accent: null, width }, `/#/__gallery/${id}`);
  await page.getByRole(triggerRole, { name: trigger, exact: true }).first().click();
  // The newest layer: an inline listbox example may already be on the page.
  const layer = page.getByRole(role).last();
  await expect(layer).toBeVisible();
  // Let the entrance animation finish so the shot is stable.
  await layer.evaluate((el) => Promise.all(el.getAnimations().map((a) => a.finished)));
  await expect(page).toHaveScreenshot(['overlays', `${name}.png`]);
}

test('open overlays · wave 2 · light', async ({ page }) => {
  test.setTimeout(120_000);
  await shootOpen(page, 'dropdown-menu', 'Document', 'menu', 'menu-danger-light-390', 390);
  await shootOpen(page, 'dialog', 'Rename board', 'dialog', 'dialog-form-light-390', 390);
  await shootOpen(page, 'confirm-dialog', 'Delete project', 'alertdialog', 'confirm-dialog-light');
  await shootOpen(page, 'sheet', 'Open details', 'dialog', 'sheet-light');
  await shootOpen(page, 'sheet', 'Open details', 'dialog', 'sheet-light-390', 390);
  await shootOpen(page, 'sheet', 'Share', 'dialog', 'sheet-bottom-light-390', 390);
  await shootOpen(page, 'responsive-dialog', 'Invite people', 'dialog', 'responsive-dialog-light');
  await shootOpen(
    page,
    'responsive-dialog',
    'Invite people',
    'dialog',
    'responsive-dialog-light-390',
    390,
  );
});

test('open overlays · wave 3 pickers · light', async ({ page }) => {
  test.setTimeout(120_000);
  await shootOpen(page, 'combobox', 'Show options', 'listbox', 'combobox-light');
  await shootOpen(
    page,
    'entity-picker',
    'Project',
    'dialog',
    'entity-picker-light',
    1280,
    'combobox',
  );
  await shootOpen(
    page,
    'entity-picker',
    'Project',
    'dialog',
    'entity-picker-light-390',
    390,
    'combobox',
  );
  await shootOpen(
    page,
    'assignee-picker',
    'Assignee',
    'dialog',
    'assignee-picker-light',
    1280,
    'combobox',
  );
  await shootOpen(page, 'timezone-select', 'Show options', 'listbox', 'timezone-select-light');
});

test('open overlays · wave 4 flows · light', async ({ page }) => {
  test.setTimeout(60_000);
  await shootOpen(page, 'command-palette', 'Search documents', 'dialog', 'command-palette-light');
  await shootOpen(
    page,
    'command-palette',
    'Search documents',
    'dialog',
    'command-palette-light-390',
    390,
  );
});

test('open overlays · wave 3 date pickers · light', async ({ page }) => {
  test.setTimeout(120_000);
  await shootOpen(page, 'date-picker', 'Choose date', 'dialog', 'date-picker-light');
  await shootOpen(page, 'date-picker', 'Choose date', 'dialog', 'date-picker-light-390', 390);
  await shootOpen(
    page,
    'date-range-picker',
    'Change dates, 6 Oct 2026 to 4 Nov 2026',
    'dialog',
    'date-range-picker-light',
  );
  await shootOpen(
    page,
    'date-range-picker',
    'Change dates, 6 Oct 2026 to 4 Nov 2026',
    'dialog',
    'date-range-picker-light-390',
    390,
  );
});

test('open overlays · wave 4 data · light', async ({ page }) => {
  test.setTimeout(120_000);
  await shootOpen(page, 'data-table', 'Columns', 'dialog', 'data-table-columns-light');
  await shootOpen(page, 'bulk-action-bar', 'More', 'menu', 'bulk-action-bar-more-light');
  await shootOpen(
    page,
    'saved-views',
    'View: My open tickets 7 Edited',
    'menu',
    'saved-views-menu-light-390',
    390,
  );
});

test('open overlays · wave 3 calendar · light', async ({ page }) => {
  await shootOpen(
    page,
    'calendar-month-view',
    '+3 more',
    'dialog',
    'calendar-month-view-more-light',
  );
});

test('open overlays · wave 6 conversations · light', async ({ page }) => {
  test.setTimeout(120_000);
  await shootOpen(
    page,
    'notification-center',
    'Notifications, 2 unread',
    'dialog',
    'notification-center-light',
  );
  await shootOpen(
    page,
    'notification-center',
    'Notifications, 2 unread',
    'dialog',
    'notification-center-light-390',
    390,
  );
  await shootOpen(
    page,
    'audit-log-viewer',
    'Details Changed a member’s role',
    'dialog',
    'audit-log-viewer-light',
  );

  // The mention list opens from typing, not from a trigger button.
  await page.goto('about:blank');
  await openGallery(
    page,
    { theme: 'light', accent: null, width: 1280 },
    '/#/__gallery/mention-picker',
  );
  const field = page.getByRole('textbox', { name: 'Comment' }).first();
  await field.click();
  await field.press('End');
  await field.pressSequentially('@');
  const list = page.getByRole('listbox', { name: 'Suggestions' });
  await expect(list).toBeVisible();
  await list.evaluate((el) => Promise.all(el.getAnimations().map((a) => a.finished)));
  await expect(page).toHaveScreenshot(['overlays', 'mention-picker-light.png']);
});

test('open overlays · wave 9 editor · light', async ({ page }) => {
  test.setTimeout(60_000);
  const light: Variant = { theme: 'light', accent: null, width: 1280 };
  // Link popover from the toolbar.
  await openGallery(page, light, '/#/__gallery/rich-text-editor');
  await page.getByRole('textbox', { name: 'Release notes' }).click();
  await page.locator('[data-tool="link"]').first().click();
  const popover = page.locator('.mn-rich-text-editor__link-popover');
  await expect(popover).toBeVisible();
  await popover.evaluate((el) => Promise.all(el.getAnimations().map((a) => a.finished)));
  await expect(page).toHaveScreenshot(['overlays', 'rich-text-editor-link-light.png']);
  // Mention suggestions under the caret.
  await page.goto('about:blank');
  await openGallery(page, light, '/#/__gallery/rich-text-editor');
  await page.getByRole('textbox', { name: 'Comment' }).click();
  await page.keyboard.type('Thanks @');
  const list = page.getByRole('listbox', { name: 'Suggestions' });
  await expect(list).toBeVisible();
  await expect(list.getByRole('option')).toHaveCount(4);
  await list.evaluate((el) => Promise.all(el.getAnimations().map((a) => a.finished)));
  await expect(page).toHaveScreenshot(['overlays', 'rich-text-editor-mentions-light.png']);
});

test('open overlays · wave 8 pages · light', async ({ page }) => {
  test.setTimeout(120_000);
  const more = 'More actions for Spring launch';
  await shootOpen(page, 'page-header', more, 'menu', 'page-header-more-light');
  await shootOpen(page, 'page-header', more, 'menu', 'page-header-more-light-390', 390);
  await shootOpen(
    page,
    'split-inbox-layout',
    'Show ticket details',
    'dialog',
    'split-inbox-drawer-light-390',
    390,
  );
});

test('open overlays · wave 7 shell · light', async ({ page }) => {
  test.setTimeout(180_000);
  await shootOpen(page, 'theme-toggle', 'Theme: Dark', 'menu', 'theme-toggle-menu-light');
  await shootOpen(page, 'space-switcher', 'Space: All spaces', 'dialog', 'space-switcher-light');
  await shootOpen(
    page,
    'space-switcher',
    'Space: All spaces',
    'dialog',
    'space-switcher-light-390',
    390,
  );
  await shootOpen(
    page,
    'workspace-switcher',
    'Workspace: Acme @acme',
    'dialog',
    'workspace-switcher-light',
  );
  await shootOpen(
    page,
    'rook-rail',
    'Account: Ada Lovelace ada@example.com',
    'menu',
    'rook-rail-account-light',
  );
  await shootOpen(
    page,
    'rook-rail',
    'Open navigation',
    'dialog',
    'rook-rail-drawer-light-390',
    390,
  );
});

test('open overlays · wave 7 shell panels · light', async ({ page }) => {
  test.setTimeout(120_000);
  await shootOpen(page, 'support-panel', 'Help & support', 'dialog', 'support-panel-light');
  await shootOpen(
    page,
    'support-panel',
    'Help & support',
    'dialog',
    'support-panel-light-390',
    390,
  );
});
