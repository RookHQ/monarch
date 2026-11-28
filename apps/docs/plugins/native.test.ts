import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { afterAll, describe, expect, it } from 'vitest';
import { highlight } from '../src/features/code/highlight.js';
import { highlightNative, nativeLang } from '../src/features/code/highlight-native.js';
import { renderBody, renderGuide } from './guides.js';
import {
  catalogVersions,
  catalogueRows,
  codeBlocks,
  componentModule,
  latestRelease,
  nativeFacts,
  platformModule,
  pngSize,
  presentGuides,
  readNative,
  splitSections,
  toEntry,
  topicsUnder,
} from './native.js';

const repoRoot = path.resolve(__dirname, '../../..');

const ENTRY = {
  id: 'button',
  name: 'MonarchButton',
  title: 'Button',
  family: 'actions',
  category: 'B',
  coverage: 'build',
  kind: 'wrapper',
  wrapsNative: 'Button',
  status: 'available',
  since: '0.1.0',
  summary: 'A button.',
  whenToUse: 'For an action.',
  whenNotToUse: 'For navigation.',
  api: ['fun MonarchButton()'],
  options: [{ name: 'text', type: 'String', default: 'required', description: 'The label.' }],
  states: [{ name: 'resting', description: 'The fill.' }],
  appearance: 'Filled.',
  platformNotes: 'Taller than the web.',
  accessibility: ['Has the button role.'],
  edgeCases: [],
  tests: ['MonarchButtonTest'],
  examples: [{ title: 'Primary', code: 'MonarchButton()', setup: 'val x = 1' }],
  webEquivalent: 'Button',
  dependsOn: [],
};
const PARITY = {
  components: [
    {
      id: 'button',
      family: 'actions',
      title: 'Button',
      android: { coverage: 'build', name: 'MonarchButton', uses: 'Button', note: 'Sizes.' },
      ios: { coverage: 'build', name: 'MonarchButton', uses: 'Button', note: 'Sizes.' },
      wave: 0,
    },
    {
      id: 'tabs',
      family: 'navigation',
      title: 'Tabs',
      android: { coverage: 'native', name: null, uses: 'PrimaryTabRow', note: 'Material tabs.' },
      ios: { coverage: 'web-only', name: null, uses: null, note: 'Use TabView.' },
      wave: 2,
    },
  ],
};

/** A 1×1 PNG header is enough for the size reader: signature, IHDR length and type, width, height. */
function png(width: number, height: number): Buffer {
  const b = Buffer.alloc(24);
  b.writeUInt32BE(0x89504e47, 0);
  b.writeUInt32BE(0x0d0a1a0a, 4);
  b.writeUInt32BE(13, 8);
  b.write('IHDR', 12, 'latin1');
  b.writeUInt32BE(width, 16);
  b.writeUInt32BE(height, 20);
  return b;
}

const roots: string[] = [];
/** A throwaway repository with only the files a test writes. */
function repo(files: Record<string, string | Buffer>): string {
  const root = mkdtempSync(path.join(tmpdir(), 'monarch-native-'));
  roots.push(root);
  for (const [file, content] of Object.entries(files)) {
    mkdirSync(path.dirname(path.join(root, file)), { recursive: true });
    writeFileSync(path.join(root, file), content);
  }
  return root;
}
afterAll(() => {
  for (const root of roots) rmSync(root, { recursive: true, force: true });
});

