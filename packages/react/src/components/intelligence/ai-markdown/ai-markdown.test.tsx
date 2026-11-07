import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  AiMarkdown,
  blockText,
  parseInline,
  parseMarkdownBlocks,
  parseMarkdownIncremental,
} from './index.js';

const md = (text: string, props: Partial<Parameters<typeof AiMarkdown>[0]> = {}) =>
  render(<AiMarkdown data-testid="md" text={text} {...props} />).container.querySelector(
    '.mn-ai-markdown',
  )!;

describe('parseMarkdownBlocks', () => {
  it('parses each block construct', () => {
    const blocks = parseMarkdownBlocks(
      [
        '# Title',
        '',
        'Setext',
        '===',
        '',
        'A paragraph',
        'on two lines.',
        '',
        '> quoted',
        '',
        '```ts title="app.ts"',
        'const a = 1;',
        '```',
        '',
        '    indented code',
        '',
        '***',
        '',
        '| a | b |',
        '| :- | -: |',
        '| 1 | 2 |',
        '',
        '[^n]: A note.',
      ].join('\n'),
    );
    expect(blocks.map((b) => b.type)).toEqual([
      'heading',
      'heading',
      'paragraph',
      'blockquote',
      'code',
      'code',
      'thematicBreak',
      'table',
      'footnoteDefinition',
    ]);
    const code = blocks[4]!;
    expect(code).toMatchObject({
      lang: 'ts',
      filename: 'app.ts',
      code: 'const a = 1;',
      closed: true,
    });
    expect(blocks[5]).toMatchObject({ code: 'indented code' });
    expect(blocks[7]).toMatchObject({ align: ['left', 'right'] });
    expect(blocks[1]).toMatchObject({ level: 1 });
  });

  it('parses nested, ordered, loose and task lists', () => {
    const [list] = parseMarkdownBlocks('3. one\n4. two\n   - nested\n     - deeper\n5. three');
    expect(list).toMatchObject({ type: 'list', ordered: true, start: 3, loose: false });
    if (list?.type !== 'list') throw new Error('not a list');
    expect(list.items).toHaveLength(3);
    const nested = list.items[1]!.children[1];
    expect(nested).toMatchObject({ type: 'list', ordered: false });
    if (nested?.type !== 'list') throw new Error('not nested');
    expect(nested.items[0]!.children[1]).toMatchObject({ type: 'list' });

    const [loose] = parseMarkdownBlocks('- a\n\n- b');
    expect(loose).toMatchObject({ loose: true });
    const [tasks] = parseMarkdownBlocks('- [x] done\n- [ ] open\n- plain');
    if (tasks?.type !== 'list') throw new Error('not a list');
    expect(tasks.items.map((i) => i.checked)).toEqual([true, false, null]);
  });

  it('marks an unclosed fence as code that is not closed', () => {
    expect(parseMarkdownBlocks('```py\nprint(1)')[0]).toMatchObject({
      type: 'code',
      lang: 'py',
      closed: false,
      code: 'print(1)',
    });
  });

  it('parses inline emphasis, strong, strikethrough, code, links and breaks', () => {
    expect(parseInline('*em* **strong** ~~del~~ `x` ***both***')).toEqual([
      { type: 'em', children: [{ type: 'text', value: 'em' }] },
      { type: 'text', value: ' ' },
      { type: 'strong', children: [{ type: 'text', value: 'strong' }] },
      { type: 'text', value: ' ' },
      { type: 'del', children: [{ type: 'text', value: 'del' }] },
      { type: 'text', value: ' ' },
      { type: 'code', value: 'x' },
      { type: 'text', value: ' ' },
      { type: 'em', children: [{ type: 'strong', children: [{ type: 'text', value: 'both' }] }] },
    ]);
    expect(parseInline('snake_case_name and 2 * 3 * 4')).toEqual([
      { type: 'text', value: 'snake_case_name and 2 * 3 * 4' },
    ]);
    expect(parseInline('[a *b*](https://example.com "T")')[0]).toMatchObject({
      type: 'link',
      href: 'https://example.com',
      title: 'T',
    });
    expect(parseInline('line  \nnext').map((n) => n.type)).toEqual(['text', 'break', 'text']);
    expect(parseInline('see https://example.com/a_b. or www.example.org')).toMatchObject([
      { type: 'text', value: 'see ' },
      { type: 'link', href: 'https://example.com/a_b', autolink: true },
      { type: 'text', value: '. or ' },
      { type: 'link', href: 'https://www.example.org', autolink: true },
    ]);
    expect(parseInline('<https://example.com> \\*not em\\* &amp;')).toMatchObject([
      { type: 'link', href: 'https://example.com' },
      { type: 'text', value: ' *not em* &' },
    ]);
  });

  it('keeps completed blocks when re-parsing a growing text', () => {
    const first = parseMarkdownIncremental('Para one.\n\nPara tw');
    const next = parseMarkdownIncremental('Para one.\n\nPara two.\n\n- item', first);
    expect(next.blocks[0]).toBe(first.blocks[0]);
    expect(next.blocks.map((b) => b.type)).toEqual(['paragraph', 'paragraph', 'list']);
    const reset = parseMarkdownIncremental('Other', next);
    expect(reset.blocks).toHaveLength(1);
    expect(reset.blocks[0]).not.toBe(first.blocks[0]);
  });

  it('extracts plain text', () => {
    const blocks = parseMarkdownBlocks('**Hi** [there](https://example.com).\n\n```\ncode\n```');
    expect(blocks.map((b) => blockText(b, { code: () => 'Code block' }))).toEqual([
      'Hi there.',
      'Code block',
    ]);
  });
});

