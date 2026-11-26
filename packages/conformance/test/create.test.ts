import {
  existsSync,
  mkdirSync,
  readdirSync,
  readFileSync,
  statSync,
  symlinkSync,
  writeFileSync,
} from 'node:fs';
import { basename, join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { check } from '../src/check.js';
import {
  CreateError,
  STARTER_TEMPLATES,
  createProject,
  isStarterTemplate,
  normaliseProjectName,
  startersDir,
  type StarterTemplate,
} from '../src/create.js';
import { here, tempDir, write } from './helpers.js';

const REPO = join(here, '../../..');
const STARTERS = join(REPO, 'templates/starters');
const SOURCE_DIR: Record<StarterTemplate, string> = { 'next-app': 'app', 'vite-react': 'src' };

/** Every file under `dir`, relative, with `/` separators, sorted. */
function filesIn(dir: string, prefix = ''): string[] {
  return readdirSync(dir)
    .sort()
    .flatMap((entry) => {
      const rel = prefix ? `${prefix}/${entry}` : entry;
      return statSync(join(dir, entry)).isDirectory() ? filesIn(join(dir, entry), rel) : [rel];
    });
}

const read = (root: string, rel: string): string => readFileSync(join(root, rel), 'utf8');
const readJson = <T>(path: string): T => JSON.parse(readFileSync(path, 'utf8')) as T;

/** A new, empty directory path that does not exist yet. */
const freshTarget = (name = 'my-app'): string => join(tempDir(), name);

/** A templates root with one small `next-app` template, for edge cases. */
function fakeTemplates(files: Readonly<Record<string, string | Buffer>>): string {
  const root = tempDir();
  for (const [rel, content] of Object.entries(files)) {
    const abs = join(root, 'next-app', rel);
    mkdirSync(join(abs, '..'), { recursive: true });
    writeFileSync(abs, content);
  }
  return root;
}

describe('starter templates', () => {
  it('finds the templates of this checkout', () => {
    expect(startersDir()).toBe(STARTERS);
    for (const template of STARTER_TEMPLATES)
      expect(existsSync(join(startersDir(), template, 'package.json'))).toBe(true);
  });

  it('knows its template names', () => {
    expect(isStarterTemplate('next-app')).toBe(true);
    expect(isStarterTemplate('vite-react')).toBe(true);
    expect(isStarterTemplate('remix')).toBe(false);
    expect(isStarterTemplate('')).toBe(false);
  });
});

describe.each(STARTER_TEMPLATES)('createProject with %s', (template) => {
  it('creates the directory and copies every template file', () => {
    const target = freshTarget();
    const result = createProject(target, { template });
    const expected = filesIn(join(STARTERS, template))
      .map((f) => f.replace(/(^|\/)_gitignore$/, '$1.gitignore'))
      .sort();
    expect(result.dir).toBe(target);
    expect(result.template).toBe(template);
    expect(result.files).toEqual(expected);
    expect(filesIn(target)).toEqual(expected);
    expect(result.files).toContain('package.json');
    expect(result.files).toContain('AGENTS.md');
    expect(result.files).toContain('monarch.config.json');
  });

  it('fills in the project name everywhere', () => {
    const target = freshTarget('shop-front');
    const result = createProject(target, { template });
    expect(result.name).toBe('shop-front');
    expect(readJson<{ name: string }>(join(target, 'package.json')).name).toBe('shop-front');
    for (const file of result.files) {
      const text = read(target, file);
      expect(text, file).not.toContain('__PROJECT_NAME__');
      expect(text, file).not.toContain('__ACCENT__');
    }
    expect(read(target, 'README.md')).toContain('# `shop-front`');
  });

  it('writes .gitignore instead of _gitignore', () => {
    const target = freshTarget();
    createProject(target, { template });
    expect(existsSync(join(target, '.gitignore'))).toBe(true);
    expect(existsSync(join(target, '_gitignore'))).toBe(false);
    expect(read(target, '.gitignore')).toContain('node_modules');
  });

  it('uses the neutral accent when no colour is given', () => {
    const target = freshTarget();
    createProject(target, { template });
    expect(readJson<{ accent: string }>(join(target, 'brand.json')).accent).toBe('');
  });

  it('writes the brand colour into brand.json', () => {
    const target = freshTarget();
    createProject(target, { template, accent: '#3355AA' });
    expect(readJson<{ accent: string }>(join(target, 'brand.json')).accent).toBe('#3355aa');
  });

  it('never installs anything', () => {
    const target = freshTarget();
    createProject(target, { template });
    expect(existsSync(join(target, 'node_modules'))).toBe(false);
    expect(existsSync(join(target, 'package-lock.json'))).toBe(false);
  });

  it('gives a project that passes monarch check', () => {
    const target = freshTarget();
    createProject(target, { template, accent: '#3355aa' });
    const config = readJson<{ include: string[] }>(join(target, 'monarch.config.json'));
    expect(config.include).toEqual([SOURCE_DIR[template]]);
    const result = check({ root: target, include: config.include });
    expect(result.filesScanned).toBeGreaterThan(0);
    expect(result.findings.filter((f) => f.severity !== 'info')).toEqual([]);
  });

  it('copies AGENTS.md from the agent templates unchanged', () => {
    expect(read(join(STARTERS, template), 'AGENTS.md')).toBe(
      read(join(REPO, 'templates/agents'), 'AGENTS.md'),
    );
  });
});

describe('createProject: the target directory', () => {
  it('refuses a directory that is not empty and writes nothing', () => {
    const target = tempDir();
    write(target, 'notes.txt', 'mine');
    expect(() => createProject(target, { template: 'next-app' })).toThrow(CreateError);
    expect(() => createProject(target, { template: 'next-app' })).toThrow(/not empty/);
    expect(filesIn(target)).toEqual(['notes.txt']);
  });

  it('accepts an existing empty directory', () => {
    const target = tempDir();
    expect(createProject(target, { template: 'vite-react' }).files).toContain('index.html');
  });

  it('accepts a directory that holds only .git and leaves it alone', () => {
    const target = tempDir();
    write(target, '.git/HEAD', 'ref: refs/heads/main\n');
    const result = createProject(target, { template: 'vite-react' });
    expect(result.files).toContain('package.json');
    expect(result.files).not.toContain('.git/HEAD');
    expect(read(target, '.git/HEAD')).toBe('ref: refs/heads/main\n');
  });

  it('refuses a target that is a file', () => {
    const dir = tempDir();
    write(dir, 'taken', 'a file');
    const target = join(dir, 'taken');
    expect(() => createProject(target, { template: 'next-app' })).toThrow(CreateError);
    expect(() => createProject(target, { template: 'next-app' })).toThrow(/not a directory/);
    expect(read(dir, 'taken')).toBe('a file');
  });

  it('creates missing parent directories', () => {
    const target = join(tempDir(), 'a', 'b', 'app');
    createProject(target, { template: 'next-app' });
    expect(existsSync(join(target, 'package.json'))).toBe(true);
  });

  it('says what to run next', () => {
    const target = freshTarget('next steps');
    const { next } = createProject(target, { template: 'next-app' });
    expect(next).toEqual([`cd ${JSON.stringify(target)}`, 'npm install', 'npm run dev']);
    const plain = tempDir();
    expect(createProject(plain, { template: 'next-app' }).next[0]).toBe(`cd ${plain}`);
  });
});

describe('createProject: names', () => {
  it('uses the directory name by default and normalises it', () => {
    const target = freshTarget('My Shop_Front!');
    const result = createProject(target, { template: 'next-app' });
    expect(basename(result.dir)).toBe('My Shop_Front!');
    expect(result.name).toBe('my-shop_front');
    expect(readJson<{ name: string }>(join(target, 'package.json')).name).toBe('my-shop_front');
  });

  it('prefers the name option', () => {
    const target = freshTarget('folder');
    expect(createProject(target, { template: 'vite-react', name: 'Acme Portal' }).name).toBe(
      'acme-portal',
    );
  });

  it('normalises to a valid npm package name', () => {
    expect(normaliseProjectName('  Hello World  ')).toBe('hello-world');
    expect(normaliseProjectName('.hidden')).toBe('hidden');
    expect(normaliseProjectName('_private-')).toBe('private');
    expect(normaliseProjectName('a/b')).toBe('a-b');
    expect(normaliseProjectName('Café Münster')).toBe('caf-m-nster');
    expect(normaliseProjectName('v1.2')).toBe('v1.2');
    expect(normaliseProjectName('@Team/Web App')).toBe('@team/web-app');
  });

  it('refuses a name with nothing valid in it, before writing', () => {
    const target = freshTarget('ok');
    expect(() => createProject(target, { template: 'next-app', name: '!!!' })).toThrow(CreateError);
    expect(() => normaliseProjectName('')).toThrow(CreateError);
    expect(() => normaliseProjectName('@/app')).toThrow(CreateError);
    expect(() => normaliseProjectName('a'.repeat(215))).toThrow(/longer than 214/);
    expect(existsSync(target)).toBe(false);
  });
});

describe('createProject: the accent', () => {
  it.each(['#35a', '#3355aa', '#ABCDEF', ' #fff '])('accepts %s', (accent) => {
    const target = freshTarget();
    createProject(target, { template: 'next-app', accent });
    expect(readJson<{ accent: string }>(join(target, 'brand.json')).accent).toBe(
      accent.trim().toLowerCase(),
    );
  });

  it.each(['3355aa', '#3355a', '#gggggg', 'blue', '#3355aaff', '', '#fff"; x'])(
    'refuses %j before writing',
    (accent) => {
      const target = freshTarget();
      expect(() => createProject(target, { template: 'next-app', accent })).toThrow(CreateError);
      expect(existsSync(target)).toBe(false);
    },
  );
});

describe('createProject: templates', () => {
  it('refuses an unknown template', () => {
    const target = freshTarget();
    const template = 'remix' as StarterTemplate;
    expect(() => createProject(target, { template })).toThrow(CreateError);
    expect(() => createProject(target, { template })).toThrow(/next-app or vite-react/);
    expect(existsSync(target)).toBe(false);
  });

  it('refuses a template name that points outside the templates folder', () => {
    const from = fakeTemplates({ 'package.json': '{}' });
    const template = '../next-app' as StarterTemplate;
    expect(() => createProject(freshTarget(), { template, from })).toThrow(CreateError);
  });

  it('reports a templates folder without the template', () => {
    expect(() => createProject(freshTarget(), { template: 'vite-react', from: tempDir() })).toThrow(
      /not found/,
    );
  });

  it('renames _gitignore in sub-folders and returns sorted paths', () => {
    const from = fakeTemplates({
      'z.txt': '__PROJECT_NAME__',
      _gitignore: 'node_modules\n',
      'nested/_gitignore': 'out\n',
      'a/b.txt': 'b',
    });
    const target = freshTarget('demo');
    const result = createProject(target, { template: 'next-app', from });
    expect(result.files).toEqual(['.gitignore', 'a/b.txt', 'nested/.gitignore', 'z.txt']);
    expect(read(target, 'z.txt')).toBe('demo');
    expect(read(target, 'nested/.gitignore')).toBe('out\n');
  });

  it('copies binary files byte for byte', () => {
    const bytes = Buffer.concat([Buffer.from([0x89, 0x50, 0x00, 0xff]), Buffer.from('__ACCENT__')]);
    const from = fakeTemplates({ 'logo.png': bytes });
    const target = freshTarget();
    createProject(target, { template: 'next-app', from });
    expect(readFileSync(join(target, 'logo.png')).equals(bytes)).toBe(true);
  });

  it('does not follow symbolic links out of the template', () => {
    const outside = tempDir();
    write(outside, 'secret.txt', 'private');
    write(outside, 'folder/inner.txt', 'private');
    const from = fakeTemplates({ 'package.json': '{"name":"__PROJECT_NAME__"}' });
    symlinkSync(join(outside, 'secret.txt'), join(from, 'next-app', 'link.txt'));
    symlinkSync(join(outside, 'folder'), join(from, 'next-app', 'linked'), 'dir');
    const target = freshTarget();
    const result = createProject(target, { template: 'next-app', from });
    expect(result.files).toEqual(['package.json']);
    expect(filesIn(target)).toEqual(['package.json']);
  });

  it('leaves out installed or built folders inside a template', () => {
    const from = fakeTemplates({
      'package.json': '{}',
      'node_modules/pkg/index.js': '',
      '.next/cache': '',
      'dist/out.js': '',
    });
    expect(createProject(freshTarget(), { template: 'next-app', from }).files).toEqual([
      'package.json',
    ]);
  });
});

/* ---------- template integrity: no install needed ---------- */

interface PackageJson {
  readonly name: string;
  readonly version: string;
  readonly exports?: Readonly<Record<string, unknown>>;
  readonly dependencies?: Readonly<Record<string, string>>;
  readonly devDependencies?: Readonly<Record<string, string>>;
}

interface ManifestEntry {
  readonly name: string;
  readonly entry: string;
  readonly alsoEntries?: readonly string[];
  readonly scope?: string;
}

const REACT = '@rookcom/monarch-react';
const TOKENS = '@rookcom/monarch-tokens';
const CONFORMANCE = '@rookcom/monarch-conformance';
const PACKAGES: Readonly<Record<string, string>> = {
  [REACT]: join(REPO, 'packages/react'),
  [TOKENS]: join(REPO, 'packages/tokens'),
  [CONFORMANCE]: join(REPO, 'packages/conformance'),
};
const MANIFEST_PATH = join(PACKAGES[REACT] ?? '', 'dist/manifest.json');

const templateFiles = (template: StarterTemplate, ext: RegExp): string[] =>
  filesIn(join(STARTERS, template)).filter((f) => ext.test(f));

interface NamedImport {
  readonly file: string;
  readonly name: string;
  /** Manifest entry: `.` or `./ai`. */
  readonly entry: string;
}

/** Value imports from @rookcom/monarch-react entries (type-only imports are left out). */
function monarchReactImports(template: StarterTemplate): NamedImport[] {
  const pattern =
    /import\s+(type\s+)?\{([^}]*)\}\s*from\s*['"](@rookcom\/monarch-react[^'"]*)['"]/g;
  return templateFiles(template, /\.tsx?$/).flatMap((file) =>
    [...read(join(STARTERS, template), file).matchAll(pattern)].flatMap((match) => {
      if (match[1]) return [];
      const entry = `.${(match[3] ?? '').slice(REACT.length)}`;
      return (match[2] ?? '')
        .split(',')
        .map((part) => part.trim())
        .filter((part) => part !== '' && !part.startsWith('type '))
        .map((part) => ({ file, name: part.split(/\s+as\s+/)[0] ?? part, entry }));
    }),
  );
}

