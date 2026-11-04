import { execFileSync } from 'node:child_process';
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { extname, join, relative, sep } from 'node:path';
import { matchesAny } from './glob.js';
import {
  RULES,
  readSuppressions,
  ruleMatches,
  scanFile,
  type RawFinding,
  type RuleId,
} from './rules.js';
import { compare, format, minVersion, parseVersion, satisfies } from './semver.js';
import type {
  Compatibility,
  Deprecation,
  Finding,
  MonarchConfig,
  ProductException,
  RuleSetting,
  Severity,
} from './types.js';
import { TOOL_NAME, VERSION } from './version.js';

export const SCANNED_EXTENSIONS = new Set([
  '.ts',
  '.tsx',
  '.js',
  '.jsx',
  '.mjs',
  '.cjs',
  '.css',
  '.scss',
]);
export const SKIPPED_DIRS = new Set([
  'node_modules',
  '.next',
  '.git',
  'dist',
  'build',
  'out',
  'coverage',
  '.turbo',
  '.vercel',
  '.open-next',
  '.wrangler',
  'storybook-static',
  'generated',
  '__generated__',
]);
const GENERATED_FILE = /(?:\.d\.ts|\.min\.(?:js|css)|\.generated\.[a-z]+|\.gen\.[a-z]+)$/;
const MAX_FILE_BYTES = 1_000_000;
/** Tests and stories legitimately contain colour literals and odd markup. */
export const DEFAULT_EXCLUDE = [
  '**/*.test.*',
  '**/*.spec.*',
  '**/__tests__/**',
  '**/*.stories.*',
  '**/__mocks__/**',
];

export interface CheckOptions {
  readonly root: string;
  readonly include?: readonly string[];
  readonly exclude?: readonly string[];
  readonly rules?: Readonly<Record<string, RuleSetting>>;
  readonly exceptions?: readonly ProductException[];
  readonly compatibility?: Compatibility;
  readonly deprecations?: readonly Deprecation[];
  readonly baseline?: Baseline;
  readonly product?: string;
  /** Injectable clock so expiry tests are deterministic. */
  readonly today?: string;
}

export interface Baseline {
  readonly version: 1;
  readonly tool: string;
  readonly createdAt: string;
  /** file → rule → count */
  readonly counts: Readonly<Record<string, Readonly<Record<string, number>>>>;
}

export interface ExceptionReport extends ProductException {
  readonly status: 'active' | 'expired';
  readonly suppressed: number;
}

export interface CheckResult {
  readonly tool: { readonly name: string; readonly version: string };
  readonly generatedAt: string;
  readonly root: string;
  readonly product: string | null;
  readonly git: { readonly commit: string | null; readonly dirty: boolean | null };
  readonly filesScanned: number;
  readonly linesScanned: number;
  readonly findings: readonly Finding[];
  readonly counts: {
    readonly byRule: Readonly<Record<string, number>>;
    readonly bySeverity: Readonly<Record<Severity, number>>;
    /** Findings not covered by the baseline. */
    readonly new: Readonly<Record<Severity, number>>;
  };
  /** Findings per 1,000 scanned lines, by rule. A density, not a score. */
  readonly densityPer1kLines: Readonly<Record<string, number>>;
  readonly suppressed: { readonly inline: number; readonly exceptions: number };
  readonly exceptions: readonly ExceptionReport[];
  readonly baseline: null | {
    readonly createdAt: string;
    readonly covered: number;
    /** (file, rule) groups now below their baseline count: the baseline can be tightened. */
    readonly improved: number;
  };
  /** Paths that import Monarch CSS (used by registry verify). */
  readonly monarchCssImports: readonly string[];
  readonly exitCode: 0 | 1;
}

function toPosix(p: string): string {
  return p.split(sep).join('/');
}

export function walk(
  root: string,
  include: readonly string[],
  exclude: readonly string[],
): string[] {
  const files: string[] = [];
  const visit = (abs: string) => {
    let st;
    try {
      st = statSync(abs);
    } catch {
      return;
    }
    const rel = toPosix(relative(root, abs));
    if (rel && matchesAny(rel, exclude)) return;
    if (st.isDirectory()) {
      for (const name of readdirSync(abs).sort()) {
        if (SKIPPED_DIRS.has(name)) continue;
        visit(join(abs, name));
      }
    } else if (st.isFile()) {
      if (!SCANNED_EXTENSIONS.has(extname(abs))) return;
      if (GENERATED_FILE.test(abs) || st.size > MAX_FILE_BYTES) return;
      files.push(rel);
    }
  };
  for (const inc of include.length ? include : ['.']) visit(join(root, inc));
  return [...new Set(files)].sort();
}

export function gitState(root: string): { commit: string | null; dirty: boolean | null } {
  try {
    const commit = execFileSync('git', ['-C', root, 'rev-parse', 'HEAD'], {
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'ignore'],
    }).trim();
    const status = execFileSync(
      'git',
      ['-C', root, 'status', '--porcelain', '--untracked-files=no'],
      {
        encoding: 'utf8',
        stdio: ['ignore', 'pipe', 'ignore'],
        maxBuffer: 64 * 1024 * 1024,
      },
    );
    return { commit, dirty: status.trim().length > 0 };
  } catch {
    return { commit: null, dirty: null };
  }
}

