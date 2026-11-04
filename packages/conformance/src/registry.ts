import { existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import {
  check,
  gitState,
  loadConfig,
  monarchDependencies,
  readJson,
  type CheckResult,
} from './check.js';
import { assertSupported, validate, type Schema } from './schema.js';
import { minVersion, parseVersion, satisfies, format } from './semver.js';
import type { Compatibility, Deprecation, ProductException, Severity } from './types.js';
import { TOOL_NAME, VERSION } from './version.js';

export interface Product {
  readonly id: string;
  readonly name: string;
  readonly kind: 'application' | 'marketing' | 'public-surface';
  readonly repository?: string;
  readonly domain: string;
  readonly theme: { readonly accent: string; readonly schemes: readonly ('light' | 'dark')[] };
  readonly source: { readonly paths: readonly string[] };
  readonly exceptions?: readonly ProductException[];
}

export interface RegistryValidation {
  readonly products: number;
  readonly errors: readonly string[];
}

export function loadProducts(registry: string): Product[] {
  const dir = join(registry, 'products');
  return readdirSync(dir)
    .filter((f) => f.endsWith('.json'))
    .sort()
    .map((f) => readJson<Product>(join(dir, f)));
}

export function loadCompatibility(registry: string): Compatibility | undefined {
  const p = join(registry, 'compatibility.json');
  return existsSync(p) ? readJson<Compatibility>(p) : undefined;
}

export function loadDeprecations(registry: string): Deprecation[] {
  const p = join(registry, 'deprecations.json');
  return existsSync(p) ? readJson<{ deprecations: Deprecation[] }>(p).deprecations : [];
}

export function validateRegistry(registry: string): RegistryValidation {
  const errors: string[] = [];
  const schema = readJson<Schema>(join(registry, 'product.schema.json'));
  assertSupported(schema);
  const dir = join(registry, 'products');
  const files = readdirSync(dir)
    .filter((f) => f.endsWith('.json'))
    .sort();
  const seen = {
    id: new Map<string, string>(),
    domain: new Map<string, string>(),
    repository: new Map<string, string>(),
  };
  for (const f of files) {
    let data: unknown;
    try {
      data = JSON.parse(readFileSync(join(dir, f), 'utf8'));
    } catch (e) {
      errors.push(`${f}: invalid JSON (${(e as Error).message})`);
      continue;
    }
    for (const e of validate(data, schema)) errors.push(`${f}: ${e}`);
    const p = data as Partial<Product>;
    if (typeof p.id === 'string' && `${p.id}.json` !== f)
      errors.push(`${f}: id "${p.id}" must match the file name`);
    for (const key of ['id', 'domain', 'repository'] as const) {
      const v = p[key];
      if (typeof v !== 'string') continue;
      const prev = seen[key].get(v);
      if (prev) errors.push(`${f}: ${key} "${v}" already used by ${prev}`);
      else seen[key].set(v, f);
    }
  }
  const compat = loadCompatibility(registry);
  if (compat) {
    for (const [name, entry] of Object.entries(compat.packages)) {
      if (!parseVersion(entry.current))
        errors.push(`compatibility.json: ${name}.current is not a version`);
      else if (!satisfies(entry.current, entry.supported))
        errors.push(`compatibility.json: ${name}.current is outside its supported range`);
    }
  }
  return { products: files.length, errors };
}

// ----------------------------------------------------------------- verify

export type AdoptionStatus = 'not-adopted' | 'partial' | 'adopted';
export type CompatibilityStatus = 'compatible' | 'needs-migration' | 'unknown';

export interface ProductStatus {
  readonly product: string;
  readonly verifiedAt: string;
  readonly verifiedBy: string;
  readonly tool: string;
  readonly checkout: { readonly commit: string | null; readonly dirty: boolean | null };
  readonly framework: {
    readonly name: string;
    readonly versions: Readonly<Record<string, string>>;
  };
  readonly monarch: { readonly packages: Readonly<Record<string, string>> };
  readonly adoption: { readonly status: AdoptionStatus; readonly evidence: readonly string[] };
  readonly compatibility: {
    readonly status: CompatibilityStatus;
    readonly notes: readonly string[];
  };
  readonly conformance: {
    readonly paths: readonly string[];
    readonly filesScanned: number;
    readonly linesScanned: number;
    readonly byRule: Readonly<Record<string, number>>;
    readonly bySeverity: Readonly<Record<Severity, number>>;
    readonly exceptions: number;
  };
}

const TRACKED = ['next', 'react', 'react-dom', 'vite', 'tailwindcss', 'typescript'];

export function detectFramework(checkout: string): ProductStatus['framework'] {
  const pkgPath = join(checkout, 'package.json');
  if (!existsSync(pkgPath)) return { name: 'unknown', versions: {} };
  const pkg = readJson<{
    dependencies?: Record<string, string>;
    devDependencies?: Record<string, string>;
  }>(pkgPath);
  const deps = { ...pkg.devDependencies, ...pkg.dependencies };
  const versions: Record<string, string> = {};
  for (const k of TRACKED) if (deps[k]) versions[k] = deps[k];
  const name = deps.next
    ? 'next'
    : deps.vite && deps.react
      ? 'vite-react'
      : deps.react
        ? 'react'
        : 'unknown';
  return { name, versions };
}

export function classifyCompatibility(
  versions: Readonly<Record<string, string>>,
  compat: Compatibility | undefined,
): ProductStatus['compatibility'] {
  if (!compat) return { status: 'unknown', notes: ['registry/compatibility.json not found'] };
  const notes: string[] = [];
  let failed = false;
  let unknown = !versions.react;
  if (!versions.react) notes.push('react not found in package.json');
  for (const [peer, range] of Object.entries(compat.peers)) {
    const spec = versions[peer];
    if (!spec) continue;
    const min = minVersion(spec);
    if (!min) {
      unknown = true;
      notes.push(`${peer}@${spec}: version spec not understood`);
      continue;
    }
    if (!satisfies(min, range)) {
      failed = true;
      if (peer === 'tailwindcss' && min.major >= 4) {
        notes.push(
          `tailwindcss@${spec}: the Monarch Tailwind preset targets v3. Use the CSS-only path (import @rookcom/monarch-tokens/css and the mn-* variables) or the v4 @theme mapping once it ships.`,
        );
      } else {
        notes.push(`${peer}@${spec} (min ${format(min)}) is outside the supported range ${range}`);
      }
    }
  }
  if (failed) return { status: 'needs-migration', notes };
  if (unknown) return { status: 'unknown', notes };
  return {
    status: 'compatible',
    notes: notes.length ? notes : ['react, next and tailwindcss are within Monarch peer ranges'],
  };
}

export function classifyAdoption(
  monarchPkgs: Readonly<Record<string, string>>,
  result: Pick<CheckResult, 'counts' | 'monarchCssImports'>,
): ProductStatus['adoption'] {
  const names = Object.keys(monarchPkgs);
  const localTokens = result.counts.byRule['tokens/local-token-definitions'] ?? 0;
  const duplicates = result.counts.byRule['components/duplicated-primitive'] ?? 0;
  const cssImported = result.monarchCssImports.length > 0;
  const evidence: string[] = [];
  if (names.length === 0) {
    evidence.push('no @rookcom/monarch-* dependency in package.json');
    if (localTokens) evidence.push(`${localTokens} local token definition block(s)`);
    if (duplicates) evidence.push(`${duplicates} local component file(s) duplicating Monarch`);
    return { status: 'not-adopted', evidence };
  }
  evidence.push(`depends on ${names.map((n) => `${n}@${monarchPkgs[n]}`).join(', ')}`);
  evidence.push(
    cssImported
      ? `imports Monarch CSS in ${result.monarchCssImports.slice(0, 3).join(', ')}`
      : 'does not import @rookcom/monarch-tokens/css',
  );
  if (localTokens) evidence.push(`${localTokens} local token definition block(s) remain`);
  if (duplicates) evidence.push(`${duplicates} duplicated component file(s) remain`);
  const adopted = cssImported && localTokens === 0 && duplicates === 0;
  return { status: adopted ? 'adopted' : 'partial', evidence };
}

export interface VerifyOptions {
  readonly registry: string;
  readonly productId: string;
  readonly checkout: string;
  readonly write?: boolean;
  readonly now?: Date;
}

export function verifyProduct(opts: VerifyOptions): { status: ProductStatus; result: CheckResult } {
  const product = loadProducts(opts.registry).find((p) => p.id === opts.productId);
  if (!product) throw new Error(`Unknown product "${opts.productId}" in ${opts.registry}`);
  if (!existsSync(join(opts.checkout, 'package.json')))
    throw new Error(`${opts.checkout} has no package.json`);
  const compat = loadCompatibility(opts.registry);
  const appConfig = loadConfig(join(opts.checkout, 'monarch.config.json'));
  const include = product.source.paths.filter((p) => existsSync(join(opts.checkout, p)));
  const result = check({
    root: opts.checkout,
    include,
    exclude: appConfig?.exclude ?? [],
    rules: appConfig?.rules ?? {},
    exceptions: product.exceptions ?? [],
    deprecations: loadDeprecations(opts.registry),
    product: product.id,
    ...(compat ? { compatibility: compat } : {}),
  });
  const framework = detectFramework(opts.checkout);
  const deps = monarchDependencies(opts.checkout);
  const packages = Object.fromEntries(
    Object.entries(deps).map(([k, v]) => [k, v.installed ?? v.spec]),
  );
  const status: ProductStatus = {
    product: product.id,
    verifiedAt: (opts.now ?? new Date()).toISOString(),
    verifiedBy: 'monarch registry verify',
    tool: `${TOOL_NAME}@${VERSION}`,
    checkout: gitState(opts.checkout),
    framework,
    monarch: { packages },
    adoption: classifyAdoption(packages, result),
    compatibility: classifyCompatibility(framework.versions, compat),
    conformance: {
      paths: include,
      filesScanned: result.filesScanned,
      linesScanned: result.linesScanned,
      byRule: result.counts.byRule,
      bySeverity: result.counts.bySeverity,
      exceptions: result.suppressed.exceptions,
    },
  };
  if (opts.write !== false) {
    const out = join(opts.registry, 'status', `${product.id}.json`);
    mkdirSync(dirname(out), { recursive: true });
    writeFileSync(out, JSON.stringify(status, null, 2) + '\n');
  }
  return { status, result };
}

// ----------------------------------------------------------------- report

export function loadStatuses(registry: string): ProductStatus[] {
  const dir = join(registry, 'status');
  if (!existsSync(dir)) return [];
  return readdirSync(dir)
    .filter((f) => f.endsWith('.json'))
    .sort()
    .map((f) => readJson<ProductStatus>(join(dir, f)));
}

const A11Y = [
  'a11y/img-alt',
  'a11y/click-without-key',
  'a11y/positive-tabindex',
  'a11y/outline-none-without-ring',
];

export function statusMarkdown(registry: string): string {
  const products = new Map(loadProducts(registry).map((p) => [p.id, p]));
  const statuses = loadStatuses(registry);
  const n = (s: ProductStatus, r: string) => s.conformance.byRule[r] ?? 0;
  const rows = statuses.map((s) => {
    const p = products.get(s.product);
    const fw = s.framework.versions;
    const fwText = [
      s.framework.name,
      fw.next && `next ${fw.next}`,
      fw.react && `react ${fw.react}`,
      fw.tailwindcss && `tw ${fw.tailwindcss}`,
    ]
      .filter(Boolean)
      .join(' · ');
    const monarch =
      Object.entries(s.monarch.packages)
        .map(([k, v]) => `${k.replace('@rookcom/monarch-', '')} ${v}`)
        .join(', ') || '—';
    const a11y = A11Y.reduce((acc, r) => acc + n(s, r), 0);
    return `| ${p?.name ?? s.product} | ${p?.domain ?? '—'} | ${fwText} | ${monarch} | ${s.adoption.status} | ${s.compatibility.status} | ${s.conformance.filesScanned} | ${n(s, 'color/no-raw-palette')} | ${n(s, 'color/no-hard-coded-color')} | ${n(s, 'tokens/local-token-definitions')} | ${n(s, 'components/duplicated-primitive')} | ${a11y} | ${s.verifiedAt.slice(0, 10)} | ${s.checkout.commit ? '`' + s.checkout.commit.slice(0, 7) + '`' + (s.checkout.dirty ? ' (dirty)' : '') : '—'} |`;
  });
  const unverified = [...products.keys()].filter((id) => !statuses.some((s) => s.product === id));
  return [
    '# Monarch registry status',
    '',
    '<!-- Generated by `monarch registry report`. Do not edit by hand. -->',
    '',
    'Numbers come from a static scan of each product at the commit shown (“dirty” means the checkout had uncommitted changes). They are counts, not scores. Adoption is decided by `monarch registry verify`; see [conformance guide](../guides/conformance.md#how-registry-verify-decides-adoption).',
    '',
    '| Product | Domain | Framework | Monarch | Adoption | Compatibility | Files | Palette | Colour literals | Local token blocks | Duplicated components | A11y | Verified | Commit |',
    '| --- | --- | --- | --- | --- | --- | --: | --: | --: | --: | --: | --: | --- | --- |',
    ...rows,
    '',
    ...(unverified.length ? [`Not yet verified: ${unverified.join(', ')}.`, ''] : []),
    '## Compatibility notes',
    '',
    ...statuses
      .filter((s) => s.compatibility.status !== 'compatible')
      .map(
        (s) =>
          `- **${products.get(s.product)?.name ?? s.product}** (${s.compatibility.status}): ${s.compatibility.notes.join(' ')}`,
      ),
    '',
  ].join('\n');
}
