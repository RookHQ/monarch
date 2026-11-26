import { describe, expect, it } from 'vitest';
import { check } from '../src/check.js';
import { RULES_DOCS_URL, ruleDocsUrl, sarifFix, toSarif } from '../src/report.js';
import { RULE_IDS } from '../src/rules.js';
import type { Fix } from '../src/types.js';
import { tempDir, write } from './helpers.js';

interface SarifRegion {
  startLine: number;
  startColumn: number;
  endLine: number;
  endColumn: number;
}
interface SarifResult {
  ruleId: string;
  fixes?: {
    description: { text: string };
    artifactChanges: {
      artifactLocation: { uri: string };
      replacements: { deletedRegion: SarifRegion; insertedContent: { text: string } }[];
    }[];
  }[];
}
interface SarifLog {
  $schema: string;
  version: string;
  runs: {
    tool: { driver: { rules: { id: string; helpUri: string }[] } };
    results: SarifResult[];
  }[];
}

/** The parts of the SARIF 2.1.0 schema a `fix` object must satisfy (§3.55 to §3.57). */
function expectValidFix(fix: NonNullable<SarifResult['fixes']>[number]): void {
  expect(Object.keys(fix).sort()).toEqual(['artifactChanges', 'description']);
  expect(typeof fix.description.text).toBe('string');
  expect(fix.artifactChanges.length).toBeGreaterThan(0); // schema: minItems 1
  for (const change of fix.artifactChanges) {
    expect(Object.keys(change).sort()).toEqual(['artifactLocation', 'replacements']);
    expect(change.artifactLocation.uri).not.toMatch(/^\/|\\/); // relative, forward slashes
    expect(change.replacements.length).toBeGreaterThan(0); // schema: minItems 1
    for (const r of change.replacements) {
      expect(Object.keys(r).sort()).toEqual(['deletedRegion', 'insertedContent']);
      const d = r.deletedRegion;
      for (const n of [d.startLine, d.startColumn, d.endLine, d.endColumn]) {
        expect(Number.isInteger(n)).toBe(true);
        expect(n).toBeGreaterThanOrEqual(1); // schema: minimum 1
      }
      expect(d.endLine).toBeGreaterThanOrEqual(d.startLine);
      expect(d.endColumn).toBeGreaterThanOrEqual(d.startColumn);
      expect(typeof r.insertedContent.text).toBe('string');
    }
  }
}

describe('sarifFix', () => {
  it('turns each edit into a replacement, grouped by file', () => {
    const fix: Fix = {
      description: 'Import the sheet',
      safety: 'safe',
      edits: [
        { file: 'a.tsx', line: 3, column: 9, length: 4, text: 'var(--x)' },
        { file: 'b.css', line: 1, column: 1, length: 0, text: "@import 'x';\n" },
        { file: 'a.tsx', line: 7, column: 2, length: 1, text: '' },
      ],
    };
    expect(sarifFix(fix)).toEqual({
      description: { text: 'Import the sheet' },
      artifactChanges: [
        {
          artifactLocation: { uri: 'a.tsx' },
          replacements: [
            {
              deletedRegion: { startLine: 3, startColumn: 9, endLine: 3, endColumn: 13 },
              insertedContent: { text: 'var(--x)' },
            },
            {
              deletedRegion: { startLine: 7, startColumn: 2, endLine: 7, endColumn: 3 },
              insertedContent: { text: '' },
            },
          ],
        },
        {
          artifactLocation: { uri: 'b.css' },
          replacements: [
            {
              // An insertion deletes nothing: an empty region.
              deletedRegion: { startLine: 1, startColumn: 1, endLine: 1, endColumn: 1 },
              insertedContent: { text: "@import 'x';\n" },
            },
          ],
        },
      ],
    });
  });

  it('says when a fix needs review', () => {
    const fix: Fix = { description: 'Rename', safety: 'review', edits: [] };
    expect(sarifFix(fix).description.text).toBe('Rename (review before applying)');
  });
});

describe('toSarif', () => {
  const root = tempDir();
  write(
    root,
    'src/app.tsx',
    [
      `import '@rookcom/monarch-tokens/dist/css/monarch.css';`,
      `import '@rookcom/monarch-react/styles/core.css';`,
      `import { AiThread } from '@rookcom/monarch-react/ai';`,
      `import { RookRail } from '@rookcom/monarch-react/shell';`,
      `export const a = <div className="bg-red-500"><AiThread /><RookRail /><img src="x.png" /></div>;`,
      '',
    ].join('\n'),
  );
  const result = check({ root });
  const log = JSON.parse(toSarif(result)) as SarifLog;
  const run = log.runs[0];

  it('is a SARIF 2.1.0 log', () => {
    expect(log.version).toBe('2.1.0');
    expect(log.$schema).toContain('sarif-2.1.0');
    expect(run?.results).toHaveLength(result.findings.length);
  });

  it('gives every finding that has a machine fix a valid fixes array, and no other', () => {
    const withFix = result.findings.filter((f) => f.fix);
    expect([...new Set(withFix.map((f) => f.rule))].sort()).toEqual([
      'api/deprecated',
      'imports/package-path',
      'styles/subpath-css',
    ]);
    result.findings.forEach((f, i) => {
      const fixes = run?.results[i]?.fixes;
      if (!f.fix) {
        expect(fixes).toBeUndefined();
        return;
      }
      expect(fixes).toHaveLength(1);
      const fix = fixes?.[0];
      if (!fix) throw new Error('missing fix');
      expectValidFix(fix);
      expect(fix.artifactChanges.flatMap((c) => c.replacements)).toHaveLength(f.fix.edits.length);
    });
  });

  it('writes the replacement text of the fix', () => {
    const deep = run?.results.find((r) => r.ruleId === 'imports/package-path');
    expect(deep?.fixes?.[0]?.artifactChanges[0]).toEqual({
      artifactLocation: { uri: 'src/app.tsx' },
      replacements: [
        {
          deletedRegion: { startLine: 1, startColumn: 9, endLine: 1, endColumn: 53 },
          insertedContent: { text: '@rookcom/monarch-tokens/css' },
        },
      ],
    });
  });

  it('links each rule to its own place in the docs', () => {
    const rules = run?.tool.driver.rules ?? [];
    expect(rules.length).toBeGreaterThan(2);
    for (const rule of rules) {
      expect(rule.helpUri).toBe(ruleDocsUrl(rule.id));
      expect(rule.helpUri.startsWith(RULES_DOCS_URL)).toBe(true);
      expect(() => new URL(rule.helpUri)).not.toThrow();
    }
    expect(new Set(rules.map((r) => r.helpUri)).size).toBe(rules.length);
    expect(new Set(RULE_IDS.map(ruleDocsUrl)).size).toBe(RULE_IDS.length);
    expect(ruleDocsUrl('color/no-raw-palette')).toBe(
      'https://monarchui.dev/conformance/#rules:~:text=color%2Fno-raw-palette',
    );
  });
});
