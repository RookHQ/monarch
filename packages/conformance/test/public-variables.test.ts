import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { PUBLIC_MONARCH_VARIABLES } from '../src/public-variables.js';
import { here } from './helpers.js';

const REACT_SRC = join(here, '../../react/src');

function files(dir: string, ext: string): string[] {
  return readdirSync(dir, { withFileTypes: true }).flatMap((e) =>
    e.isDirectory()
      ? files(join(dir, e.name), ext)
      : e.name.endsWith(ext)
        ? [join(dir, e.name)]
        : [],
  );
}

describe('PUBLIC_MONARCH_VARIABLES', () => {
  const css = files(REACT_SRC, '.css')
    .map((f) => readFileSync(f, 'utf8'))
    .join('\n');
  const docs = files(REACT_SRC, '.docs.tsx')
    .map((f) => readFileSync(f, 'utf8'))
    .join('\n');

  it.each([...PUBLIC_MONARCH_VARIABLES])('--%s is read by a Monarch stylesheet', (name) => {
    expect(css).toContain(`var(--${name}`);
  });

  it.each([...PUBLIC_MONARCH_VARIABLES])('--%s is documented on a component page', (name) => {
    expect(docs).toContain(`--${name}`);
  });
});