describe('highlight: Kotlin and Swift', () => {
  const kinds = (code: string, lang: 'kotlin' | 'swift') =>
    highlightNative(code, lang)
      .filter((t) => t.kind)
      .map((t) => `${t.kind}:${t.text}`);

  it('keeps the text exactly', () => {
    const code = '@Composable\nfun Demo(items: List<String>) {\n    val n = 2 // two\n}';
    expect(
      highlightNative(code, 'kotlin')
        .map((t) => t.text)
        .join(''),
    ).toBe(code);
  });

  it('reads Kotlin keywords, annotations, types, strings and comments', () => {
    expect(kinds('@Composable fun Demo() { val s = "a" } // x', 'kotlin')).toEqual([
      'attr:@Composable',
      'keyword:fun',
      'tag:Demo',
      'punct:()',
      'punct:{',
      'keyword:val',
      'punct:=',
      'string:"a"',
      'punct:}',
      'comment:// x',
    ]);
  });

  it('does not read generics as JSX tags', () => {
    expect(kinds('List<String>', 'kotlin')).toEqual([
      'tag:List',
      'punct:<',
      'tag:String',
      'punct:>',
    ]);
  });

  it('reads Swift keywords and property wrappers', () => {
    expect(kinds('@State private var isOn = false', 'swift')).toEqual([
      'attr:@State',
      'keyword:private',
      'keyword:var',
      'punct:=',
      'keyword:false',
    ]);
  });

  it('names the native languages and leaves everything else to the TSX highlighter', () => {
    expect(['kotlin', 'kt', 'kts', 'swift', 'tsx', 'sh', undefined].map(nativeLang)).toEqual([
      'kotlin',
      'kotlin',
      'kotlin',
      'swift',
      null,
      null,
      null,
    ]);
    expect(highlight('const a = 1;').some((t) => t.kind === 'keyword' && t.text === 'const')).toBe(
      true,
    );
  });
});

describe('guides renderer', () => {
  it('keeps links to pages on the site and sends the rest to the repository', () => {
    const { html } = renderGuide(
      '# T\n\n## S\n\n[a](theming.md#x) [b](../architecture/NATIVE-PLATFORMS.md) [c](native-adaptive-layout.md)\n',
      { base: '/monarch/', source: 'docs/guides/native-contributing.md' },
    );
    expect(html).toContain('href="/monarch/theming/#x"');
    expect(html).toContain(
      'href="https://github.com/RookHQ/monarch/blob/main/docs/architecture/NATIVE-PLATFORMS.md"',
    );
    expect(html).toContain('href="/monarch/native/adaptive-layout/"');
  });

  it('links a guide that does not exist to the repository, not to a missing page', () => {
    const root = repo({ 'docs/guides/native-adaptive-layout.md': '# A\n' });
    const html = renderBody(
      '[a](native-accessibility-and-testing.md) [b](native-adaptive-layout.md)',
      {
        source: 'docs/guides/native-contributing.md',
        repoRoot: root,
      },
    );
    expect(html).toContain('blob/main/docs/guides/native-accessibility-and-testing.md');
    expect(html).toContain('href="/native/adaptive-layout/"');
  });

  it('highlights Kotlin and Swift blocks, names every block and records its language', () => {
    const html = renderBody('```kotlin\nval a = "<b>"\n```\n\n```sh\nls\n```\n', {
      codeLabel: (n) => `Install: code example ${n}`,
    });
    expect(html).toContain(
      '<pre tabindex="0" role="region" aria-label="Install: code example 1" data-lang="kotlin">',
    );
    expect(html).toContain('<span class="docs-tok" data-kind="keyword">val</span>');
    expect(html).toContain(
      '<span class="docs-tok" data-kind="string">&quot;&lt;b&gt;&quot;</span>',
    );
    expect(html).toContain(
      'aria-label="Install: code example 2" data-lang="sh"><code class="language-sh">ls',
    );
  });

  it('renders an empty header cell and task lists without unnamed elements', () => {
    const html = renderBody(
      '| | Version |\n| --- | --- |\n| Swift | 6.0 |\n\n- [ ] Tested\n- [x] Named\n',
    );
    expect(html).not.toContain('<th></th>');
    expect(html).toContain('<td></td>');
    expect(html).not.toContain('<input');
    expect(html).toContain('<li>Tested</li>');
    expect(html).toContain('<li>✓ Named</li>');
  });

  it('points fragment links of an embedded README section at the README', () => {
    expect(
      renderBody('See [limits](#known-limitations).', {
        source: 'platforms/ios/README.md',
        fragment: true,
      }),
    ).toContain('blob/main/platforms/ios/README.md#known-limitations');
  });
});