/** Monarch stylesheet specifiers: side-effect imports in code and `@import` in CSS. */
function monarchCssSpecifiers(template: StarterTemplate): { file: string; specifier: string }[] {
  const inCode = /^\s*import\s+['"](@rookcom\/monarch-(?:react|tokens)\/[^'"]+)['"]/gm;
  const inCss = /@import\s+['"](@rookcom\/monarch-(?:react|tokens)\/[^'"]+)['"]/g;
  const root = join(STARTERS, template);
  const find = (ext: RegExp, pattern: RegExp) =>
    templateFiles(template, ext).flatMap((file) =>
      [...read(root, file).matchAll(pattern)].map((m) => ({ file, specifier: m[1] ?? '' })),
    );
  return [...find(/\.(?:tsx?|mjs)$/, inCode), ...find(/\.css$/, inCss)];
}

/** Resolves a subpath through a package.json `exports` map, patterns (`./styles/*.css`) included. */
function resolveExport(pkg: PackageJson, subpath: string): string | null {
  const targetOf = (value: unknown): string | null => {
    if (typeof value === 'string') return value;
    if (typeof value !== 'object' || value === null) return null;
    const fallback = (value as Record<string, unknown>).default;
    return typeof fallback === 'string' ? fallback : null;
  };
  const exports = pkg.exports ?? {};
  if (subpath in exports) return targetOf(exports[subpath]);
  for (const [key, value] of Object.entries(exports)) {
    const [before, after] = key.split('*');
    if (before === undefined || after === undefined) continue;
    if (!subpath.startsWith(before) || !subpath.endsWith(after)) continue;
    if (subpath.length < before.length + after.length + 1) continue;
    const star = subpath.slice(before.length, subpath.length - after.length);
    const target = targetOf(value);
    if (target && !star.includes('/')) return target.replace('*', star);
  }
  return null;
}

