/**
 * TypeScript-program helpers for scripts/manifest.ts: props (with defaults and
 * literal values), the component's implementation function, and the `mn-*`
 * classes on the element it forwards its ref to.
 */
import ts from 'typescript';
import { defaultFromDescription, normaliseDefault, portableType } from './manifest-lib.js';

export interface PropDoc {
  name: string;
  type: string;
  required: boolean;
  description: string;
  default?: string;
  values?: (string | number)[];
}

/** Props that every element accepts but Monarch documents anyway. */
const COMMON_PROPS = new Set(['className', 'children', 'disabled', 'id', 'asChild']);

/**
 * Where a prop is declared decides whether it is documented: Monarch's own
 * source and the component libraries it wraps (Radix and its other direct
 * dependencies) yes; inherited DOM attributes from @types/react no.
 */
export function isDocumentedSource(fileName: string, dependencies: readonly string[]): boolean {
  const f = fileName.replace(/\\/g, '/');
  if (f.includes('/packages/react/src/')) return true;
  if (f.includes('/node_modules/@types/')) return false;
  if (f.includes('/node_modules/@radix-ui/')) return true;
  return dependencies.some((d) => f.includes(`/node_modules/${d}/`));
}

export const docOf = (checker: ts.TypeChecker, sym: ts.Symbol | undefined): string =>
  sym ? ts.displayPartsToString(sym.getDocumentationComment(checker)).trim() : '';

function jsDocDefault(sym: ts.Symbol): string | undefined {
  const tag = sym.getJsDocTags().find((t) => t.name === 'default' || t.name === 'defaultValue');
  const text = tag?.text ? ts.displayPartsToString(tag.text).trim() : '';
  return text ? text.replace(/^`|`$/g, '') : undefined;
}

/** The literal members of a union type (`'sm' | 'md'`), or undefined. */
export function literalValues(
  checker: ts.TypeChecker,
  type: ts.Type,
): (string | number)[] | undefined {
  const t = checker.getNonNullableType(type);
  if (!t.isUnion()) return undefined;
  const values: (string | number)[] = [];
  for (const m of t.types) {
    if (m.isStringLiteral() || m.isNumberLiteral()) values.push(m.value);
    else return undefined;
  }
  return values.length > 1 ? values : undefined;
}

export interface PropsOptions {
  readonly checker: ts.TypeChecker;
  readonly dependencies: readonly string[];
  /** Defaults read from the implementation's destructuring. */
  readonly defaults: ReadonlyMap<string, string>;
}

export function propsOf(type: ts.Type, opts: PropsOptions): PropDoc[] {
  const { checker } = opts;
  const out: PropDoc[] = [];
  for (const p of type.getProperties()) {
    const decl = p.valueDeclaration ?? p.declarations?.[0];
    if (!decl) continue;
    const documented = isDocumentedSource(decl.getSourceFile().fileName, opts.dependencies);
    if (!documented && !COMMON_PROPS.has(p.name)) continue;
    // React internals that Radix types re-declare.
    if (p.name === 'ref' || p.name === 'key') continue;
    const t = checker.getTypeOfSymbolAtLocation(p, decl);
    const description = docOf(checker, p);
    const def = jsDocDefault(p) ?? opts.defaults.get(p.name) ?? defaultFromDescription(description);
    const values = literalValues(checker, t);
    out.push({
      name: p.name,
      type: portableType(
        checker
          .typeToString(t, decl, ts.TypeFormatFlags.NoTruncation)
          .replace(/ \| undefined$/, ''),
      ),
      required: !(p.flags & ts.SymbolFlags.Optional),
      description,
      ...(def !== undefined ? { default: normaliseDefault(def, values) } : {}),
      ...(values ? { values } : {}),
    });
  }
  return out.sort(
    (a, b) => Number(b.required) - Number(a.required) || a.name.localeCompare(b.name),
  );
}

type Fn = ts.FunctionLikeDeclaration;

const isFn = (n: ts.Node): n is Fn =>
  ts.isFunctionDeclaration(n) || ts.isFunctionExpression(n) || ts.isArrowFunction(n);

