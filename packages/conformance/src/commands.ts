/**
 * The `monarch` command line, separate from cli.ts (the executable) so tests can
 * run commands with their own output streams.
 */
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { addText, planAdd, writeCss } from './add.js';
import { BUNDLED_PRODUCTS } from './bundled-data.js';
import {
  check,
  loadConfig,
  makeBaseline,
  readJson,
  type Baseline,
  type CheckOptions,
} from './check.js';
import { CreateError, STARTER_TEMPLATES, createProject, isStarterTemplate } from './create.js';
import { FigmaConnectError, generateFigmaConnect, parseFigmaMap } from './figma-connect.js';
import { applyFixes } from './fixes.js';
import { initAgents } from './init-agents.js';
import { loadManifest } from './manifest.js';
import { runStdio } from './mcp.js';
import {
  loadCompatibility,
  loadDeprecations,
  loadImportPathDeprecations,
  loadProducts,
  statusMarkdown,
  validateRegistry,
  verifyProduct,
} from './registry.js';
import { toJson, toSarif, toText } from './report.js';
import { search, searchText } from './search.js';
import { VERSION } from './version.js';

export const HELP = `monarch ${VERSION}: Monarch design-system conformance

Usage:
  monarch check [--root DIR] [--config FILE] [--product ID] [--accent HEX]
                [--registry DIR] [--format text|json|sarif] [--out FILE]
                [--baseline FILE] [--write-baseline FILE] [--fix[=all]]
  monarch search <query> [--root DIR] [--limit N] [--components] [--json]
  monarch add <Component> [--root DIR] [--write] [--json]
  monarch mcp [--root DIR] [--manifest FILE]
  monarch init-agents [--root DIR] [--force]
  monarch create <dir> --template next-app|vite-react [--name NAME] [--accent HEX]
  monarch figma-connect --map FILE [--out DIR] [--root DIR] [--manifest FILE] [--force]
  monarch registry validate --registry DIR
  monarch registry verify --registry DIR (--product ID --checkout DIR | --all --checkouts FILE)
  monarch registry report --registry DIR [--out FILE]

check works in any app: --product is optional (a Rook product id), and
compatibility and deprecation data are bundled. --fix applies safe fixes;
--fix=all also applies fixes that need review.

create copies a starter app into an empty directory; it never runs an install.
figma-connect writes Figma Code Connect files from the component manifest and
your map of component to Figma node URL.

Exit codes: 0 pass, 1 new error-severity findings or invalid registry, 2 usage error.`;

export type Flags = Record<string, string | true>;

export interface Io {
  readonly out: (s: string) => void;
  readonly err: (s: string) => void;
}

const processIo: Io = {
  out: (s) => process.stdout.write(s),
  err: (s) => process.stderr.write(s),
};

export function parseArgs(argv: readonly string[]): { positional: string[]; flags: Flags } {
  const positional: string[] = [];
  const flags: Flags = {};
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i] as string;
    if (a.startsWith('--')) {
      const [k, v] = a.slice(2).split('=', 2) as [string, string | undefined];
      if (v !== undefined) flags[k] = v;
      else if (BOOLEAN_FLAGS.has(k)) flags[k] = true;
      else if (argv[i + 1] !== undefined && !(argv[i + 1] as string).startsWith('--'))
        flags[k] = argv[++i] as string;
      else flags[k] = true;
    } else positional.push(a);
  }
  return { positional, flags };
}

/** Flags that never take a value, so `--fix src` is not read as `--fix=src`. */
const BOOLEAN_FLAGS = new Set([
  'fix',
  'write',
  'force',
  'json',
  'components',
  'all',
  'help',
  'version',
]);

export class UsageError extends Error {}

function str(flags: Flags, key: string): string | undefined {
  const v = flags[key];
  if (v === true) throw new UsageError(`--${key} needs a value`);
  return v;
}

