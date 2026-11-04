/**
 * Package boundaries. The dependency direction is one-way:
 *   tokens  <-  react
 *   tokens  -/- conformance (governance never imports runtime design code)
 * and nothing may form a cycle.
 *
 * Inside @rookcom/monarch-react the source layout is enforced too
 * (docs/architecture/SOURCE-LAYOUT.md): component folders talk to each other
 * only through index.ts, patterns sit above components, src/internal is a
 * leaf, entries re-export folder indexes, and docs files use the public package.
 */
const DOCS_OR_TEST = '\\.(test|docs)\\.tsx?$';
const COMPONENT_INDEX = '^packages/react/src/components/[^/]+/[^/]+/index\\.ts$';
const PATTERN_INDEX = '^packages/react/src/patterns/[^/]+/[^/]+/index\\.ts$';
const EDITOR_FOLDERS =
  '^packages/react/src/components/collaboration/(rich-text-editor|message-composer)/';
const EDITOR_PATHS = `${EDITOR_FOLDERS}|^packages/react/src/entries/editor\\.ts$`;
const ENTRY_FILE = '^packages/react/src/(index\\.ts|entries/[^/]+\\.ts)$';
const SHELL_INTERNAL = '^packages/react/src/internal/shell/';

module.exports = {
  forbidden: [
    { name: 'no-circular', severity: 'error', from: {}, to: { circular: true } },
    {
      name: 'tokens-is-a-leaf',
      comment: 'Tokens are framework-independent: no React, no other Monarch package.',
      severity: 'error',
      from: { path: '^packages/tokens/src' },
      to: { path: ['^packages/(react|conformance)', 'node_modules/(react|react-dom|@radix-ui)/'] },
    },
    {
      name: 'react-uses-tokens-public-api-only',
      severity: 'error',
      from: { path: '^packages/react/src' },
      to: { path: ['^packages/tokens/(src|scripts)', '^packages/conformance'] },
    },
    {
      name: 'conformance-has-no-design-runtime',
      severity: 'error',
      from: { path: '^packages/conformance/src' },
      to: { path: ['^packages/(react|tokens)', 'node_modules/(react|react-dom|@radix-ui)/'] },
    },
    {
      name: 'no-test-code-in-src',
      comment:
        'Runtime source never imports tests, test helpers or docs files. Colocated <id>.test.tsx and <id>.docs.tsx are the only files in src that may.',
      severity: 'error',
      from: { path: '^packages/[^/]+/src', pathNot: DOCS_OR_TEST },
      to: {
        path: [
          '/test/',
          'node_modules/(vitest|@testing-library)/',
          DOCS_OR_TEST,
          '^packages/react/src/internal/(component-docs|docs-demo)\\.tsx?$',
        ],
      },
    },

    // ---- Source layout of @rookcom/monarch-react (docs/architecture/SOURCE-LAYOUT.md)
    {
      name: 'component-folder-boundary',
      comment:
        'A component folder imports only its own files, src/internal, and other components through their index.ts (never deep files). Tokens come from the @rookcom/monarch-tokens package, not from src.',
      severity: 'error',
      from: { path: '^(packages/react/src/components/[^/]+/[^/]+)/' },
      to: {
        path: '^packages/react/src/',
        pathNot: ['^$1/', '^packages/react/src/internal/', COMPONENT_INDEX],
      },
    },
    {
      name: 'pattern-folder-boundary',
      comment:
        'A pattern folder imports only its own files, src/internal, and components or other patterns through their index.ts.',
      severity: 'error',
      from: { path: '^(packages/react/src/patterns/[^/]+/[^/]+)/' },
      to: {
        path: '^packages/react/src/',
        pathNot: ['^$1/', '^packages/react/src/internal/', COMPONENT_INDEX, PATTERN_INDEX],
      },
    },
    {
      name: 'components-do-not-import-patterns',
      comment: 'Layers compose upwards only: patterns may use components, never the reverse.',
      severity: 'error',
      from: { path: '^packages/react/src/(components|internal)/' },
      to: { path: '^packages/react/src/patterns/' },
    },
    {
      name: 'internal-is-a-leaf',
      comment:
        'Shared helpers in src/internal never depend on components, patterns or entries. The one exception is src/internal/shell (internal-shell-uses-component-indexes).',
      severity: 'error',
      from: { path: '^packages/react/src/internal/', pathNot: [DOCS_OR_TEST, SHELL_INTERNAL] },
      to: { path: '^packages/react/src/', pathNot: '^packages/react/src/internal/' },
    },
    {
      name: 'internal-shell-uses-component-indexes',
      comment:
        'src/internal/shell holds the parts the ./shell components share (the account menu, a tooltip button), so it may compose components, through their index.ts only, and never patterns or entries.',
      severity: 'error',
      from: { path: SHELL_INTERNAL, pathNot: DOCS_OR_TEST },
      to: {
        path: '^packages/react/src/',
        pathNot: ['^packages/react/src/internal/', COMPONENT_INDEX],
      },
    },
    {
      name: 'entries-import-folder-indexes',
      comment:
        'Package entries (src/index.ts, src/entries/*.ts) re-export component and pattern folders through their index.ts, plus src/internal; nothing deeper.',
      severity: 'error',
      from: { path: ENTRY_FILE },
      to: {
        path: '^packages/react/src/',
        pathNot: [COMPONENT_INDEX, PATTERN_INDEX, '^packages/react/src/internal/'],
      },
    },
    {
      name: 'nothing-imports-an-entry',
      comment:
        'Entries are leaves of the import graph: one subpath entry never imports another, and no component imports an entry (that would pull the whole catalogue into its chunk).',
      severity: 'error',
      from: { path: '^packages/react/src/' },
      to: { path: ENTRY_FILE },
    },
    {
      name: 'docs-files-use-the-public-package',
      comment:
        "Docs files render live examples with components imported from '@rookcom/monarch-react', exactly as an app does, so the docs site never bundles a second copy of a component.",
      severity: 'error',
      from: { path: '\\.docs\\.tsx?$' },
      to: { path: '^packages/react/src/(components|patterns)/' },
    },
    // ---- ./editor (docs/architecture/PACKAGE-ARCHITECTURE.md): Tiptap is an optional peer dependency
    {
      name: 'tiptap-only-in-the-editor-entry',
      comment:
        'Only the editor folders (and so only @rookcom/monarch-react/editor) import @tiptap/*, so apps that never import ./editor never need the optional Tiptap peers.',
      severity: 'error',
      from: { path: '^packages/react/src/', pathNot: EDITOR_PATHS },
      to: { path: '(^|node_modules/)@tiptap/' },
    },
    {
      name: 'editor-only-through-its-entry',
      comment:
        'No other entry or component reaches the editor folders, which would pull Tiptap into their chunk.',
      severity: 'error',
      from: { path: '^packages/react/src/', pathNot: [EDITOR_PATHS, DOCS_OR_TEST] },
      to: { path: EDITOR_FOLDERS },
    },
    {
      name: 'no-undeclared-deps',
      severity: 'error',
      from: { path: '^packages/[^/]+/src' },
      // depcruise reads the root package.json; the Tiptap peers are declared in
      // packages/react/package.json (and are the only npm deps not excluded above).
      to: { dependencyTypes: ['npm-no-pkg', 'npm-unknown'], pathNot: 'node_modules/@tiptap/' },
    },
  ],
  options: {
    doNotFollow: { path: 'node_modules' },
    // Build output is left out, except Tiptap's: npm packages resolve into their
    // dist/, and tiptap-only-in-the-editor-entry must see those imports.
    exclude: { path: ['^(?!node_modules/@tiptap/).*dist', 'generated', 'test/fixtures'] },
    tsPreCompilationDeps: true,
    tsConfig: { fileName: 'tsconfig.base.json' },
  },
};
