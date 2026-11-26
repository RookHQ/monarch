import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { check } from '../src/check.js';
import { ENTRY_COMPONENT_SHEETS } from '../src/entry-styles.js';
import {
  emptyStyleImports,
  readStyleImports,
  readSubpathImports,
  subpathCssFindings,
  type StyleImports,
} from '../src/subpath-css.js';
import { REGISTRY, tempDir, write } from './helpers.js';

const RULE = 'styles/subpath-css';

function styles(...texts: string[]): StyleImports {
  const s = emptyStyleImports();
  for (const t of texts) readStyleImports(t, s);
  return s;
}

const findings = (code: string, ...css: string[]) =>
  subpathCssFindings(readSubpathImports('src/app/page.tsx', code), styles(code, ...css));

describe('reading imports', () => {
  it('reads named, aliased, re-exported, namespace, side-effect and dynamic imports', () => {
    const code = [
      "import { Button } from '@rookcom/monarch-react';",
      "import { PageHeader as Header, ActivityWidget, type ActivityWidgetProps } from '@rookcom/monarch-react/patterns';",
      'export default function App() {}',
      "import * as Ai from '@rookcom/monarch-react/ai';",
      "export { BarChart } from '@rookcom/monarch-react/charts';",
      "import '@rookcom/monarch-react/data';",
      "const Editor = lazy(() => import('@rookcom/monarch-react/editor'));",
    ].join('\n');
    expect(readSubpathImports('a.tsx', code)).toEqual([
      {
        file: 'a.tsx',
        line: 2,
        column: 1,
        entry: 'patterns',
        names: ['PageHeader', 'ActivityWidget'],
      },
      { file: 'a.tsx', line: 4, column: 1, entry: 'ai', names: null },
      { file: 'a.tsx', line: 5, column: 1, entry: 'charts', names: ['BarChart'] },
      { file: 'a.tsx', line: 6, column: 1, entry: 'data', names: null },
      { file: 'a.tsx', line: 7, column: 27, entry: 'editor', names: null },
    ]);
  });

  it('skips type-only imports, comments and the stylesheet paths themselves', () => {
    const code = [
      "import type { AiThreadProps } from '@rookcom/monarch-react/ai';",
      "import { type BarChartProps } from '@rookcom/monarch-react/charts';",
      "// import { PageHeader } from '@rookcom/monarch-react/patterns';",
      "/* import { AgendaList } from '@rookcom/monarch-react/scheduling'; */",
      "import '@rookcom/monarch-react/styles.css';",
      "import '@rookcom/monarch-react/styles/core.css';",
    ].join('\n');
    expect(readSubpathImports('a.tsx', code)).toEqual([]);
  });

  it('reads stylesheet imports from code and CSS, ignoring commented ones', () => {
    const s = styles(
      "import '@rookcom/monarch-react/styles/core.css';",
      "@import '@rookcom/monarch-react/styles/components/ai-thread.css';\n/* @import '@rookcom/monarch-react/styles.css'; */",
      '@import "../node_modules/@rookcom/monarch-react/dist/styles/patterns.css";',
    );
    expect(s.all).toBe(false);
    expect([...s.entries].sort()).toEqual(['core', 'patterns']);
    expect([...s.components]).toEqual(['ai-thread']);
  });
});

describe('styles/subpath-css', () => {
  const page = "import { ActivityWidget } from '@rookcom/monarch-react/patterns';";
  const core = "import '@rookcom/monarch-react/styles/core.css';";

  it('reports a subpath component when only core.css is loaded, naming the import to add', () => {
    const [f, ...rest] = findings(page, core);
    expect(rest).toEqual([]);
    expect(f).toMatchObject({ file: 'src/app/page.tsx', line: 1, column: 1 });
    expect(f?.message).toContain('`ActivityWidget` comes from @rookcom/monarch-react/patterns');
    expect(f?.message).toContain("Add `import '@rookcom/monarch-react/styles/patterns.css';`");
  });

  it('is satisfied by styles.css, the entry sheet, or a sheet per component', () => {
    expect(findings(page, "import '@rookcom/monarch-react/styles.css';")).toEqual([]);
    expect(findings(page, "@import '@rookcom/monarch-react/styles/patterns.css';")).toEqual([]);
    expect(
      findings(
        page,
        core,
        "@import '@rookcom/monarch-react/styles/components/activity-widget.css';",
      ),
    ).toEqual([]);
  });

  it('names the missing component sheet when the app loads sheets per component', () => {
    const code = "import { AiThread, AiActionLog } from '@rookcom/monarch-react/ai';";
    const [f] = findings(code, "import '@rookcom/monarch-react/styles/components/ai-thread.css';");
    const sheet = ENTRY_COMPONENT_SHEETS.ai?.AiActionLog;
    expect(f?.message).toContain('`AiActionLog` comes from @rookcom/monarch-react/ai');
    expect(f?.message).toContain(
      `Add \`import '@rookcom/monarch-react/styles/components/${sheet}.css';\``,
    );
    expect(f?.message).not.toContain('AiThread`');
  });

  it('reports one finding per entry, at the first import that needs the sheet', () => {
    const imports = [
      ...readSubpathImports(
        'src/a.tsx',
        "import { useAiThread } from '@rookcom/monarch-react/ai';",
      ),
      ...readSubpathImports('src/b.tsx', "import { AiThread } from '@rookcom/monarch-react/ai';"),
      ...readSubpathImports(
        'src/c.tsx',
        "import { AiActionLog } from '@rookcom/monarch-react/ai';",
      ),
      ...readSubpathImports(
        'src/d.tsx',
        "import { BarChart } from '@rookcom/monarch-react/charts';",
      ),
    ];
    const out = subpathCssFindings(imports, styles(core));
    expect(out.map((f) => f.file)).toEqual(['src/b.tsx', 'src/d.tsx']);
    expect(out[0]?.message).toContain('`AiActionLog`, `AiThread` come from');
  });

  it('reports a namespace import only when nothing of the entry is styled', () => {
    const ns = "import * as Charts from '@rookcom/monarch-react/charts';";
    expect(findings(ns, core)).toHaveLength(1);
    expect(findings(ns, core)[0]?.message).toContain('styles/charts.css');
    expect(
      findings(ns, "import '@rookcom/monarch-react/styles/components/bar-chart.css';"),
    ).toEqual([]);
  });

  it('does not invent findings for hooks, helpers or names this version does not know', () => {
    expect(findings("import { useAiThread } from '@rookcom/monarch-react/ai';", core)).toEqual([]);
    expect(
      findings(
        "import { AiThread, AiBrandNewThing } from '@rookcom/monarch-react/ai';",
        "import '@rookcom/monarch-react/styles/components/ai-thread.css';",
      ),
    ).toEqual([]);
    // An entry newer than this version: a component name still needs styles.
    expect(
      findings("import { Foo } from '@rookcom/monarch-react/future';", core)[0]?.message,
    ).toContain('styles/future.css');
    expect(findings("import { helper } from '@rookcom/monarch-react/future';", core)).toEqual([]);
  });
});

