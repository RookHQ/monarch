import { createRef, useRef, useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { InlineErrorRetry } from './index.js';

function deferred() {
  let resolve!: () => void;
  let reject!: (e: unknown) => void;
  const promise = new Promise<void>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

describe('InlineErrorRetry', () => {
  it('announces the failure as an alert and retries from the keyboard', async () => {
    const onRetry = vi.fn();
    render(
      <main>
        <InlineErrorRetry onRetry={onRetry}>Your earnings didn&apos;t load.</InlineErrorRetry>
      </main>,
    );
    expect(screen.getByRole('alert')).toHaveTextContent("Your earnings didn't load.");
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Try again' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onRetry).toHaveBeenCalledOnce();
    await expectNoA11yViolations();
  });

  it('uncontrolled: stays busy until the returned promise settles, keeping focus', async () => {
    const d = deferred();
    const onRetry = vi.fn(() => d.promise);
    render(<InlineErrorRetry onRetry={onRetry}>Could not load.</InlineErrorRetry>);
    const user = userEvent.setup();
    const button = screen.getByRole('button', { name: 'Try again' });
    await user.click(button);
    expect(button).toHaveAttribute('aria-busy', 'true');
    expect(button).toHaveFocus();
    expect(screen.getByRole('status')).toHaveTextContent('Retrying');
    await user.click(button);
    expect(onRetry).toHaveBeenCalledOnce();
    await act(async () => d.resolve());
    expect(button).not.toHaveAttribute('aria-busy');
    expect(button).toHaveFocus();
    expect(screen.getByRole('status')).toHaveTextContent('');
  });

  it('re-inserts the message after a failed retry so it is announced again', async () => {
    const d = deferred();
    render(<InlineErrorRetry onRetry={() => d.promise}>Could not load.</InlineErrorRetry>);
    const before = screen.getByText('Could not load.');
    await userEvent.setup().click(screen.getByRole('button', { name: 'Try again' }));
    await act(async () => d.reject(new Error('still down')));
    const after = screen.getByText('Could not load.');
    expect(after).not.toBe(before);
    expect(screen.getByRole('alert')).toContainElement(after);
  });

  it('controlled: follows `retrying` and re-announces on busy → idle', () => {
    const { rerender } = render(
      <InlineErrorRetry onRetry={() => Promise.resolve()} retrying={false}>
        Failed.
      </InlineErrorRetry>,
    );
    const first = screen.getByText('Failed.');
    rerender(
      <InlineErrorRetry onRetry={() => {}} retrying>
        Failed.
      </InlineErrorRetry>,
    );
    expect(screen.getByRole('button', { name: 'Try again' })).toHaveAttribute('aria-busy', 'true');
    rerender(
      <InlineErrorRetry onRetry={() => {}} retrying={false}>
        Failed.
      </InlineErrorRetry>,
    );
    expect(screen.getByText('Failed.')).not.toBe(first);
  });

  it('moves focus to returnFocusTo when a successful retry removes it', async () => {
    function Panel() {
      const [ok, setOk] = useState(false);
      const heading = useRef<HTMLHeadingElement>(null);
      return (
        <section aria-label="Earnings">
          {ok ? (
            <h2 ref={heading} tabIndex={-1}>
              Earnings
            </h2>
          ) : (
            <InlineErrorRetry onRetry={() => setOk(true)} returnFocusTo={heading}>
              Your earnings didn&apos;t load.
            </InlineErrorRetry>
          )}
        </section>
      );
    }
    render(<Panel />);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Try again' }));
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Earnings' })).toHaveFocus());
  });

  it('does not steal focus when it unmounts without holding focus', async () => {
    const target = vi.fn(() => null);
    const { rerender } = render(
      <>
        <button type="button">Elsewhere</button>
        <InlineErrorRetry onRetry={() => {}} returnFocusTo={target}>
          Failed.
        </InlineErrorRetry>
      </>,
    );
    screen.getByRole('button', { name: 'Elsewhere' }).focus();
    rerender(<button type="button">Elsewhere</button>);
    await new Promise((r) => setTimeout(r, 5));
    expect(target).not.toHaveBeenCalled();
  });

  it('banner variant, tone, extra actions, custom labels and ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <main>
        <InlineErrorRetry
          ref={ref}
          variant="banner"
          tone="warning"
          retryLabel="Retry"
          onRetry={() => {}}
          actions={<a href="#status">View status</a>}
          icon={null}
        >
          Showing data from 10 minutes ago.
        </InlineErrorRetry>
      </main>,
    );
    expect(ref.current).toHaveAttribute('data-variant', 'banner');
    expect(ref.current).toHaveAttribute('data-tone', 'warning');
    expect(ref.current!.querySelector('.mn-inline-error-retry__icon')).toBeNull();
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'View status' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<InlineErrorRetry onRetry={() => {}}>Failed.</InlineErrorRetry>);
    expect(html).toContain('mn-inline-error-retry');
    expect(html).toContain('role="alert"');
  });
});