export function readJson<T>(path: string): T {
  return JSON.parse(readFileSync(path, 'utf8')) as T;
}

export function loadConfig(path: string): MonarchConfig | null {
  return existsSync(path) ? readJson<MonarchConfig>(path) : null;
}

interface PackageJson {
  dependencies?: Record<string, string>;
  devDependencies?: Record<string, string>;
  peerDependencies?: Record<string, string>;
}

export function monarchDependencies(
  root: string,
): Record<string, { spec: string; installed: string | null }> {
  const pkgPath = join(root, 'package.json');
  if (!existsSync(pkgPath)) return {};
  const pkg = readJson<PackageJson>(pkgPath);
  const out: Record<string, { spec: string; installed: string | null }> = {};
  for (const [name, spec] of Object.entries({ ...pkg.devDependencies, ...pkg.dependencies })) {
    if (!name.startsWith('@rookcom/monarch-')) continue;
    let installed: string | null = null;
    const installedPkg = join(root, 'node_modules', name, 'package.json');
    if (existsSync(installedPkg))
      installed = readJson<{ version?: string }>(installedPkg).version ?? null;
    out[name] = { spec, installed };
  }
  return out;
}

function lineOf(text: string, needle: string): number {
  const idx = text.indexOf(needle);
  return idx === -1 ? 1 : text.slice(0, idx).split('\n').length;
}

/** deps/monarch-version: one finding per Monarch dependency (or one "not adopted"). */
function scanDependencies(root: string, compat: Compatibility | undefined): Finding[] {
  const pkgPath = join(root, 'package.json');
  if (!existsSync(pkgPath)) return [];
  const text = readFileSync(pkgPath, 'utf8');
  const deps = monarchDependencies(root);
  const rule = 'deps/monarch-version';
  const mk = (severity: Severity, line: number, message: string): Finding => ({
    rule,
    severity,
    file: 'package.json',
    line,
    column: 1,
    message,
    snippet: text.split('\n')[line - 1]?.trim() ?? '',
  });
  const names = Object.keys(deps);
  if (names.length === 0)
    return [mk('info', 1, 'No @rookcom/monarch-* dependency: product has not adopted Monarch')];
  const out: Finding[] = [];
  for (const name of names) {
    const { spec, installed } = deps[name] as { spec: string; installed: string | null };
    const line = lineOf(text, `"${name}"`);
    const resolved = installed ? parseVersion(installed) : minVersion(spec);
    const entry = compat?.packages[name];
    if (!compat) continue;
    if (!entry) {
      out.push(mk('error', line, `${name} is not a known Monarch package`));
      continue;
    }
    if (!resolved) {
      out.push(
        mk('warn', line, `${name}@${spec}: cannot determine version (pin an exact semver release)`),
      );
      continue;
    }
    const ver = format(resolved);
    if (!satisfies(resolved, entry.supported)) {
      out.push(
        mk('error', line, `${name}@${ver} is outside the supported range ${entry.supported}`),
      );
      continue;
    }
    const dep = entry.deprecated?.find((d) => satisfies(resolved, d.range));
    if (dep) {
      out.push(mk('warn', line, `${name}@${ver} is deprecated: ${dep.message}`));
      continue;
    }
    const current = parseVersion(entry.current);
    if (current && compare(resolved, current) < 0) {
      out.push(mk('info', line, `${name}@${ver} is supported; ${entry.current} is available`));
    }
  }
  return out;
}

const MONARCH_CSS_IMPORT = /@rookcom\/monarch-tokens\/(?:css|dist\/css)/;

