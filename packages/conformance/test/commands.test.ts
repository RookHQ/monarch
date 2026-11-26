import { execFileSync } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { exampleOf, planAdd, writeCss } from '../src/add.js';
import { main, parseArgs, type Io } from '../src/commands.js';
import { initAgents } from '../src/init-agents.js';
import { loadManifest, type ManifestExport } from '../src/manifest.js';
import { MONARCH_COMPONENTS } from '../src/monarch-components.js';
import { search } from '../src/search.js';
import { here, tempDir, write } from './helpers.js';

const MANIFEST = join(here, 'fixtures/manifest/manifest.json');
const { manifest } = loadManifest(tempDir(), MANIFEST);

function run(argv: string[]): Promise<{ code: number; out: string; err: string }> {
  let out = '';
  let err = '';
  const io: Io = { out: (s) => (out += s), err: (s) => (err += s) };
  return main(argv, io).then((code) => ({ code, out, err }));
}

describe('arguments', () => {
  it('never reads the next word as the value of a boolean flag', () => {
    expect(parseArgs(['check', '--fix', '--root', 'app']).flags).toEqual({
      fix: true,
      root: 'app',
    });
    expect(parseArgs(['add', 'Select', '--write']).positional).toEqual(['add', 'Select']);
    expect(parseArgs(['check', '--fix=all']).flags.fix).toBe('all');
  });
});

describe('monarch check without a registry', () => {
  it('runs with no product and checks versions against the bundled compatibility data', async () => {
    const root = tempDir();
    write(
      root,
      'package.json',
      JSON.stringify({ dependencies: { '@rookcom/monarch-react': '0.1.0' } }),
    );
    write(root, 'src/a.ts', 'export const a = 1;\n');
    const r = await run(['check', '--root', root, '--format', 'json']);
    const json = JSON.parse(r.out) as {
      product: null;
      findings: { rule: string; message: string }[];
    };
    expect(json.product).toBeNull();
    expect(json.findings.find((f) => f.rule === 'deps/monarch-version')?.message).toContain(
      'outside the supported range',
    );
    expect(r.code).toBe(1);
  });

  it('accepts a known product and warns about an unknown one', async () => {
    const root = tempDir();
    write(root, 'src/a.ts', 'export const a = 1;\n');
    const known = await run(['check', '--root', root, '--product', 'app', '--format', 'json']);
    expect(JSON.parse(known.out).product).toBe('app');
    expect(known.err).toBe('');
    const unknown = await run(['check', '--root', root, '--product', 'acme']);
    expect(unknown.code).toBe(0);
    expect(unknown.err).toContain('"acme" is not a registered Monarch product');
  });

  it('takes --accent for teams outside Rook and rejects a bad colour', async () => {
    const root = tempDir();
    write(root, 'src/a.ts', 'export const a = 1;\n');
    const ok = await run(['check', '--root', root, '--accent', '#3355aa']);
    expect(ok.code).toBe(0);
    expect(ok.err).toContain("deriveTheme('#3355aa')");
    const bad = await run(['check', '--root', root, '--accent', 'blue']);
    expect(bad.code).toBe(2);
  });

  it('--fix rewrites deep imports and reports what it did', async () => {
    const root = tempDir();
    write(root, 'src/a.ts', "import '@rookcom/monarch-tokens/dist/css/monarch.css';\n");
    const r = await run(['check', '--root', root, '--fix']);
    expect(r.err).toContain('applied 1 fix(es) in 1 file(s)');
    expect(readFileSync(join(root, 'src/a.ts'), 'utf8')).toBe(
      "import '@rookcom/monarch-tokens/css';\n",
    );
    expect(r.out).not.toContain('imports/package-path');
  });
});

describe('duplicated-primitive list', () => {
  it('is generated from the component exports, Rook-specific ones left out', () => {
    const names = MONARCH_COMPONENTS.map((c) => c.name);
    for (const n of ['Button', 'Select', 'Dialog', 'DataTable', 'ErrorState'])
      expect(names).toContain(n);
    expect(names.length).toBeGreaterThan(38);
    expect(names.some((n) => /Rook/.test(n))).toBe(false);
  });
});

