import { describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ToastProvider, type ToastProviderProps } from '../toast/index.js';
import {
  useUndoToast,
  type UndoOutcome,
  type UndoableOptions,
  type UseUndoToastOptions,
  type UseUndoToastResult,
} from './index.js';

/** Captures the hook so a test can call undoable() directly. */
function capture(props: Partial<ToastProviderProps> = {}, options: UseUndoToastOptions = {}) {
  const ref: { current: UseUndoToastResult | null } = { current: null };
  function Grab() {
    ref.current = useUndoToast(options);
    return <button type="button">Outside</button>;
  }
  render(
    <ToastProvider {...props}>
      <Grab />
    </ToastProvider>,
  );
  return ref as { current: UseUndoToastResult };
}

function start(
  t: { current: UseUndoToastResult },
  options: Partial<UndoableOptions> = {},
): {
  result: Promise<UndoOutcome>;
  onUndo: ReturnType<typeof vi.fn>;
  onCommit: ReturnType<typeof vi.fn>;
} {
  const onUndo = vi.fn();
  const onCommit = vi.fn();
  let result!: Promise<UndoOutcome>;
  act(() => {
    result = t.current.undoable({ title: 'Note deleted', onUndo, onCommit, ...options });
  });
  return { result, onUndo, onCommit };
}

