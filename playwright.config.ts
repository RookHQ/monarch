import { defineConfig, devices } from '@playwright/test';

/**
 * Two projects against the built docs site (apps/docs/dist via `vite preview`):
 *   a11y   — axe-core on every route, light and dark
 *   visual — gallery screenshots, themes × accents × widths
 */
/**
 * Port of the docs preview the tests run against. Several worktrees on one
 * machine each set their own (`MONARCH_DOCS_PORT=4301 npm run test:visual`), and
 * an already-running server is reused only when asked for explicitly
 * (`MONARCH_REUSE_SERVER=1`), so a run never silently tests another
 * worktree's build.
 */
const PORT = Number(process.env.MONARCH_DOCS_PORT ?? 4173);
const BASE_URL = `http://127.0.0.1:${PORT}`;

export default defineConfig({
  testDir: 'apps/docs/tests',
  // Platform-specific: Chromium's text rasterisation differs between macOS and Linux (CI).
  snapshotPathTemplate: '{testDir}/__screenshots__/{projectName}/{arg}-{platform}{ext}',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  outputDir: 'apps/docs/test-results',
  timeout: 60_000,
  expect: {
    // Font antialiasing differs slightly between machines; keep it tight but not brittle.
    toHaveScreenshot: { maxDiffPixelRatio: 0.002, animations: 'disabled', caret: 'hide' },
  },
  use: {
    baseURL: BASE_URL,
    trace: 'retain-on-failure',
  },
  webServer: {
    command: `npm run preview -w @rookcom/monarch-docs -- --host 127.0.0.1 --port ${PORT}`,
    url: BASE_URL,
    reuseExistingServer: !process.env.CI && process.env.MONARCH_REUSE_SERVER === '1',
    timeout: 60_000,
  },
  projects: [
    // Accessibility and keyboard behaviour run in all three engines.
    { name: 'a11y-chromium', testMatch: /a11y\.spec\.ts/, use: { ...devices['Desktop Chrome'] } },
    { name: 'a11y-firefox', testMatch: /a11y\.spec\.ts/, use: { ...devices['Desktop Firefox'] } },
    { name: 'a11y-webkit', testMatch: /a11y\.spec\.ts/, use: { ...devices['Desktop Safari'] } },
    // Visual baselines are Chromium only: pixel output differs per engine by design.
    { name: 'visual', testMatch: /visual\.spec\.ts/, use: { ...devices['Desktop Chrome'] } },
  ],
});