describe('monarch search', () => {
  it('ranks the exact component first and Rook-specific exports last', () => {
    expect(search(manifest, 'select')[0]?.export.name).toBe('Select');
    const signIn = search(manifest, 'sign in').map((h) => h.export.name);
    expect(signIn).toEqual(['SignInForm', 'SignInWithRookButton']);
    expect(search(manifest, 'sign in', { generalOnly: true }).map((h) => h.export.name)).toEqual([
      'SignInForm',
    ]);
    expect(search(manifest, 'confirm', { componentsOnly: true })).toEqual([]);
    expect(search(manifest, 'nothing matches this')).toEqual([]);
  });

  it('prints results and fails when there are none', async () => {
    const r = await run(['search', 'select', '--manifest', MANIFEST]);
    expect(r.code).toBe(0);
    expect(r.out.split('\n')[0]).toMatch(/^Select\s/);
    expect((await run(['search', 'zzz', '--manifest', MANIFEST])).code).toBe(1);
    expect((await run(['search', '--manifest', MANIFEST])).code).toBe(2);
  });
});

describe('monarch add', () => {
  it('plans the import, stylesheets, example and related exports', () => {
    const plan = planAdd(manifest, 'Select');
    expect(plan.import).toBe("import { Select } from '@rookcom/monarch-react';");
    expect(plan.css).toContain('@rookcom/monarch-react/styles/core.css');
    expect(plan.siblings).toEqual(['SelectItem']);
    expect(plan.docs).toBe('https://monarchui.dev/components/select/');
    expect(
      exampleOf(manifest.components.find((c) => c.name === 'SelectItem') as ManifestExport),
    ).toBe('<SelectItem value="value">…</SelectItem>');
  });

  it('derives import and CSS for manifests from before 0.14.4', () => {
    const plan = planAdd(manifest, 'AiThread');
    expect(plan.import).toBe("import { AiThread } from '@rookcom/monarch-react/ai';");
    expect(plan.css).toContain('@rookcom/monarch-react/styles/ai.css');
    expect(plan.example).toBe('<AiThread messages={[]} />');
  });

  it('notes Rook-specific exports and fails for unknown names', async () => {
    expect(planAdd(manifest, 'SignInWithRookButton').notes.join(' ')).toContain('Rook-specific');
    expect((await run(['add', 'Nope', '--manifest', MANIFEST])).code).toBe(1);
  });

  it('--write adds only the missing sheets after the existing Monarch imports', () => {
    const root = tempDir();
    write(
      root,
      'app/globals.css',
      "@import '@rookcom/monarch-tokens/css';\n@import '@rookcom/monarch-react/styles/core.css';\n",
    );
    const result = writeCss(root, planAdd(manifest, 'SignInForm').css);
    expect(result.added).toEqual(['@rookcom/monarch-react/styles/patterns.css']);
    expect(readFileSync(join(root, 'app/globals.css'), 'utf8')).toBe(
      "@import '@rookcom/monarch-tokens/css';\n@import '@rookcom/monarch-react/styles/core.css';\n@import '@rookcom/monarch-react/styles/patterns.css';\n",
    );
    expect(writeCss(root, planAdd(manifest, 'SignInForm').css).file).toBeNull();
  });

  it('--write explains what to add when no file imports Monarch CSS', () => {
    const root = tempDir();
    write(root, 'src/a.ts', 'export {};\n');
    const result = writeCss(root, ['@rookcom/monarch-tokens/css']);
    expect(result.file).toBeNull();
    expect(result.message).toContain("import '@rookcom/monarch-tokens/css';");
  });
});

describe('monarch init-agents', () => {
  it('writes the agent templates and keeps existing files', () => {
    const root = tempDir();
    write(root, 'AGENTS.md', 'mine\n');
    const r = initAgents(root);
    expect(r.skipped).toEqual(['AGENTS.md']);
    expect(r.written).toEqual(
      expect.arrayContaining([
        'CLAUDE.md',
        '.cursor/rules/monarch.mdc',
        '.github/copilot-instructions.md',
      ]),
    );
    expect(readFileSync(join(root, 'AGENTS.md'), 'utf8')).toBe('mine\n');
    expect(initAgents(root, { force: true }).written).toContain('AGENTS.md');
    expect(existsSync(join(root, '.cursor/rules/monarch.mdc'))).toBe(true);
  });
});

describe('bundled-data.ts', () => {
  it('matches the registry (run `npm run registry:conformance`)', () => {
    const script = join(here, '../../../scripts/registry/build-conformance-data.mjs');
    expect(() =>
      execFileSync(process.execPath, [script, '--check'], { stdio: 'pipe' }),
    ).not.toThrow();
  });
});
