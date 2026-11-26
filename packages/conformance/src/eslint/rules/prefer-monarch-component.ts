import { ENTRY_COMPONENT_SHEETS } from '../../entry-styles.js';
import { MONARCH_COMPONENTS } from '../../monarch-components.js';
import {
  MONARCH_PACKAGE_RE,
  importedName,
  on,
  optionsOf,
  ruleDocsUrl,
  stringList,
  stringValue,
  defineRule,
} from '../ast.js';
import type {
  AstNode,
  FunctionDeclaration,
  Identifier,
  ImportDeclaration,
  MonarchRule,
  VariableDeclarator,
} from '../types.js';

interface Options {
  /** Component names a project may declare itself, such as a router `Link`. */
  readonly allow: readonly string[];
}

const COMPONENT_NAMES: ReadonlySet<string> = new Set(MONARCH_COMPONENTS.map((c) => c.name));

/** The module a Monarch component is imported from. */
export function moduleOfComponent(name: string): string {
  for (const [entry, names] of Object.entries(ENTRY_COMPONENT_SHEETS))
    if (name in names) return `@rookcom/monarch-react/${entry}`;
  return '@rookcom/monarch-react';
}

/** A declaration at the top of a module, exported or not. */
function isTopLevel(node: AstNode): boolean {
  const parent = node.parent;
  if (!parent) return false;
  if (parent.type === 'Program') return true;
  return (
    /^Export(?:Named|Default)Declaration$/.test(parent.type) && parent.parent?.type === 'Program'
  );
}

const COMPONENT_INIT = new Set([
  'ArrowFunctionExpression',
  'FunctionExpression',
  'CallExpression',
  'TaggedTemplateExpression',
]);

export const preferMonarchComponent: MonarchRule = defineRule(
  {
    type: 'suggestion',
    docs: {
      description:
        'Disallow declaring a local component with the name of a Monarch component; import the Monarch one.',
      url: ruleDocsUrl('prefer-monarch-component'),
      recommended: true,
    },
    messages: {
      duplicate:
        "`{{name}}` is a Monarch component. Use `import { {{name}} } from '{{module}}';` instead of a local copy (`npx monarch add {{name}}` prints its props and styles). If this one is deliberate, add it to the rule's `allow` option.",
    },
    schema: [
      {
        type: 'object',
        additionalProperties: false,
        properties: { allow: { type: 'array', items: { type: 'string' } } },
      },
    ],
  },
  (context) => {
    const allow = new Set(stringList(optionsOf<Options>(context).allow) ?? []);
    /** Monarch exports this file imports: a local wrapper around one is not a copy. */
    const wrapped = new Set<string>();
    const declared: Identifier[] = [];
    const consider = (id: AstNode | null | undefined) => {
      if (id?.type !== 'Identifier') return;
      const name = (id as Identifier).name;
      if (COMPONENT_NAMES.has(name) && !allow.has(name)) declared.push(id as Identifier);
    };
    const declaration = on<FunctionDeclaration>((node) => {
      if (isTopLevel(node)) consider(node.id);
    });
    return {
      ImportDeclaration: on<ImportDeclaration>((node) => {
        if (!MONARCH_PACKAGE_RE.test(stringValue(node.source) ?? '')) return;
        for (const spec of node.specifiers) {
          const name = importedName(spec);
          if (name) wrapped.add(name);
        }
      }),
      FunctionDeclaration: declaration,
      ClassDeclaration: declaration,
      VariableDeclarator: on<VariableDeclarator>((node) => {
        if (!node.parent || !isTopLevel(node.parent)) return;
        if (node.init && COMPONENT_INIT.has(node.init.type)) consider(node.id);
      }),
      'Program:exit': () => {
        for (const id of declared) {
          if (wrapped.has(id.name)) continue;
          context.report({
            node: id,
            messageId: 'duplicate',
            data: { name: id.name, module: moduleOfComponent(id.name) },
          });
        }
      },
    };
  },
);
