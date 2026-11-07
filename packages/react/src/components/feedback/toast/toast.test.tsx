import { describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import {
  ToastProvider,
  getErrorMessage,
  useToast,
  type ToastContextValue,
  type ToastOptions,
  type ToastProviderProps,
} from './index.js';

function Demo({ options }: { options: ToastOptions[] }) {
  const { toast } = useToast();
  return (
    <Button
      onClick={() => {
        for (const o of options) toast(o);
      }}
    >
      Notify
    </Button>
  );
}

const renderToast = (options: ToastOptions[], props: Partial<ToastProviderProps> = {}) =>
  render(
    <ToastProvider {...props}>
      <Demo options={options} />
    </ToastProvider>,
  );

/** Captures the context so a test can call toast() directly. */
function capture(props: Partial<ToastProviderProps> = {}) {
  const ref: { current: ToastContextValue | null } = { current: null };
  function Grab() {
    ref.current = useToast();
    return <button type="button">Outside</button>;
  }
  render(
    <ToastProvider {...props}>
      <Grab />
    </ToastProvider>,
  );
  return ref as { current: ToastContextValue };
}

/** Radix announces through a visually hidden live region outside the list. */
const announcer = () =>
  [...document.querySelectorAll('[aria-live]')].find(
    (n) => !n.closest('.mn-toast-viewport') && n.textContent,
  );

describe('Toast', () => {
  it('shows the toast, dismisses with the close button and runs the action', async () => {
    const onUndo = vi.fn();
    renderToast([
      {
        title: 'Ticket archived',
        description: 'You can undo this.',
        tone: 'success',
        action: { label: 'Undo', onClick: onUndo },
      },
    ]);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Notify' }));
    expect(await screen.findByText('Ticket archived')).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Notifications (F8)' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Dismiss' })).toBeInTheDocument();
    // Activating the action also dismisses the toast.
    await user.click(screen.getByRole('button', { name: 'Undo' }));
    expect(onUndo).toHaveBeenCalledOnce();
    await waitFor(() => expect(screen.queryByText('Ticket archived')).not.toBeInTheDocument());
  });

  // Regression: the toast <li> used to carry role="status"/"alert", which broke
  // the viewport list (axe: list, aria-allowed-role) and announced twice.
  it('has no axe violations while toasts of every tone are open', async () => {
    renderToast(
      (['neutral', 'info', 'success', 'warning', 'danger'] as const).map((tone): ToastOptions => ({
        title: `${tone} toast`,
        description: 'Details',
        tone,
        ...(tone === 'success' ? { action: { label: 'Undo', onClick: () => {} } } : {}),
      })),
      { limit: 5 },
    );
    await userEvent.setup().click(screen.getByRole('button', { name: 'Notify' }));
    await screen.findByText('danger toast');
    for (const li of document.querySelectorAll('.mn-toast')) {
      expect(li.tagName).toBe('LI');
      expect(li).not.toHaveAttribute('role');
    }
    expect(screen.getAllByRole('listitem')).toHaveLength(5);
    await expectNoA11yViolations();
  });

  it('has no axe violations with the provider mounted and nothing open', async () => {
    renderToast([]);
    await expectNoA11yViolations();
  });

  it('announces danger assertively and other tones politely', async () => {
    const t = capture();
    act(() => {
      t.current.toast({ title: 'Payment failed', tone: 'danger' });
    });
    await waitFor(() => expect(announcer()).toHaveTextContent('Payment failed'));
    expect(announcer()).toHaveAttribute('aria-live', 'assertive');
    expect(document.querySelector('.mn-toast')).toHaveAttribute('data-politeness', 'assertive');
    act(() => t.current.dismiss());
    await waitFor(() => expect(announcer()).toBeUndefined(), { timeout: 2000 });

    act(() => {
      t.current.toast({ title: 'Saved', tone: 'success' });
    });
    await waitFor(() => expect(announcer()).toHaveTextContent('Saved'));
    expect(announcer()).toHaveAttribute('aria-live', 'polite');
  });

  it('lets a toast override its politeness', async () => {
    const t = capture();
    act(() => {
      t.current.toast({
        title: 'Session ends in 1 minute',
        tone: 'warning',
        politeness: 'assertive',
      });
    });
    await waitFor(() => expect(announcer()).toHaveAttribute('aria-live', 'assertive'));
  });

  it('never moves focus when a toast appears', async () => {
    renderToast([{ title: 'Saved', action: { label: 'Undo', onClick: () => {} } }]);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Notify' });
    await user.click(trigger);
    await screen.findByText('Saved');
    expect(trigger).toHaveFocus();
  });

  it('F8 moves focus to the toast region, Tab reaches the action, Escape dismisses', async () => {
    renderToast([{ title: 'Ticket archived', action: { label: 'Undo', onClick: () => {} } }]);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Notify' }));
    await screen.findByText('Ticket archived');
    // user-event has no `code` for F8; Radix matches on event.code.
    fireEvent.keyDown(document, { key: 'F8', code: 'F8' });
    const region = screen.getByRole('region', { name: 'Notifications (F8)' });
    expect(region.contains(document.activeElement)).toBe(true);
    await user.tab();
    expect(document.activeElement?.closest('.mn-toast')).not.toBeNull();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByText('Ticket archived')).not.toBeInTheDocument());
  });

  it('accepts a custom hotkey and names the region with it', async () => {
    renderToast([{ title: 'Saved' }], { hotkey: ['altKey', 'KeyT'], label: 'Alerts' });
    await userEvent.setup().click(screen.getByRole('button', { name: 'Notify' }));
    await screen.findByText('Saved');
    expect(screen.getByRole('region', { name: 'Alerts (alt+T)' })).toBeInTheDocument();
  });

  it('labels each Dismiss button and ties it to its toast title', async () => {
    renderToast([{ title: 'First' }, { title: 'Second' }], { dismissLabel: 'Close notification' });
    await userEvent.setup().click(screen.getByRole('button', { name: 'Notify' }));
    await screen.findByText('Second');
    const buttons = screen.getAllByRole('button', { name: 'Close notification' });
    expect(buttons).toHaveLength(2);
    expect(buttons[0]).toHaveAccessibleDescription('First');
    expect(buttons[1]).toHaveAccessibleDescription('Second');
    // Each list item is named by its title.
    expect(screen.getByRole('listitem', { name: 'First' })).toBeInTheDocument();
  });

  it('keeps at most `limit` open (default three), closing the oldest first', async () => {
    renderToast([1, 2, 3, 4].map((n) => ({ title: `Toast ${n}` })));
    await userEvent.setup().click(screen.getByRole('button', { name: 'Notify' }));
    await screen.findByText('Toast 4');
    await waitFor(() => {
      expect(document.querySelectorAll('.mn-toast[data-state="open"]')).toHaveLength(3);
    });
    expect(document.querySelector('.mn-toast[data-state="open"]')).not.toHaveTextContent('Toast 1');
  });

  it('dismiss() without an id closes every toast; onDismiss fires once each', async () => {
    const onDismiss = vi.fn();
    const t = capture();
    act(() => {
      t.current.toast({ title: 'A', onDismiss });
      t.current.toast({ title: 'B', onDismiss });
    });
    await screen.findByText('B');
    act(() => t.current.dismiss());
    await waitFor(() => expect(screen.queryByText('A')).not.toBeInTheDocument());
    expect(screen.queryByText('B')).not.toBeInTheDocument();
    expect(onDismiss).toHaveBeenCalledTimes(2);
  });

  it('shorthands set the tone', async () => {
    const t = capture({ limit: 4 });
    act(() => {
      t.current.toast.success('Saved');
      t.current.toast.error('Could not save');
      t.current.toast.warning('Partly saved');
      t.current.toast.info('Syncing');
    });
    await screen.findByText('Syncing');
    const tones = [...document.querySelectorAll('.mn-toast')].map((n) =>
      n.getAttribute('data-tone'),
    );
    expect(tones).toEqual(['success', 'danger', 'warning', 'info']);
  });

  it('onDismiss still fires when the cleanup timer runs before the close renders', async () => {
    vi.useFakeTimers();
    try {
      const ctx = capture();
      const onDismiss = vi.fn();
      let handle!: { dismiss: () => void };
      act(() => {
        handle = ctx.current.toast({ title: 'Tile deleted', duration: Infinity, onDismiss });
      });
      act(() => {
        handle.dismiss();
        vi.advanceTimersByTime(1500);
      });
      expect(onDismiss).toHaveBeenCalledTimes(1);
      act(() => {
        vi.advanceTimersByTime(1500);
      });
      expect(screen.queryByText('Tile deleted')).toBeNull();
      expect(onDismiss).toHaveBeenCalledTimes(1);
    } finally {
      vi.useRealTimers();
    }
  });

  it('returns a handle whose dismiss closes that toast only', async () => {
    const t = capture();
    let handle: ReturnType<ToastContextValue['toast']> | undefined;
    act(() => {
      handle = t.current.toast({ title: 'Close me' });
      t.current.toast({ title: 'Keep' });
    });
    await screen.findByText('Keep');
    expect(handle?.id).toMatch(/^mn-toast-/);
    act(() => handle?.dismiss());
    await waitFor(() => expect(screen.queryByText('Close me')).not.toBeInTheDocument());
    expect(screen.getByText('Keep')).toBeInTheDocument();
  });

  describe('timing (WCAG 2.2.1)', () => {
    const at = async (props: Partial<ToastProviderProps>, options: ToastOptions, ms: number) => {
      const t = capture(props);
      act(() => {
        t.current.toast(options);
      });
      expect(screen.getByText(String(options.title))).toBeInTheDocument();
      await act(async () => {
        vi.advanceTimersByTime(ms);
      });
      return screen.queryByText(String(options.title));
    };

    it('auto-dismisses after the duration', async () => {
      vi.useFakeTimers();
      try {
        expect(await at({}, { title: 'Saved', duration: 1000 }, 1700)).toBeNull();
      } finally {
        vi.useRealTimers();
      }
    });

    it('defaults to 5s, and 10s for danger or a toast with an action', async () => {
      vi.useFakeTimers();
      try {
        expect(await at({}, { title: 'Plain' }, 6000)).toBeNull();
        expect(
          await at({}, { title: 'Undoable', action: { label: 'Undo', onClick: () => {} } }, 6000),
        ).not.toBeNull();
        expect(await at({}, { title: 'Failed', tone: 'danger' }, 6000)).not.toBeNull();
      } finally {
        vi.useRealTimers();
      }
    });

    it('durationScale stretches every timer, and Infinity turns auto-dismiss off', async () => {
      vi.useFakeTimers();
      try {
        expect(
          await at({ durationScale: 3 }, { title: 'Slow', duration: 1000 }, 2000),
        ).not.toBeNull();
        expect(await at({ durationScale: Infinity }, { title: 'Sticky' }, 60_000)).not.toBeNull();
        expect(await at({}, { title: 'Pinned', duration: Infinity }, 60_000)).not.toBeNull();
      } finally {
        vi.useRealTimers();
      }
    });

    it('pauses while focus is inside the region and resumes when it leaves', async () => {
      vi.useFakeTimers();
      try {
        const t = capture();
        act(() => {
          t.current.toast({ title: 'Paused', duration: 1000 });
        });
        const close = screen.getByRole('button', { name: 'Dismiss' });
        act(() => close.focus());
        await act(async () => {
          vi.advanceTimersByTime(5000);
        });
        expect(screen.getByText('Paused')).toBeInTheDocument();
        act(() => screen.getByRole('button', { name: 'Outside' }).focus());
        await act(async () => {
          vi.advanceTimersByTime(2000);
        });
        expect(screen.queryByText('Paused')).toBeNull();
      } finally {
        vi.useRealTimers();
      }
    });

    it('pauses while the pointer is over the region', async () => {
      vi.useFakeTimers();
      try {
        const t = capture();
        act(() => {
          t.current.toast({ title: 'Hovered', duration: 1000 });
        });
        const region = screen.getByRole('region');
        fireEvent.pointerMove(region);
        await act(async () => {
          vi.advanceTimersByTime(5000);
        });
        expect(screen.getByText('Hovered')).toBeInTheDocument();
        fireEvent.pointerLeave(region);
        await act(async () => {
          vi.advanceTimersByTime(2000);
        });
        expect(screen.queryByText('Hovered')).toBeNull();
      } finally {
        vi.useRealTimers();
      }
    });
  });

  it('uses the action altText for screen readers', async () => {
    renderToast([
      {
        title: 'Archived',
        action: { label: 'Undo', altText: 'Undo from the Archive page', onClick: () => {} },
      },
    ]);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Notify' }));
    await screen.findByText('Archived');
    expect(screen.getByRole('button', { name: 'Undo' })).toHaveAttribute(
      'data-radix-toast-announce-alt',
      'Undo from the Archive page',
    );
  });

  it('throws outside a provider', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => render(<Demo options={[]} />)).toThrow(/ToastProvider/);
    spy.mockRestore();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <ToastProvider>
        <p>App</p>
      </ToastProvider>,
    );
    expect(html).toContain('mn-toast-viewport');
    expect(html).toContain('Notifications (F8)');
  });
});