export function check(opts: CheckOptions): CheckResult {
  const root = opts.root;
  const today = opts.today ?? new Date().toISOString().slice(0, 10);
  const settings = opts.rules ?? {};
  const deprecations = opts.deprecations ?? [];
  const files = walk(root, opts.include ?? ['.'], [...DEFAULT_EXCLUDE, ...(opts.exclude ?? [])]);
  let linesScanned = 0;
  const findings: Finding[] = [];
  let inlineSuppressed = 0;
  const monarchCssImports: string[] = [];

  const severityFor = (rule: string, raw: Severity | undefined): Severity | null => {
    const s = settings[rule];
    if (s === 'off') return null;
    if (s) return s;
    return raw ?? (RULES[rule as RuleId]?.severity as Severity | undefined) ?? 'warn';
  };

  for (const file of files) {
    const text = readFileSync(join(root, file), 'utf8');
    const lines = text.split(/\r?\n/);
    linesScanned += lines.length;
    if (MONARCH_CSS_IMPORT.test(text)) monarchCssImports.push(file);
    const raw = scanFile({ file, ext: extname(file), text, lines, deprecations });
    const suppressions = readSuppressions(lines);
    for (const s of suppressions) {
      if (!s.reason || s.rules.length === 0) {
        const sev = severityFor('governance/suppression-without-reason', undefined);
        if (sev)
          findings.push({
            rule: 'governance/suppression-without-reason',
            severity: sev,
            file,
            line: s.line,
            column: s.column,
            message:
              s.rules.length === 0
                ? 'Suppression names no rule'
                : 'Suppression has no `-- reason`; it is ignored',
            snippet: (lines[s.line - 1] ?? '').trim().slice(0, 200),
          });
      }
    }
    for (const r of raw as RawFinding[]) {
      const sev = severityFor(r.rule, r.severity);
      if (!sev) continue;
      const sup = suppressions.find(
        (s) => s.target === r.line && s.reason && s.rules.some((p) => ruleMatches(p, r.rule)),
      );
      if (sup) {
        inlineSuppressed++;
        continue;
      }
      findings.push({
        rule: r.rule,
        severity: sev,
        file,
        line: r.line,
        column: r.column,
        message: r.message,
        snippet: (lines[r.line - 1] ?? '').trim().slice(0, 200),
      });
    }
  }

  for (const f of scanDependencies(root, opts.compatibility)) {
    const sev = severityFor(f.rule, f.severity);
    if (sev) findings.push({ ...f, severity: sev });
  }

  // Registry exceptions: active ones suppress, expired ones are findings.
  const exceptionReports: ExceptionReport[] = [];
  let exceptionSuppressed = 0;
  let kept = findings;
  for (const ex of opts.exceptions ?? []) {
    const expired = ex.expires < today;
    let suppressed = 0;
    if (!expired) {
      kept = kept.filter((f) => {
        const hit = ruleMatches(ex.rule, f.rule) && matchesAny(f.file, ex.paths);
        if (hit) suppressed++;
        return !hit;
      });
      exceptionSuppressed += suppressed;
    } else {
      const sev = severityFor('governance/expired-exception', undefined);
      if (sev)
        kept.push({
          rule: 'governance/expired-exception',
          severity: sev,
          file: 'monarch-registry',
          line: 1,
          column: 1,
          message: `Exception for ${ex.rule} on ${ex.paths.join(', ')} (owner ${ex.owner}) expired ${ex.expires}`,
          snippet: ex.reason,
        });
    }
    exceptionReports.push({ ...ex, status: expired ? 'expired' : 'active', suppressed });
  }

  // Baseline ratchet: the first N findings of each (file, rule) group are known.
  let covered = 0;
  let improved = 0;
  if (opts.baseline) {
    const groups = new Map<string, Finding[]>();
    for (const f of kept) {
      const k = `${f.file}\u0000${f.rule}`;
      const list = groups.get(k) ?? [];
      list.push(f);
      groups.set(k, list);
    }
    for (const [k, list] of groups) {
      const [file, rule] = k.split('\u0000') as [string, string];
      const allowed = opts.baseline.counts[file]?.[rule] ?? 0;
      list.sort((a, b) => a.line - b.line || a.column - b.column);
      list.forEach((f, i) => {
        if (i < allowed) {
          f.baselined = true;
          covered++;
        }
      });
    }
    for (const [file, rules] of Object.entries(opts.baseline.counts)) {
      for (const [rule, n] of Object.entries(rules)) {
        if ((groups.get(`${file}\u0000${rule}`)?.length ?? 0) < n) improved++;
      }
    }
  }

  kept.sort(
    (a, b) =>
      a.file.localeCompare(b.file) ||
      a.line - b.line ||
      a.column - b.column ||
      a.rule.localeCompare(b.rule),
  );

  const byRule: Record<string, number> = {};
  const bySeverity: Record<Severity, number> = { error: 0, warn: 0, info: 0 };
  const fresh: Record<Severity, number> = { error: 0, warn: 0, info: 0 };
  for (const f of kept) {
    byRule[f.rule] = (byRule[f.rule] ?? 0) + 1;
    bySeverity[f.severity]++;
    if (!f.baselined) fresh[f.severity]++;
  }
  const density: Record<string, number> = {};
  for (const [r, c] of Object.entries(byRule)) {
    density[r] = linesScanned ? Math.round((c / linesScanned) * 1000 * 100) / 100 : 0;
  }

  return {
    tool: { name: TOOL_NAME, version: VERSION },
    generatedAt: new Date().toISOString(),
    root,
    product: opts.product ?? null,
    git: gitState(root),
    filesScanned: files.length,
    linesScanned,
    findings: kept,
    counts: { byRule, bySeverity, new: fresh },
    densityPer1kLines: density,
    suppressed: { inline: inlineSuppressed, exceptions: exceptionSuppressed },
    exceptions: exceptionReports,
    baseline: opts.baseline ? { createdAt: opts.baseline.createdAt, covered, improved } : null,
    monarchCssImports,
    exitCode: fresh.error > 0 ? 1 : 0,
  };
}

export function makeBaseline(result: CheckResult): Baseline {
  const counts: Record<string, Record<string, number>> = {};
  for (const f of result.findings) {
    if (f.severity === 'info') continue;
    const byRule = (counts[f.file] ??= {});
    byRule[f.rule] = (byRule[f.rule] ?? 0) + 1;
  }
  return { version: 1, tool: `${TOOL_NAME}@${VERSION}`, createdAt: result.generatedAt, counts };
}
