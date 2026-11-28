import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';
import { BUNDLED_IMPORT_PATH_DEPRECATIONS } from '../src/bundled-data.js';
import { check } from '../src/check.js';
import { applyEdits, applyFixes, withFile } from '../src/fixes.js';
import {
  isMovedName,
  movedImports,
  readImportList,
  scanImportPaths,
  splitImport,
} from '../src/import-paths.js';
import { loadImportPathDeprecations } from '../src/registry.js';
import type { ImportPathDeprecation } from '../src/types.js';

const SHELL: ImportPathDeprecation = {
  names: ['RookRail', 'ROOK_RAIL_COOKIE'],
  pattern: 'Rook|^rook|^ROOK_',
  from: '@rookcom/monarch-react/shell',
  to: '@rookcom/monarch-react/rook',
  since: '0.14.5',
  removeNoEarlierThan: '2027-02-11',
};

/** Applies the fixes of every finding to the text, as `monarch check --fix` does. */
function fixed(text: string): string {
  const edits = scanImportPaths(text, [SHELL]).flatMap((f) =>
    f.fix ? withFile(f.fix, 'a.tsx').edits : [],
  );
  return applyEdits(text, edits);
}

describe('moved exports', () => {
  it('matches listed names and names the pattern covers, such as types', () => {
    expect(isMovedName(SHELL, 'RookRail')).toBe(true);
    expect(isMovedName(SHELL, 'RookRailProps')).toBe(true);
    expect(isMovedName(SHELL, 'AppShell')).toBe(false);
    expect(isMovedName({ ...SHELL, pattern: '(' }, 'AppShell')).toBe(false);
    expect(isMovedName({ ...SHELL, pattern: '(' }, 'RookRail')).toBe(true);
  });

  it('reads an import list with types and aliases', () => {
    expect(readImportList('A, type B,\n  C as D, ').map((n) => [n.name, n.text])).toEqual([
      ['A', 'A'],
      ['B', 'type B'],
      ['C', 'C as D'],
    ]);
  });

  it('separates moved names from the ones that stay', () => {
    const hit = movedImports([SHELL], SHELL.from, readImportList('AppShell, RookRail'));
    expect(hit?.moved.map((n) => n.name)).toEqual(['RookRail']);
    expect(hit?.kept.map((n) => n.name)).toEqual(['AppShell']);
    expect(movedImports([SHELL], SHELL.from, readImportList('AppShell'))).toBeNull();
    expect(movedImports([SHELL], SHELL.to, readImportList('RookRail'))).toBeNull();
  });

  it('rebuilds an import for the kept and the moved names', () => {
    const parts = { keyword: 'import', quote: "'", semicolon: ';', from: 'a', to: 'b' };
    expect(splitImport(parts, [], readImportList('X'))).toBe("import { X } from 'b';");
    expect(splitImport(parts, readImportList('K'), readImportList('X as Y'))).toBe(
      "import { K } from 'a';\nimport { X as Y } from 'b';",
    );
  });
});

describe('scanImportPaths', () => {
  it('changes the module when every imported name moved', () => {
    const text = `import { RookRail, type RookRailProps } from '@rookcom/monarch-react/shell';\n`;
    const [f] = scanImportPaths(text, [SHELL]);
    expect(f).toMatchObject({ line: 1, column: 1 });
    expect(f?.message).toContain('`RookRail`, `RookRailProps` have moved');
    expect(f?.message).toContain('no earlier than 2027-02-11');
    expect(f?.fix?.safety).toBe('safe');
    expect(fixed(text)).toBe(
      `import { RookRail, type RookRailProps } from '@rookcom/monarch-react/rook';\n`,
    );
  });

  it('changes only the module of a statement written over several lines', () => {
    const text = `import {\n  RookRail,\n  ROOK_RAIL_COOKIE,\n} from "@rookcom/monarch-react/shell";\n`;
    expect(fixed(text)).toBe(
      `import {\n  RookRail,\n  ROOK_RAIL_COOKIE,\n} from "@rookcom/monarch-react/rook";\n`,
    );
  });

  it('splits a one-line statement when only some names moved', () => {
    const text = `  import { AppShell, RookRail as Rail } from '@rookcom/monarch-react/shell'\nconst x = 1;\n`;
    expect(fixed(text)).toBe(
      `  import { AppShell } from '@rookcom/monarch-react/shell'\n  import { RookRail as Rail } from '@rookcom/monarch-react/rook'\nconst x = 1;\n`,
    );
  });

  it('reports a mixed statement over several lines without a fix', () => {
    const text = `import {\n  AppShell,\n  RookRail,\n} from '@rookcom/monarch-react/shell';\n`;
    const findings = scanImportPaths(text, [SHELL]);
    expect(findings).toHaveLength(1);
    expect(findings[0]?.fix).toBeUndefined();
  });

  it('handles type-only imports and re-exports', () => {
    expect(fixed(`import type { RookRailProps } from '@rookcom/monarch-react/shell';`)).toBe(
      `import type { RookRailProps } from '@rookcom/monarch-react/rook';`,
    );
    expect(fixed(`export { RookRail } from '@rookcom/monarch-react/shell';`)).toBe(
      `export { RookRail } from '@rookcom/monarch-react/rook';`,
    );
  });

  it('leaves other modules, other names and the new path alone', () => {
    for (const text of [
      `import { AppShell } from '@rookcom/monarch-react/shell';`,
      `import { RookRail } from '@rookcom/monarch-react/rook';`,
      `import { RookRail } from './local/shell';`,
    ])
      expect(scanImportPaths(text, [SHELL])).toEqual([]);
    expect(scanImportPaths(`import { RookRail } from '${SHELL.from}';`, [])).toEqual([]);
  });
});

