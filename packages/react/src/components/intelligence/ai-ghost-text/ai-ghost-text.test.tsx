import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, renderHook, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  AiGhostText,
  aiGhostTextNextWord,
  applyAiRewrite,
  useAiCompletion,
  useAiRewrite,
  type AiCompletionFunction,
  type AiGhostTextProps,
} from './index.js';

beforeEach(() => {
  vi.useFakeTimers();
});
afterEach(() => {
  vi.useRealTimers();
});

/** Lets the debounce pass and the completion promise settle. */
async function settle(ms = 300) {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(ms);
  });
}

/** Types into a field the way a browser does: value, caret at the end, then `input`. */
function type(field: HTMLTextAreaElement | HTMLInputElement, value: string) {
  fireEvent.input(field, { target: { value } });
  field.setSelectionRange(value.length, value.length);
}

const suggestionOf = (root: HTMLElement) =>
  root.querySelector('.mn-ai-ghost-text__suggestion')?.textContent ?? null;

function setup(props: Partial<AiGhostTextProps> = {}, field = <textarea aria-label="Reply" />) {
  const complete = vi.fn<AiCompletionFunction>(async () => ' for your message');
  const view = render(
    <AiGhostText data-testid="root" complete={complete} {...props}>
      {field}
    </AiGhostText>,
  );
  const root = screen.getByTestId('root');
  const box = screen.getByRole('textbox', { name: 'Reply' }) as HTMLTextAreaElement;
  box.focus();
  return { ...view, root, box, complete };
}

describe('aiGhostTextNextWord', () => {
  it('takes leading space, one word and the punctuation attached to it', () => {
    expect(aiGhostTextNextWord(' for your message')).toBe(' for');
    expect(aiGhostTextNextWord('your message')).toBe('your');
    expect(aiGhostTextNextWord(' message, then')).toBe(' message,');
    expect(aiGhostTextNextWord(' (see below)')).toBe(' (see');
    expect(aiGhostTextNextWord('last')).toBe('last');
    expect(aiGhostTextNextWord('   ')).toBe('   ');
    expect(aiGhostTextNextWord('')).toBe('');
  });
});

