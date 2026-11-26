/**
 * The small part of ESLint's API the Monarch rules use, declared here so the
 * package builds and installs without `eslint` (an optional peer). The shapes
 * are structural subsets of ESLint 9's `Rule.RuleModule` and of ESTree / JSX
 * nodes, so a rule defined with them is a valid ESLint rule.
 */
export interface Position {
  readonly line: number;
  readonly column: number;
}

export interface SourceLocation {
  readonly start: Position;
  readonly end: Position;
}

export type Range = readonly [number, number];

export interface AstNode {
  readonly type: string;
  readonly range: Range;
  readonly loc: SourceLocation;
  readonly parent?: AstNode;
}

export interface Identifier extends AstNode {
  readonly type: 'Identifier';
  readonly name: string;
}

export interface Literal extends AstNode {
  readonly type: 'Literal';
  readonly value: unknown;
  readonly raw?: string;
}

export interface TemplateElement extends AstNode {
  readonly type: 'TemplateElement';
}

export interface ImportSpecifier extends AstNode {
  readonly type: 'ImportSpecifier' | 'ImportDefaultSpecifier' | 'ImportNamespaceSpecifier';
  /** Missing on default and namespace specifiers. A string literal name is possible (`'a-b' as x`). */
  readonly imported?: Identifier | Literal;
  readonly local: Identifier;
  /** typescript-eslint: `import { type X }`. */
  readonly importKind?: 'type' | 'value';
}

export interface ImportDeclaration extends AstNode {
  readonly type: 'ImportDeclaration';
  readonly source: Literal;
  readonly specifiers: readonly ImportSpecifier[];
  /** typescript-eslint: `import type { X }`. */
  readonly importKind?: 'type' | 'value';
}

/** `export { a } from 'x'` and `export * from 'x'`. */
export interface ExportFrom extends AstNode {
  readonly source?: Literal | null;
}

export interface CallExpression extends AstNode {
  readonly type: 'CallExpression';
  readonly callee: AstNode;
  readonly arguments: readonly AstNode[];
}

export interface ImportExpression extends AstNode {
  readonly type: 'ImportExpression';
  readonly source: AstNode;
}

export interface JsxName extends AstNode {
  readonly type: 'JSXIdentifier' | 'JSXMemberExpression' | 'JSXNamespacedName';
  /** Present on JSXIdentifier. */
  readonly name?: string | JsxName;
}

export interface JsxAttribute extends AstNode {
  readonly type: 'JSXAttribute' | 'JSXSpreadAttribute';
  /** Missing on a spread. */
  readonly name?: JsxName;
  readonly value?: AstNode | null;
}

export interface JsxOpeningElement extends AstNode {
  readonly type: 'JSXOpeningElement';
  readonly name: JsxName;
  readonly attributes: readonly JsxAttribute[];
  readonly selfClosing: boolean;
}

export interface JsxElement extends AstNode {
  readonly type: 'JSXElement';
  readonly openingElement: JsxOpeningElement;
  readonly children: readonly AstNode[];
}

export interface JsxExpressionContainer extends AstNode {
  readonly type: 'JSXExpressionContainer';
  readonly expression: AstNode;
}

export interface JsxText extends AstNode {
  readonly type: 'JSXText';
  readonly value: string;
}

export interface FunctionDeclaration extends AstNode {
  readonly type: 'FunctionDeclaration' | 'ClassDeclaration';
  readonly id: Identifier | null;
}

export interface VariableDeclarator extends AstNode {
  readonly type: 'VariableDeclarator';
  readonly id: AstNode;
  readonly init?: AstNode | null;
}

export interface Fixer {
  replaceTextRange(range: Range, text: string): unknown;
}

export type Fix = (fixer: Fixer) => unknown;

export interface Suggestion {
  readonly messageId: string;
  readonly data?: Readonly<Record<string, string>>;
  readonly fix: Fix;
}

export interface Report {
  readonly node?: AstNode;
  readonly loc?: SourceLocation;
  readonly messageId: string;
  readonly data?: Readonly<Record<string, string>>;
  readonly fix?: Fix;
  readonly suggest?: readonly Suggestion[];
}

export interface SourceCode {
  getText(node?: AstNode): string;
  getLocFromIndex(index: number): Position;
}

export interface RuleContext {
  readonly options: readonly unknown[];
  readonly filename: string;
  readonly cwd: string;
  readonly sourceCode: SourceCode;
  report(descriptor: Report): void;
}

/** ESLint calls a listener with the node; `on()` in ast.ts gives it its type. */
export type Listener = (node: unknown) => void;
export type Listeners = Record<string, Listener>;

export interface RuleMeta {
  type: 'problem' | 'suggestion';
  docs: { description: string; url: string; recommended: boolean };
  fixable?: 'code';
  hasSuggestions?: boolean;
  messages: Record<string, string>;
  schema: object[];
}

/**
 * A rule as ESLint sees it. `create` takes ESLint's own context, which this
 * package does not name (no dependency on `eslint`); `defineRule` in ast.ts
 * hands the rule body the subset declared above.
 */
export interface MonarchRule {
  meta: RuleMeta;
  create(context: unknown): Listeners;
}

export type RuleLevel = 'off' | 'warn' | 'error';

/** One flat-config object. Mutable arrays and records, as ESLint's own config type has them. */
export interface FlatConfig {
  name: string;
  files: string[];
  plugins: Record<string, MonarchPlugin>;
  rules: Record<string, RuleLevel>;
}

export interface MonarchPlugin {
  meta: { name: string; version: string };
  rules: Record<string, MonarchRule>;
  configs: { recommended: FlatConfig; general: FlatConfig };
}
