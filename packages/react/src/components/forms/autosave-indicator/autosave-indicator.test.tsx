import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { act, render, renderHook, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AutosaveIndicator, useAutosave, type UseAutosaveOptions } from './index.js';

function deferred() {
  let resolve!: () => void;
  let reject!: (error: unknown) => void;
  const promise = new Promise<void>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

/** Advances fake timers and lets settled promises run. */
async function tick(ms: number) {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(ms);
  });
}

function setup<T>(initial: UseAutosaveOptions<T>) {
  return renderHook((props: UseAutosaveOptions<T>) => useAutosave(props), {
    initialProps: initial,
  });
}

describe('useAutosave', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('does nothing for the initial value and saves once after the delay', async () => {
    const onSave = vi.fn(() => Promise.resolve());
    const { result, rerender } = setup({ value: 'a', onSave, delay: 500 });
    expect(result.current.status).toBe('idle');
    await tick(1000);
    expect(onSave).not.toHaveBeenCalled();

    rerender({ value: 'ab', onSave, delay: 500 });
    expect(result.current.status).toBe('unsaved');
    expect(result.current.isDirty).toBe(true);
    await tick(300);
    rerender({ value: 'abc', onSave, delay: 500 });
    await tick(300);
    expect(onSave).not.toHaveBeenCalled();
    await tick(300);
    expect(onSave).toHaveBeenCalledOnce();
    expect(onSave).toHaveBeenCalledWith('abc');
    expect(result.current.status).toBe('saved');
    expect(result.current.savedAt).toBeInstanceOf(Date);
    expect(result.current.isDirty).toBe(false);
  });

  it('does not save re-created but identical objects, or re-renders with the same content', async () => {
    const onSave = vi.fn(() => Promise.resolve());
    const { result, rerender } = setup({ value: { title: 'A' }, onSave, delay: 100 });
    rerender({ value: { title: 'A' }, onSave, delay: 100 });
    await tick(200);
    expect(onSave).not.toHaveBeenCalled();
    rerender({ value: { title: 'B' }, onSave, delay: 100 });
    await tick(60);
    rerender({ value: { title: 'B' }, onSave, delay: 100 });
    await tick(60);
    expect(onSave).toHaveBeenCalledOnce();
    expect(result.current.status).toBe('saved');
  });

  it('runs one save at a time and saves edits made during a save afterwards', async () => {
    const first = deferred();
    const onSave = vi
      .fn<(v: string) => Promise<void>>()
      .mockReturnValueOnce(first.promise)
      .mockResolvedValue(undefined);
    const { result, rerender } = setup({ value: 'a', onSave, delay: 100 });
    rerender({ value: 'b', onSave, delay: 100 });
    await tick(100);
    expect(result.current.status).toBe('saving');
    rerender({ value: 'c', onSave, delay: 100 });
    await tick(500);
    expect(onSave).toHaveBeenCalledTimes(1);
    await act(async () => first.resolve());
    await tick(100);
    expect(onSave).toHaveBeenCalledTimes(2);
    expect(onSave).toHaveBeenLastCalledWith('c');
    expect(result.current.status).toBe('saved');
  });

  it('retries with back-off, then reports the error; retry() saves again', async () => {
    const onSave = vi.fn<(v: number) => Promise<void>>(() => Promise.reject(new Error('offline')));
    const { result, rerender } = setup({
      value: 1,
      onSave,
      delay: 10,
      retries: 2,
      retryDelay: 100,
    });
    rerender({ value: 2, onSave, delay: 10, retries: 2, retryDelay: 100 });
    await tick(10);
    expect(onSave).toHaveBeenCalledTimes(1);
    expect(result.current.status).toBe('saving');
    await tick(100);
    expect(onSave).toHaveBeenCalledTimes(2);
    await tick(199);
    expect(onSave).toHaveBeenCalledTimes(2);
    await tick(1);
    expect(onSave).toHaveBeenCalledTimes(3);
    expect(result.current.status).toBe('error');
    expect(result.current.error).toEqual(new Error('offline'));
    expect(result.current.isDirty).toBe(true);

    onSave.mockImplementation(() => Promise.resolve());
    await act(async () => result.current.retry());
    await tick(0);
    expect(result.current.status).toBe('saved');
    expect(result.current.error).toBeNull();
  });

  it('flush() saves at once and resolves true; disabled waits but stays unsaved', async () => {
    const onSave = vi.fn(() => Promise.resolve());
    const { result, rerender } = setup({ value: 'a', onSave, delay: 5000, enabled: false });
    rerender({ value: 'b', onSave, delay: 5000, enabled: false });
    await tick(10_000);
    expect(onSave).not.toHaveBeenCalled();
    expect(result.current.status).toBe('unsaved');
    let ok: boolean | undefined;
    await act(async () => {
      ok = await result.current.flush();
    });
    expect(ok).toBe(true);
    expect(onSave).toHaveBeenCalledWith('b');
    expect(result.current.status).toBe('saved');
  });

  it('returns to idle when the value changes back before saving', async () => {
    const onSave = vi.fn(() => Promise.resolve());
    const { result, rerender } = setup({ value: 'a', onSave, delay: 100 });
    rerender({ value: 'b', onSave, delay: 100 });
    expect(result.current.status).toBe('unsaved');
    rerender({ value: 'a', onSave, delay: 100 });
    expect(result.current.status).toBe('idle');
    await tick(200);
    expect(onSave).not.toHaveBeenCalled();
  });
});

