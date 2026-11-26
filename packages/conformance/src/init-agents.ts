/**
 * `monarch init-agents`: writes instructions for AI coding agents into an app
 * (AGENTS.md, CLAUDE.md, .cursor/rules/monarch.mdc,
 * .github/copilot-instructions.md), from templates/agents in the Monarch repo,
 * bundled into this package at build time.
 */
import { copyFileSync, existsSync, mkdirSync, readdirSync, statSync } from 'node:fs';
import { dirname, join, relative, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

/** The templates folder: bundled (dist/templates/agents) or a Monarch checkout's. */
export function templatesDir(): string {
  const here = fileURLToPath(new URL('.', import.meta.url));
  const candidates = [
    join(here, 'templates', 'agents'),
    join(here, '..', '..', '..', 'templates', 'agents'),
  ];
  const found = candidates.find((c) => existsSync(c));
  if (!found) throw new Error('agent templates not found in this package');
  return found;
}

function listFiles(dir: string): string[] {
  const out: string[] = [];
  const visit = (d: string) => {
    for (const name of readdirSync(d).sort()) {
      const abs = join(d, name);
      if (statSync(abs).isDirectory()) visit(abs);
      else out.push(relative(dir, abs).split(sep).join('/'));
    }
  };
  visit(dir);
  return out;
}

export interface InitAgentsResult {
  readonly written: readonly string[];
  /** Files that exist already and were left alone (pass force to overwrite). */
  readonly skipped: readonly string[];
}

export function initAgents(
  root: string,
  opts: { readonly force?: boolean; readonly from?: string } = {},
): InitAgentsResult {
  const from = opts.from ?? templatesDir();
  const written: string[] = [];
  const skipped: string[] = [];
  for (const file of listFiles(from)) {
    const target = join(root, file);
    if (existsSync(target) && !opts.force) {
      skipped.push(file);
      continue;
    }
    mkdirSync(dirname(target), { recursive: true });
    copyFileSync(join(from, file), target);
    written.push(file);
  }
  return { written, skipped };
}