describe('useAiCompletion', () => {
  it('debounces, passes text, cursor and a signal, and returns the suggestion', async () => {
    const complete = vi.fn<AiCompletionFunction>(async () => ' world');
    const { result } = renderHook(() => useAiCompletion({ complete, debounceMs: 200 }));
    act(() => result.current.update('hel', 3));
    expect(result.current.status).toBe('waiting');
    act(() => result.current.update('hell', 4));
    act(() => result.current.update('hello', 5));
    await settle(199);
    expect(complete).not.toHaveBeenCalled();
    await settle(1);
    expect(complete).toHaveBeenCalledTimes(1);
    expect(complete).toHaveBeenCalledWith('hello', 5, expect.any(AbortSignal));
    expect(result.current).toMatchObject({
      status: 'ready',
      suggestion: ' world',
      text: 'hello',
      cursor: 5,
    });
  });

  it('aborts the stale request and ignores its late answer', async () => {
    const signals: AbortSignal[] = [];
    const answers: ((value: string) => void)[] = [];
    const complete: AiCompletionFunction = (_text, _cursor, signal) => {
      signals.push(signal);
      return new Promise<string>((resolve) => answers.push(resolve));
    };
    const { result } = renderHook(() => useAiCompletion({ complete, debounceMs: 100 }));
    act(() => result.current.update('first', 5));
    await settle(100);
    expect(result.current.status).toBe('loading');
    act(() => result.current.update('first one', 9));
    expect(signals[0]?.aborted).toBe(true);
    await settle(100);
    await act(async () => {
      answers[0]?.(' STALE');
      answers[1]?.(' fresh');
    });
    expect(result.current.suggestion).toBe(' fresh');
    expect(signals[1]?.aborted).toBe(false);
  });

  it('asks nothing below minChars, while disabled, or while composing', async () => {
    const complete = vi.fn<AiCompletionFunction>(async () => 'x');
    const { result, rerender } = renderHook(
      ({ enabled }: { enabled: boolean }) => useAiCompletion({ complete, enabled, minChars: 4 }),
      { initialProps: { enabled: true } },
    );
    act(() => result.current.update('abc', 3));
    await settle();
    expect(complete).not.toHaveBeenCalled();
    expect(result.current.status).toBe('idle');

    act(() => result.current.setComposing(true));
    act(() => result.current.update('abcdef', 6));
    await settle();
    expect(complete).not.toHaveBeenCalled();
    act(() => result.current.setComposing(false));

    rerender({ enabled: false });
    act(() => result.current.update('abcdefg', 7));
    await settle();
    expect(complete).not.toHaveBeenCalled();

    rerender({ enabled: true });
    act(() => result.current.update('abcdefgh', 8));
    await settle();
    expect(complete).toHaveBeenCalledTimes(1);

    // Turning it off clears what is showing.
    rerender({ enabled: false });
    expect(result.current.suggestion).toBe('');
  });

  it('keeps the rest of a suggestion the person types through, without a new request', async () => {
    const complete = vi.fn<AiCompletionFunction>(async () => ' world');
    const { result } = renderHook(() => useAiCompletion({ complete }));
    act(() => result.current.update('hello', 5));
    await settle();
    act(() => result.current.update('hello w', 7));
    expect(result.current).toMatchObject({ status: 'ready', suggestion: 'orld', cursor: 7 });
    expect(complete).toHaveBeenCalledTimes(1);
    // A different character is a new question.
    act(() => result.current.update('hello wx', 8));
    expect(result.current).toMatchObject({ status: 'waiting', suggestion: '' });
  });

  it('accepts whole or by word, and stays dismissed until the text changes', async () => {
    const complete = vi.fn<AiCompletionFunction>(async () => ' big world');
    const { result } = renderHook(() => useAiCompletion({ complete }));
    expect(result.current.accept()).toBeNull();
    act(() => result.current.update('hello!', 5));
    await settle();

    let word = null as ReturnType<typeof result.current.acceptWord>;
    act(() => {
      word = result.current.acceptWord();
    });
    expect(word).toEqual({ inserted: ' big', text: 'hello big!', cursor: 9 });
    expect(result.current).toMatchObject({ status: 'ready', suggestion: ' world' });

    let all = null as ReturnType<typeof result.current.accept>;
    act(() => {
      all = result.current.accept();
    });
    expect(all).toEqual({ inserted: ' world', text: 'hello big world!', cursor: 15 });
    expect(result.current).toMatchObject({ status: 'idle', suggestion: '' });

    // The echo of the insertion asks nothing.
    act(() => result.current.update('hello big world!', 15));
    await settle();
    expect(complete).toHaveBeenCalledTimes(1);

    act(() => result.current.update('hello big world! more', 21));
    await settle();
    act(() => result.current.dismiss());
    expect(result.current.suggestion).toBe('');
    act(() => result.current.update('hello big world! more', 21));
    await settle();
    expect(complete).toHaveBeenCalledTimes(2);
  });

  it('reports a failure once and treats null as no suggestion', async () => {
    const onError = vi.fn();
    const failure = new Error('offline');
    const complete = vi
      .fn<AiCompletionFunction>()
      .mockRejectedValueOnce(failure)
      .mockResolvedValueOnce(null);
    const { result } = renderHook(() => useAiCompletion({ complete, onError }));
    act(() => result.current.update('hello', 5));
    await settle();
    expect(result.current.status).toBe('error');
    expect(onError).toHaveBeenCalledExactlyOnceWith(failure);
    act(() => result.current.update('hello there', 11));
    await settle();
    expect(result.current.status).toBe('idle');
  });

  it('cancels the timer and the request on unmount', async () => {
    let signal: AbortSignal | undefined;
    const complete = vi.fn<AiCompletionFunction>((_t, _c, s) => {
      signal = s;
      return new Promise<string>(() => undefined);
    });
    const { result, unmount } = renderHook(() => useAiCompletion({ complete }));
    act(() => result.current.update('hello', 5));
    await settle();
    unmount();
    expect(signal?.aborted).toBe(true);
    expect(vi.getTimerCount()).toBe(0);
  });
});

