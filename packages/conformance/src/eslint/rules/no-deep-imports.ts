import { publicPathOf } from '../../fixes.js';
import { defineRule, on, replaceRange, ruleDocsUrl, stringValue } from '../ast.js';
import type {
  AstNode,
  CallExpression,
  ExportFrom,
  ImportDeclaration,
  ImportExpression,
  Literal,
  MonarchRule,
  RuleContext,
} from '../types.js';

const DEEP_RE = /^@rookcom\/monarch-[a-z-]+\/dist(?:\/|$)/;

function checkSource(context: RuleContext, source: AstNode | null | undefined): void {
  const specifier = stringValue(source);
  if (!source || specifier === undefined || !DEEP_RE.test(specifier)) return;
  const to = publicPathOf(specifier);
  const quote = ((source as Literal).raw ?? context.sourceCode.getText(source))[0] ?? "'";
  context.report({
    node: source,
    messageId: to ? 'deep' : 'deepUnknown',
    data: { specifier, to: to ?? '' },
    ...(to ? { fix: replaceRange(source.range, `${quote}${to}${quote}`) } : {}),
  });
}

export const noDeepImports: MonarchRule = defineRule(
  {
    type: 'problem',
    docs: {
      description:
        "Disallow imports that reach into a Monarch package's dist/ folder; use the public entry instead.",
      url: ruleDocsUrl('no-deep-imports'),
      recommended: true,
    },
    fixable: 'code',
    messages: {
      deep: '`{{specifier}}` reaches into dist/, which can move in any release. Import `{{to}}`.',
      deepUnknown:
        "`{{specifier}}` reaches into dist/, which is not part of the package's exports and can move in any release. Use a public entry listed in the package's `exports`.",
    },
    schema: [],
  },
  (context) => {
    const fromSource = on<ImportDeclaration | ExportFrom>((node) =>
      checkSource(context, node.source),
    );
    return {
      ImportDeclaration: fromSource,
      ExportNamedDeclaration: fromSource,
      ExportAllDeclaration: fromSource,
      ImportExpression: on<ImportExpression>((node) => checkSource(context, node.source)),
      CallExpression: on<CallExpression>((node) => {
        const callee = node.callee as AstNode & { name?: string };
        if (callee.type === 'Identifier' && callee.name === 'require')
          checkSource(context, node.arguments[0]);
      }),
    };
  },
);
