import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { check, makeBaseline } from '../src/check.js';
import { toSarif, toText } from '../src/report.js';
import type { Compatibility } from '../src/types.js';
import { VERSION } from '../src/version.js';
import { SAMPLE_APP, copySample, tempDir, write } from './helpers.js';

const compat: Compatibility = {
  packages: {
    '@rookcom/monarch-tokens': {
      current: '0.3.0',
      supported: '>=0.1.0 <0.4.0',
      deprecated: [{ range: '<0.2.0', message: 'use 0.2+' }],
    },
  },
  peers: { react: '>=18.3 <20' },
};

describe('check on the sample app', () => {
  const r = check({ root: SAMPLE_APP, include: ['src'], today: '2026-11-03' });
  const at = (rule: string) => r.findings.filter((f) => f.rule === rule);

  it('finds one of each seeded problem', () => {
    expect(r.counts.byRule).toMatchObject({
      'components/duplicated-primitive': 1,
      'tokens/local-token-definitions': 2,
      'a11y/img-alt': 1,
      'a11y/click-without-key': 1,
      'a11y/positive-tabindex': 1,
      'a11y/outline-none-without-ring': 1,
      'governance/suppression-without-reason': 1,
      'legacy/bridge-variable': 1,
    });
    // app-local --app-local: #123456 and .card color: #ff0000; the token block's values are not hex.
    expect(at('color/no-hard-coded-color').map((f) => f.line)).toEqual([5, 13]);
    expect(at('typography/no-arbitrary-type')).toHaveLength(2);
    expect(at('spacing/off-grid').map((f) => f.message)).toEqual([
      'Off-grid spacing `p-[7px]`',
      'Off-grid padding: `7px 8px`',
    ]);
  });

  it('honours a reasoned suppression and ignores an unreasoned one', () => {
    const palette = at('color/no-raw-palette').map((f) => f.snippet);
    expect(palette.some((s) => s.includes('bg-blue-600'))).toBe(false);
    expect(palette.some((s) => s.includes('bg-green-600'))).toBe(true);
    expect(r.suppressed.inline).toBe(1);
  });

  it('fails on error-severity findings and reports files and lines', () => {
    expect(r.exitCode).toBe(1);
    expect(r.filesScanned).toBe(4);
    expect(r.linesScanned).toBeGreaterThan(30);
    expect(r.tool.version).toBe(VERSION);
  });

  it('applies rule settings from config', () => {
    const off = check({
      root: SAMPLE_APP,
      include: ['src'],
      rules: { 'color/no-raw-palette': 'off', 'a11y/img-alt': 'info' },
    });
    expect(off.counts.byRule['color/no-raw-palette']).toBeUndefined();
    expect(off.findings.find((f) => f.rule === 'a11y/img-alt')?.severity).toBe('info');
  });

  it('applies exclude globs', () => {
    const ex = check({ root: SAMPLE_APP, include: ['src'], exclude: ['src/styles/**'] });
    expect(ex.filesScanned).toBe(3);
  });
});

describe('exceptions', () => {
  const base = { owner: 'team-tickets', reason: 'Partner logos need exact brand colours' };
  it('active exceptions suppress and are listed; expired ones are errors', () => {
    const r = check({
      root: SAMPLE_APP,
      include: ['src'],
      today: '2026-11-03',
      exceptions: [
        { ...base, rule: 'color/*', paths: ['src/styles/**'], expires: '2027-01-01' },
        { ...base, rule: 'a11y/img-alt', paths: ['src/app/page.tsx'], expires: '2026-10-01' },
      ],
    });
    expect(r.counts.byRule['color/no-hard-coded-color']).toBeUndefined();
    expect(r.exceptions.map((e) => [e.status, e.suppressed])).toEqual([
      ['active', 2],
      ['expired', 0],
    ]);
    expect(r.counts.byRule['a11y/img-alt']).toBe(1);
    expect(r.findings.find((f) => f.rule === 'governance/expired-exception')?.severity).toBe(
      'error',
    );
  });
});