describe('native inventory', () => {
  it('accepts an entry in the schema shape and rejects anything else', () => {
    expect(toEntry(ENTRY)?.name).toBe('MonarchButton');
    expect(toEntry({ name: 'MonarchButton', api: 'fun MonarchButton()' })).toBeNull();
    expect(toEntry({ ...ENTRY, status: 'shipped' })).toBeNull();
    expect(toEntry([ENTRY])).toBeNull();
  });

  it('marks an entry in the patterns layer, and only that one', () => {
    expect(toEntry(ENTRY)?.pattern).toBe(false);
    expect(toEntry({ ...ENTRY, status: 'experimental', layer: 'patterns' })?.pattern).toBe(true);
    expect(toEntry({ ...ENTRY, layer: 'components' })?.pattern).toBe(false);
  });

  it('tolerates a platform with no inventory, a missing plan and broken files', () => {
    expect(readNative(repo({}))).toEqual({
      parity: [],
      inventory: { android: [], ios: [] },
      skipped: [],
    });
    const root = repo({
      'platforms/parity.json': JSON.stringify(PARITY),
      'platforms/ios/inventory/schema.json': '{}',
      'platforms/ios/inventory/button.json': JSON.stringify(ENTRY),
      'platforms/ios/inventory/half-written.json': '{ "id": ',
      'platforms/android/components.json': JSON.stringify({ components: [{ name: 'Old shape' }] }),
    });
    const data = readNative(root);
    expect(data.inventory.ios.map((e) => e.id)).toEqual(['button']);
    expect(data.inventory.android).toEqual([]);
    expect(data.skipped).toEqual(['platforms/ios/inventory/half-written.json']);
  });

  it('reads the generated roll-up when there is no inventory folder', () => {
    const root = repo({ 'platforms/android/components.json': JSON.stringify([ENTRY]) });
    expect(readNative(root).inventory.android.map((e) => e.id)).toEqual(['button']);
  });

  it('gives a component its entry where one exists and the plan where it does not', () => {
    const root = repo({
      'platforms/parity.json': JSON.stringify(PARITY),
      'platforms/ios/inventory/button.json': JSON.stringify(ENTRY),
    });
    const data = readNative(root);
    const button = componentModule(data, root, 'button');
    const value = JSON.parse(
      button.replace(/^\nexport default /, '').replace(/;\n$/, ''),
    ) as Record<
      string,
      { entry: { name: string } | null; plan: { coverage: string; wave: number } | null }
    >;
    expect(value.ios?.entry?.name).toBe('MonarchButton');
    expect(value.android?.entry).toBeNull();
    expect(value.android?.plan).toMatchObject({ coverage: 'build', wave: 0 });
    // An id that is in neither the plan nor an inventory has nothing to show.
    expect(componentModule(data, root, 'unknown')).toContain('"entry":null,"plan":null');
  });

  it('lists inventory entries, then planned components, and never marks a plan as available', () => {
    const root = repo({
      'platforms/parity.json': JSON.stringify(PARITY),
      'platforms/android/inventory/button.json': JSON.stringify(ENTRY),
    });
    const rows = catalogueRows(readNative(root), 'android', new Set(['button', 'tabs']));
    expect(rows.map((r) => [r.id, r.status, r.entry, r.name ?? r.uses])).toEqual([
      ['button', 'available', true, 'MonarchButton'],
      ['tabs', 'planned', false, 'PrimaryTabRow'],
    ]);
  });

  it('carries the patterns layer into the catalogue row', () => {
    const root = repo({
      'platforms/parity.json': JSON.stringify(PARITY),
      'platforms/ios/inventory/button.json': JSON.stringify({
        ...ENTRY,
        status: 'experimental',
        layer: 'patterns',
      }),
    });
    const [row] = catalogueRows(readNative(root), 'ios', new Set(['button']));
    expect(row).toMatchObject({ id: 'button', status: 'experimental', pattern: true });
    const plain = catalogueRows(
      readNative(repo({ 'platforms/ios/inventory/button.json': JSON.stringify(ENTRY) })),
      'ios',
      new Set(),
    );
    expect(plain[0]).not.toHaveProperty('pattern');
  });

  it('shows a preview only when the image exists, at its size in points (iOS 2x), as an imported asset', () => {
    const root = repo({
      'platforms/parity.json': JSON.stringify(PARITY),
      'platforms/ios/inventory/button.json': JSON.stringify(ENTRY),
      'platforms/ios/previews/button.dark.png': png(390, 120),
      'platforms/ios/previews/button.light.png': 'not a png',
    });
    expect(pngSize(path.join(root, 'platforms/ios/previews/button.dark.png'))).toEqual({
      width: 390,
      height: 120,
    });
    const source = componentModule(readNative(root), root, 'button');
    expect(source).toMatch(/^import __preview0__ from ".*button\.dark\.png";/);
    expect(source).toContain('"previews":{"dark":{"src":__preview0__,"width":195,"height":60}}');
    expect(source).not.toContain('button.light.png');
    expect(componentModule(readNative(root), root, 'tabs')).not.toContain('import ');
  });
});

