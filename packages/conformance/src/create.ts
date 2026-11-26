/**
 * `monarch create`: copies a starter app (templates/starters in the Monarch
 * repo, bundled into this package at build time) into a new directory and
 * fills in the project name and, optionally, a brand colour. It only writes
 * files: it never installs packages and never starts a process.
 */
import {
  existsSync,
  lstatSync,
  mkdirSync,
  readdirSync,
  readFileSync,
  writeFileSync,
} from 'node:fs';
import { basename, dirname, isAbsolute, join, relative, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

export const STARTER_TEMPLATES = ['next-app', 'vite-react'] as const;
export type StarterTemplate = (typeof STARTER_TEMPLATES)[number];

export function isStarterTemplate(value: string): value is StarterTemplate {
  return (STARTER_TEMPLATES as readonly string[]).includes(value);
}

/** Bundled (dist/templates/starters) or a Monarch checkout's templates/starters. */
export function startersDir(): string {
  const here = fileURLToPath(new URL('.', import.meta.url));
  const candidates = [
    join(here, 'templates', 'starters'),
    join(here, '..', '..', '..', 'templates', 'starters'),
  ];
  const found = candidates.find((c) => existsSync(c));
  if (!found) throw new CreateError('starter templates not found in this package');
  return found;
}

export interface CreateOptions {
  readonly template: StarterTemplate;
  /** Package name. Default: the target directory's name. */
  readonly name?: string;
  /** Brand colour as `#rgb` or `#rrggbb`. Default: the neutral accent. */
  readonly accent?: string;
  /** Templates root override (the folder that holds `next-app` and `vite-react`), for tests. */
  readonly from?: string;
}

export interface CreateResult {
  /** Absolute path of the new project. */
  readonly dir: string;
  readonly name: string;
  readonly template: StarterTemplate;
  /** Files written, relative to `dir`, with `/` separators, sorted. */
  readonly files: readonly string[];
  /** Commands to run next, in order. */
  readonly next: readonly string[];
}

/** A problem the person can fix: a bad name, colour or target directory. */
export class CreateError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'CreateError';
  }
}

const NAME_TOKEN = /__PROJECT_NAME__/g;
const ACCENT_TOKEN = /__ACCENT__/g;
const ACCENT = /^#(?:[0-9a-f]{3}|[0-9a-f]{6})$/i;
/** npm's limit for a package name. */
const MAX_NAME_LENGTH = 214;
/** npm leaves `.gitignore` out of published packages, so templates ship `_gitignore`. */
const RENAMES: Readonly<Record<string, string>> = { _gitignore: '.gitignore' };
/** Never copied, in case a template was installed or built inside a checkout. */
const SKIPPED = new Set(['node_modules', '.next', 'dist', '.git', '.DS_Store']);

function normaliseSegment(segment: string): string {
  return segment
    .toLowerCase()
    .replace(/[^a-z0-9._-]+/g, '-')
    .replace(/^[._-]+/, '')
    .replace(/[.-]+$/, '');
}

/**
 * A valid npm package name from any text: lower-case, invalid characters
 * become `-`. A scope (`@team/app`) is kept. Throws when nothing valid is left.
 */
export function normaliseProjectName(input: string): string {
  const trimmed = input.trim();
  const parts = trimmed.split('/');
  const scoped = trimmed.startsWith('@') && parts.length === 2;
  const segments = scoped ? parts.map(normaliseSegment) : [normaliseSegment(trimmed)];
  if (segments.some((s) => s === ''))
    throw new CreateError(
      `"${input}" cannot be used as a package name: use letters, digits, "-", "_" or "."`,
    );
  const name = scoped ? `@${segments.join('/')}` : (segments[0] ?? '');
  if (name.length > MAX_NAME_LENGTH)
    throw new CreateError(`The package name is longer than ${MAX_NAME_LENGTH} characters`);
  return name;
}

function normaliseAccent(accent: string | undefined): string {
  if (accent === undefined) return '';
  const value = accent.trim();
  if (!ACCENT.test(value))
    throw new CreateError(`"${accent}" is not a colour: use #rgb or #rrggbb, for example #3355aa`);
  return value.toLowerCase();
}

