import { cpSync, existsSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import {
  classifyAdoption,
  classifyCompatibility,
  statusMarkdown,
  validateRegistry,
  verifyProduct,
} from '../src/registry.js';
import { validate, type Schema } from '../src/schema.js';
import { REGISTRY, SAMPLE_APP, copySample, tempDir, write } from './helpers.js';

const compat = JSON.parse(readFileSync(join(REGISTRY, 'compatibility.json'), 'utf8'));

function tempRegistry(): string {
  const dir = tempDir();
  cpSync(join(REGISTRY, 'product.schema.json'), join(dir, 'product.schema.json'));
  cpSync(join(REGISTRY, 'compatibility.json'), join(dir, 'compatibility.json'));
  cpSync(join(REGISTRY, 'deprecations.json'), join(dir, 'deprecations.json'));
  write(
    dir,
    'products/sample.json',
    JSON.stringify({
      id: 'sample',
      name: 'Sample',
      kind: 'application',
      repository: 'RookHQ/sample',
      domain: 'sample.rookhq.com',
      theme: { accent: '354 71% 52%', schemes: ['light'] },
      source: { paths: ['src'] },
    }),
  );
  return dir;
}

describe('registry validate', () => {
  it('accepts the real registry', () => {
    const r = validateRegistry(REGISTRY);
    expect(r.errors).toEqual([]);
    expect(r.products).toBeGreaterThanOrEqual(19);
  });

  it('reports schema, naming and uniqueness problems', () => {
    const dir = tempRegistry();
    write(
      dir,
      'products/dupe.json',
      JSON.stringify({
        id: 'other',
        name: 'X',
        kind: 'app',
        repository: 'RookHQ/sample',
        domain: 'sample.rookhq.com',
        theme: { accent: 'red', schemes: [] },
        source: { paths: ['src'] },
        extra: 1,
        exceptions: [
          { rule: 'color/*', paths: ['src'], owner: 'x', reason: 'short', expires: '2026-02-30' },
        ],
      }),
    );
    const errors = validateRegistry(dir).errors.join('\n');
    for (const e of [
      'kind: must be one of',
      'theme.accent: does not match',
      'theme.schemes: needs at least 1',
      'extra: unknown property',
      'exceptions[0].reason: shorter than 10',
      'exceptions[0].expires: not a valid YYYY-MM-DD date',
      'id "other" must match the file name',
      'repository "RookHQ/sample" already used by',
      'domain "sample.rookhq.com" already used by',
    ])
      expect(errors).toContain(e);
  });

  it('rejects schemas using keywords the validator does not implement', () => {
    const dir = tempRegistry();
    const schema = JSON.parse(readFileSync(join(dir, 'product.schema.json'), 'utf8'));
    schema.properties.id.maxLength = 3;
    writeFileSync(join(dir, 'product.schema.json'), JSON.stringify(schema));
    expect(() => validateRegistry(dir)).toThrow(/Unsupported schema keyword/);
  });

  it('validates types', () => {
    expect(
      validate({ a: 1 }, { type: 'object', properties: { a: { type: 'string' } } } as Schema),
    ).toEqual(['a: expected string, got integer']);
  });
});

describe('compatibility classification', () => {
  it('accepts the common Rook stack', () => {
    expect(
      classifyCompatibility({ next: '^15.2.0', react: '^19.0.0', tailwindcss: '^3.4.0' }, compat)
        .status,
    ).toBe('compatible');
    expect(
      classifyCompatibility({ next: '^16.2.2', react: '^18.3.1', tailwindcss: '^3.4.17' }, compat)
        .status,
    ).toBe('compatible');
  });
  it('flags Tailwind 4 with a migration note', () => {
    const c = classifyCompatibility({ next: '16.4.2', react: '19.3.0', tailwindcss: '^4' }, compat);
    expect(c.status).toBe('needs-migration');
    expect(c.notes.join(' ')).toContain('CSS-only path');
  });
  it('flags old React and unknown specs', () => {
    expect(classifyCompatibility({ react: '^17.0.2' }, compat).status).toBe('needs-migration');
    expect(classifyCompatibility({ react: 'latest' }, compat).status).toBe('unknown');
    expect(classifyCompatibility({}, compat).status).toBe('unknown');
  });
});

describe('adoption classification', () => {
  const counts = (byRule: Record<string, number>) => ({
    counts: {
      byRule,
      bySeverity: { error: 0, warn: 0, info: 0 },
      new: { error: 0, warn: 0, info: 0 },
    },
  });
  it('not-adopted without a dependency', () => {
    expect(classifyAdoption({}, { ...counts({}), monarchCssImports: ['x.css'] }).status).toBe(
      'not-adopted',
    );
  });
  it('partial while CSS is missing or local copies remain', () => {
    expect(
      classifyAdoption(
        { '@rookcom/monarch-tokens': '0.1.0' },
        { ...counts({}), monarchCssImports: [] },
      ).status,
    ).toBe('partial');
    expect(
      classifyAdoption(
        { '@rookcom/monarch-tokens': '0.1.0' },
        { ...counts({ 'tokens/local-token-definitions': 1 }), monarchCssImports: ['g.css'] },
      ).status,
    ).toBe('partial');
    expect(
      classifyAdoption(
        { '@rookcom/monarch-tokens': '0.1.0' },
        { ...counts({ 'components/duplicated-primitive': 2 }), monarchCssImports: ['g.css'] },
      ).status,
    ).toBe('partial');
  });
  it('adopted when all three hold', () => {
    expect(
      classifyAdoption(
        { '@rookcom/monarch-tokens': '0.1.0' },
        { ...counts({ 'color/no-raw-palette': 9 }), monarchCssImports: ['g.css'] },
      ).status,
    ).toBe('adopted');
  });
});

describe('registry verify and report', () => {
  it('writes a status file for the sample app and renders it', () => {
    const reg = tempRegistry();
    const { status } = verifyProduct({
      registry: reg,
      productId: 'sample',
      checkout: SAMPLE_APP,
      now: new Date('2026-11-03T00:00:00Z'),
    });
    expect(status.adoption.status).toBe('not-adopted');
    expect(status.compatibility.status).toBe('compatible');
    expect(status.framework.name).toBe('next');
    expect(status.conformance.filesScanned).toBe(4);
    expect(existsSync(join(reg, 'status/sample.json'))).toBe(true);
    const md = statusMarkdown(reg);
    expect(md).toContain('| Sample | sample.rookhq.com | next');
    expect(md).toContain('not-adopted');
    expect(md).toMatch(/static scan/);
    rmSync(reg, { recursive: true, force: true });
  });

  it('detects an adopted app', () => {
    const reg = tempRegistry();
    const app = copySample();
    rmSync(join(app, 'src/components/ui/button.tsx'));
    write(
      app,
      'src/styles/globals.css',
      `@import '@rookcom/monarch-tokens/css';\n@import '@rookcom/monarch-tokens/css/products/sample';\n`,
    );
    write(
      app,
      'package.json',
      JSON.stringify({
        dependencies: { next: '^15.2.0', react: '^19.0.0', '@rookcom/monarch-tokens': '0.1.0' },
      }),
    );
    const { status } = verifyProduct({
      registry: reg,
      productId: 'sample',
      checkout: app,
      write: false,
    });
    expect(status.adoption.status).toBe('adopted');
    expect(status.monarch.packages).toEqual({ '@rookcom/monarch-tokens': '0.1.0' });
  });
});

describe('compatibility matrix', () => {
  it('tracks the current version of every Monarch package', async () => {
    const { readFileSync } = await import('node:fs');
    const root = new URL('../../../', import.meta.url);
    const compat = JSON.parse(readFileSync(new URL('registry/compatibility.json', root), 'utf8'));
    for (const name of ['tokens', 'react', 'conformance']) {
      const pkg = JSON.parse(readFileSync(new URL(`packages/${name}/package.json`, root), 'utf8'));
      expect(compat.packages[pkg.name]?.current, `${pkg.name} current`).toBe(pkg.version);
    }
  });
});