function need(flags: Flags, key: string): string {
  const v = str(flags, key);
  if (!v) throw new UsageError(`--${key} is required`);
  return v;
}

function emit(io: Io, content: string, out: string | undefined): void {
  if (out) {
    mkdirSync(dirname(resolve(out)), { recursive: true });
    writeFileSync(out, content);
  } else io.out(content);
}

const HEX = /^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/;

interface ProductInfo {
  readonly id: string;
  readonly paths?: readonly string[];
  readonly exceptions?: CheckOptions['exceptions'];
}

/** A product from the registry when one is given, else from the bundled list. Unknown: a warning. */
function resolveProduct(
  io: Io,
  id: string | undefined,
  registry: string | undefined,
): ProductInfo | undefined {
  if (!id) return undefined;
  if (registry) {
    const p = loadProducts(resolve(registry)).find((x) => x.id === id);
    if (p) return { id, paths: p.source.paths, exceptions: p.exceptions ?? [] };
  } else if (BUNDLED_PRODUCTS.some((p) => p.id === id)) return { id };
  io.err(
    `monarch: "${id}" is not a registered Monarch product; checking without product settings. Teams outside Rook can leave --product out (and pass --accent #hex for their brand colour).\n`,
  );
  return undefined;
}

export function runCheck(flags: Flags, io: Io = processIo): number {
  const root = resolve(str(flags, 'root') ?? '.');
  const config = loadConfig(resolve(root, str(flags, 'config') ?? 'monarch.config.json'));
  const registry = str(flags, 'registry');
  const productId = str(flags, 'product') ?? config?.product;
  const product = resolveProduct(io, productId, registry);
  const accent = str(flags, 'accent');
  if (accent !== undefined && !HEX.test(accent))
    throw new UsageError(`--accent must be a hex colour such as #3355aa, not "${accent}"`);
  const compat = loadCompatibility(registry ? resolve(registry) : undefined);
  const baselinePath = str(flags, 'baseline');
  const options: CheckOptions = {
    root,
    include: config?.include ?? product?.paths ?? ['.'],
    exclude: config?.exclude ?? [],
    rules: config?.rules ?? {},
    exceptions: product?.exceptions ?? [],
    deprecations: loadDeprecations(registry ? resolve(registry) : undefined),
    importPaths: loadImportPathDeprecations(registry ? resolve(registry) : undefined),
    ...(compat ? { compatibility: compat } : {}),
    ...(product ? { product: product.id } : {}),
    ...(baselinePath ? { baseline: readJson<Baseline>(resolve(baselinePath)) } : {}),
  };
  let result = check(options);
  const fix = flags.fix;
  if (fix !== undefined) {
    if (fix !== true && fix !== 'all' && fix !== 'safe')
      throw new UsageError('--fix takes no value, or --fix=all to include fixes that need review');
    const summary = applyFixes(root, result.findings, { all: fix === 'all' });
    io.err(
      `monarch: applied ${summary.applied} fix(es) in ${summary.files.length} file(s)` +
        (summary.skipped ? `; ${summary.skipped} need review (run --fix=all to apply them)` : '') +
        '\n',
    );
    if (summary.applied) result = check(options);
  }
  if (accent)
    io.err(
      `monarch: brand colour ${accent}. Derive its accessible theme at build time with deriveTheme('${accent}') and brandThemeCss from @rookcom/monarch-tokens, or use the neutral accent (css/products/neutral). See https://monarchui.dev/theming/\n`,
    );
  const writeBaseline = str(flags, 'write-baseline');
  if (writeBaseline) {
    emit(io, JSON.stringify(makeBaseline(result), null, 2) + '\n', writeBaseline);
    io.err(`baseline written to ${writeBaseline}\n`);
  }
  const format = str(flags, 'format') ?? 'text';
  if (!['text', 'json', 'sarif'].includes(format))
    throw new UsageError(`Unknown --format ${format}`);
  emit(
    io,
    format === 'json' ? toJson(result) : format === 'sarif' ? toSarif(result) : toText(result),
    str(flags, 'out'),
  );
  return writeBaseline ? 0 : result.exitCode;
}

