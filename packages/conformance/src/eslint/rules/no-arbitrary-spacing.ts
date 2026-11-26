import { ARB_SPACE_RE, ARB_TYPE_RE, onGrid } from '../../rules.js';
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
import type { AstNode, MonarchRule, Suggestion } from '../types.js';

interface Options {
  /**
   * Allow arbitrary spacing that sits on the 4px grid or its 2px half-step
   * (`p-[18px]`), as `monarch check` does. Default true.
   */
  readonly allowOnGrid: boolean;
  readonly callees: readonly string[];
}

/** Steps of Tailwind's default spacing scale, in units of 4px. */
const SCALE = new Set([
  0.5, 1, 1.5, 2, 2.5, 3, 3.5, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 16, 20, 24, 28, 32, 36, 40, 44, 48,
  52, 56, 60, 64, 72, 80, 96,
]);

/** `p-[8px]` → `p-2` when the value is one px or rem length on the scale. */
export function scaleClass(className: string): string | undefined {
  const m = /^(.*)-\[(\d*\.?\d+)(px|rem)\]$/.exec(className);
  if (!m) return undefined;
  const step = m[3] === 'rem' ? Number(m[2]) * 4 : Number(m[2]) / 4;
  return SCALE.has(step) ? `${m[1]}-${step}` : undefined;
}

const scaleSuggestion = (m: TextMatch): Suggestion[] => {
  const to = scaleClass(m.text);
  return to ? [{ messageId: 'useScale', data: { to }, fix: replaceRange(m.range, to) }] : [];
};

/** Values that are not sizes: colours and tokens are other rules' business. */
const NOT_A_SIZE = /^(?:#|rgba?\(|hsla?\(|oklch|color-mix|var\(|length:var)/;

export const noArbitrarySpacing: MonarchRule = defineRule(
  {
    type: 'suggestion',
    docs: {
      description:
        'Disallow arbitrary spacing and type values in class names (`p-[7px]`, `text-[13px]`); use the Tailwind scale.',
      url: ruleDocsUrl('no-arbitrary-spacing'),
      recommended: true,
    },
    hasSuggestions: true,
    messages: {
      spacing:
        'Arbitrary spacing `{{value}}`. Use the spacing scale (p-1 = 4px, p-1.5 = 6px, p-2 = 8px …) or a var(--mn-space-…).',
      type: 'Arbitrary type value `{{value}}`. Use the type scale (text-2xs…text-5xl, font-medium, leading-snug, tracking-tight).',
      useScale: 'Use `{{to}}`, the same size on the scale.',
    },
    schema: [
      {
        type: 'object',
        additionalProperties: false,
        properties: {
          allowOnGrid: { type: 'boolean' },
          callees: { type: 'array', items: { type: 'string' } },
        },
      },
    ],
  },
  (context) => {
    const options = optionsOf<Options>(context);
    const allowOnGrid = options.allowOnGrid !== false;
    const callees = stringList(options.callees) ?? CLASS_CALLEES;
    const check = on<AstNode>((node) => {
      if (stringContextOf(node, callees) !== 'class') return;
      for (const m of matchesIn(context, node, ARB_TYPE_RE)) {
        if (NOT_A_SIZE.test(m.groups[1] ?? '')) continue;
        context.report({ loc: m.loc, messageId: 'type', data: { value: m.text } });
      }
      for (const m of matchesIn(context, node, ARB_SPACE_RE)) {
        const value = m.groups[1] ?? '';
        if (/var\(|calc\(/.test(value) || (allowOnGrid && onGrid(value))) continue;
        context.report({
          loc: m.loc,
          messageId: 'spacing',
          data: { value: m.text },
          suggest: scaleSuggestion(m),
        });
      }
    });
    return { Literal: check, TemplateElement: check };
  },
);
