import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { HELP, main, type Io } from '../src/commands.js';
import { here, tempDir } from './helpers.js';

const MANIFEST = join(here, 'fixtures/manifest/manifest.json');

function run(argv: string[]): Promise<{ code: number; out: string; err: string }> {
  let out = '';
  let err = '';
  const io: Io = { out: (s) => (out += s), err: (s) => (err += s) };
  return main(argv, io).then((code) => ({ code, out, err }));
}

describe('monarch create', () => {
  it('is listed in the help', () => {
    expect(HELP).toContain('monarch create <dir> --template next-app|vite-react');
    expect(HELP).toContain('monarch figma-connect --map FILE');
  });

  it('copies a starter, fills the name and prints what to run next', async () => {
    const dir = join(tempDir(), 'My App');
    const r = await run(['create', dir, '--template', 'vite-react']);
    expect(r.code).toBe(0);
    expect(r.out).toContain('created my-app');
    expect(r.out).toContain('npm install');
    const pkg = JSON.parse(readFileSync(join(dir, 'package.json'), 'utf8')) as { name: string };
    expect(pkg.name).toBe('my-app');
    expect(existsSync(join(dir, '.gitignore'))).toBe(true);
    // It never installs anything.
    expect(existsSync(join(dir, 'node_modules'))).toBe(false);
    expect(existsSync(join(dir, 'package-lock.json'))).toBe(false);
  });

  it('takes a name and a brand colour', async () => {
    const dir = join(tempDir(), 'app');
    const r = await run([
      'create',
      dir,
      '--template',
      'next-app',
      '--name',
      'shop-front',
      '--accent',
      '#3355AA',
    ]);
    expect(r.code).toBe(0);
    expect(JSON.parse(readFileSync(join(dir, 'package.json'), 'utf8'))).toMatchObject({
      name: 'shop-front',
    });
    expect(readFileSync(join(dir, 'brand.json'), 'utf8')).toContain('#3355aa');
  });

  it('refuses a directory that is not empty, and writes nothing', async () => {
    const dir = tempDir();
    writeFileSync(join(dir, 'notes.txt'), 'mine\n');
    const r = await run(['create', dir, '--template', 'next-app']);
    expect(r.code).toBe(2);
    expect(r.err).toMatch(/not empty/i);
    expect(existsSync(join(dir, 'package.json'))).toBe(false);
  });

  it('needs a directory and a known template', async () => {
    expect((await run(['create', '--template', 'next-app'])).code).toBe(2);
    const dir = join(tempDir(), 'x');
    const unknown = await run(['create', dir, '--template', 'remix']);
    expect(unknown.code).toBe(2);
    expect(unknown.err).toContain('next-app, vite-react');
    expect((await run(['create', dir])).code).toBe(2);
    expect(existsSync(dir)).toBe(false);
  });

  it('rejects a colour that is not a hex', async () => {
    const dir = join(tempDir(), 'x');
    const r = await run(['create', dir, '--template', 'vite-react', '--accent', 'red']);
    expect(r.code).toBe(2);
    expect(existsSync(dir)).toBe(false);
  });
});

describe('monarch figma-connect', () => {
  const URL_OK = 'https://www.figma.com/design/AbC123xyz/Library?node-id=12-345';

  function mapFile(components: unknown): string {
    const dir = tempDir();
    const file = join(dir, 'map.json');
    writeFileSync(file, JSON.stringify({ components }));
    return file;
  }

  it('writes one Code Connect file per mapped component', async () => {
    const out = join(tempDir(), 'figma');
    const r = await run([
      'figma-connect',
      '--map',
      mapFile([{ component: 'Button', url: URL_OK }]),
      '--out',
      out,
      '--manifest',
      MANIFEST,
    ]);
    expect(r.code).toBe(0);
    expect(r.out).toContain('1 Code Connect file(s) written');
    const text = readFileSync(join(out, 'Button.figma.tsx'), 'utf8');
    expect(text).toContain("import figma from '@figma/code-connect';");
    expect(text).toContain(URL_OK);
  });

  it('keeps an existing file unless --force', async () => {
    const out = join(tempDir(), 'figma');
    mkdirSync(out, { recursive: true });
    writeFileSync(join(out, 'Button.figma.tsx'), '// mine\n');
    const args = [
      'figma-connect',
      '--map',
      mapFile([{ component: 'Button', url: URL_OK }]),
      '--out',
      out,
      '--manifest',
      MANIFEST,
    ];
    const kept = await run(args);
    expect(kept.code).toBe(1);
    expect(kept.err).toContain('skipped Button');
    expect(readFileSync(join(out, 'Button.figma.tsx'), 'utf8')).toBe('// mine\n');
    expect((await run([...args, '--force'])).code).toBe(0);
    expect(readFileSync(join(out, 'Button.figma.tsx'), 'utf8')).toContain('figma.connect');
  });

  it('rejects the example map, whose node URLs are placeholders', async () => {
    const out = join(tempDir(), 'figma');
    const r = await run([
      'figma-connect',
      '--map',
      join(here, '../../../figma/map.example.json'),
      '--out',
      out,
      '--manifest',
      MANIFEST,
    ]);
    expect(r.code).toBe(2);
    expect(existsSync(out)).toBe(false);
  });

  it('reports a missing map, a map that is not JSON and a missing --map as usage errors', async () => {
    expect((await run(['figma-connect'])).code).toBe(2);
    expect((await run(['figma-connect', '--map', join(tempDir(), 'none.json')])).code).toBe(2);
    const dir = tempDir();
    writeFileSync(join(dir, 'bad.json'), '{ nope');
    const r = await run(['figma-connect', '--map', join(dir, 'bad.json'), '--manifest', MANIFEST]);
    expect(r.code).toBe(2);
    expect(r.err).toContain('not valid JSON');
  });
});
