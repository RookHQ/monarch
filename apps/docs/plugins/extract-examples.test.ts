import { readFileSync } from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';
import { ExampleExtractionError, extractDocs, extractExamples } from './extract-examples.js';
import { buildIndex, docsFiles } from './monarch-docs.js';
import { highlight } from '../src/features/code/highlight.js';

const repoRoot = path.resolve(__dirname, '../../..');

const doc = (examples: string, head = '') => `${head}
import { Button } from '@rookcom/monarch-react';
import { PlusIcon } from '../../../internal/docs-demo.js';
import type { ComponentDocs } from '../../../internal/component-docs.js';
export default {
  id: 'demo',
  title: 'Demo',
  summary: 'A demo.',
  sections: { examples: ${examples} },
} satisfies ComponentDocs;
`;

describe('extractExamples', () => {
  it('returns the render body with its imports, unwrapping a fragment', () => {
    const [ex] = extractExamples(
      'demo.docs.tsx',
      doc(`{ items: [{ title: 'Basic', render: () => (
        <>
          <Button>One</Button>
          <Button leadingIcon={<PlusIcon />}>Two</Button>
        </>
      ) }] }`),
    );
    expect(ex).toEqual({
      title: 'Basic',
      code: [
        "import { Button } from '@rookcom/monarch-react';",
        '// PlusIcon: demo icon from the docs site; use your own.',
        '<Button>One</Button>\n<Button leadingIcon={<PlusIcon />}>Two</Button>',
      ].join('\n\n'),
    });
  });

  it('includes the top-level declarations an example uses, transitively and in file order', () => {
    const [ex] = extractExamples(
      'demo.docs.tsx',
      doc(
        `{ items: [{ title: 'Stateful', render: () => <Counter /> }] }`,
        `import { useState } from 'react';
const START = 2;
const UNUSED = 1;
function Counter() {
  const [n, setN] = useState(START);
  return <Button onClick={() => setN(n + 1)}>{n}</Button>;
}`,
      ),
    );
    expect(ex?.code).toContain("import { useState } from 'react';");
    expect(ex?.code).toContain("import { Button } from '@rookcom/monarch-react';");
    expect(ex?.code).toContain('const START = 2;');
    expect(ex?.code).not.toContain('UNUSED');
    expect(ex?.code.indexOf('const START')).toBeLessThan(ex?.code.indexOf('function Counter') ?? 0);
    expect(ex?.code.endsWith('<Counter />')).toBe(true);
  });

  it('does not treat a local that shadows a top-level name as a dependency', () => {
    const [ex] = extractExamples(
      'demo.docs.tsx',
      doc(
        `{ items: [{ title: 'Shadow', render: () => { const label = 'Local'; return <Button>{label}</Button>; } }] }`,
        `const label = 'Top level';`,
      ),
    );
    expect(ex?.code).not.toContain('Top level');
    expect(ex?.code).toContain("const label = 'Local';");
  });

  it('reads id, title and summary', () => {
    expect(extractDocs('demo.docs.tsx', doc(`{ items: [] }`))).toMatchObject({
      id: 'demo',
      title: 'Demo',
      summary: 'A demo.',
      examples: [],
    });
  });

  it.each([
    ['a computed title', `{ items: [{ title: 'A' + 'B', render: () => <Button /> }] }`],
    ['a spread into items', `{ items: [...more] }`],
    ['a render that is not a function', `{ items: [{ title: 'A', render: renderer }] }`],
    ['a missing render', `{ items: [{ title: 'A' }] }`],
  ])('fails loudly on %s', (_, examples) => {
    expect(() => extractExamples('demo.docs.tsx', doc(examples))).toThrow(ExampleExtractionError);
  });
});

describe('every docs file in packages/react', () => {
  const files = docsFiles(repoRoot);
  const modules = import.meta.glob<{ default: { sections: { examples: unknown } } }>(
    '../../../packages/react/src/**/*.docs.tsx',
    { eager: true },
  );

  it('is found', () => {
    expect(files.length).toBe(Object.keys(modules).length);
    expect(files.length).toBeGreaterThan(100);
  });

  it('has one extracted snippet per example, with the same titles, and none empty', () => {
    for (const [path, mod] of Object.entries(modules)) {
      const file = files.find((f) => f.endsWith(path.replace(/^(\.\.\/)+/, '')));
      expect(file, path).toBeDefined();
      const extracted = extractExamples(file as string, readFileSync(file as string, 'utf8'));
      const section = mod.default.sections.examples as {
        items?: { title: string }[];
        partial?: { items?: { title: string }[] };
      };
      const titles = (section.items ?? section.partial?.items ?? []).map((e) => e.title);
      expect(
        extracted.map((e) => e.title),
        path,
      ).toEqual(titles);
      for (const e of extracted) expect(e.code.trim(), `${path}: ${e.title}`).not.toBe('');
    }
  });

  it('builds the site index', () => {
    const index = buildIndex(repoRoot);
    expect(index.pages.length).toBe(files.length);
    expect(index.pages.every((p) => p.exports.length > 0)).toBe(true);
  });
});

describe('highlight', () => {
  it('only colours tokens: the text is always the source', () => {
    const code = `import { A } from 'x';\n// note\n<A size="sm" onClick={() => go(1)}>Hi</A>`;
    const tokens = highlight(code);
    expect(tokens.map((t) => t.text).join('')).toBe(code);
    expect(tokens).toContainEqual({ kind: 'tag', text: 'A' });
    expect(tokens).toContainEqual({ kind: 'attr', text: 'size' });
    expect(tokens).toContainEqual({ kind: 'string', text: '"sm"' });
    expect(tokens).toContainEqual({ kind: 'comment', text: '// note' });
  });
});