export function runSearch(positional: readonly string[], flags: Flags, io: Io = processIo): number {
  const query = positional.join(' ').trim();
  if (!query)
    throw new UsageError('monarch search needs a query, such as `monarch search date picker`');
  const root = resolve(str(flags, 'root') ?? '.');
  const { manifest } = loadManifest(root, str(flags, 'manifest'));
  const limit = Number(str(flags, 'limit') ?? 20);
  if (!Number.isInteger(limit) || limit < 1)
    throw new UsageError('--limit must be a positive whole number');
  const hits = search(manifest, query, { limit, componentsOnly: flags.components === true });
  if (flags.json === true)
    io.out(
      JSON.stringify(
        hits.map((h) => ({ ...h.export, score: Math.round(h.score * 10) / 10 })),
        null,
        2,
      ) + '\n',
    );
  else io.out(searchText(hits));
  return hits.length ? 0 : 1;
}

export function runAdd(positional: readonly string[], flags: Flags, io: Io = processIo): number {
  const name = positional[0];
  if (!name)
    throw new UsageError('monarch add needs a component name, such as `monarch add Select`');
  const root = resolve(str(flags, 'root') ?? '.');
  const { manifest } = loadManifest(root, str(flags, 'manifest'));
  const plan = planAdd(manifest, name);
  if (flags.json === true)
    io.out(JSON.stringify({ ...plan, export: plan.export.name }, null, 2) + '\n');
  else io.out(addText(plan));
  if (flags.write === true) io.err(`${writeCss(root, plan.css).message}\n`);
  return 0;
}

export function runInitAgents(flags: Flags, io: Io = processIo): number {
  const root = resolve(str(flags, 'root') ?? '.');
  const { written, skipped } = initAgents(root, { force: flags.force === true });
  for (const f of written) io.out(`wrote ${join(root, f)}\n`);
  for (const f of skipped) io.out(`kept ${join(root, f)} (exists; --force overwrites)\n`);
  return 0;
}

export function runCreate(positional: readonly string[], flags: Flags, io: Io = processIo): number {
  const dir = positional[0];
  if (!dir)
    throw new UsageError('monarch create needs a directory, such as `monarch create my-app`');
  const template = str(flags, 'template');
  if (!template || !isStarterTemplate(template))
    throw new UsageError(`--template must be one of ${STARTER_TEMPLATES.join(', ')}`);
  const name = str(flags, 'name');
  const accent = str(flags, 'accent');
  try {
    const result = createProject(dir, {
      template,
      ...(name ? { name } : {}),
      ...(accent ? { accent } : {}),
    });
    io.out(`created ${result.name} in ${result.dir} (${template}, ${result.files.length} files)\n`);
    io.out(`\nNext:\n${result.next.map((line) => `  ${line}`).join('\n')}\n`);
    return 0;
  } catch (e) {
    if (e instanceof CreateError) throw new UsageError(e.message);
    throw e;
  }
}

export function runFigmaConnect(flags: Flags, io: Io = processIo): number {
  const mapPath = resolve(need(flags, 'map'));
  if (!existsSync(mapPath)) throw new UsageError(`--map ${mapPath} does not exist`);
  const root = resolve(str(flags, 'root') ?? '.');
  const out = resolve(str(flags, 'out') ?? 'figma');
  const { manifest } = loadManifest(root, str(flags, 'manifest'));
  let raw: unknown;
  try {
    raw = JSON.parse(readFileSync(mapPath, 'utf8'));
  } catch (e) {
    throw new UsageError(`${mapPath} is not valid JSON: ${(e as Error).message}`);
  }
  try {
    const result = generateFigmaConnect(manifest, parseFigmaMap(raw), out, {
      force: flags.force === true,
    });
    for (const f of result.written) io.out(`wrote ${join(out, f)}\n`);
    for (const s of result.skipped) io.err(`skipped ${s.component}: ${s.reason}\n`);
    io.out(
      `${result.written.length} Code Connect file(s) written, ${result.skipped.length} skipped. Review them, then publish with the Figma CLI: npx figma connect publish\n`,
    );
    return result.skipped.length && !result.written.length ? 1 : 0;
  } catch (e) {
    if (e instanceof FigmaConnectError) throw new UsageError(e.message);
    throw e;
  }
}

