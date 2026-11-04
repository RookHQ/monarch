import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, render, renderHook, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { CopyButton, useCopyToClipboard, writeToClipboard } from './index.js';

/** user-event installs its own clipboard stub; replace it per test so we control the result. */
function mockClipboard(writeText: (text: string) => Promise<void>) {
  const spy = vi.fn(writeText);
  Object.defineProperty(navigator, 'clipboard', {
    configurable: true,
    value: { writeText: spy },
  });
  return spy;
}

describe('CopyButton', () => {
  let execCommand: ReturnType<typeof vi.fn>;
  beforeEach(() => {
    execCommand = vi.fn(() => false);
    Object.defineProperty(document, 'execCommand', { configurable: true, value: execCommand });
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('copies the value, shows the copied state and announces it', async () => {
    const user = userEvent.setup();
    const writeText = mockClipboard(() => Promise.resolve());
    const onCopy = vi.fn();
    render(<CopyButton value="https://example.com/hook" onCopy={onCopy} />);
    const button = screen.getByRole('button', { name: 'Copy' });
    await user.click(button);
    expect(writeText).toHaveBeenCalledWith('https://example.com/hook');
    expect(onCopy).toHaveBeenCalledWith('https://example.com/hook');
    expect(button).toHaveAttribute('data-state', 'copied');
    expect(screen.getByRole('button', { name: 'Copied' })).toBe(button);
    expect(screen.getByRole('status')).toHaveTextContent('Copied to clipboard');
  });

  it('activates from the keyboard with Enter and Space', async () => {
    const user = userEvent.setup();
    const writeText = mockClipboard(() => Promise.resolve());
    render(<CopyButton value="abc" />);
    await user.tab();
    expect(screen.getByRole('button')).toHaveFocus();
    await user.keyboard('{Enter}');
    await user.keyboard(' ');
    expect(writeText).toHaveBeenCalledTimes(2);
  });

  it('reports a refused write to the caller and announces the failure', async () => {
    const user = userEvent.setup();
    mockClipboard(() => Promise.reject(new DOMException('Denied', 'NotAllowedError')));
    const onCopyError = vi.fn();
    render(<CopyButton value="secret" onCopyError={onCopyError} />);
    await user.click(screen.getByRole('button'));
    expect(onCopyError).toHaveBeenCalledTimes(1);
    expect(onCopyError.mock.calls[0]?.[0]).toBeInstanceOf(Error);
    expect(screen.getByRole('button')).toHaveAttribute('data-state', 'error');
    expect(screen.getByRole('button', { name: "Couldn't copy" })).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent(/copy it manually/);
  });

  it('falls back to execCommand when the Clipboard API rejects', async () => {
    const user = userEvent.setup();
    mockClipboard(() => Promise.reject(new Error('insecure context')));
    execCommand.mockReturnValue(true);
    const onCopyError = vi.fn();
    render(<CopyButton value="fallback" onCopyError={onCopyError} />);
    await user.click(screen.getByRole('button'));
    expect(execCommand).toHaveBeenCalledWith('copy');
    expect(onCopyError).not.toHaveBeenCalled();
    expect(screen.getByRole('button')).toHaveAttribute('data-state', 'copied');
  });

  it('fails with an error when no clipboard is available at all', async () => {
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value: undefined });
    await expect(writeToClipboard('x')).rejects.toThrow(/not available/);
  });

  it('returns to idle after resetAfter', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    mockClipboard(() => Promise.resolve());
    render(<CopyButton value="abc" resetAfter={1000} />);
    await user.click(screen.getByRole('button'));
    expect(screen.getByRole('button')).toHaveAttribute('data-state', 'copied');
    act(() => {
      vi.advanceTimersByTime(1000);
    });
    expect(screen.getByRole('button')).toHaveAttribute('data-state', 'idle');
  });

  it('accepts a function value, including an async one', async () => {
    const user = userEvent.setup();
    const writeText = mockClipboard(() => Promise.resolve());
    render(<CopyButton value={() => Promise.resolve('computed')} />);
    await user.click(screen.getByRole('button'));
    await waitFor(() => expect(writeText).toHaveBeenCalledWith('computed'));
  });

  it('icon-only keeps the label as its stable accessible name', async () => {
    const user = userEvent.setup();
    mockClipboard(() => Promise.resolve());
    render(<CopyButton value="k" iconOnly label="Copy API key" />);
    const button = screen.getByRole('button', { name: 'Copy API key' });
    expect(button).toHaveAttribute('data-icon-only');
    await user.click(button);
    expect(screen.getByRole('button', { name: 'Copy API key' })).toBe(button);
    expect(screen.getByRole('status')).toHaveTextContent('Copied to clipboard');
    await expectNoA11yViolations();
  });

  it('does not copy when disabled or when onClick prevents default', async () => {
    const user = userEvent.setup();
    const writeText = mockClipboard(() => Promise.resolve());
    const { rerender } = render(<CopyButton value="a" disabled />);
    await user.click(screen.getByRole('button'));
    rerender(<CopyButton value="a" onClick={(e) => e.preventDefault()} />);
    await user.click(screen.getByRole('button'));
    expect(writeText).not.toHaveBeenCalled();
  });

  it('keeps the width stable: inactive labels are hidden from the accessible name', () => {
    render(<CopyButton value="a" label="Copy link" copiedLabel="Link copied" />);
    const button = screen.getByRole('button', { name: 'Copy link' });
    expect(button.querySelectorAll('.mn-copy-button__label > span')).toHaveLength(2);
  });

  it('has no axe violations', async () => {
    render(<CopyButton value="abc" label="Copy webhook URL" />);
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<CopyButton value="abc" />);
    expect(html).toContain('mn-copy-button');
    expect(html).toContain('role="status"');
  });
});

describe('useCopyToClipboard', () => {
  it('exposes status and error, and reset returns to idle', async () => {
    mockClipboard(() => Promise.reject(new Error('nope')));
    Object.defineProperty(document, 'execCommand', { configurable: true, value: () => false });
    const onError = vi.fn();
    const { result } = renderHook(() => useCopyToClipboard({ onError, resetAfter: 0 }));
    let ok = true;
    await act(async () => {
      ok = await result.current.copy('x');
    });
    expect(ok).toBe(false);
    expect(result.current.status).toBe('error');
    expect(result.current.error?.message).toBe('nope');
    expect(onError).toHaveBeenCalledTimes(1);
    act(() => result.current.reset());
    expect(result.current.status).toBe('idle');
    expect(result.current.error).toBeNull();
  });
});
