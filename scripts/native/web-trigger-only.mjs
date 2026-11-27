#!/usr/bin/env node
/**
 * Lists the web docs examples whose primary example (the first of `sections.examples`, the one iOS and
 * Android copy) renders only a trigger button or placeholder text, instead of the real component state.
 *
 * It renders each component's gallery route in Chromium (a running docs dev server or preview), reads the
 * first example's DOM and applies these rules. An example is "trigger only" when
 *   - it holds one visible interactive element, that element is a bare trigger (a button that opens
 *     something: aria-haspopup or aria-expanded="false" or aria-controls) and nothing it opens is on screen; or
 *   - it has no visible text beyond a placeholder (empty, "Placeholder", "Lorem", "Content", "Anchor", an
 *     input's placeholder only) and no more than one element.
 *
 * Usage: node scripts/native/web-trigger-only.mjs [--url http://127.0.0.1:4391] [--ids a,b] [--shots dir] [--json]
 * Needs the docs site running (npm run dev -w @rookcom/monarch-docs) and playwright installed.
 */
import { readdirSync, mkdirSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { chromium } from 'playwright';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
const arg = (name, fallback) => {
  const i = process.argv.indexOf(name);
  return i > 0 ? process.argv[i + 1] : fallback;
};
const base = arg('--url', 'http://127.0.0.1:4391');
const shots = arg('--shots', '');
const only = arg('--ids', '');

function docsIds(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((e) => {
    const p = join(dir, e.name);
    if (e.isDirectory()) return docsIds(p);
    return e.name.endsWith('.docs.tsx') ? [e.name.replace(/\.docs\.tsx$/, '')] : [];
  });
}
const ids = only ? only.split(',') : docsIds(join(root, 'packages/react/src')).sort();

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1100, height: 800 } });
const errors = [];
page.on('pageerror', (e) => errors.push(String(e.message).slice(0, 200)));
await page.goto(`${base}/#/__gallery/button`);
await page.waitForSelector('[data-gallery-id]', { timeout: 60000 });
if (shots) mkdirSync(shots, { recursive: true });

const result = [];
const placeholderText =
  /^(placeholder|lorem|content|anchor|trigger|demo|example|panel|item)?[\s.…]*$/i;
for (const id of ids) {
  errors.length = 0;
  await page.evaluate((h) => {
    window.location.hash = h;
  }, `/__gallery/${id}`);
  let ok = true;
  try {
    await page.waitForSelector(`[data-gallery-id="${id}"] .gallery-example`, { timeout: 8000 });
  } catch {
    ok = false;
  }
  await page.waitForTimeout(250);
  const rows = ok
    ? await page.evaluate((gid) => {
        const sec = document.querySelector(`[data-gallery-id="${gid}"]`);
        const visible = (el) => {
          const r = el.getBoundingClientRect();
          const s = getComputedStyle(el);
          return r.width > 0 && r.height > 0 && s.visibility !== 'hidden';
        };
        const OPEN =
          '[role=dialog],[role=alertdialog],[role=menu],[role=listbox],[role=tooltip],[popover],[aria-expanded="true"],dialog[open]';
        return [...sec.querySelectorAll('.gallery-example')].map((ex, index) => {
          const body = [...ex.children].filter((c) => c.tagName !== 'H2');
          const inter = [
            ...ex.querySelectorAll(
              'button,a[href],input,select,textarea,[role=button],[role=combobox],[tabindex="0"]',
            ),
          ].filter(visible);
          const text = body
            .map((b) => b.innerText)
            .join(' ')
            .replace(/\s+/g, ' ')
            .trim();
          const triggers = inter.filter((e) =>
            e.matches('[aria-haspopup],[aria-expanded="false"],[aria-controls]'),
          );
          const buttons = inter.filter((e) => e.matches('button,[role=button]'));
          const buttonText = buttons
            .map((b) => b.innerText)
            .join(' ')
            .replace(/\s+/g, ' ')
            .trim();
          const scope =
            index === 0 ? [...document.querySelectorAll(OPEN)] : [...ex.querySelectorAll(OPEN)];
          const opened = scope.filter(visible).length;
          const elements = body.reduce((n, b) => n + b.querySelectorAll('*').length + 1, 0);
          return {
            index,
            title: ex.getAttribute('aria-label'),
            text: text.slice(0, 160),
            textLen: text.length,
            interactive: inter.length,
            buttons: buttons.length,
            buttonOnly:
              buttons.length > 0 && inter.length === buttons.length && text === buttonText,
            triggers: triggers.length,
            opened,
            elements,
          };
        });
      }, id)
    : [{ index: 0, missing: true }];
  for (const info of rows) {
    let reason = '';
    if (info.missing) reason = 'no-render';
    else if (info.interactive === 1 && info.triggers === 1 && info.opened === 0)
      reason = 'lone-trigger-closed';
    else if (info.buttonOnly && info.opened === 0 && info.textLen < 40 && info.elements <= 6)
      reason = 'button-only';
    else if (info.textLen === 0 && info.elements <= 3) reason = 'empty';
    else if (placeholderText.test(info.text) && info.elements <= 4) reason = 'placeholder-text';
    result.push({ id, reason, errors: [...errors], ...info });
  }
  if (shots && ok) {
    const el = page.locator(`[data-gallery-id="${id}"] .gallery-example`).first();
    await el.screenshot({ path: join(shots, `${id}.png`) }).catch(() => {});
  }
}
await browser.close();
const flagged = result.filter((r) => r.reason);
if (process.argv.includes('--json')) console.log(JSON.stringify(result, null, 1));
else {
  for (const r of flagged)
    console.log(`${r.id}\t#${r.index}\t${r.reason}\t${(r.text ?? '').slice(0, 60)}`);
  const primary = flagged.filter((r) => r.index === 0);
  console.error(
    `${primary.length} primary examples and ${flagged.length - primary.length} later examples are trigger-only (${result.length} examples scanned); ${result.filter((r) => r.errors.length).length} raised page errors`,
  );
}