describe('useUndoToast', () => {
  it('Undo calls onUndo once, resolves undone, never commits, and confirms politely', async () => {
    const t = capture();
    const { result, onUndo, onCommit } = start(t, { description: 'Weekly plan' });
    expect(screen.getByText('Note deleted')).toBeInTheDocument();
    expect(screen.getByText('Weekly plan')).toBeInTheDocument();
    await expectNoA11yViolations();
    await userEvent.setup().click(screen.getByRole('button', { name: 'Undo' }));
    await expect(result).resolves.toBe('undone');
    expect(onUndo).toHaveBeenCalledOnce();
    await waitFor(() => expect(screen.queryByText('Note deleted')).not.toBeInTheDocument());
    expect(onCommit).not.toHaveBeenCalled();
    const restored = await screen.findByText('Restored');
    const toast = restored.closest('.mn-toast');
    expect(toast).toHaveAttribute('data-politeness', 'polite');
    expect(toast).toHaveAttribute('data-tone', 'success');
  });

  it('Dismiss commits once and resolves committed', async () => {
    const t = capture();
    const { result, onUndo, onCommit } = start(t);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Dismiss' }));
    await expect(result).resolves.toBe('committed');
    expect(onCommit).toHaveBeenCalledOnce();
    expect(onUndo).not.toHaveBeenCalled();
    expect(screen.queryByText('Restored')).not.toBeInTheDocument();
  });

  it('Escape from the region (F8) commits', async () => {
    const t = capture();
    const { result, onCommit } = start(t);
    fireEvent.keyDown(document, { key: 'F8', code: 'F8' });
    await userEvent.setup().keyboard('{Escape}');
    await expect(result).resolves.toBe('committed');
    expect(onCommit).toHaveBeenCalledOnce();
  });

  it('the Undo button is reachable by keyboard: F8, Tab, Enter', async () => {
    const t = capture();
    const { result, onUndo } = start(t);
    const user = userEvent.setup();
    fireEvent.keyDown(document, { key: 'F8', code: 'F8' });
    await user.tab();
    const undo = screen.getByRole('button', { name: 'Undo' });
    while (document.activeElement !== undo && document.activeElement?.closest('.mn-toast'))
      await user.tab();
    expect(undo).toHaveFocus();
    await user.keyboard('{Enter}');
    await expect(result).resolves.toBe('undone');
    expect(onUndo).toHaveBeenCalledOnce();
  });

  it('a newer toast pushing it out commits it', async () => {
    const t = capture({ limit: 1 });
    const first = start(t, { title: 'First deleted' });
    const second = start(t, { title: 'Second deleted' });
    await expect(first.result).resolves.toBe('committed');
    expect(first.onCommit).toHaveBeenCalledOnce();
    expect(second.onCommit).not.toHaveBeenCalled();
  });

  it('commits when the page is hidden, then not again when the toast closes', async () => {
    const t = capture();
    const { result, onCommit } = start(t);
    act(() => {
      window.dispatchEvent(new Event('pagehide'));
    });
    await expect(result).resolves.toBe('committed');
    await waitFor(() => expect(screen.queryByText('Note deleted')).not.toBeInTheDocument());
    expect(onCommit).toHaveBeenCalledOnce();
  });

  it('accepts custom labels, a custom confirmation, or none', async () => {
    const t = capture({}, { messages: { undo: 'Revert', restored: 'Back again' } });
    const a = start(t, { title: 'Tile deleted' });
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Revert' }));
    await a.result;
    expect(await screen.findByText('Back again')).toBeInTheDocument();
    const b = start(t, { title: 'View deleted', undoLabel: 'Bring back', restoredTitle: false });
    await user.click(screen.getByRole('button', { name: 'Bring back' }));
    await expect(b.result).resolves.toBe('undone');
    expect(screen.getAllByText('Back again')).toHaveLength(1);
  });

  it('waits for an async onUndo before resolving', async () => {
    const t = capture();
    let finish!: () => void;
    const onUndo = vi.fn(() => new Promise<void>((resolve) => (finish = resolve)));
    const { result } = start(t, { onUndo });
    let outcome: UndoOutcome | undefined;
    void result.then((o) => (outcome = o));
    await userEvent.setup().click(screen.getByRole('button', { name: 'Undo' }));
    await Promise.resolve();
    expect(outcome).toBeUndefined();
    expect(screen.queryByText('Restored')).not.toBeInTheDocument();
    finish();
    await expect(result).resolves.toBe('undone');
    expect(await screen.findByText('Restored')).toBeInTheDocument();
  });

  it('a failed onUndo shows an error toast and rejects', async () => {
    const t = capture();
    const { result, onCommit } = start(t, {
      onUndo: () => Promise.reject(new Error('The note was already removed.')),
    });
    const caught = result.catch((e: Error) => e.message);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Undo' }));
    await expect(caught).resolves.toBe('The note was already removed.');
    const message = await screen.findByText('The note was already removed.');
    expect(message.closest('.mn-toast')).toHaveAttribute('data-tone', 'danger');
    expect(onCommit).not.toHaveBeenCalled();
  });

  it('uses undoAltText for screen readers', async () => {
    const t = capture();
    start(t, { undoAltText: 'Restore it from Trash' });
    expect(
      screen.getByRole('button', { name: 'Undo' }).closest('[data-radix-toast-announce-alt]'),
    ).toHaveAttribute('data-radix-toast-announce-alt', 'Restore it from Trash');
  });

  describe('timing', () => {
    const remaining = async (options: Partial<UndoableOptions>, ms: number) => {
      const t = capture();
      const run = start(t, options);
      await act(async () => {
        vi.advanceTimersByTime(ms);
      });
      return run;
    };

    it('defaults to 8 seconds, then commits', async () => {
      vi.useFakeTimers();
      try {
        const run = await remaining({}, 7500);
        expect(screen.getByText('Note deleted')).toBeInTheDocument();
        expect(run.onCommit).not.toHaveBeenCalled();
        await act(async () => {
          // Less than the toast's exit time, so the closed toast renders once.
          vi.advanceTimersByTime(550);
        });
        expect(run.onCommit).toHaveBeenCalledOnce();
        await expect(run.result).resolves.toBe('committed');
      } finally {
        vi.useRealTimers();
      }
    });

    it('clamps the duration to 5–10 seconds', async () => {
      vi.useFakeTimers();
      try {
        const short = await remaining({ duration: 1000 }, 4500);
        expect(short.onCommit).not.toHaveBeenCalled();
        await act(async () => {
          vi.advanceTimersByTime(1000);
        });
        expect(short.onCommit).toHaveBeenCalledOnce();
      } finally {
        vi.useRealTimers();
      }
    });

    it('clamps long durations to 10 seconds; Infinity keeps it open', async () => {
      vi.useFakeTimers();
      try {
        const long = await remaining({ duration: 60_000, title: 'Long' }, 9_900);
        expect(long.onCommit).not.toHaveBeenCalled();
        await act(async () => {
          vi.advanceTimersByTime(300);
        });
        expect(long.onCommit).toHaveBeenCalledOnce();
        const sticky = await remaining({ duration: Infinity, title: 'Sticky' }, 60_000);
        expect(sticky.onCommit).not.toHaveBeenCalled();
        expect(screen.getByText('Sticky')).toBeInTheDocument();
      } finally {
        vi.useRealTimers();
      }
    });
  });

  it('renders a hidden countdown bar timed to the duration when asked', () => {
    const t = capture();
    start(t, { countdown: true, duration: 6000 });
    const root = document.querySelector('.mn-undo-toast') as HTMLElement;
    expect(root).toHaveAttribute('data-countdown');
    expect(root.style.getPropertyValue('--_duration')).toBe('6000ms');
    expect(root.querySelector('.mn-undo-toast__countdown')).toHaveAttribute('aria-hidden', 'true');
    expect(screen.getByRole('listitem', { name: 'Note deleted' })).toBeInTheDocument();
  });

  it('throws a helpful error outside ToastProvider', () => {
    function Lonely() {
      useUndoToast();
      return null;
    }
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => render(<Lonely />)).toThrow(/ToastProvider/);
    spy.mockRestore();
  });

  it('renders on the server', () => {
    function Page() {
      const { undoable } = useUndoToast();
      return (
        <button type="button" onClick={() => void undoable({ title: 'x', onUndo: () => {} })}>
          Delete
        </button>
      );
    }
    expect(
      renderToString(
        <ToastProvider>
          <Page />
        </ToastProvider>,
      ),
    ).toContain('Delete');
  });
});
