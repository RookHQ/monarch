import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { check } from '../src/check.js';
import {
  applyEdits,
  applyFixes,
  colourFix,
  colourTokenOf,
  normaliseHex,
  publicPathOf,
} from '../src/fixes.js';
import { COLOUR_TOKENS } from '../src/bundled-data.js';
import { toJson } from '../src/report.js';
import { scan, tempDir, write } from './helpers.js';

const someToken = Object.entries(COLOUR_TOKENS)[0] as [string, { variable: string }];

describe('fix helpers', () => {
  it('normalises hex colours', () => {
    expect(normaliseHex('#FFF')).toBe('#ffffff');
    expect(normaliseHex('#A1B2C3')).toBe('#a1b2c3');
    expect(normaliseHex('#ffffff80')).toBeNull();
  });

  it('maps deep dist imports to public subpaths', () => {
    expect(publicPathOf('@rookcom/monarch-tokens/dist/css/monarch.css')).toBe(
      '@rookcom/monarch-tokens/css',
    );
    expect(publicPathOf('@rookcom/monarch-tokens/dist/css/products/app.css')).toBe(
      '@rookcom/monarch-tokens/css/products/app',
    );
    expect(publicPathOf('@rookcom/monarch-react/dist/styles/components/ai-thread.css')).toBe(
      '@rookcom/monarch-react/styles/components/ai-thread.css',
    );
    expect(publicPathOf('@rookcom/monarch-react/dist/entries/ai.js')).toBe(
      '@rookcom/monarch-react/ai',
    );
    expect(publicPathOf('@rookcom/monarch-react/dist/internal/x.js')).toBeUndefined();
  });

  it('offers a colour fix only for an exact token match', () => {
    const [hex, token] = someToken;
    expect(colourTokenOf(hex.toUpperCase())?.variable).toBe(token.variable);
    const fix = colourFix(hex.slice(1), 3, 9);
    expect(fix?.edits).toEqual([{ line: 3, column: 9, length: 7, text: `var(${token.variable})` }]);
    expect(colourFix('123457', 1, 1)).toBeUndefined();
  });

  it('applies edits bottom-up, including insertions', () => {
    const text = "import a from 'a';\nconst c = '#abc';\n";
    expect(
      applyEdits(text, [
        { file: 'x', line: 2, column: 12, length: 4, text: 'var(--x)' },
        { file: 'x', line: 2, column: 1, length: 0, text: "import 'b';\n" },
      ]),
    ).toBe("import a from 'a';\nimport 'b';\nconst c = 'var(--x)';\n");
    expect(applyEdits('a\r\nb', [{ file: 'x', line: 2, column: 1, length: 1, text: 'c' }])).toBe(
      'a\r\nc',
    );
    expect(applyEdits('a\n', [{ file: 'x', line: 9, column: 1, length: 0, text: 'z\n' }])).toBe(
      'a\nz\n',
    );
  });
});

describe('rules with fixes', () => {
  it('imports/package-path reports deep imports with a safe fix', () => {
    const [f] = scan('src/app.tsx', "import '@rookcom/monarch-tokens/dist/css/monarch.css';\n");
    expect(f?.rule).toBe('imports/package-path');
    expect(f?.fix?.safety).toBe('safe');
    expect(f?.fix?.edits[0]?.text).toBe('@rookcom/monarch-tokens/css');
  });

  it('api/deprecated offers the replacement for review', () => {
    const found = scan('src/a.tsx', "import { Old } from '@rookcom/monarch-react';\n<Old />\n", [
      { name: 'Old', package: '@rookcom/monarch-react', since: '0.14.0', replacement: 'New' },
    ]);
    const f = found.find((x) => x.rule === 'api/deprecated');
    expect(f?.fix?.safety).toBe('review');
    expect(f?.fix?.edits[0]?.text).toBe('New');
  });
});

describe('monarch check --fix', () => {
  it('adds a missing subpath stylesheet next to the existing Monarch CSS imports', () => {
    const root = tempDir();
    write(
      root,
      'src/app/layout.tsx',
      "import '@rookcom/monarch-tokens/css';\nimport '@rookcom/monarch-react/styles/core.css';\nexport default function L() { return null; }\n",
    );
    write(
      root,
      'src/app/page.tsx',
      "import { AiThread } from '@rookcom/monarch-react/ai';\nexport default () => <AiThread messages={[]} />;\n",
    );
    const before = check({ root, include: ['src'] });
    const finding = before.findings.find((f) => f.rule === 'styles/subpath-css');
    expect(finding?.fix?.edits[0]?.file).toBe('src/app/layout.tsx');
    const summary = applyFixes(root, before.findings);
    expect(summary.files).toEqual(['src/app/layout.tsx']);
    expect(readFileSync(join(root, 'src/app/layout.tsx'), 'utf8')).toContain(
      "import '@rookcom/monarch-react/styles/core.css';\nimport '@rookcom/monarch-react/styles/ai.css';\n",
    );
    const after = check({ root, include: ['src'] });
    expect(after.findings.some((f) => f.rule === 'styles/subpath-css')).toBe(false);
  });

  it('applies review fixes only when asked', () => {
    const root = tempDir();
    const [hex, token] = someToken;
    write(root, 'src/a.css', `.x { color: ${hex}; }\n`);
    const r = check({ root, include: ['src'] });
    const fix = r.findings.find((f) => f.rule === 'color/no-hard-coded-color')?.fix;
    expect(fix).toBeDefined();
    const safe = applyFixes(root, r.findings);
    const expected = `.x { color: var(${token.variable}); }\n`;
    if (fix?.safety === 'review') {
      expect(safe.skipped).toBe(1);
      expect(readFileSync(join(root, 'src/a.css'), 'utf8')).not.toBe(expected);
      applyFixes(root, r.findings, { all: true });
    }
    expect(readFileSync(join(root, 'src/a.css'), 'utf8')).toBe(expected);
  });

  it('JSON findings carry help, docs and fix', () => {
    const root = tempDir();
    write(root, 'src/a.ts', "import '@rookcom/monarch-react/dist/styles.css';\n");
    const json = JSON.parse(toJson(check({ root, include: ['src'] }))) as {
      findings: { rule: string; help: string; docs: string; fix?: unknown }[];
    };
    const f = json.findings.find((x) => x.rule === 'imports/package-path');
    expect(f?.help).toContain('public subpath');
    expect(f?.docs).toBe('https://monarchui.dev/conformance/');
    expect(f?.fix).toBeDefined();
  });
});