describe('AiGhostText', () => {
  it('shows the suggestion after the typed text, describes the field and announces it', async () => {
    const { root, box, complete } = setup();
    expect(root).toHaveClass('mn-ai-ghost-text');
    expect(box).not.toHaveAttribute('aria-describedby');
    type(box, 'Thanks');
    expect(root).toHaveAttribute('data-status', 'waiting');
    await settle();
    expect(complete).toHaveBeenCalledWith('Thanks', 6, expect.any(AbortSignal));
    expect(root).toHaveAttribute('data-suggesting', 'true');
    expect(suggestionOf(root)).toBe(' for your message');
    expect(root.querySelector('.mn-ai-ghost-text__before')).toHaveTextContent('Thanks');
    // The dimmed text is for the eyes; the words reach a screen reader through the status.
    expect(root.querySelector('.mn-ai-ghost-text__suggestion')).toHaveAttribute(
      'aria-hidden',
      'true',
    );
    expect(screen.getByRole('status')).toHaveTextContent(
      'Suggestion: for your message. Press Tab to accept it, Escape to dismiss it.',
    );
    expect(box).toHaveAccessibleDescription(/Press Tab to accept it/);
    // The field's own value is untouched until the person accepts.
    expect(box.value).toBe('Thanks');
    vi.useRealTimers();
    await expectNoA11yViolations();
  });

  it('accepts with Tab into an uncontrolled field and keeps focus there', async () => {
    const onAccept = vi.fn();
    const { root, box } = setup({ onAccept });
    type(box, 'Thanks');
    await settle();
    const tab = fireEvent.keyDown(box, { key: 'Tab' });
    expect(tab).toBe(false);
    expect(box.value).toBe('Thanks for your message');
    expect(box.selectionStart).toBe(23);
    expect(box).toHaveFocus();
    expect(suggestionOf(root)).toBeNull();
    expect(box).not.toHaveAttribute('aria-describedby');
    expect(screen.getByRole('status')).toHaveTextContent('Suggestion accepted.');
    expect(onAccept).toHaveBeenCalledExactlyOnceWith(
      { inserted: ' for your message', text: 'Thanks for your message', cursor: 23 },
      'all',
    );
    // With nothing to accept, Tab is left alone and moves focus as usual.
    expect(fireEvent.keyDown(box, { key: 'Tab' })).toBe(true);
  });

  it('works with a controlled field: the accepted text arrives through onChange', async () => {
    const changes: string[] = [];
    function Controlled() {
      const [value, setValue] = useState('');
      return (
        <AiGhostText data-testid="root" complete={async () => ' world'}>
          <input
            aria-label="Title"
            value={value}
            onChange={(event) => {
              changes.push(event.target.value);
              setValue(event.target.value);
            }}
          />
        </AiGhostText>
      );
    }
    render(<Controlled />);
    const box = screen.getByRole('textbox', { name: 'Title' }) as HTMLInputElement;
    box.focus();
    type(box, 'Hello');
    await settle();
    expect(suggestionOf(screen.getByTestId('root'))).toBe(' world');
    fireEvent.keyDown(box, { key: 'Tab' });
    expect(box.value).toBe('Hello world');
    expect(changes).toEqual(['Hello', 'Hello world']);
  });

  it('drops a suggestion when a controlled value is replaced from outside', async () => {
    function Resettable() {
      const [value, setValue] = useState('');
      return (
        <>
          <AiGhostText data-testid="root" complete={async () => ' world'}>
            <textarea
              aria-label="Reply"
              value={value}
              onChange={(event) => setValue(event.target.value)}
            />
          </AiGhostText>
          <button type="button" onClick={() => setValue('')}>
            Reset
          </button>
        </>
      );
    }
    render(<Resettable />);
    const box = screen.getByRole('textbox') as HTMLTextAreaElement;
    box.focus();
    type(box, 'Hello');
    await settle();
    expect(suggestionOf(screen.getByTestId('root'))).toBe(' world');
    fireEvent.click(screen.getByRole('button', { name: 'Reset' }));
    expect(suggestionOf(screen.getByTestId('root'))).toBeNull();
  });

  it('accepts with the forward arrow at the end, word by word with a modifier', async () => {
    const onAccept = vi.fn();
    const { root, box } = setup({ onAccept });
    type(box, 'Thanks');
    await settle();
    // The other arrow is not for the suggestion.
    expect(fireEvent.keyDown(box, { key: 'ArrowLeft' })).toBe(true);

    expect(fireEvent.keyDown(box, { key: 'ArrowRight', ctrlKey: true })).toBe(false);
    expect(box.value).toBe('Thanks for');
    expect(suggestionOf(root)).toBe(' your message');
    expect(onAccept).toHaveBeenLastCalledWith(
      { inserted: ' for', text: 'Thanks for', cursor: 10 },
      'word',
    );
    fireEvent.keyDown(box, { key: 'ArrowRight', altKey: true });
    expect(box.value).toBe('Thanks for your');
    // Taking a word is not announced again as a new suggestion.
    expect(screen.getByRole('status')).toHaveTextContent('Suggestion: for your message.');

    fireEvent.keyDown(box, { key: 'ArrowRight' });
    expect(box.value).toBe('Thanks for your message');
    expect(suggestionOf(root)).toBeNull();
  });

  it('uses the left arrow as the forward key in a right-to-left field', async () => {
    const { root, box } = setup(
      { complete: async () => ' عليكم' },
      <textarea aria-label="Reply" dir="rtl" />,
    );
    type(box, 'السلام');
    await settle();
    expect(suggestionOf(root)).toBe(' عليكم');
    expect(fireEvent.keyDown(box, { key: 'ArrowRight' })).toBe(true);
    expect(box.value).toBe('السلام');
    expect(fireEvent.keyDown(box, { key: 'ArrowLeft' })).toBe(false);
    expect(box.value).toBe('السلام عليكم');
  });

  it('dismisses with Escape without letting the key reach a dialog, and stays dismissed', async () => {
    const onDismiss = vi.fn();
    const outer = vi.fn();
    const complete = vi.fn<AiCompletionFunction>(async () => ' for your message');
    render(
      // eslint-disable-next-line jsx-a11y/no-static-element-interactions
      <div onKeyDown={outer}>
        <AiGhostText data-testid="root" complete={complete} onDismiss={onDismiss}>
          <textarea aria-label="Reply" />
        </AiGhostText>
      </div>,
    );
    const root = screen.getByTestId('root');
    const box = screen.getByRole('textbox') as HTMLTextAreaElement;
    box.focus();
    type(box, 'Thanks');
    await settle();
    expect(fireEvent.keyDown(box, { key: 'Escape' })).toBe(false);
    expect(outer).not.toHaveBeenCalled();
    expect(suggestionOf(root)).toBeNull();
    expect(box.value).toBe('Thanks');
    expect(onDismiss).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('status')).toHaveTextContent('Suggestion dismissed.');
    // The same text is not asked about again; with nothing showing Escape passes through.
    fireEvent.keyUp(box, { key: 'Escape' });
    await settle();
    expect(complete).toHaveBeenCalledTimes(1);
    fireEvent.keyDown(box, { key: 'Escape' });
    expect(outer).toHaveBeenCalledTimes(1);
  });

  it('never suggests while an input method is composing', async () => {
    const { root, box, complete } = setup();
    type(box, 'Thanks');
    await settle();
    expect(suggestionOf(root)).toBe(' for your message');

    fireEvent.compositionStart(box);
    expect(suggestionOf(root)).toBeNull();
    type(box, 'Thanksに');
    type(box, 'Thanksにほ');
    await settle();
    expect(complete).toHaveBeenCalledTimes(1);
    // Keys belong to the input method while it composes.
    expect(fireEvent.keyDown(box, { key: 'Tab', isComposing: true })).toBe(true);

    fireEvent.compositionEnd(box);
    await settle();
    expect(complete).toHaveBeenCalledTimes(2);
    expect(complete).toHaveBeenLastCalledWith('Thanksにほ', 8, expect.any(AbortSignal));
  });

  it('suggests only at the end of the text and drops the suggestion when the caret leaves', async () => {
    const { root, box, complete } = setup();
    fireEvent.input(box, { target: { value: 'Thanks a lot' } });
    box.setSelectionRange(6, 6);
    fireEvent.input(box);
    await settle();
    expect(complete).not.toHaveBeenCalled();

    type(box, 'Thanks a lot.');
    await settle();
    expect(suggestionOf(root)).toBe(' for your message');
    box.setSelectionRange(2, 2);
    fireEvent.keyUp(box, { key: 'ArrowLeft' });
    expect(suggestionOf(root)).toBeNull();

    type(box, 'Thanks a lot!!');
    await settle();
    expect(suggestionOf(root)).toBe(' for your message');
    fireEvent.blur(box);
    expect(suggestionOf(root)).toBeNull();
  });

  it('aborts the request in flight when the person keeps typing', async () => {
    const signals: AbortSignal[] = [];
    const { box } = setup({
      complete: (_t, _c, signal) => {
        signals.push(signal);
        return new Promise<string>(() => undefined);
      },
    });
    type(box, 'Thanks');
    await settle();
    type(box, 'Thanks ');
    expect(signals[0]?.aborted).toBe(true);
  });

  it('does nothing when disabled, read-only, or without a complete function', async () => {
    const complete = vi.fn<AiCompletionFunction>(async () => 'x');
    const { box, rerender } = setup({ complete, enabled: false });
    type(box, 'Thanks');
    await settle();
    expect(complete).not.toHaveBeenCalled();

    rerender(
      <AiGhostText data-testid="root" complete={complete}>
        <textarea aria-label="Reply" readOnly />
      </AiGhostText>,
    );
    type(screen.getByRole('textbox') as HTMLTextAreaElement, 'Thanks again');
    await settle();
    expect(complete).not.toHaveBeenCalled();
  });

  it('accepts by touch through the hint button, which is not a tab stop', async () => {
    const { root, box, rerender, complete } = setup();
    type(box, 'Thanks');
    await settle();
    // The name starts with the visible key (WCAG 2.5.3 Label in Name).
    const hint = screen.getByRole('button', { name: 'Tab to accept the suggestion' });
    expect(hint).toHaveAttribute('tabindex', '-1');
    // Pressing it does not take focus from the field.
    expect(fireEvent.mouseDown(hint)).toBe(false);
    fireEvent.click(hint);
    expect(box.value).toBe('Thanks for your message');
    expect(suggestionOf(root)).toBeNull();

    rerender(
      <AiGhostText data-testid="root" complete={complete} showAcceptHint={false}>
        <textarea aria-label="Reply" />
      </AiGhostText>,
    );
    type(box, 'Thanks for your message.');
    await settle();
    expect(suggestionOf(root)).toBe(' for your message');
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('keeps the field’s own description and removes only its own on unmount of the suggestion', async () => {
    const { box } = setup(
      {},
      <>
        <textarea aria-label="Reply" aria-describedby="help" />
        <p id="help">Keep it short.</p>
      </>,
    );
    type(box, 'Thanks');
    await settle();
    expect(box.getAttribute('aria-describedby')).toMatch(/^help \S+$/);
    expect(box).toHaveAccessibleDescription(/^Keep it short\. A suggestion is showing/);
    fireEvent.keyDown(box, { key: 'Escape' });
    expect(box).toHaveAttribute('aria-describedby', 'help');
  });

  it('uses translated strings', async () => {
    const { box } = setup({
      messages: {
        announce: (s) => `Vorschlag: ${s}`,
        instruction: 'Tab übernimmt den Vorschlag.',
        acceptKey: 'Tab ⇥',
        accept: 'übernimmt den Vorschlag',
      },
    });
    type(box, 'Danke');
    await settle();
    expect(screen.getByRole('status')).toHaveTextContent('Vorschlag: for your message');
    expect(box).toHaveAccessibleDescription('Tab übernimmt den Vorschlag.');
    expect(screen.getByRole('button', { name: 'Tab ⇥ übernimmt den Vorschlag' })).toBeVisible();
  });

  it('can be driven by a completion from useAiCompletion', async () => {
    const complete = vi.fn<AiCompletionFunction>(async () => ' there');
    function Driven() {
      const completion = useAiCompletion({ complete, debounceMs: 50 });
      return (
        <>
          <AiGhostText data-testid="root" completion={completion}>
            <textarea aria-label="Reply" />
          </AiGhostText>
          <output>{completion.status}</output>
        </>
      );
    }
    render(<Driven />);
    const box = screen.getByRole('textbox') as HTMLTextAreaElement;
    box.focus();
    type(box, 'Hello');
    await settle(50);
    expect(suggestionOf(screen.getByTestId('root'))).toBe(' there');
    expect(document.querySelector('output')).toHaveTextContent('ready');
  });

  it('forwards the ref and native props, and renders on the server', () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiGhostText ref={ref} complete={async () => ''} className="mine" id="ghost">
        <textarea aria-label="Reply" defaultValue="Hello" />
      </AiGhostText>,
    );
    expect(ref.current).toHaveClass('mn-ai-ghost-text', 'mine');
    expect(ref.current).toHaveAttribute('id', 'ghost');

    const html = renderToString(
      <AiGhostText complete={async () => ''}>
        <textarea aria-label="Reply" defaultValue="Hello" />
      </AiGhostText>,
    );
    expect(html).toContain('mn-ai-ghost-text');
    expect(html).toContain('Hello');
    expect(html).not.toContain('mn-ai-ghost-text__suggestion');
  });
});

