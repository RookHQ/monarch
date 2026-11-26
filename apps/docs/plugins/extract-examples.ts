/**
 * Build-time extraction of example source code from `<id>.docs.tsx` files.
 *
 * Every example in a docs file is `{ title, render, ... }`. The docs site shows
 * the source of `render` under each live preview ("Show code"), so the code a
 * visitor copies is the code that produced the preview: nothing is written
 * twice by hand, and nothing can drift.
 *
 * For each example the snippet is:
 *   1. the imports the example uses (only the names it needs, by module);
 *   2. the file's top-level declarations it depends on, transitively
 *      (demo components, sample data, helper types), in file order;
 *   3. the body of `render` (a JSX expression, with an outer fragment unwrapped).
 *
 * Names are resolved with the TypeScript checker, so a local variable that
 * shadows a top-level one is never mistaken for it. Anything the extractor
 * cannot read statically (a computed title, a spread into `items`, a `render`
 * that is not a function) throws an `ExampleExtractionError`, which fails the
 * docs build and the tests rather than showing wrong code.
 */
import ts from 'typescript';

export interface ExtractedExample {
  title: string;
  code: string;
}

/** What the docs site needs from a docs file without loading it. */
export interface ExtractedDocs {
  id: string;
  title: string;
  summary: string;
  examples: ExtractedExample[];
}

export class ExampleExtractionError extends Error {
  constructor(file: string, node: ts.Node | undefined, message: string) {
    let where = file;
    if (node) {
      const sf = node.getSourceFile();
      const { line, character } = sf.getLineAndCharacterOfPosition(node.getStart(sf));
      where = `${file}:${line + 1}:${character + 1}`;
    }
    super(`${where}: ${message}`);
    this.name = 'ExampleExtractionError';
  }
}

/** Module specifiers shown as-is in snippets. Relative imports are docs-only helpers. */
const isPackageImport = (specifier: string) => !specifier.startsWith('.');

/** The docs contract helpers never appear in an example's snippet. */
const DOCS_CONTRACT = /\/internal\/component-docs(\.js)?$/;

function createChecker(fileName: string, text: string) {
  const options: ts.CompilerOptions = {
    noLib: true,
    noResolve: true,
    allowJs: true,
    jsx: ts.JsxEmit.Preserve,
    target: ts.ScriptTarget.ES2022,
    module: ts.ModuleKind.ESNext,
    types: [],
  };
  const sourceFile = ts.createSourceFile(
    fileName,
    text,
    ts.ScriptTarget.ES2022,
    true,
    ts.ScriptKind.TSX,
  );
  const host = ts.createCompilerHost(options, true);
  host.getSourceFile = (name) => (name === fileName ? sourceFile : undefined);
  host.fileExists = (name) => name === fileName;
  host.readFile = (name) => (name === fileName ? text : undefined);
  host.writeFile = () => undefined;
  const program = ts.createProgram({ rootNames: [fileName], options, host });
  return { sourceFile, checker: program.getTypeChecker() };
}

function unwrap(node: ts.Expression): ts.Expression {
  let n = node;
  while (
    ts.isParenthesizedExpression(n) ||
    ts.isSatisfiesExpression(n) ||
    ts.isAsExpression(n) ||
    ts.isTypeAssertionExpression(n)
  ) {
    n = n.expression;
  }
  return n;
}

function propertyName(name: ts.PropertyName): string | undefined {
  if (ts.isIdentifier(name) || ts.isStringLiteral(name) || ts.isNoSubstitutionTemplateLiteral(name))
    return name.text;
  return undefined;
}

/** Left-aligns a block of code whose first line starts at `firstColumn` in the file. */
function dedent(code: string, firstColumn: number): string {
  const lines = code.split('\n');
  let indent = firstColumn;
  for (const line of lines.slice(1)) {
    if (line.trim() === '') continue;
    indent = Math.min(indent, line.length - line.trimStart().length);
  }
  return [lines[0] ?? '', ...lines.slice(1).map((l) => l.slice(Math.min(indent, l.length)))]
    .join('\n')
    .replace(/[ \t]+$/gm, '')
    .trim();
}

/** The example snippets of one docs file. */
export function extractExamples(fileName: string, text: string): ExtractedExample[] {
  return extractDocs(fileName, text).examples;
}

