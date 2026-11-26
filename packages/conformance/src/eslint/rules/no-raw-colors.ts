import { colourTokenOf } from '../../fixes.js';
import { ARBITRARY_COLOR_RE, FUNC_COLOR_RE, HEX_RE, PALETTE_RE } from '../../rules.js';
import {
  CLASS_CALLEES,
  matchesIn,
  on,
  optionsOf,
  replaceRange,
  ruleDocsUrl,
  stringContextOf,
  stringList,
  type TextMatch,
  defineRule,
} from '../ast.js';
import type { AstNode, MonarchRule, RuleContext, Suggestion } from '../types.js';

interface Options {
  /** Functions whose string arguments are class names. */
  readonly callees: readonly string[];
}

/** `var(--mn-color-…)` for a hex that equals exactly one semantic token; the conformance token data. */
function tokenSuggestion(match: TextMatch, hexAt: number, hex: string): Suggestion[] {
  const token = colourTokenOf(hex);
  if (!token) return [];
  const start = match.range[0] + hexAt;
  return [
    {
      messageId: token.invariant ? 'useToken' : 'useTokenReview',
      data: { token: token.variable },
      fix: replaceRange([start, start + hex.length], `var(${token.variable})`),
    },
  ];
}

function checkClasses(context: RuleContext, node: AstNode): void {
  for (const m of matchesIn(context, node, PALETTE_RE))
    context.report({ loc: m.loc, messageId: 'palette', data: { value: m.text } });
  for (const m of matchesIn(context, node, ARBITRARY_COLOR_RE)) {
    // `bg-[#fff]` → `bg-[var(--mn-color-…)]` when the whole value is one known hex.
    const hex = /\[(#[0-9a-fA-F]{3,8})\]$/.exec(m.text)?.[1];
    context.report({
      loc: m.loc,
      messageId: 'arbitrary',
      data: { value: m.text },
      ...(hex ? { suggest: tokenSuggestion(m, m.text.lastIndexOf(hex), hex) } : {}),
    });
  }
}

function checkStyle(context: RuleContext, node: AstNode): void {
  for (const m of matchesIn(context, node, HEX_RE))
    context.report({
      loc: m.loc,
      messageId: 'literal',
      data: { value: m.text },
      suggest: tokenSuggestion(m, 0, m.text),
    });
  for (const m of matchesIn(context, node, FUNC_COLOR_RE))
    context.report({
      loc: m.loc,
      messageId: 'literal',
      data: { value: `${m.text.trimEnd()}…)` },
    });
}

export const noRawColors: MonarchRule = defineRule(
  {
    type: 'problem',
    docs: {
      description:
        'Disallow raw colours in `className` and `style`: hex, rgb() and hsl() literals, and Tailwind palette classes such as `bg-red-500`.',
      url: ruleDocsUrl('no-raw-colors'),
      recommended: true,
    },
    hasSuggestions: true,
    messages: {
      palette:
        'Palette class `{{value}}` ignores theme, contrast mode and product accent. Use a semantic class (bg-mn-surface, text-mn-fg-muted, border-mn-line, bg-mn-danger-subtle …).',
      arbitrary:
        'Arbitrary colour class `{{value}}`. Use a semantic class (bg-mn-surface, text-mn-fg-muted …) or a token: `bg-[var(--mn-color-…)]`.',
      literal:
        'Colour literal `{{value}}`. Reference a token: var(--mn-color-…); for charts, var(--mn-color-data-1…5).',
      useToken: 'Use var({{token}}), which has this value in every theme.',
      useTokenReview:
        'Use var({{token}}), which has this value in the light theme and adapts in dark and more-contrast themes.',
    },
    schema: [
      {
        type: 'object',
        additionalProperties: false,
        properties: { callees: { type: 'array', items: { type: 'string' } } },
      },
    ],
  },
  (context) => {
    const callees = stringList(optionsOf<Options>(context).callees) ?? CLASS_CALLEES;
    const check = on<AstNode>((node) => {
      const where = stringContextOf(node, callees);
      if (where === 'class') checkClasses(context, node);
      else if (where === 'style') checkStyle(context, node);
    });
    return { Literal: check, TemplateElement: check };
  },
);
