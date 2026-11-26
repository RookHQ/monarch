import { cpSync, mkdtempSync, mkdirSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { scanFile, type RawFinding } from '../src/rules.js';
import type { Deprecation } from '../src/types.js';

export const here = dirname(fileURLToPath(import.meta.url));
export const SAMPLE_APP = join(here, 'fixtures/sample-app');
export const REGISTRY = join(here, '../../../registry');

export function scan(file: string, text: string, deprecations: Deprecation[] = []): RawFinding[] {
  const ext = file.slice(file.lastIndexOf('.'));
  return scanFile({ file, ext, text, lines: text.split('\n'), deprecations });
}

export const rules = (fs: readonly { rule: string }[]) => fs.map((f) => f.rule);

export function tempDir(): string {
  return mkdtempSync(join(tmpdir(), 'monarch-conformance-'));
}

/** Copies the sample app so tests can mutate it. */
export function copySample(): string {
  const dir = tempDir();
  cpSync(SAMPLE_APP, dir, { recursive: true });
  return dir;
}

export function write(root: string, rel: string, content: string): void {
  mkdirSync(dirname(join(root, rel)), { recursive: true });
  writeFileSync(join(root, rel), content);
}
