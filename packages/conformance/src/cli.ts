#!/usr/bin/env node
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { check, loadConfig, makeBaseline, readJson, type Baseline } from './check.js';
import {
  loadCompatibility,
  loadDeprecations,
  loadProducts,
  statusMarkdown,
  validateRegistry,
  verifyProduct,
} from './registry.js';
import { toJson, toSarif, toText } from './report.js';
import { VERSION } from './version.js';

const HELP = `monarch ${VERSION}: Rook Monarch conformance

Usage:
  monarch check [--root DIR] [--config FILE] [--product ID --registry DIR]
                [--format text|json|sarif] [--out FILE]
                [--baseline FILE] [--write-baseline FILE]
  monarch registry validate --registry DIR
  monarch registry verify --registry DIR (--product ID --checkout DIR | --all --checkouts FILE)
  monarch registry report --registry DIR [--out FILE]

Exit codes: 0 pass, 1 new error-severity findings or invalid registry, 2 usage error.`;

type Flags = Record<string, string | true>;

function parse(argv: string[]): { positional: string[]; flags: Flags } {
  const positional: string[] = [];
  const flags: Flags = {};
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i] as string;
    if (a.startsWith('--')) {
      const [k, v] = a.slice(2).split('=', 2) as [string, string | undefined];
      if (v !== undefined) flags[k] = v;
      else if (argv[i + 1] !== undefined && !(argv[i + 1] as string).startsWith('--'))
        flags[k] = argv[++i] as string;
      else flags[k] = true;
    } else positional.push(a);
  }
  return { positional, flags };
}

class UsageError extends Error {}

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

function emit(content: string, out: string | undefined): void {
  if (out) {
    mkdirSync(dirname(resolve(out)), { recursive: true });
    writeFileSync(out, content);
  } else process.stdout.write(content);
}

function runCheck(flags: Flags): number {
  const root = resolve(str(flags, 'root') ?? '.');
  const config = loadConfig(resolve(root, str(flags, 'config') ?? 'monarch.config.json'));
  const registry = str(flags, 'registry');
  const productId = str(flags, 'product') ?? config?.product;
  const product =
    registry && productId
      ? loadProducts(resolve(registry)).find((p) => p.id === productId)
      : undefined;
  if (registry && productId && !product)
    throw new UsageError(`Unknown product "${productId}" in ${registry}`);
  const compat = registry ? loadCompatibility(resolve(registry)) : undefined;
  const baselinePath = str(flags, 'baseline');
  const result = check({
    root,
    include: config?.include ?? product?.source.paths ?? ['.'],
    exclude: config?.exclude ?? [],
    rules: config?.rules ?? {},
    exceptions: product?.exceptions ?? [],
    deprecations: registry ? loadDeprecations(resolve(registry)) : [],
    ...(compat ? { compatibility: compat } : {}),
    ...(productId ? { product: productId } : {}),
    ...(baselinePath ? { baseline: readJson<Baseline>(resolve(baselinePath)) } : {}),
  });
  const writeBaseline = str(flags, 'write-baseline');
  if (writeBaseline) {
    emit(JSON.stringify(makeBaseline(result), null, 2) + '\n', writeBaseline);
    process.stderr.write(`baseline written to ${writeBaseline}\n`);
  }
  const format = str(flags, 'format') ?? 'text';
  if (!['text', 'json', 'sarif'].includes(format))
    throw new UsageError(`Unknown --format ${format}`);
  emit(
    format === 'json' ? toJson(result) : format === 'sarif' ? toSarif(result) : toText(result),
    str(flags, 'out'),
  );
  return writeBaseline ? 0 : result.exitCode;
}

function runRegistry(sub: string | undefined, flags: Flags): number {
  const registry = resolve(need(flags, 'registry'));
  switch (sub) {
    case 'validate': {
      const { products, errors } = validateRegistry(registry);
      if (errors.length) {
        for (const e of errors) process.stderr.write(`✗ ${e}\n`);
        process.stderr.write(`${errors.length} problem(s) in ${products} product file(s)\n`);
        return 1;
      }
      process.stdout.write(`registry valid: ${products} products\n`);
      return 0;
    }
    case 'verify': {
      const jobs: { id: string; path: string }[] = [];
      if (flags.all) {
        const map = readJson<Record<string, string>>(resolve(need(flags, 'checkouts')));
        for (const p of loadProducts(registry)) {
          const path = map[p.id];
          if (path) jobs.push({ id: p.id, path });
          else process.stderr.write(`skip ${p.id}: no checkout in map\n`);
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
          process.stdout.write(
            `${j.id.padEnd(12)} ${status.adoption.status.padEnd(12)} ${status.compatibility.status.padEnd(16)} ${String(c.filesScanned).padStart(5)} files  ${c.bySeverity.error} errors ${c.bySeverity.warn} warnings\n`,
          );
        } catch (e) {
          failed++;
          process.stderr.write(`✗ ${j.id}: ${(e as Error).message}\n`);
        }
      }
      process.stdout.write(`status written to ${join(registry, 'status')}\n`);
      return failed ? 1 : 0;
    }
    case 'report':
      emit(statusMarkdown(registry), str(flags, 'out'));
      return 0;
    default:
      throw new UsageError(`Unknown registry command ${sub ?? ''}`);
  }
}

export function main(argv: string[]): number {
  const { positional, flags } = parse(argv);
  try {
    if (flags.version) {
      process.stdout.write(VERSION + '\n');
      return 0;
    }
    if (flags.help || positional.length === 0) {
      process.stdout.write(HELP + '\n');
      return positional.length === 0 && !flags.help ? 2 : 0;
    }
    if (positional[0] === 'check') return runCheck(flags);
    if (positional[0] === 'registry') return runRegistry(positional[1], flags);
    throw new UsageError(`Unknown command ${positional[0]}`);
  } catch (e) {
    if (e instanceof UsageError) {
      process.stderr.write(`${e.message}\n\n${HELP}\n`);
      return 2;
    }
    process.stderr.write(`monarch: ${(e as Error).message}\n`);
    return 1;
  }
}

process.exitCode = main(process.argv.slice(2));