describe('facts read from the packages', () => {
  it('reads the newest version of a changelog and whether it is released', () => {
    expect(latestRelease('# Changelog\n\n## 0.1.0 (unreleased)\n')).toEqual({
      version: '0.1.0',
      unreleased: true,
      versions: 1,
    });
    expect(latestRelease('## 0.2.0 - 2027-01-12\n\n## 0.1.0 - 2026-12-01\n')).toMatchObject({
      version: '0.2.0',
      unreleased: false,
      versions: 2,
    });
    expect(latestRelease(undefined)).toMatchObject({ version: null, unreleased: true });
  });

  it('reads the [versions] table of the version catalog only', () => {
    expect(
      catalogVersions(
        '[versions]\n# c\nmonarch = "0.1.0"\nminSdk = "24"\n\n[libraries]\nx = "1"\n',
      ),
    ).toEqual({ monarch: '0.1.0', minSdk: '24' });
    expect(catalogVersions(undefined)).toEqual({});
  });

  it('splits a README at its headings, ignoring headings inside code', () => {
    const sections = splitSections(
      '# T\n\n## Install\n\na\n\n```sh\n## not a heading\n```\n\n### Theme setup\n\nb\n',
    );
    expect([...sections.keys()]).toEqual(['install', 'theme-setup']);
    expect(sections.get('install')?.body).toContain('## not a heading');
  });

  it('reads the fenced code blocks of a README section', () => {
    expect(codeBlocks('a\n\n```swift\nlet a = 1\n```\n\nb\n\n```kts\nx()\n```\n')).toEqual([
      { lang: 'swift', code: 'let a = 1' },
      { lang: 'kts', code: 'x()' },
    ]);
  });

  it('lists the topics of one platform section of a guide', () => {
    expect(
      topicsUnder('## Android\n\n### Insets\n\n### Back\n\n## iOS\n\n### Safe areas\n', 'iOS'),
    ).toEqual(['Safe areas']);
  });

  it('omits what it cannot read instead of failing', () => {
    const root = repo({});
    const facts = nativeFacts(readNative(root), root);
    expect(facts.android).toMatchObject({ version: null, unreleased: true, entries: 0, total: 0 });
    expect(facts.ios.requirements).toEqual({ ios: null, swiftTools: null, dependencies: false });
    expect(presentGuides(root)).toEqual([]);
    expect(platformModule(readNative(root), root, 'ios', '/')).toContain('"sections":{}');
  });
});

describe('this repository', () => {
  const data = readNative(repoRoot);
  const facts = nativeFacts(data, repoRoot);

  it('has a plan and counts that agree with it', () => {
    expect(data.parity.length).toBeGreaterThan(0);
    for (const platform of ['android', 'ios'] as const) {
      const f = facts[platform];
      expect(f.total).toBe(data.parity.length);
      expect(f.covered).toBeLessThanOrEqual(f.entries);
      expect(f.covered).toBeLessThanOrEqual(f.total);
      expect(Object.values(f.status).reduce((a, b) => a + b, 0)).toBe(f.covered);
      expect(catalogueRows(data, platform, new Set()).length).toBe(f.total + f.entries - f.covered);
    }
  });

  it('gives each landing page its install snippet, code and the count of each catalogue group', () => {
    for (const platform of ['android', 'ios'] as const) {
      const source = platformModule(data, repoRoot, platform, '/');
      const landing = JSON.parse(
        /"landing":(\{[\s\S]*\}),"previews":/.exec(source.replace(/__preview\d+__/g, '""'))?.[1] ??
          'null',
      ) as { groups: { group: string; count: number }[]; install: unknown; code: unknown[] };
      expect(landing.install).not.toBeNull();
      expect(landing.code.length).toBe(2);
      expect(landing.groups.length).toBeGreaterThan(10);
      expect(landing.groups.every((g) => g.count > 0)).toBe(true);
      expect(landing.groups.map((g) => g.group)).toContain('actions');
    }
  });

  it('reads no inventory file it has to skip', () => {
    expect(data.skipped).toEqual([]);
  });
});