/**
 * The function that renders a component export: the declaration itself, the
 * initializer, or the function passed to forwardRef / memo (nested).
 */
export function implementationOf(decl: ts.Declaration): Fn | undefined {
  if (isFn(decl)) return decl;
  if (ts.isVariableDeclaration(decl) && decl.initializer) return unwrap(decl.initializer);
  return undefined;
}

function unwrap(expr: ts.Expression): Fn | undefined {
  let e: ts.Expression = expr;
  while (ts.isParenthesizedExpression(e) || ts.isAsExpression(e) || ts.isSatisfiesExpression(e))
    e = e.expression;
  if (isFn(e)) return e;
  if (ts.isCallExpression(e)) {
    for (const arg of e.arguments) {
      const fn = unwrap(arg);
      if (fn) return fn;
    }
  }
  return undefined;
}

/** `{ size = 'md', ...rest }` in the parameter list or `const { … } = props` in the body. */
export function destructuringDefaults(fn: Fn | undefined): Map<string, string> {
  const out = new Map<string, string>();
  if (!fn) return out;
  const read = (pattern: ts.ObjectBindingPattern) => {
    for (const el of pattern.elements) {
      if (!el.initializer || el.dotDotDotToken) continue;
      const key = el.propertyName ?? el.name;
      if (ts.isIdentifier(key) || ts.isStringLiteral(key))
        out.set(key.text, el.initializer.getText());
    }
  };
  const first = fn.parameters[0];
  if (!first) return out;
  if (ts.isObjectBindingPattern(first.name)) read(first.name);
  else if (ts.isIdentifier(first.name) && fn.body && ts.isBlock(fn.body)) {
    const propsName = first.name.text;
    for (const st of fn.body.statements) {
      if (!ts.isVariableStatement(st)) continue;
      for (const d of st.declarationList.declarations)
        if (
          ts.isObjectBindingPattern(d.name) &&
          d.initializer &&
          ts.isIdentifier(d.initializer) &&
          d.initializer.text === propsName
        )
          read(d.name);
    }
  }
  return out;
}

function mnLiterals(node: ts.Node): string[] {
  const out: string[] = [];
  const visit = (n: ts.Node) => {
    if (ts.isStringLiteralLike(n))
      for (const part of n.text.split(/\s+/)) if (part.startsWith('mn-')) out.push(part);
    n.forEachChild(visit);
  };
  visit(node);
  return out;
}

type JsxTag = ts.JsxOpeningElement | ts.JsxSelfClosingElement;

function attr(tag: JsxTag, name: string): ts.JsxAttribute | undefined {
  return tag.attributes.properties.find(
    (p): p is ts.JsxAttribute => ts.isJsxAttribute(p) && p.name.getText() === name,
  );
}

/**
 * `mn-*` literals in the className of the element the component forwards its
 * ref to; without a ref, of the first element with an `mn-*` class.
 */
/** Every `mn-*` literal in a component's function. */
export function allClassLiterals(fn: Fn | undefined): string[] {
  return fn?.body ? mnLiterals(fn.body) : [];
}

export function rootClassCandidates(fn: Fn | undefined): string[] {
  if (!fn?.body) return [];
  const refParam = fn.parameters[1]?.name;
  const refName = refParam && ts.isIdentifier(refParam) ? refParam.text : 'ref';
  const tags: JsxTag[] = [];
  const visit = (n: ts.Node) => {
    if (ts.isJsxOpeningElement(n) || ts.isJsxSelfClosingElement(n)) tags.push(n);
    n.forEachChild(visit);
  };
  visit(fn.body);
  const classesOf = (t: JsxTag) => {
    const a = attr(t, 'className');
    return a?.initializer ? mnLiterals(a.initializer) : [];
  };
  const withRef = tags.find((t) => {
    const r = attr(t, 'ref');
    const init = r?.initializer;
    return (
      init !== undefined &&
      ts.isJsxExpression(init) &&
      init.expression !== undefined &&
      init.expression.getText().includes(refName)
    );
  });
  if (withRef) {
    const own = classesOf(withRef);
    if (own.length) return own;
  }
  for (const t of tags) {
    const own = classesOf(t);
    if (own.length) return own;
  }
  return [];
}