function resolveSpecifier(specifier: string): string | null {
  const name = [REACT, TOKENS].find((p) => specifier === p || specifier.startsWith(`${p}/`));
  const dir = name ? PACKAGES[name] : undefined;
  if (!name || !dir) return null;
  const target = resolveExport(
    readJson<PackageJson>(join(dir, 'package.json')),
    `.${specifier.slice(name.length)}`,
  );
  return target ? join(dir, target) : null;
}

describe('the exports resolver used by these tests', () => {
  const pkg: PackageJson = {
    name: 'x',
    version: '1.0.0',
    exports: {
      './css': './dist/css/monarch.css',
      './css/products/*': './dist/css/products/*.css',
      './styles/*.css': './dist/styles/*.css',
      './ai': { types: './dist/ai.d.ts', default: './dist/ai.js' },
    },
  };

  it('resolves exact, conditional and pattern exports', () => {
    expect(resolveExport(pkg, './css')).toBe('./dist/css/monarch.css');
    expect(resolveExport(pkg, './ai')).toBe('./dist/ai.js');
    expect(resolveExport(pkg, './css/products/neutral')).toBe('./dist/css/products/neutral.css');
    expect(resolveExport(pkg, './styles/core.css')).toBe('./dist/styles/core.css');
  });

  it('returns null for a path that is not exported', () => {
    expect(resolveExport(pkg, './dist/css/monarch.css')).toBeNull();
    expect(resolveExport(pkg, './styles/core')).toBeNull();
    expect(resolveExport(pkg, './styles/.css')).toBeNull();
    expect(resolveExport(pkg, './css/base')).toBeNull();
  });
});