describe('bundled import-path deprecations', () => {
  it('send every Rook-specific export to /rook, with removal no earlier than 2027-02-11', () => {
    expect(BUNDLED_IMPORT_PATH_DEPRECATIONS.map((d) => d.from).sort()).toEqual([
      '@rookcom/monarch-react/ai',
      '@rookcom/monarch-react/patterns',
      '@rookcom/monarch-react/shell',
    ]);
    for (const d of BUNDLED_IMPORT_PATH_DEPRECATIONS) {
      expect(d.to).toBe('@rookcom/monarch-react/rook');
      expect(d.removeNoEarlierThan).toBe('2027-02-11');
      for (const n of d.names) expect(n).toMatch(/Rook|^rook|^ROOK_/);
    }
    expect(loadImportPathDeprecations()).toEqual([...BUNDLED_IMPORT_PATH_DEPRECATIONS]);
  });
});

describe('api/deprecated for moved exports', () => {
  let root = '';
  afterEach(() => rmSync(root, { recursive: true, force: true }));

  it('reports the old path as a warning and --fix rewrites it', () => {
    root = mkdtempSync(join(tmpdir(), 'monarch-moved-'));
    const file = join(root, 'nav.tsx');
    writeFileSync(
      file,
      [
        `import '@rookcom/monarch-react/styles.css';`,
        `import { AppShell, RookRail } from '@rookcom/monarch-react/shell';`,
        `import { SignInWithRookButton } from '@rookcom/monarch-react/patterns';`,
        `import { mockTransport, rookIntelligenceTransport } from '@rookcom/monarch-react/ai';`,
        `// import { RookBar } from '@rookcom/monarch-react/shell';`,
        `export const x = [AppShell, RookRail, SignInWithRookButton, mockTransport, rookIntelligenceTransport];`,
        '',
      ].join('\n'),
    );
    const before = check({ root }).findings.filter((f) => f.rule === 'api/deprecated');
    expect(before.map((f) => [f.line, f.severity, f.fix?.safety])).toEqual([
      [2, 'warn', 'safe'],
      [3, 'warn', 'safe'],
      [4, 'warn', 'safe'],
    ]);
    expect(applyFixes(root, before).applied).toBe(3);
    const text = readFileSync(file, 'utf8');
    expect(text).toContain(`import { AppShell } from '@rookcom/monarch-react/shell';`);
    expect(text).toContain(`import { RookRail } from '@rookcom/monarch-react/rook';`);
    expect(text).toContain(`import { SignInWithRookButton } from '@rookcom/monarch-react/rook';`);
    expect(text).toContain(
      `import { rookIntelligenceTransport } from '@rookcom/monarch-react/rook';`,
    );
    const after = check({ root });
    expect(after.findings.filter((f) => f.rule === 'api/deprecated')).toEqual([]);
  });

  it('does not ask for styles/rook.css when the owning entry sheet is loaded', () => {
    root = mkdtempSync(join(tmpdir(), 'monarch-moved-'));
    writeFileSync(
      join(root, 'layout.tsx'),
      [
        `import '@rookcom/monarch-react/styles/core.css';`,
        `import '@rookcom/monarch-react/styles/shell.css';`,
        `import { RookRail } from '@rookcom/monarch-react/rook';`,
        `export const x = RookRail;`,
        '',
      ].join('\n'),
    );
    expect(check({ root }).findings.filter((f) => f.rule === 'styles/subpath-css')).toEqual([]);
    writeFileSync(
      join(root, 'layout.tsx'),
      [
        `import '@rookcom/monarch-react/styles/core.css';`,
        `import { RookRail } from '@rookcom/monarch-react/rook';`,
        `export const x = RookRail;`,
        '',
      ].join('\n'),
    );
    const [f] = check({ root }).findings.filter((x) => x.rule === 'styles/subpath-css');
    expect(f?.message).toContain('styles/rook.css');
  });

  it('can be turned off with an empty list', () => {
    root = mkdtempSync(join(tmpdir(), 'monarch-moved-'));
    writeFileSync(
      join(root, 'a.tsx'),
      `import '@rookcom/monarch-react/styles.css';\nimport { RookRail } from '@rookcom/monarch-react/shell';\nexport const x = RookRail;\n`,
    );
    expect(check({ root, importPaths: [] }).findings).toEqual([]);
  });
});