describe('baseline ratchet', () => {
  it('passes at baseline and fails only on new findings', () => {
    const dir = copySample();
    const first = check({ root: dir, include: ['src'] });
    const baseline = makeBaseline(first);
    const same = check({ root: dir, include: ['src'], baseline });
    expect(same.exitCode).toBe(0);
    expect(same.counts.new.error).toBe(0);
    expect(same.baseline?.covered).toBeGreaterThan(0);

    write(dir, 'src/app/new.tsx', `export const N = () => <p className="text-red-500">new</p>;\n`);
    const worse = check({ root: dir, include: ['src'], baseline });
    expect(worse.exitCode).toBe(1);
    expect(worse.counts.new.error).toBe(1);
    expect(
      worse.findings.filter((f) => !f.baselined && f.severity === 'error').map((f) => f.file),
    ).toEqual(['src/app/new.tsx']);

    write(dir, 'src/app/legacy.tsx', `export function Legacy() {\n  return <div>clean</div>;\n}\n`);
    const better = check({ root: dir, include: ['src'], baseline });
    expect(better.baseline?.improved).toBeGreaterThan(0);
  });
});

describe('deps/monarch-version', () => {
  const app = (deps: Record<string, string>) => {
    const dir = tempDir();
    write(dir, 'package.json', JSON.stringify({ dependencies: deps }, null, 2));
    write(dir, 'src/a.ts', 'export {};\n');
    return dir;
  };
  const dep = (dir: string) =>
    check({ root: dir, include: ['src'], compatibility: compat }).findings.filter(
      (f) => f.rule === 'deps/monarch-version',
    );

  it('reports not adopted as info', () => {
    const f = dep(app({ react: '^19.0.0' }));
    expect(f.map((x) => [x.severity, x.message])).toEqual([
      ['info', 'No @rookcom/monarch-* dependency: product has not adopted Monarch'],
    ]);
  });
  it('classifies supported, deprecated, unsupported and unknown packages', () => {
    expect(dep(app({ '@rookcom/monarch-tokens': '^0.3.0' }))).toEqual([]);
    expect(dep(app({ '@rookcom/monarch-tokens': '^0.2.0' }))[0]?.severity).toBe('info');
    expect(dep(app({ '@rookcom/monarch-tokens': '0.1.4' }))[0]?.severity).toBe('warn');
    expect(dep(app({ '@rookcom/monarch-tokens': '^0.5.0' }))[0]?.severity).toBe('error');
    expect(dep(app({ '@rookcom/monarch-gizmo': '1.0.0' }))[0]?.message).toContain(
      'not a known Monarch package',
    );
  });
  it('points at the dependency line', () => {
    const f = dep(app({ react: '^19.0.0', '@rookcom/monarch-tokens': '^0.5.0' }));
    expect(f[0]?.line).toBe(4);
  });
});

describe('reports', () => {
  const r = check({ root: SAMPLE_APP, include: ['src'] });

  it('SARIF 2.1.0 with rules, locations and fingerprints', () => {
    const s = JSON.parse(toSarif(r));
    expect(s.version).toBe('2.1.0');
    const run = s.runs[0];
    expect(run.tool.driver.name).toBe('monarch');
    expect(run.results).toHaveLength(r.findings.length);
    const res = run.results[0];
    expect(run.tool.driver.rules[res.ruleIndex].id).toBe(res.ruleId);
    expect(res.locations[0].physicalLocation.region.startLine).toBeGreaterThan(0);
    expect(['error', 'warning', 'note']).toContain(res.level);
    expect(res.partialFingerprints['monarch/v1']).toMatch(/^[0-9a-f]{8}$/);
  });

  it('text report has counts and densities, never a score', () => {
    const t = toText(r);
    expect(t).toContain('color/no-raw-palette');
    expect(t).toMatch(/\/1k lines/);
    expect(t).not.toMatch(/score|%/i);
    expect(t).toContain('Result: fail');
  });

  it('package version matches the reported tool version', () => {
    const pkg = JSON.parse(readFileSync(join(__dirname, '../package.json'), 'utf8')) as {
      version: string;
    };
    expect(pkg.version).toBe(VERSION);
  });
});