describe.each(STARTER_TEMPLATES)('template integrity: %s', (template) => {
  const hasManifest = existsSync(MANIFEST_PATH);
  if (!hasManifest)
    console.warn(
      `create.test.ts: skipping the import checks for ${template}: ${MANIFEST_PATH} is missing. Build @rookcom/monarch-react first.`,
    );

  it.skipIf(!hasManifest)('imports only names the manifest has, from the right entry', () => {
    const { components } = readJson<{ components: ManifestEntry[] }>(MANIFEST_PATH);
    const imports = monarchReactImports(template);
    expect(imports.length).toBeGreaterThan(5);
    expect(imports.some((i) => i.entry === './ai')).toBe(true);
    const unknown = imports.filter(
      (i) =>
        !components.some(
          (c) => c.name === i.name && (c.entry === i.entry || c.alsoEntries?.includes(i.entry)),
        ),
    );
    expect(unknown).toEqual([]);
  });

  it.skipIf(!hasManifest)('imports nothing that is specific to Rook', () => {
    const { components } = readJson<{ components: ManifestEntry[] }>(MANIFEST_PATH);
    const rook = new Set(components.filter((c) => c.scope === 'rook').map((c) => c.name));
    expect(rook.size).toBeGreaterThan(0);
    expect(monarchReactImports(template).filter((i) => rook.has(i.name))).toEqual([]);
  });

  it('loads only stylesheets that the packages export', () => {
    const specifiers = monarchCssSpecifiers(template);
    const names = specifiers.map((s) => s.specifier);
    expect(names).toContain(`${TOKENS}/css`);
    expect(names).toContain(`${TOKENS}/css/tailwind-v4`);
    expect(names).toContain(`${TOKENS}/css/products/neutral`);
    expect(names).toContain(`${REACT}/styles/core.css`);
    expect(names).toContain(`${REACT}/styles/ai.css`);
    for (const { file, specifier } of specifiers) {
      const target = resolveSpecifier(specifier);
      expect(target, `${file}: ${specifier}`).not.toBeNull();
      expect(target, `${file}: ${specifier}`).toMatch(/\.css$/);
      // When the package is built, the file must be there too.
      const built = existsSync(join(target ?? '', '..'));
      if (built) expect(existsSync(target ?? ''), `${file}: ${specifier}`).toBe(true);
    }
  });

  it('pins the Monarch packages to the versions in this repository', () => {
    const pkg = readJson<PackageJson>(join(STARTERS, template, 'package.json'));
    const pinned = { ...pkg.dependencies, ...pkg.devDependencies };
    for (const [name, dir] of Object.entries(PACKAGES))
      expect(pinned[name], name).toBe(readJson<PackageJson>(join(dir, 'package.json')).version);
  });

  it('has the name placeholder and the scripts a new project needs', () => {
    const pkg = readJson<PackageJson & { private: boolean; scripts: Record<string, string> }>(
      join(STARTERS, template, 'package.json'),
    );
    expect(pkg.name).toBe('__PROJECT_NAME__');
    expect(pkg.private).toBe(true);
    expect(pkg.scripts.check).toBe('monarch check');
    for (const script of ['dev', 'build', 'lint']) expect(pkg.scripts[script]).toBeTruthy();
    expect(read(join(STARTERS, template), 'brand.json')).toContain('__ACCENT__');
  });

  it('ships _gitignore, never .gitignore, and does not use the word "free"', () => {
    const files = filesIn(join(STARTERS, template));
    expect(files).toContain('_gitignore');
    expect(files).not.toContain('.gitignore');
    for (const file of files.filter((f) => f !== 'AGENTS.md'))
      expect(read(join(STARTERS, template), file), file).not.toMatch(/\bfree\b/i);
  });
});