describe('AiMarkdown', () => {
  it('renders headings below the page outline (# is h3 by default, offset configurable)', () => {
    md('# One\n## Two\n###### Six');
    expect(screen.getByRole('heading', { level: 3, name: 'One' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 4, name: 'Two' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 6, name: 'Six' })).toBeInTheDocument();
  });

  it('honours headingLevelOffset', () => {
    md('# One', { headingLevelOffset: 0 });
    expect(screen.getByRole('heading', { level: 1, name: 'One' })).toBeInTheDocument();
  });

  it('renders inline formatting as elements', () => {
    const root = md('*em* **strong** ~~gone~~ `code`');
    expect(root.querySelector('em')).toHaveTextContent('em');
    expect(root.querySelector('strong')).toHaveTextContent('strong');
    expect(root.querySelector('del')).toHaveTextContent('gone');
    expect(root.querySelector('code.mn-ai-markdown__code')).toHaveTextContent('code');
  });

  it('renders safe links with rel and drops unsafe ones', () => {
    md(
      '[Docs](https://example.com/docs) [bad](javascript:alert(1)) [data](data:text/html,x) <https://example.org>',
    );
    const docs = screen.getByRole('link', { name: 'Docs' });
    expect(docs).toHaveAttribute('href', 'https://example.com/docs');
    expect(docs).toHaveAttribute('rel', 'noopener noreferrer nofollow');
    expect(docs).not.toHaveAttribute('target');
    expect(screen.queryByRole('link', { name: 'bad' })).toBeNull();
    expect(screen.getByText('bad')).toHaveClass('mn-ai-markdown__unsafe-link');
    expect(screen.queryByRole('link', { name: 'data' })).toBeNull();
    expect(screen.getByRole('link', { name: 'https://example.org' })).toBeInTheDocument();
    expect(document.querySelector('a[href^="javascript"]')).toBeNull();
  });

  it('opens external links in a new tab only when asked, and says so', () => {
    md('[Docs](https://example.com) [Local](/help)', { externalLinksInNewTab: true });
    expect(screen.getByRole('link', { name: 'Docs (opens in a new tab)' })).toHaveAttribute(
      'target',
      '_blank',
    );
    expect(screen.getByRole('link', { name: 'Local' })).not.toHaveAttribute('target');
  });

  it('shows raw HTML as text', () => {
    const root = md('<script>alert(1)</script>\n\nHi <img src=x onerror=alert(1)> <b>bold</b>');
    expect(root.querySelector('script, img, b')).toBeNull();
    expect(root).toHaveTextContent('<script>alert(1)</script>');
    expect(root).toHaveTextContent('<b>bold</b>');
    expect(renderToString(<AiMarkdown text="<b>x</b>" />)).toContain('&lt;b&gt;x&lt;/b&gt;');
  });

  it('renders images only with allowImages, alt text and a safe source', () => {
    const { rerender } = render(<AiMarkdown text="![A chart](https://example.com/c.png)" />);
    expect(screen.queryByRole('img')).toBeNull();
    expect(screen.getByText('A chart')).toHaveClass('mn-ai-markdown__image-alt');
    rerender(
      <AiMarkdown
        allowImages
        text="![A chart](https://example.com/c.png) ![](https://example.com/x.png) ![bad](javascript:x)"
      />,
    );
    expect(screen.getAllByRole('img')).toHaveLength(1);
    expect(screen.getByRole('img', { name: 'A chart' })).toHaveAttribute(
      'referrerpolicy',
      'no-referrer',
    );
  });

  it('renders ordered, nested and task lists', () => {
    const root = md('2. first\n3. second\n   - nested\n\n- [x] shipped\n- [ ] pending');
    const ol = root.querySelector('ol')!;
    expect(ol).toHaveAttribute('start', '2');
    expect(within(ol).getByText('nested').closest('ul')).toBeInTheDocument();
    const boxes = screen.getAllByRole('checkbox', { name: 'Task' });
    expect(boxes.map((b) => (b as HTMLInputElement).checked)).toEqual([true, false]);
    expect(boxes[0]).toBeDisabled();
  });

  it('renders tables with alignment inside a labelled, focusable scroll region', () => {
    md('| Name | Count |\n| :-- | --: |\n| Alpha | 3 |\n| Beta |');
    const region = screen.getByRole('region', { name: 'Table' });
    expect(region).toHaveAttribute('tabindex', '0');
    expect(screen.getAllByRole('columnheader').map((h) => h.textContent)).toEqual([
      'Name',
      'Count',
    ]);
    const cells = screen.getAllByRole('cell');
    expect(cells[1]).toHaveStyle({ textAlign: 'right' });
    expect(cells).toHaveLength(4);
  });

  it('renders blockquotes, rules and hard breaks', () => {
    const root = md('> quote\n\n---\n\na\\\nb');
    expect(root.querySelector('blockquote')).toHaveTextContent('quote');
    expect(root.querySelector('hr')).toBeInTheDocument();
    expect(root.querySelector('p br')).toBeInTheDocument();
  });

  it('renders fenced code through AiCodeBlock, or renderCode when it returns something', () => {
    const renderCode = vi.fn((lang: string, code: string) =>
      lang === 'mermaid' ? <figure>{code}</figure> : undefined,
    );
    md('```mermaid\ngraph TD\n```\n\n```js\nlet a\n```', { renderCode });
    expect(screen.getByRole('figure')).toHaveTextContent('graph TD');
    expect(screen.getByRole('region', { name: 'JavaScript code' })).toHaveTextContent('let a');
    expect(renderCode).toHaveBeenCalledWith('js', 'let a');
  });

  it('renders footnote references and a footnotes list with back links', () => {
    md('Claim[^a] and again[^a], other[^b], missing[^x].\n\n[^b]: Second.\n[^a]: First *note*.');
    const refs = screen.getAllByRole('link', { name: /^Footnote/ });
    expect(refs.map((r) => r.textContent)).toEqual(['1', '1', '2']);
    expect(refs[0]!.id).not.toBe('');
    expect(refs[1]!.id).toBe('');
    const list = screen.getByRole('list', { name: 'Footnotes' });
    const items = within(list).getAllByRole('listitem');
    expect(items[0]).toHaveTextContent('First note.');
    expect(refs[0]).toHaveAttribute('href', `#${items[0]!.id}`);
    expect(within(items[0]!).getByRole('link', { name: 'Back to reference 1' })).toHaveAttribute(
      'href',
      `#${refs[0]!.id}`,
    );
    expect(screen.getByText(/missing\[\^x\]/)).toBeInTheDocument();
  });

  it('has no axe violations for a full answer', async () => {
    md(
      '# Plan\n\nIntro with a [link](https://example.com)[^1].\n\n- [x] Step one\n- [ ] Step two\n\n| A | B |\n| - | - |\n| 1 | 2 |\n\n```ts\nconst x = 1;\n```\n\n[^1]: Source.',
    );
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiMarkdown text={'# Hi\n\n**bold** [x](https://example.com)\n\n```js\n1\n```'} />,
    );
    expect(html).toContain('mn-ai-markdown');
    expect(html).toContain('<h3');
    expect(html).toContain('<strong>bold</strong>');
    expect(html).toContain('mn-ai-code-block');
  });
});
