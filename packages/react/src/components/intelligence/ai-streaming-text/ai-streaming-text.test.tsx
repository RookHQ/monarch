import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiStreamingText, completeStreamingMarkdown } from './index.js';

afterEach(() => vi.useRealTimers());

const live = () => document.querySelector('.mn-ai-streaming-text > [role="status"]')!;

describe('completeStreamingMarkdown', () => {
  it('closes open emphasis and code spans, hides partial links and lone delimiters', () => {
    expect(completeStreamingMarkdown('This is **bol')).toBe('This is **bol**');
    expect(completeStreamingMarkdown('An *em and `co')).toBe('An *em and `co`');
    expect(completeStreamingMarkdown('~~gone and **both')).toBe('~~gone and **both**~~');
    expect(completeStreamingMarkdown('See [the docs](https://exa')).toBe('See the docs');
    expect(completeStreamingMarkdown('See [the do')).toBe('See the do');
    expect(completeStreamingMarkdown('Trailing **')).toBe('Trailing ');
    expect(completeStreamingMarkdown('2 * 3 and snake_case')).toBe('2 * 3 and snake_case');
  });

  it('holds back table rows until the delimiter row arrives', () => {
    expect(completeStreamingMarkdown('| a | b |')).toBe('');
    expect(completeStreamingMarkdown('| a | b |\n| -- |')).toBe('');
    expect(completeStreamingMarkdown('| a | b |\n| - | - |\n| 1')).toBe(
      '| a | b |\n| - | - |\n| 1',
    );
  });

  it('leaves an open fence alone', () => {
    expect(completeStreamingMarkdown('```ts\nconst **a')).toBe('```ts\nconst **a');
  });
});

describe('AiStreamingText', () => {
  it('re-renders only the last block as text grows', () => {
    const renderCode = vi.fn(() => undefined);
    const base = '```js\nlet a = 1;\n```\n\nFirst paragraph.\n\nSecond';
    const { rerender } = render(<AiStreamingText text={base} streaming renderCode={renderCode} />);
    const first = screen.getByText('First paragraph.');
    expect(renderCode).toHaveBeenCalledTimes(1);
    rerender(<AiStreamingText text={`${base} grows`} streaming renderCode={renderCode} />);
    rerender(<AiStreamingText text={`${base} grows more.`} streaming renderCode={renderCode} />);
    rerender(
      <AiStreamingText text={`${base} grows more.\n\nThird`} streaming renderCode={renderCode} />,
    );
    expect(renderCode).toHaveBeenCalledTimes(1);
    expect(screen.getByText('First paragraph.')).toBe(first);
    expect(screen.getByText('Third')).toBeInTheDocument();
  });

  it('renders a half-open fence as code with its actions disabled', () => {
    const onApply = vi.fn();
    const { rerender } = render(
      <AiStreamingText
        text={'Here:\n\n```ts\nconst a = 1'}
        streaming
        codeBlockProps={{ onApply }}
      />,
    );
    expect(screen.getByRole('region', { name: 'TypeScript code' })).toHaveTextContent(
      'const a = 1',
    );
    expect(screen.getByRole('button', { name: 'Apply TypeScript code' })).toBeDisabled();
    rerender(
      <AiStreamingText
        text={'Here:\n\n```ts\nconst a = 1\n```'}
        streaming={false}
        codeBlockProps={{ onApply }}
      />,
    );
    expect(screen.getByRole('button', { name: 'Apply TypeScript code' })).toBeEnabled();
  });

  it('shows half-open emphasis and tables without raw syntax', () => {
    const { container, rerender } = render(<AiStreamingText text="Some **bold te" streaming />);
    expect(container.querySelector('strong')).toHaveTextContent('bold te');
    expect(container).not.toHaveTextContent('**');
    rerender(<AiStreamingText text={'Rows:\n\n| Name | Size |'} streaming />);
    expect(container).not.toHaveTextContent('|');
    rerender(<AiStreamingText text={'Rows:\n\n| Name | Size |\n| - | - |\n| A'} streaming />);
    expect(screen.getAllByRole('columnheader')).toHaveLength(2);
  });

  it('shows a caret and aria-busy only while streaming', () => {
    const { container, rerender } = render(<AiStreamingText text="Hello" streaming />);
    const content = container.querySelector('.mn-ai-streaming-text__content')!;
    expect(content).toHaveAttribute('aria-busy', 'true');
    expect(container.querySelector('p .mn-ai-streaming-text__caret')).toHaveAttribute(
      'aria-hidden',
      'true',
    );
    rerender(<AiStreamingText text="Hello" streaming={false} />);
    expect(content).not.toHaveAttribute('aria-busy');
    expect(container.querySelector('.mn-ai-streaming-text__caret')).toBeNull();
  });

  it('announces complete sentences at most once a second, then "Response complete"', () => {
    vi.useFakeTimers();
    const { rerender } = render(<AiStreamingText text="Hello wor" streaming />);
    expect(live()).toBeEmptyDOMElement();
    rerender(<AiStreamingText text="Hello world. Next" streaming />);
    expect(live()).toHaveTextContent(/^Hello world\.$/);
    rerender(<AiStreamingText text="Hello world. Next part. More" streaming />);
    rerender(<AiStreamingText text="Hello world. Next part. More text! Tail" streaming />);
    expect(live()).toHaveTextContent(/^Hello world\.$/);
    act(() => {
      vi.advanceTimersByTime(999);
    });
    expect(live()).toHaveTextContent(/^Hello world\.$/);
    act(() => {
      vi.advanceTimersByTime(1);
    });
    expect(live()).toHaveTextContent(/^Next part\. More text!$/);
    rerender(
      <AiStreamingText text="Hello world. Next part. More text! Tail end" streaming={false} />,
    );
    expect(live()).toHaveTextContent(/^Next part\. More text!$/);
    act(() => {
      vi.advanceTimersByTime(1000);
    });
    expect(live()).toHaveTextContent(/^Tail end Response complete$/);
  });

  it('announces finished paragraphs and speaks code blocks by name', () => {
    vi.useFakeTimers();
    const { rerender } = render(
      <AiStreamingText text={'Intro without stop\n\n```js\nx'} streaming />,
    );
    expect(live()).toHaveTextContent(/^Intro without stop$/);
    act(() => {
      vi.advanceTimersByTime(1000);
    });
    rerender(
      <AiStreamingText
        text={'Intro without stop\n\n```js\nx\n```\n\nAfter'}
        streaming
        messages={{ codeBlock: 'Code sample' }}
      />,
    );
    expect(live()).toHaveTextContent(/^Code sample$/);
  });

  it('does not announce an answer that was never streaming, or when announce is false', () => {
    const { rerender } = render(<AiStreamingText text="Old answer." streaming={false} />);
    expect(live()).toBeEmptyDOMElement();
    rerender(<AiStreamingText text="Old answer." streaming={false} announce={false} />);
    expect(document.querySelector('[role="status"]')).toBeNull();
  });

  it('has no axe violations while streaming', async () => {
    render(<AiStreamingText text={'## Steps\n\n1. One\n2. Two\n\nNow the **resu'} streaming />);
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiStreamingText text="Hello **wor" streaming />);
    expect(html).toContain('mn-ai-streaming-text');
    expect(html).toContain('aria-busy="true"');
    expect(html).toContain('<strong>wor</strong>');
  });
});