describe('getErrorMessage', () => {
  it('passes readable strings and Error messages through', () => {
    expect(getErrorMessage('Name is taken')).toBe('Name is taken');
    expect(getErrorMessage(new Error('Workspace is archived'))).toBe('Workspace is archived');
  });

  it('reads the response body before the transport message', () => {
    const axiosLike = Object.assign(new Error('Request failed with status code 409'), {
      response: { status: 409, data: { message: 'That handle is already in use' } },
    });
    expect(getErrorMessage(axiosLike)).toBe('That handle is already in use');
  });

  it('flattens field errors', () => {
    const err = {
      response: {
        data: {
          message: 'Validation failed',
          details: { fields: { name: ['Name is required'], email: ['Email is invalid'] } },
        },
      },
    };
    expect(getErrorMessage(err)).toBe('Name is required Email is invalid');
  });

  it('joins message lists and errors[] entries', () => {
    expect(getErrorMessage({ data: { message: ['Too short', 'Needs a number'] } })).toBe(
      'Too short Needs a number',
    );
    expect(getErrorMessage({ body: { errors: [{ detail: 'Plan limit reached' }] } })).toBe(
      'Plan limit reached',
    );
  });

  it('never shows transport messages or junk; uses the fallback', () => {
    expect(getErrorMessage(new Error('Request failed with status code 500'))).toBe(
      'Something went wrong. Try again.',
    );
    expect(getErrorMessage(null, { fallback: 'Could not save' })).toBe('Could not save');
    expect(getErrorMessage(42)).toBe('Something went wrong. Try again.');
    expect(getErrorMessage({ response: { data: '<html>502 Bad Gateway</html>' } })).toBe(
      'Something went wrong. Try again.',
    );
    expect(getErrorMessage('   ')).toBe('Something went wrong. Try again.');
  });

  it('uses the network message for fetch failures when given', () => {
    expect(
      getErrorMessage(new TypeError('Failed to fetch'), { network: 'You appear to be offline.' }),
    ).toBe('You appear to be offline.');
    expect(getErrorMessage(new TypeError('Failed to fetch'))).toBe(
      'Something went wrong. Try again.',
    );
  });
});
