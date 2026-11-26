import {
  REACT_PACKAGE_RE,
  attributeName,
  importedName,
  jsxNameOf,
  on,
  ruleDocsUrl,
  stringValue,
  defineRule,
} from '../ast.js';
import type {
  AstNode,
  ImportDeclaration,
  JsxAttribute,
  JsxElement,
  JsxExpressionContainer,
  JsxText,
  MonarchRule,
} from '../types.js';

const NAMING = new Set(['aria-label', 'aria-labelledby']);

/** `label=""`, `label={''}` and `label={``}`: present but empty. */
function isEmptyString(attr: JsxAttribute): boolean {
  const value = attr.value;
  if (!value) return false;
  if (stringValue(value) !== undefined) return stringValue(value)?.trim() === '';
  if (value.type !== 'JSXExpressionContainer') return false;
  const inner = (value as JsxExpressionContainer).expression;
  return stringValue(inner) !== undefined && stringValue(inner)?.trim() === '';
}

const attr = (attrs: readonly JsxAttribute[], name: string) =>
  attrs.find((a) => attributeName(a) === name);

const hasName = (attrs: readonly JsxAttribute[]) =>
  attrs.some((a) => NAMING.has(attributeName(a) ?? '') && !isEmptyString(a));

/** True for a child that is only an icon: `<TrashIcon />`, `<Icon … />`, `<svg>`. */
function isIcon(child: AstNode): boolean {
  if (child.type !== 'JSXElement') return false;
  const name = jsxNameOf((child as JsxElement).openingElement.name) ?? '';
  return /Icon$|^svg$/.test(name);
}

/** Children that can carry text: anything but white space, comments and icons. */
function hasTextContent(children: readonly AstNode[]): boolean {
  return children.some((child) => {
    if (child.type === 'JSXText') return (child as JsxText).value.trim() !== '';
    if (child.type === 'JSXExpressionContainer')
      return (child as JsxExpressionContainer).expression.type !== 'JSXEmptyExpression';
    return !isIcon(child);
  });
}

export const requireAccessibleName: MonarchRule = defineRule(
  {
    type: 'problem',
    docs: {
      description:
        'Require an accessible name on Monarch icon-only buttons: `label` on IconButton, and text, `aria-label` or `aria-labelledby` on a Button with no text.',
      url: ruleDocsUrl('require-accessible-name'),
      recommended: true,
    },
    messages: {
      iconButton:
        '`IconButton` needs a `label`: it is the accessible name and the tooltip. Screen reader users otherwise hear only "button".',
      emptyLabel: '`{{prop}}` is empty, so this control has no accessible name.',
      button:
        'This `Button` has no text, so it has no accessible name. Add text, `aria-label`, or use `IconButton` with a `label`.',
    },
    schema: [],
  },
  (context) => {
    /** Local name → Monarch export, for IconButton and Button. */
    const locals = new Map<string, 'IconButton' | 'Button'>();
    return {
      ImportDeclaration: on<ImportDeclaration>((node) => {
        if (!REACT_PACKAGE_RE.test(stringValue(node.source) ?? '')) return;
        for (const spec of node.specifiers) {
          const name = importedName(spec);
          if (name === 'IconButton' || name === 'Button') locals.set(spec.local.name, name);
        }
      }),
      JSXElement: on<JsxElement>((node) => {
        const opening = node.openingElement;
        const kind = locals.get(jsxNameOf(opening.name) ?? '');
        if (!kind) return;
        const attrs = opening.attributes;
        // Spread props can carry the name; prefer missing a case over inventing one.
        if (attrs.some((a) => a.type === 'JSXSpreadAttribute')) return;
        for (const prop of ['label', 'aria-label']) {
          const a = attr(attrs, prop);
          if (a && isEmptyString(a) && (prop === 'aria-label' || kind === 'IconButton')) {
            context.report({ node: a, messageId: 'emptyLabel', data: { prop } });
            return;
          }
        }
        if (hasName(attrs)) return;
        if (kind === 'IconButton') {
          if (!attr(attrs, 'label')) context.report({ node: opening, messageId: 'iconButton' });
          return;
        }
        if (attr(attrs, 'asChild') || attr(attrs, 'children')) return;
        if (!hasTextContent(node.children)) context.report({ node: opening, messageId: 'button' });
      }),
    };
  },
);
