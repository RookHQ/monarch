import { afterEach, describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiCodeBlock, tokenizeCode } from './index.js';

const SAMPLE = 'const greeting = "hello"; // say hi\nlet count = 42;\n';

afterEach(() => vi.restoreAllMocks());

describe('tokenizeCode', () => {
  it('finds keywords, strings, comments and numbers', () => {
    const types = (code: string, lang: string) =>
      tokenizeCode(code, lang)
        .filter((t) => t.type !== 'plain')
        .map((t) => `${t.type}:${t.text}`);
    expect(types(SAMPLE, 'ts')).toEqual([
      'keyword:const',
      'string:"hello"',
      'comment:// say hi',
      'keyword:let',
      'number:42',
    ]);
    expect(types('def f():\n    return """doc"""  # c', 'python')).toEqual([
      'keyword:def',
      'keyword:return',
      'string:"""doc"""',
      'comment:# c',
    ]);
    expect(types('{"a": 1, "b": true}', 'json')).toEqual([
      'property:"a"',
      'number:1',
      'property:"b"',
      'keyword:true',
    ]);
    expect(types('SELECT id FROM t -- note', 'sql')).toEqual([
      'keyword:SELECT',
      'keyword:FROM',
      'comment:-- note',
    ]);
    expect(types('<a href="/x">hi</a>', 'html')).toEqual([
      'tag:<a',
      'attr:href',
      'string:"/x"',
      'tag:>',
      'tag:</a>',
    ]);
    expect(types('a { color: red; }', 'css')).toEqual(['property:color']);
    expect(types('echo $HOME # c', 'bash')).toEqual(['keyword:echo', 'comment:# c']);
    expect(types('fn main() {}', 'rust')).toEqual(['keyword:fn']);
    expect(types('func main() {}', 'go')).toEqual(['keyword:func']);
  });

  it('returns one plain token for unknown languages', () => {
    expect(tokenizeCode('x = 1', 'cobol')).toEqual([{ type: 'plain', text: 'x = 1' }]);
  });
});

describe('AiCodeBlock', () => {
  it('shows the language, highlighted code in a labelled focusable region, and forwards props', async () => {
    render(<AiCodeBlock data-testid="root" code={SAMPLE} language="ts" />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-code-block');
    expect(root).toHaveTextContent('TypeScript');
    const region = screen.getByRole('region', { name: 'TypeScript code' });
    expect(region).toHaveAttribute('tabindex', '0');
    expect(region.querySelectorAll('.mn-ai-code-block__line')).toHaveLength(2);
    expect(region.querySelector('[data-token="keyword"]')).toHaveTextContent('const');
    await userEvent.setup().tab();
    expect(screen.getByRole('button', { name: 'Wrap lines' })).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('names the block after the filename', () => {
    render(<AiCodeBlock code="x" language="ts" filename="app.ts" onApply={() => {}} />);
    expect(screen.getByRole('region', { name: 'app.ts' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Copy app.ts' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Apply app.ts' })).toBeInTheDocument();
  });

  it('copies the exact code and confirms it', async () => {
    const user = userEvent.setup();
    const writeText = vi.spyOn(navigator.clipboard, 'writeText').mockResolvedValue();
    render(<AiCodeBlock code={SAMPLE} language="ts" />);
    await user.click(screen.getByRole('button', { name: 'Copy TypeScript code' }));
    expect(writeText).toHaveBeenCalledWith(SAMPLE);
    await waitFor(() =>
      expect(screen.getByRole('status')).toHaveTextContent('Copied to clipboard'),
    );
  });

  it('toggles wrapping with a pressed button (uncontrolled and controlled)', async () => {
    const user = userEvent.setup();
    const onWrapChange = vi.fn();
    const { container } = render(<AiCodeBlock code="x" onWrapChange={onWrapChange} />);
    const toggle = screen.getByRole('button', { name: 'Wrap lines' });
    expect(toggle).toHaveAttribute('aria-pressed', 'false');
    toggle.focus();
    await user.keyboard('{Enter}');
    expect(toggle).toHaveAttribute('aria-pressed', 'true');
    expect(container.firstChild).toHaveAttribute('data-wrap');
    expect(onWrapChange).toHaveBeenCalledWith(true);

    function Controlled() {
      const [wrap, setWrap] = useState(true);
      return (
        <AiCodeBlock code="y" wrap={wrap} onWrapChange={setWrap} showWrapToggle data-testid="c" />
      );
    }
    render(<Controlled />);
    const toggles = screen.getAllByRole('button', { name: 'Wrap lines' });
    await user.click(toggles[1]!);
    expect(screen.getByTestId('c')).not.toHaveAttribute('data-wrap');
  });

  it('renders diffs with markers and hidden added/removed text, not colour alone', () => {
    const code = '--- a/x\n+++ b/x\n@@ -1 +1 @@\n-old line\n+new line\n same';
    render(<AiCodeBlock code={code} language="diff" lineNumbers />);
    const lines = [...document.querySelectorAll('.mn-ai-code-block__line')];
    expect(lines.map((l) => l.getAttribute('data-diff'))).toEqual([
      'meta',
      'meta',
      'hunk',
      'remove',
      'add',
      'context',
    ]);
    expect(lines[3]!.querySelector('.mn-ai-code-block__marker')).toHaveTextContent('−');
    expect(lines[4]!.querySelector('.mn-ai-code-block__marker')).toHaveTextContent('+');
    expect(lines[3]).toHaveTextContent('Removed: old line');
    expect(lines[4]).toHaveTextContent('Added: new line');
    expect(lines[4]!.querySelector('.mn-ai-code-block__number')).toHaveAttribute(
      'aria-hidden',
      'true',
    );
    expect(lines[4]!.querySelector('.mn-ai-code-block__number')).toHaveTextContent('5');
  });

  it('calls onApply and onInsert with the code, disabled while incomplete', async () => {
    const user = userEvent.setup();
    const onApply = vi.fn();
    const onInsert = vi.fn();
    const { rerender } = render(
      <AiCodeBlock code="a()" language="js" onApply={onApply} onInsert={onInsert} incomplete />,
    );
    expect(screen.getByRole('button', { name: 'Apply JavaScript code' })).toBeDisabled();
    rerender(<AiCodeBlock code="a()" language="js" onApply={onApply} onInsert={onInsert} />);
    await user.click(screen.getByRole('button', { name: 'Apply JavaScript code' }));
    await user.click(screen.getByRole('button', { name: 'Insert JavaScript code' }));
    expect(onApply).toHaveBeenCalledWith('a()');
    expect(onInsert).toHaveBeenCalledWith('a()');
  });

  it('accepts messages', () => {
    render(
      <AiCodeBlock
        code="x"
        onApply={() => {}}
        messages={{ apply: 'Use', wrap: 'Wrap', copy: 'Duplicate', label: () => 'Snippet' }}
      />,
    );
    expect(screen.getByRole('button', { name: 'Use Snippet' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Wrap' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Duplicate Snippet' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Snippet' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiCodeBlock code={SAMPLE} language="ts" lineNumbers />);
    expect(html).toContain('mn-ai-code-block');
    expect(html).toContain('data-token="keyword"');
  });
});