function Editor({ onSave }: { onSave: (v: string) => Promise<void> }) {
  const [text, setText] = useState('');
  const autosave = useAutosave({ value: text, onSave, delay: 300, retries: 0 });
  return (
    <>
      <label htmlFor="notes">Notes</label>
      <input id="notes" value={text} onChange={(e) => setText(e.target.value)} />
      <AutosaveIndicator state={autosave} errorMessage={() => 'Offline: not saved'} />
    </>
  );
}

describe('AutosaveIndicator', () => {
  it('shows unsaved, saving and saved while the person types, and announces Saved', async () => {
    const save = deferred();
    const onSave = vi.fn(() => save.promise);
    const user = userEvent.setup();
    render(<Editor onSave={onSave} />);
    await user.type(screen.getByRole('textbox', { name: 'Notes' }), 'Hi');
    expect(screen.getByText('Unsaved changes')).toBeInTheDocument();
    expect(await screen.findByText('Saving…')).toBeInTheDocument();
    expect(onSave).toHaveBeenCalledWith('Hi');
    await act(async () => save.resolve());
    expect(await screen.findByText('Saved', { selector: '[role="status"]' })).toBeInTheDocument();
    expect(document.querySelector('.mn-autosave-indicator')).toHaveAttribute(
      'data-status',
      'saved',
    );
    await expectNoA11yViolations();
  });

  it('shows the mapped error and retries from the Retry button', async () => {
    const onSave = vi
      .fn<(v: string) => Promise<void>>()
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValue(undefined);
    const user = userEvent.setup();
    render(<Editor onSave={onSave} />);
    await user.type(screen.getByRole('textbox', { name: 'Notes' }), 'x');
    expect(
      await screen.findByText('Offline: not saved', { selector: 'span:not([role])' }),
    ).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByText('Saved', { selector: 'span:not([role])' })).toBeInTheDocument();
    expect(onSave).toHaveBeenCalledTimes(2);
  });

  it('renders on the server', () => {
    function Static() {
      const autosave = useAutosave({ value: 'a', onSave: () => {} });
      return <AutosaveIndicator state={autosave} />;
    }
    const html = renderToString(<Static />);
    expect(html).toContain('mn-autosave-indicator');
    expect(html).toContain('data-status="idle"');
  });
});
