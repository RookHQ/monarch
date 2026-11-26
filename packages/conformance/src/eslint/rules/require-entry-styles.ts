import { readFileSync, statSync } from 'node:fs';
import { resolve } from 'node:path';
import {
  emptyStyleImports,
  readStyleImports,
  subpathCssFindings,
  type StyleImports,
  type SubpathImport,
} from '../../subpath-css.js';
import {
  defineRule,
  importedName,
  on,
  optionsOf,
  ruleDocsUrl,
  stringList,
  stringValue,
} from '../ast.js';
import type { ImportDeclaration, MonarchRule } from '../types.js';

interface Options {
  /** Monarch stylesheets the app already loads, as import specifiers. */
  readonly styles: readonly string[];
  /** Files that load the app's global styles (root layout, global CSS), relative to the ESLint working directory. */
  readonly rootFiles: readonly string[];
}

const ENTRY_RE = /^@rookcom\/monarch-react\/([a-z][a-z0-9-]*)$/;

const cache = new Map<string, { mtimeMs: number; text: string }>();

/** A configured root file's text; empty when it cannot be read. Cached until the file changes. */
function readRootFile(path: string): string {
  try {
    const { mtimeMs } = statSync(path);
    const hit = cache.get(path);
    if (hit && hit.mtimeMs === mtimeMs) return hit.text;
    const text = readFileSync(path, 'utf8');
    cache.set(path, { mtimeMs, text });
    return text;
  } catch {
    return '';
  }
}

/** The stylesheets the project loads: the configured list plus what the configured root files import. */
export function configuredStyles(
  cwd: string,
  styles: readonly string[],
  rootFiles: readonly string[],
): StyleImports {
  const out = emptyStyleImports();
  readStyleImports(styles.join('\n'), out);
  for (const file of rootFiles) readStyleImports(readRootFile(resolve(cwd, file)), out);
  return out;
}

export const requireEntryStyles: MonarchRule = defineRule(
  {
    type: 'problem',
    docs: {
      description:
        'Report an import from a subpath entry (`@rookcom/monarch-react/ai`, `/data` …) whose stylesheet the project does not load. Needs the `styles` or `rootFiles` option; without either it reports nothing.',
      url: ruleDocsUrl('require-entry-styles'),
      recommended: true,
    },
    messages: { missing: '{{message}}' },
    schema: [
      {
        type: 'object',
        additionalProperties: false,
        properties: {
          styles: { type: 'array', items: { type: 'string' } },
          rootFiles: { type: 'array', items: { type: 'string' } },
        },
      },
    ],
  },
  (context) => {
    const options = optionsOf<Options>(context);
    const styles = stringList(options.styles);
    const rootFiles = stringList(options.rootFiles);
    // Unconfigured, the rule cannot know what the app loads, and guessing would be noise.
    if (!styles && !rootFiles) return {};
    const loaded = configuredStyles(context.cwd, styles ?? [], rootFiles ?? []);
    const imports: SubpathImport[] = [];
    const nodes = new Map<string, ImportDeclaration>();
    return {
      ImportDeclaration: on<ImportDeclaration>((node) => {
        const entry = ENTRY_RE.exec(stringValue(node.source) ?? '')?.[1];
        if (!entry || node.importKind === 'type') return;
        const values = node.specifiers.filter((s) => s.importKind !== 'type');
        if (node.specifiers.length > 0 && values.length === 0) return;
        const named = values.every((s) => s.type === 'ImportSpecifier');
        const names = values.map(importedName).filter((n): n is string => n !== undefined);
        const at = { line: node.loc.start.line, column: node.loc.start.column + 1 };
        nodes.set(`${at.line}:${at.column}`, node);
        imports.push({
          file: context.filename,
          ...at,
          entry,
          names: named && values.length > 0 ? names : null,
        });
      }),
      'Program:exit': () => {
        for (const f of subpathCssFindings(imports, loaded)) {
          const node = nodes.get(`${f.line}:${f.column}`);
          if (node) context.report({ node, messageId: 'missing', data: { message: f.message } });
        }
      },
    };
  },
);
