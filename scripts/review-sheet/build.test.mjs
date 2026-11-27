// Run: node --test scripts/review-sheet/build.test.mjsbuild.test.mjs
import assert from 'node:assert/strict';
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';
import {
  buildHtml,
  collect,
  defaultRoot,
  GALLERY_DIR,
  loadPrimitives,
  resolveImage,
  summarize,
} from './build.mjs';

const PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==',
  'base64',
);
const put = (root, path) => {
  mkdirSync(dirname(join(root, path)), { recursive: true });
  writeFileSync(join(root, path), PNG);
};
const fixture = () => mkdtempSync(join(tmpdir(), 'review-sheet-'));

test('primitives list has unique ids and the expected size', () => {
  const list = loadPrimitives();
  assert.ok(list.length >= 40 && list.length <= 50, `got ${list.length}`);
  assert.equal(new Set(list.map((p) => p.id)).size, list.length);
});

test('every web id in the list exists in the real gallery', () => {
  for (const primitive of loadPrimitives()) {
    if (primitive.web) {
      const image = resolveImage(defaultRoot, primitive, 'web', 'light');
      assert.notEqual(image.kind, 'missing', `${primitive.id}: ${image.note}`);
    }
  }
});

test('web image prefers 390 and falls back to 1280', () => {
  const root = fixture();
  const p = { id: 'x', web: 'x' };
  put(root, `${GALLERY_DIR}/x/light-default-1280-linux.png`);
  assert.match(resolveImage(root, p, 'web', 'light').path, /1280/);
  assert.equal(resolveImage(root, p, 'web', 'light').note, 'web 1280 (no 390 image)');
  put(root, `${GALLERY_DIR}/x/light-default-390-linux.png`);
  assert.match(resolveImage(root, p, 'web', 'light').path, /390/);
  assert.equal(resolveImage(root, p, 'web', 'dark').kind, 'missing');
  rmSync(root, { recursive: true });
});

test('native order: curated, then current look, then placeholder', () => {
  const root = fixture();
  const p = { id: 'text-field', web: null, android: 'text-field', ios: 'text-field' };
  assert.equal(resolveImage(root, p, 'android', 'light').kind, 'missing');
  put(root, 'platforms/android/previews/text-field.light.png');
  const current = resolveImage(root, p, 'android', 'light');
  assert.equal(current.kind, 'current');
  assert.match(current.note, /before restyle/);
  put(root, 'platforms/review-previews/android/text-field.light.png');
  assert.equal(resolveImage(root, p, 'android', 'light').kind, 'curated');
  assert.equal(resolveImage(root, p, 'android', 'dark').kind, 'missing');
  put(root, 'platforms/ios/Tests/Snapshots/text-field/text-field.dark.png');
  assert.equal(resolveImage(root, p, 'ios', 'dark').kind, 'current');
  rmSync(root, { recursive: true });
});

test('page shows the banner, placeholders, status and notes; embed inlines images', () => {
  const root = fixture();
  put(root, `${GALLERY_DIR}/button/light-default-390-linux.png`);
  put(root, 'platforms/android/previews/button.light.png');
  const out = join(root, 'platforms/review-sheet/index.html');
  const html = buildHtml({ root, out });
  assert.match(html, /Current look \(before restyle\)/);
  assert.match(html, /no ios image yet/);
  assert.match(html, /id="button"/);
  assert.match(
    html,
    /src="\.\.\/\.\.\/apps\/docs\/tests\/__screenshots__\/visual\/gallery\/button\/light-default-390-linux\.png"/,
  );
  assert.match(html, /contenteditable/);
  const single = buildHtml({ root, out, embed: true });
  assert.match(single, /src="data:image\/png;base64,/);
  assert.equal(collect(root).length, loadPrimitives().length);
  assert.equal(summarize(collect(root)).current, 1);
  rmSync(root, { recursive: true });
});

test('no banner when every native image is curated', () => {
  const root = fixture();
  for (const p of loadPrimitives())
    for (const platform of ['android', 'ios'])
      for (const theme of ['light', 'dark'])
        put(root, `platforms/review-previews/${platform}/${p.id}.${theme}.png`);
  const html = buildHtml({ root, out: join(root, 'platforms/review-sheet/index.html') });
  assert.doesNotMatch(html, /top-banner">/);
  assert.equal(summarize(collect(root)).missing, 0);
  rmSync(root, { recursive: true });
});

test('status.json is valid JSON with a rows object', () => {
  const file = join(dirname(fileURLToPath(import.meta.url)), 'status.json');
  assert.equal(typeof JSON.parse(readFileSync(file, 'utf8')).rows, 'object');
});