/** Id, title, summary and example snippets of one docs file. */
export function extractDocs(fileName: string, text: string): ExtractedDocs {
  const { sourceFile: sf, checker } = createChecker(fileName, text);
  const fail = (node: ts.Node | undefined, message: string): never => {
    throw new ExampleExtractionError(fileName, node, message);
  };
  const column = (node: ts.Node) => sf.getLineAndCharacterOfPosition(node.getStart(sf)).character;

  /** Top-level statement that declares a symbol, if any. */
  const topLevelStatement = (decl: ts.Node): ts.Statement | undefined => {
    let n: ts.Node = decl;
    while (n.parent && n.parent !== sf) n = n.parent;
    return n.parent === sf ? (n as ts.Statement) : undefined;
  };

  /** Follows an identifier to the initializer of a top-level `const`. */
  const resolve = (expr: ts.Expression): ts.Expression => {
    const e = unwrap(expr);
    if (!ts.isIdentifier(e)) return e;
    const decl = checker.getSymbolAtLocation(e)?.valueDeclaration;
    if (decl && ts.isVariableDeclaration(decl) && decl.initializer)
      return resolve(decl.initializer);
    return fail(e, `cannot resolve \`${e.text}\` to a value in this file`);
  };

  const objectProperty = (obj: ts.ObjectLiteralExpression, key: string) => {
    for (const p of obj.properties) {
      if (ts.isPropertyAssignment(p) && propertyName(p.name) === key) return p.initializer;
      if (ts.isShorthandPropertyAssignment(p) && p.name.text === key) return p.name;
      if (ts.isMethodDeclaration(p) && propertyName(p.name) === key) return p;
      if (ts.isSpreadAssignment(p))
        fail(p, `spread in an object is not supported here (looking for \`${key}\`)`);
    }
    return undefined;
  };

  // 1. export default { ... } (satisfies ComponentDocs)
  const exportDefault = sf.statements.find(ts.isExportAssignment);
  if (!exportDefault) return fail(undefined, 'no `export default` docs object');
  const docs = resolve(exportDefault.expression);
  if (!ts.isObjectLiteralExpression(docs)) return fail(docs, 'the default export is not an object');

  const literal = (key: string): string => {
    const v = objectProperty(docs, key);
    const e = v && !ts.isMethodDeclaration(v) ? unwrap(v) : undefined;
    if (e && (ts.isStringLiteral(e) || ts.isNoSubstitutionTemplateLiteral(e))) return e.text;
    return fail(v ?? docs, `\`${key}\` must be a string literal`);
  };
  const meta = { id: literal('id'), title: literal('title'), summary: literal('summary') };

  const sectionsExpr = objectProperty(docs, 'sections');
  if (!sectionsExpr || ts.isMethodDeclaration(sectionsExpr)) return fail(docs, 'no `sections`');
  const sections = resolve(sectionsExpr);
  if (!ts.isObjectLiteralExpression(sections)) return fail(sections, '`sections` is not an object');

  const examplesExpr = objectProperty(sections, 'examples');
  if (!examplesExpr || ts.isMethodDeclaration(examplesExpr))
    return fail(sections, 'no `sections.examples`');
  let examples = resolve(examplesExpr);

  // incomplete('reason', { items: [...] }) keeps its partial examples; without a partial there are none.
  if (ts.isCallExpression(examples)) {
    const callee = unwrap(examples.expression);
    if (!ts.isIdentifier(callee) || callee.text !== 'incomplete')
      return fail(examples, '`examples` must be an object or incomplete(...)');
    const partial = examples.arguments[1];
    if (!partial) return { ...meta, examples: [] };
    examples = resolve(partial);
  }
  if (!ts.isObjectLiteralExpression(examples)) return fail(examples, '`examples` is not an object');
  const itemsExpr = objectProperty(examples, 'items');
  if (!itemsExpr || ts.isMethodDeclaration(itemsExpr)) return fail(examples, 'no `examples.items`');
  const items = resolve(itemsExpr);
  if (!ts.isArrayLiteralExpression(items))
    return fail(items, '`examples.items` must be an array literal');

  const extracted = items.elements.map((element, index) => {
    if (ts.isSpreadElement(element)) return fail(element, 'spread in `examples.items`');
    const ex = resolve(element);
    if (!ts.isObjectLiteralExpression(ex)) return fail(ex, `example ${index + 1} is not an object`);

    const titleExpr = objectProperty(ex, 'title');
    const title =
      titleExpr &&
      !ts.isMethodDeclaration(titleExpr) &&
      (ts.isStringLiteral(unwrap(titleExpr)) ||
        ts.isNoSubstitutionTemplateLiteral(unwrap(titleExpr)))
        ? (unwrap(titleExpr) as ts.StringLiteral).text
        : fail(titleExpr ?? ex, `example ${index + 1}: the title must be a string literal`);

    const renderNode = objectProperty(ex, 'render');
    if (!renderNode) return fail(ex, `example "${title}": no \`render\``);
    let fn: ts.ArrowFunction | ts.FunctionExpression | ts.MethodDeclaration;
    if (ts.isMethodDeclaration(renderNode)) fn = renderNode;
    else {
      const r = unwrap(renderNode);
      if (!ts.isArrowFunction(r) && !ts.isFunctionExpression(r))
        return fail(r, `example "${title}": \`render\` must be a function written inline`);
      fn = r;
    }
    if (!fn.body) return fail(fn, `example "${title}": \`render\` has no body`);

    return { title, code: snippet(fn.body) };
  });
  return { ...meta, examples: extracted };

  /** Imports + dependent declarations + the render body. */
  function snippet(body: ts.ConciseBody): string {
    const statements = new Set<ts.Statement>();
    const imports = new Map<ts.ImportDeclaration, Set<ts.Node>>();
    const visited = new Set<ts.Node>();

    const collect = (root: ts.Node) => {
      const visit = (node: ts.Node) => {
        if (ts.isIdentifier(node)) {
          const symbol = checker.getSymbolAtLocation(node);
          for (const decl of symbol?.declarations ?? []) {
            if (decl.getSourceFile() !== sf) continue;
            if (
              ts.isImportSpecifier(decl) ||
              ts.isImportClause(decl) ||
              ts.isNamespaceImport(decl)
            ) {
              const importDecl = topLevelStatement(decl);
              if (importDecl && ts.isImportDeclaration(importDecl)) {
                const used = imports.get(importDecl) ?? new Set<ts.Node>();
                used.add(decl);
                imports.set(importDecl, used);
              }
              continue;
            }
            const stmt = topLevelStatement(decl);
            if (!stmt || stmt === exportDefault || ts.isImportDeclaration(stmt)) continue;
            // Locals of the render body, and the statement holding the example itself, are not dependencies.
            if (decl.pos >= body.pos && decl.end <= body.end) continue;
            if (stmt.pos <= body.pos && stmt.end >= body.end) continue;
            if (!statements.has(stmt)) {
              statements.add(stmt);
              if (!visited.has(stmt)) {
                visited.add(stmt);
                collect(stmt);
              }
            }
          }
        }
        ts.forEachChild(node, visit);
      };
      visit(root);
    };
    collect(body);

    const parts: string[] = [];

    // Imports, in file order, only the names used.
    const importLines: string[] = [];
    const localHelpers: string[] = [];
    for (const decl of sf.statements.filter(ts.isImportDeclaration)) {
      const used = imports.get(decl);
      if (!used) continue;
      const specifier = (decl.moduleSpecifier as ts.StringLiteral).text;
      if (DOCS_CONTRACT.test(specifier)) continue;
      const clause = decl.importClause;
      if (!clause) continue;
      const typeOnly = clause.isTypeOnly;
      const names: string[] = [];
      let defaultName: string | undefined;
      let namespace: string | undefined;
      for (const u of used) {
        if (ts.isImportClause(u) && u.name) defaultName = u.name.text;
        else if (ts.isNamespaceImport(u)) namespace = u.name.text;
        else if (ts.isImportSpecifier(u)) {
          const imported = u.propertyName ? `${u.propertyName.getText(sf)} as ` : '';
          names.push(`${u.isTypeOnly ? 'type ' : ''}${imported}${u.name.text}`);
        }
      }
      // Keep the file's own order of specifiers.
      const order = (
        clause.namedBindings && ts.isNamedImports(clause.namedBindings)
          ? clause.namedBindings.elements.map((e) => e.name.text)
          : []
      ) as string[];
      names.sort((a, b) => order.indexOf(lastWord(a)) - order.indexOf(lastWord(b)));
      if (!isPackageImport(specifier)) {
        localHelpers.push(...[defaultName, namespace, ...names.map(lastWord)].filter(isString));
        continue;
      }
      const bindings = [
        defaultName,
        namespace ? `* as ${namespace}` : undefined,
        names.length ? `{ ${names.join(', ')} }` : undefined,
      ].filter(isString);
      importLines.push(
        `import ${typeOnly ? 'type ' : ''}${bindings.join(', ')} from '${specifier}';`,
      );
    }
    if (importLines.length) parts.push(importLines.join('\n'));
    if (localHelpers.length)
      parts.push(
        `// ${localHelpers.join(', ')}: demo icon${localHelpers.length > 1 ? 's' : ''} from the docs site; use your own.`,
      );

    // Declarations, in file order.
    for (const stmt of sf.statements) {
      if (!statements.has(stmt)) continue;
      parts.push(dedent(sf.text.slice(stmt.getStart(sf, true), stmt.end), 0));
    }

    parts.push(renderBody(body));
    return parts.join('\n\n');
  }

  function renderBody(body: ts.ConciseBody): string {
    if (ts.isBlock(body)) {
      const stmts = body.statements;
      if (!stmts.length) return '';
      const first = stmts[0] as ts.Statement;
      const last = stmts[stmts.length - 1] as ts.Statement;
      return dedent(sf.text.slice(first.getStart(sf, true), last.end), column(first));
    }
    const expr = unwrap(body as ts.Expression);
    if (ts.isJsxFragment(expr)) {
      const children = expr.children.filter(
        (c) => !(ts.isJsxText(c) && c.containsOnlyTriviaWhiteSpaces),
      );
      const first = children[0];
      const last = children[children.length - 1];
      if (!first || !last) return '';
      return dedent(sf.text.slice(first.getStart(sf), last.end), column(first));
    }
    return dedent(expr.getText(sf), column(expr));
  }
}

const isString = (v: string | undefined): v is string => typeof v === 'string';
const lastWord = (s: string) => s.split(' ').pop() ?? s;