function templateDir(opts: CreateOptions): string {
  // A runtime guard as well as a type: the CLI passes whatever was typed.
  if (!isStarterTemplate(opts.template))
    throw new CreateError(
      `Unknown template "${String(opts.template)}": use ${STARTER_TEMPLATES.join(' or ')}`,
    );
  const dir = join(opts.from ?? startersDir(), opts.template);
  if (!existsSync(dir) || !lstatSync(dir).isDirectory())
    throw new CreateError(`Template "${opts.template}" not found at ${dir}`);
  return dir;
}

/** Creates the target directory, or checks that an existing one is empty (`.git` aside). */
function prepareTarget(dir: string): void {
  if (!existsSync(dir)) {
    mkdirSync(dir, { recursive: true });
    return;
  }
  if (!lstatSync(dir).isDirectory()) throw new CreateError(`${dir} exists and is not a directory`);
  if (readdirSync(dir).some((entry) => entry !== '.git'))
    throw new CreateError(`${dir} is not empty: choose a new directory or empty this one`);
}

/**
 * Regular files under `dir`, relative, with `/` separators. Symbolic links are
 * left out, so nothing outside the template is ever read.
 */
function listTemplateFiles(dir: string): string[] {
  const out: string[] = [];
  const visit = (current: string): void => {
    for (const entry of readdirSync(current).sort()) {
      if (SKIPPED.has(entry)) continue;
      const abs = join(current, entry);
      const stat = lstatSync(abs);
      if (stat.isDirectory()) visit(abs);
      else if (stat.isFile()) out.push(relative(dir, abs).split(sep).join('/'));
    }
  };
  visit(dir);
  return out;
}

/** The path a template file is written to: `_gitignore` becomes `.gitignore`. */
function targetPath(file: string): string {
  const parts = file.split('/');
  const last = parts[parts.length - 1] ?? '';
  return [...parts.slice(0, -1), RENAMES[last] ?? last].join('/');
}

function isInside(root: string, path: string): boolean {
  const rel = relative(root, path);
  return rel !== '' && !rel.startsWith('..') && !isAbsolute(rel);
}

/** Text files get the tokens filled in; anything with a NUL byte is copied as it is. */
function fill(content: Buffer, name: string, accent: string): Buffer | string {
  if (content.includes(0)) return content;
  return content.toString('utf8').replace(NAME_TOKEN, name).replace(ACCENT_TOKEN, accent);
}

function copyTemplate(from: string, to: string, name: string, accent: string): string[] {
  const written: string[] = [];
  for (const file of listTemplateFiles(from)) {
    const rel = targetPath(file);
    const target = resolve(to, rel);
    if (!isInside(to, target)) throw new CreateError(`Refusing to write outside ${to}: ${rel}`);
    mkdirSync(dirname(target), { recursive: true });
    writeFileSync(target, fill(readFileSync(join(from, file)), name, accent));
    written.push(rel);
  }
  return written.sort();
}

const quote = (path: string): string => (/^[\w./@-]+$/.test(path) ? path : JSON.stringify(path));

function nextSteps(targetDir: string, dir: string): string[] {
  const cd = dir === resolve(process.cwd()) ? [] : [`cd ${quote(targetDir)}`];
  return [...cd, 'npm install', 'npm run dev'];
}

/**
 * Writes a starter app into `targetDir`. The directory is created when it is
 * missing and must otherwise be empty (a `.git` folder is allowed). Throws
 * `CreateError` for a bad template, name, colour or directory, before
 * anything is written.
 */
export function createProject(targetDir: string, opts: CreateOptions): CreateResult {
  const dir = resolve(targetDir);
  const from = templateDir(opts);
  const name = normaliseProjectName(opts.name ?? basename(dir));
  const accent = normaliseAccent(opts.accent);
  prepareTarget(dir);
  const files = copyTemplate(from, dir, name, accent);
  return { dir, name, template: opts.template, files, next: nextSteps(targetDir, dir) };
}
