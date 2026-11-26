import { BUNDLED_DEPRECATIONS, BUNDLED_IMPORT_PATH_DEPRECATIONS } from '../../bundled-data.js';
import { movedImports, movedMessage, splitImport, type ImportedName } from '../../import-paths.js';
import type { Deprecation, ImportPathDeprecation } from '../../types.js';
import {
  MONARCH_PACKAGE_RE,
  importedName,
  on,
  optionsOf,
  replaceRange,
  ruleDocsUrl,
  stringValue,
  defineRule,
} from '../ast.js';
import type { ImportDeclaration, ImportSpecifier, MonarchRule, RuleContext } from '../types.js';

interface Options {
  /** Deprecated exports. Default: the list bundled with this package. */
  readonly deprecations: readonly Deprecation[];
  /** Exports that moved to another entry. Default: the list bundled with this package. */
  readonly importPaths: readonly ImportPathDeprecation[];
}

const IDENTIFIER = /^[A-Za-z_$][\w$]*$/;

/** Exports that moved to another entry: change where they are imported from. */
function checkMoved(
  context: RuleContext,
  node: ImportDeclaration,
  source: string,
  importPaths: readonly ImportPathDeprecation[],
): void {
  const named = node.specifiers.filter((s) => s.type === 'ImportSpecifier');
  const names: (ImportedName & { spec: ImportSpecifier })[] = [];
  for (const spec of named) {
    const name = importedName(spec);
    if (name) names.push({ name, text: context.sourceCode.getText(spec), spec });
  }
  const hit = movedImports(importPaths, source, names);
  if (!hit) return;
  const { deprecation, moved, kept } = hit;
  const text = context.sourceCode.getText(node);
  const quote = (node.source.raw ?? "'")[0] ?? "'";
  // A default or namespace import beside the names, or a comment inside the
  // statement, is left for a person to move.
  const plain = named.length === node.specifiers.length && !/\/[/*]/.test(text);
  const fix =
    kept.length === 0
      ? replaceRange(node.source.range, `${quote}${deprecation.to}${quote}`)
      : plain
        ? replaceRange(
            node.range,
            splitImport(
              {
                keyword: node.importKind === 'type' ? 'import type' : 'import',
                quote,
                semicolon: text.trimEnd().endsWith(';') ? ';' : '',
                from: source,
                to: deprecation.to,
              },
              kept,
              moved,
            ),
          )
        : undefined;
  const first = names.find((n) => n.name === moved[0]?.name);
  context.report({
    node: first?.spec ?? node,
    messageId: 'moved',
    data: { message: movedMessage(deprecation, moved) },
    ...(fix ? { fix } : {}),
  });
}

/** Deprecated exports: import the replacement under the old local name, so no other line changes. */
function checkRenamed(
  context: RuleContext,
  node: ImportDeclaration,
  source: string,
  deprecations: readonly Deprecation[],
): void {
  const imported = new Set(node.specifiers.map(importedName));
  for (const spec of node.specifiers) {
    const name = importedName(spec);
    const d = deprecations.find(
      (x) => x.name === name && (source === x.package || source.startsWith(`${x.package}/`)),
    );
    if (!d || !name || !spec.imported) continue;
    const to = d.replacement;
    const fixable = to !== undefined && IDENTIFIER.test(to) && !imported.has(to);
    const aliased = spec.local.name !== name;
    context.report({
      node: spec,
      messageId: to ? 'renamed' : 'deprecated',
      data: {
        name,
        package: d.package,
        since: d.since,
        replacement: to ?? '',
        note: d.message ? ` ${d.message}` : '',
      },
      ...(fixable
        ? { fix: replaceRange(spec.imported.range, aliased ? to : `${to} as ${name}`) }
        : {}),
    });
  }
}

export const noDeprecatedApi: MonarchRule = defineRule(
  {
    type: 'suggestion',
    docs: {
      description:
        'Disallow deprecated Monarch exports and import paths; the fix imports the replacement.',
      url: ruleDocsUrl('no-deprecated-api'),
      recommended: true,
    },
    fixable: 'code',
    messages: {
      moved: '{{message}}.',
      renamed:
        '`{{name}}` from {{package}} is deprecated since {{since}}; use `{{replacement}}`.{{note}}',
      deprecated: '`{{name}}` from {{package}} is deprecated since {{since}}.{{note}}',
    },
    schema: [
      {
        type: 'object',
        additionalProperties: false,
        properties: {
          deprecations: { type: 'array', items: { type: 'object' } },
          importPaths: { type: 'array', items: { type: 'object' } },
        },
      },
    ],
  },
  (context) => {
    const options = optionsOf<Options>(context);
    const deprecations = options.deprecations ?? BUNDLED_DEPRECATIONS;
    const importPaths = options.importPaths ?? BUNDLED_IMPORT_PATH_DEPRECATIONS;
    return {
      ImportDeclaration: on<ImportDeclaration>((node) => {
        const source = stringValue(node.source);
        if (source === undefined || !MONARCH_PACKAGE_RE.test(source)) return;
        checkMoved(context, node, source, importPaths);
        checkRenamed(context, node, source, deprecations);
      }),
    };
  },
);
