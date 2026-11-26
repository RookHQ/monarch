/** Helpers shared by the Monarch ESLint rules. */
import type {
  AstNode,
  CallExpression,
  Fix,
  ImportDeclaration,
  JsxAttribute,
  JsxName,
  Listener,
  Listeners,
  Literal,
  MonarchRule,
  Range,
  RuleContext,
  RuleMeta,
  SourceLocation,
} from './types.js';

/** The docs section that lists the ESLint rules, with a link to one rule's row. */
export const ruleDocsUrl = (name: string): string =>
  `https://monarchui.dev/conformance/#eslint-plugin:~:text=${encodeURIComponent(`monarch/${name}`)}`;

/** Gives a listener its node type; ESLint only calls it for the selector it is registered under. */
export function on<T extends AstNode>(fn: (node: T) => void): Listener {
  return (node) => fn(node as T);
}

/**
 * Builds a rule. ESLint passes its full context; the rule body sees only the
 * part declared in types.ts, which is a structural subset of it.
 */
export function defineRule(
  meta: RuleMeta,
  create: (context: RuleContext) => Listeners,
): MonarchRule {
  return { meta, create: (context) => create(context as RuleContext) };
}

export const MONARCH_PACKAGE_RE = /^@rookcom\/monarch-(?:react|tokens|conformance)(?:\/|$)/;
export const REACT_PACKAGE_RE = /^@rookcom\/monarch-react(?:\/|$)/;

export const stringValue = (node: AstNode | null | undefined): string | undefined =>
  node?.type === 'Literal' && typeof (node as Literal).value === 'string'
    ? ((node as Literal).value as string)
    : undefined;

/** `Button` for `<Button>`; undefined for `<a.b>` and `<svg:rect>`. */
export const jsxNameOf = (name: JsxName): string | undefined =>
  name.type === 'JSXIdentifier' && typeof name.name === 'string' ? name.name : undefined;

export const attributeName = (attr: JsxAttribute): string | undefined =>
  attr.type === 'JSXAttribute' && attr.name ? jsxNameOf(attr.name) : undefined;

/** The exported name of an import specifier (`Button` in `Button as B`); undefined for default and `* as`. */
export function importedName(spec: ImportDeclaration['specifiers'][number]): string | undefined {
  if (spec.type !== 'ImportSpecifier' || !spec.imported) return undefined;
  return spec.imported.type === 'Identifier' ? spec.imported.name : stringValue(spec.imported);
}

/** Functions whose string arguments are class names. */
export const CLASS_CALLEES = ['cn', 'clsx', 'cx', 'cva', 'twMerge', 'twJoin', 'classNames'];

export type StringContext = 'class' | 'style';

/**
 * Where a string sits: in `className`/`class`, in a class helper call
 * (`cn('…')`), or in a `style` attribute. Undefined anywhere else, where a
 * colour-like string is as likely to be data as styling.
 */
export function stringContextOf(
  node: AstNode,
  callees: readonly string[] = CLASS_CALLEES,
): StringContext | undefined {
  for (let p = node.parent; p; p = p.parent) {
    if (p.type === 'JSXAttribute') {
      const name = attributeName(p as JsxAttribute);
      if (name === 'className' || name === 'class') return 'class';
      return name === 'style' ? 'style' : undefined;
    }
    if (p.type === 'CallExpression') {
      const callee = (p as CallExpression).callee;
      if (
        callee.type === 'Identifier' &&
        callees.includes((callee as AstNode & { name: string }).name)
      )
        return 'class';
    }
  }
  return undefined;
}

export interface TextMatch {
  /** The matched text. */
  readonly text: string;
  readonly range: Range;
  readonly loc: SourceLocation;
  readonly groups: readonly (string | undefined)[];
}

/** Every match of a global expression in a node's source text, with its place in the file. */
export function matchesIn(context: RuleContext, node: AstNode, re: RegExp): TextMatch[] {
  // A leading space lets look-behinds match at the first character of the node.
  const text = ` ${context.sourceCode.getText(node)}`;
  const out: TextMatch[] = [];
  for (const m of text.matchAll(
    new RegExp(re.source, re.flags.includes('g') ? re.flags : `${re.flags}g`),
  )) {
    const start = node.range[0] + m.index - 1;
    const end = start + m[0].length;
    out.push({
      text: m[0],
      range: [start, end],
      loc: {
        start: context.sourceCode.getLocFromIndex(start),
        end: context.sourceCode.getLocFromIndex(end),
      },
      groups: m.slice(1),
    });
  }
  return out;
}

export const replaceRange =
  (range: Range, text: string): Fix =>
  (fixer) =>
    fixer.replaceTextRange(range, text);

/** The first options object of a rule, or an empty one. */
export function optionsOf<T extends object>(context: RuleContext): Partial<T> {
  const first = context.options[0];
  return typeof first === 'object' && first !== null ? (first as Partial<T>) : {};
}

/** A string array option, ignoring anything else. */
export const stringList = (value: unknown): string[] | undefined =>
  Array.isArray(value) ? value.filter((v): v is string => typeof v === 'string') : undefined;