describe('useAiRewrite', () => {
  it('sends the selection with the instruction and returns a proposal for the diff components', async () => {
    const rewrite = vi.fn(async () => 'quick');
    const { result } = renderHook(() => useAiRewrite({ rewrite, target: 'doc-1' }));
    const field = document.createElement('textarea');
    field.value = 'The very fast fox';
    field.setSelectionRange(4, 13);

    let proposal = null as Awaited<ReturnType<typeof result.current.rewriteSelection>>;
    await act(async () => {
      proposal = await result.current.rewriteSelection(field, 'Shorter');
    });
    expect(rewrite).toHaveBeenCalledWith(
      {
        text: 'The very fast fox',
        selection: 'very fast',
        start: 4,
        end: 13,
        instruction: 'Shorter',
      },
      expect.any(AbortSignal),
    );
    expect(result.current.status).toBe('ready');
    expect(proposal).toBe(result.current.proposal);
    expect(proposal).toMatchObject({
      instruction: 'Shorter',
      before: 'very fast',
      after: 'quick',
      result: 'The quick fox',
      hunk: { before: 'very fast', after: 'quick', label: 'Shorter' },
      part: { type: 'edit-proposal', target: 'doc-1', state: 'proposed' },
    });
    expect(proposal?.hunk.id).toBe(proposal?.part.id);
    const kinds = proposal?.segments.map((segment) => segment.type) ?? [];
    expect(kinds).toContain('delete');
    expect(kinds).toContain('insert');

    let text: string | null = null;
    act(() => {
      text = result.current.accept();
    });
    expect(text).toBe('The quick fox');
    expect(result.current).toMatchObject({ status: 'idle', proposal: null });
  });

  it('rewrites the whole text when nothing is selected, and asks nothing for empty input', async () => {
    const rewrite = vi.fn(async () => 'Hi.');
    const { result } = renderHook(() => useAiRewrite({ rewrite }));
    await act(async () => {
      await result.current.rewriteSelection({ text: 'Hello there', start: 3, end: 3 }, 'Shorter');
    });
    expect(rewrite).toHaveBeenCalledWith(
      expect.objectContaining({ selection: 'Hello there', start: 0, end: 11 }),
      expect.any(AbortSignal),
    );
    expect(result.current.proposal?.part.target).toBe('selection');

    rewrite.mockClear();
    await act(async () => {
      expect(
        await result.current.rewriteSelection({ text: '  ', start: 0, end: 2 }, 'x'),
      ).toBeNull();
      expect(
        await result.current.rewriteSelection({ text: 'Hello', start: 0, end: 5 }, ' '),
      ).toBeNull();
    });
    expect(rewrite).not.toHaveBeenCalled();
    expect(result.current.status).toBe('idle');
  });

  it('aborts the request in flight for a newer one, on reject and on unmount', async () => {
    const signals: AbortSignal[] = [];
    const answers: ((value: string) => void)[] = [];
    const rewrite = (_request: unknown, signal: AbortSignal) => {
      signals.push(signal);
      return new Promise<string>((resolve) => answers.push(resolve));
    };
    const { result, unmount } = renderHook(() => useAiRewrite({ rewrite }));
    const target = { text: 'Hello there', start: 0, end: 5 };
    let first: Promise<unknown> = Promise.resolve();
    act(() => {
      first = result.current.rewriteSelection(target, 'One');
    });
    expect(result.current.status).toBe('pending');
    act(() => {
      void result.current.rewriteSelection(target, 'Two');
    });
    expect(signals[0]?.aborted).toBe(true);
    await act(async () => {
      answers[0]?.('stale');
      answers[1]?.('Howdy');
      await first;
    });
    expect(await first).toBeNull();
    expect(result.current.proposal?.after).toBe('Howdy');

    act(() => {
      void result.current.rewriteSelection(target, 'Three');
    });
    act(() => result.current.reject());
    expect(signals[2]?.aborted).toBe(true);
    expect(result.current.status).toBe('idle');

    act(() => {
      void result.current.rewriteSelection(target, 'Four');
    });
    unmount();
    expect(signals[3]?.aborted).toBe(true);
  });

  it('reports a failure and applies a proposal only to the text it was made for', async () => {
    const onError = vi.fn();
    const failure = new Error('no');
    const { result } = renderHook(() =>
      useAiRewrite({ rewrite: () => Promise.reject(failure), onError }),
    );
    await act(async () => {
      await result.current.rewriteSelection({ text: 'Hello', start: 0, end: 5 }, 'Shorter');
    });
    expect(result.current).toMatchObject({ status: 'error', error: failure, proposal: null });
    expect(onError).toHaveBeenCalledExactlyOnceWith(failure);
    expect(result.current.accept()).toBeNull();

    const proposal = { start: 4, end: 13, before: 'very fast', after: 'quick' };
    expect(applyAiRewrite('The very fast fox', proposal)).toBe('The quick fox');
    expect(applyAiRewrite('A very slow fox!!', proposal)).toBe('A very slow fox!!');
  });
});
