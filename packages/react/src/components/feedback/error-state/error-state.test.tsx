import { createRef, useState, type ReactElement } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ErrorState, ErrorStateBoundary, errorKindFromStatus } from './index.js';

describe('ErrorState', () => {
  it('is an alert with retry', async () => {
    const onRetry = vi.fn();
    render(
      <ErrorState
        title="Could not load tickets"
        description="Check your connection."
        onRetry={onRetry}
      />,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('Could not load tickets');
    await userEvent.setup().click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalledOnce();
    await expectNoA11yViolations();
  });

  it('keeps the default markup: no data-kind, warning icon, retry first', () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <ErrorState ref={ref} title="Failed" onRetry={() => {}} actions={<button>Status</button>} />,
    );
    expect(ref.current).toHaveClass('mn-error-state', 'mn-empty-state');
    expect(ref.current).not.toHaveAttribute('data-kind');
    expect(screen.getAllByRole('button').map((b) => b.textContent)).toEqual([
      'Try again',
      'Status',
    ]);
  });

  it('forbidden and not-found hide retry, since retrying cannot help', () => {
    const { rerender } = render(
      <ErrorState kind="forbidden" title="You can't view this" onRetry={() => {}} />,
    );
    expect(screen.queryByRole('button', { name: 'Try again' })).toBeNull();
    expect(screen.getByRole('alert')).toHaveAttribute('data-kind', 'forbidden');
    rerender(<ErrorState kind="not-found" title="Not found" onRetry={() => {}} />);
    expect(screen.queryByRole('button', { name: 'Try again' })).toBeNull();
    rerender(<ErrorState kind="offline" title="You're offline" onRetry={() => {}} />);
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument();
  });

  it('retrying keeps the button focused and blocks repeat clicks', async () => {
    const onRetry = vi.fn();
    function Demo() {
      const [busy, setBusy] = useState(false);
      return (
        <ErrorState
          title="Failed"
          retrying={busy}
          onRetry={() => {
            onRetry();
            setBusy(true);
          }}
        />
      );
    }
    render(<Demo />);
    const user = userEvent.setup();
    await user.tab();
    const button = screen.getByRole('button', { name: 'Try again' });
    expect(button).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(button).toHaveAttribute('aria-busy', 'true');
    expect(button).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onRetry).toHaveBeenCalledOnce();
  });

  it('shows the request id with a copy button', async () => {
    render(<ErrorState title="Failed" requestId="req_8f2a91" />);
    expect(screen.getByText('req_8f2a91').tagName).toBe('CODE');
    expect(screen.getByRole('button', { name: /Copy Request ID/ })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <ErrorState title="Failed" kind="offline" requestId="r1" onRetry={() => {}} />,
    );
    expect(html).toContain('mn-error-state');
    expect(html).toContain('data-kind="offline"');
  });
});

describe('errorKindFromStatus', () => {
  it('maps HTTP statuses and connectivity to kinds', () => {
    expect(errorKindFromStatus(403)).toBe('forbidden');
    expect(errorKindFromStatus(401)).toBe('forbidden');
    expect(errorKindFromStatus(404)).toBe('not-found');
    expect(errorKindFromStatus(410)).toBe('not-found');
    expect(errorKindFromStatus(500)).toBe('server');
    expect(errorKindFromStatus(503)).toBe('server');
    expect(errorKindFromStatus(0)).toBe('offline');
    expect(errorKindFromStatus(undefined, { offline: true })).toBe('offline');
    expect(errorKindFromStatus(500, { offline: true })).toBe('offline');
    expect(errorKindFromStatus(422)).toBe('error');
    expect(errorKindFromStatus(null)).toBe('error');
  });
});

describe('ErrorStateBoundary', () => {
  function Bomb({ fail }: { fail: boolean }): ReactElement {
    if (fail) throw new Error('secret internal detail');
    return <p>Panel content</p>;
  }

  it('catches a render error, never shows its message, and retries', async () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
    const onError = vi.fn();
    const onReset = vi.fn();
    let fail = true;
    function Flaky() {
      return <Bomb fail={fail} />;
    }
    render(
      <main>
        <ErrorStateBoundary title="This panel didn't load" onError={onError} onReset={onReset}>
          <Flaky />
        </ErrorStateBoundary>
        <p>Rest of the page</p>
      </main>,
    );
    expect(screen.getByRole('alert')).toHaveTextContent("This panel didn't load");
    expect(screen.queryByText(/secret internal detail/)).toBeNull();
    expect(screen.getByText('Rest of the page')).toBeInTheDocument();
    expect(onError).toHaveBeenCalledOnce();
    expect(onError.mock.calls[0]![0]).toBeInstanceOf(Error);
    await expectNoA11yViolations();
    fail = false;
    await userEvent.setup().click(screen.getByRole('button', { name: 'Try again' }));
    expect(screen.getByText('Panel content')).toBeInTheDocument();
    expect(onReset).toHaveBeenCalledOnce();
    spy.mockRestore();
  });

  it('resets when resetKeys change and supports a render-prop fallback', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
    const { rerender } = render(
      <ErrorStateBoundary
        title="unused"
        resetKeys={['a']}
        fallback={({ reset }) => (
          <button type="button" onClick={reset}>
            Custom fallback
          </button>
        )}
      >
        <Bomb fail />
      </ErrorStateBoundary>,
    );
    expect(screen.getByRole('button', { name: 'Custom fallback' })).toBeInTheDocument();
    rerender(
      <ErrorStateBoundary title="unused" resetKeys={['b']}>
        <Bomb fail={false} />
      </ErrorStateBoundary>,
    );
    expect(screen.getByText('Panel content')).toBeInTheDocument();
    spy.mockRestore();
  });

  it('renders children on the server', () => {
    const html = renderToString(
      <ErrorStateBoundary title="Failed">
        <p>ok</p>
      </ErrorStateBoundary>,
    );
    expect(html).toContain('ok');
  });
});