describe('styles/subpath-css in check', () => {
  function app(): string {
    const root = tempDir();
    write(root, 'package.json', '{"name":"app"}');
    write(
      root,
      'src/app/layout.tsx',
      "import '@rookcom/monarch-react/styles/core.css';\nexport default function L() { return null; }\n",
    );
    write(
      root,
      'src/app/page.tsx',
      "import { ActivityWidget } from '@rookcom/monarch-react/patterns';\nexport default function P() { return <ActivityWidget />; }\n",
    );
    return root;
  }

  it('warns with the import line as snippet, and does not fail the run', () => {
    const r = check({ root: app(), include: ['src'] });
    const f = r.findings.filter((x) => x.rule === RULE);
    expect(f).toHaveLength(1);
    expect(f[0]).toMatchObject({ severity: 'warn', file: 'src/app/page.tsx', line: 1 });
    expect(f[0]?.snippet).toBe("import { ActivityWidget } from '@rookcom/monarch-react/patterns';");
    expect(r.exitCode).toBe(0);
  });

  it('passes once a stylesheet loads the entry', () => {
    const root = app();
    write(root, 'src/app/globals.css', "@import '@rookcom/monarch-react/styles/patterns.css';\n");
    expect(check({ root, include: ['src'] }).findings.filter((x) => x.rule === RULE)).toEqual([]);
  });

  it('honours config, a reasoned suppression and test-file exclusion', () => {
    expect(
      check({ root: app(), include: ['src'], rules: { [RULE]: 'off' } }).findings.some(
        (x) => x.rule === RULE,
      ),
    ).toBe(false);
    expect(check({ root: app(), include: ['src'], rules: { [RULE]: 'error' } }).exitCode).toBe(1);

    const root = app();
    write(
      root,
      'src/app/page.tsx',
      "// monarch-ignore-next-line styles/subpath-css -- styles come from the CDN link in index.html\nimport { ActivityWidget } from '@rookcom/monarch-react/patterns';\n",
    );
    write(root, 'src/app/page.test.tsx', "import { AiThread } from '@rookcom/monarch-react/ai';\n");
    const r = check({ root, include: ['src'] });
    expect(r.findings.filter((x) => x.rule === RULE)).toEqual([]);
    expect(r.suppressed.inline).toBe(1);
  });
});

describe('entry-styles.ts', () => {
  it('matches registry/components.json (run `npm run registry:entry-styles`)', () => {
    interface Entry {
      id: string;
      package: string;
      entry?: string;
      source?: string | null;
      exports?: string[];
    }
    const registry = JSON.parse(readFileSync(join(REGISTRY, 'components.json'), 'utf8')) as {
      components: Entry[];
    };
    // Same derivation as scripts/registry/build-entry-styles.mjs.
    const expected: Record<string, Record<string, string>> = {};
    for (const c of [...registry.components].sort((a, b) => a.id.localeCompare(b.id))) {
      if (c.package !== '@rookcom/monarch-react' || !c.source || !c.entry || c.entry === '.')
        continue;
      const entry = c.entry.replace(/^\.\//, '');
      const sheet = c.source.split('/').pop() ?? '';
      for (const name of c.exports ?? [])
        if (/^[A-Z][A-Za-z0-9]*$/.test(name) && /[a-z]/.test(name))
          (expected[entry] ??= {})[name] = sheet;
    }
    expect(ENTRY_COMPONENT_SHEETS).toEqual(expected);
  });
});