function runRegistry(sub: string | undefined, flags: Flags, io: Io): number {
  const registry = resolve(need(flags, 'registry'));
  switch (sub) {
    case 'validate': {
      const { products, errors } = validateRegistry(registry);
      if (errors.length) {
        for (const e of errors) io.err(`✗ ${e}\n`);
        io.err(`${errors.length} problem(s) in ${products} product file(s)\n`);
        return 1;
      }
      io.out(`registry valid: ${products} products\n`);
      return 0;
    }
    case 'verify': {
      const jobs: { id: string; path: string }[] = [];
      if (flags.all) {
        const map = readJson<Record<string, string>>(resolve(need(flags, 'checkouts')));
        for (const p of loadProducts(registry)) {
          const path = map[p.id];
          if (path) jobs.push({ id: p.id, path });
          else io.err(`skip ${p.id}: no checkout in map\n`);
        }
      } else jobs.push({ id: need(flags, 'product'), path: need(flags, 'checkout') });
      let failed = 0;
      for (const j of jobs) {
        try {
          const { status } = verifyProduct({
            registry,
            productId: j.id,
            checkout: resolve(j.path),
          });
          const c = status.conformance;
          io.out(
            `${j.id.padEnd(12)} ${status.adoption.status.padEnd(12)} ${status.compatibility.status.padEnd(16)} ${String(c.filesScanned).padStart(5)} files  ${c.bySeverity.error} errors ${c.bySeverity.warn} warnings\n`,
          );
        } catch (e) {
          failed++;
          io.err(`✗ ${j.id}: ${(e as Error).message}\n`);
        }
      }
      io.out(`status written to ${join(registry, 'status')}\n`);
      return failed ? 1 : 0;
    }
    case 'report':
      emit(io, statusMarkdown(registry), str(flags, 'out'));
      return 0;
    default:
      throw new UsageError(`Unknown registry command ${sub ?? ''}`);
  }
}

export async function main(argv: readonly string[], io: Io = processIo): Promise<number> {
  const { positional, flags } = parseArgs(argv);
  try {
    if (flags.version) {
      io.out(VERSION + '\n');
      return 0;
    }
    if (flags.help || positional.length === 0) {
      io.out(HELP + '\n');
      return positional.length === 0 && !flags.help ? 2 : 0;
    }
    const [command, ...rest] = positional;
    switch (command) {
      case 'check':
        return runCheck(flags, io);
      case 'search':
        return runSearch(rest, flags, io);
      case 'add':
        return runAdd(rest, flags, io);
      case 'mcp': {
        const manifest = str(flags, 'manifest');
        await runStdio({
          root: resolve(str(flags, 'root') ?? '.'),
          ...(manifest ? { manifestPath: resolve(manifest) } : {}),
        });
        return 0;
      }
      case 'init-agents':
        return runInitAgents(flags, io);
      case 'create':
        return runCreate(rest, flags, io);
      case 'figma-connect':
        return runFigmaConnect(flags, io);
      case 'registry':
        return runRegistry(rest[0], flags, io);
      default:
        throw new UsageError(`Unknown command ${command}`);
    }
  } catch (e) {
    if (e instanceof UsageError) {
      io.err(`${e.message}\n\n${HELP}\n`);
      return 2;
    }
    io.err(`monarch: ${(e as Error).message}\n`);
    return 1;
  }
}
