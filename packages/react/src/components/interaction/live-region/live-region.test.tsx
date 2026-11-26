import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AnnouncerProvider, LiveRegion, useAnnouncer } from './index.js';

function Announce({ text, politeness }: { text: string; politeness?: 'polite' | 'assertive' }) {
  const { announce, clear } = useAnnouncer();
  return (
    <>
      <button type="button" onClick={() => announce(text, politeness)}>
        Announce
      </button>
      <button type="button" onClick={clear}>
        Clear
      </button>
    </>
  );
}

const region = (p: 'polite' | 'assertive', scope: ParentNode = document) =>
  scope.querySelector<HTMLElement>(`[aria-live="${p}"]`);

afterEach(() => {
  document.querySelectorAll('[data-mn-announcer="global"]').forEach((el) => {
    el.textContent = '';
  });
});

describe('LiveRegion', () => {
  it('renders a polite, atomic, visually hidden region by default', async () => {
    render(<LiveRegion data-testid="r">Saved</LiveRegion>);
    const r = screen.getByTestId('r');
    expect(r).toHaveClass('mn-live-region', 'mn-visually-hidden');
    expect(r).toHaveAttribute('aria-live', 'polite');
    expect(r).toHaveAttribute('aria-atomic', 'true');
    expect(r).toHaveAttribute('data-politeness', 'polite');
    expect(r).toHaveTextContent('Saved');
    await expectNoA11yViolations();
  });

  it('can be assertive, non-atomic and visible', () => {
    render(
      <LiveRegion data-testid="r" politeness="assertive" atomic={false} visuallyHidden={false} />,
    );
    const r = screen.getByTestId('r');
    expect(r).toHaveAttribute('aria-live', 'assertive');
    expect(r).toHaveAttribute('aria-atomic', 'false');
    expect(r).not.toHaveClass('mn-visually-hidden');
  });

  it('forwards its ref', () => {
    const ref = { current: null as HTMLDivElement | null };
    render(<LiveRegion ref={ref} />);
    expect(ref.current).toBeInstanceOf(HTMLDivElement);
  });
});

describe('useAnnouncer', () => {
  it('speaks through the nearest AnnouncerProvider, politely by default', async () => {
    const user = userEvent.setup();
    const { container } = render(
      <AnnouncerProvider>
        <Announce text="3 results" />
      </AnnouncerProvider>,
    );
    await user.click(screen.getByRole('button', { name: 'Announce' }));
    expect(region('polite', container)).toHaveTextContent('3 results');
    expect(region('assertive', container)).toHaveTextContent('');
    await expectNoA11yViolations();
  });

  it('uses the assertive region when asked and empties the other one', async () => {
    const user = userEvent.setup();
    const { container, rerender } = render(
      <AnnouncerProvider>
        <Announce text="Saved" />
      </AnnouncerProvider>,
    );
    await user.click(screen.getByRole('button', { name: 'Announce' }));
    rerender(
      <AnnouncerProvider>
        <Announce text="Could not save" politeness="assertive" />
      </AnnouncerProvider>,
    );
    await user.click(screen.getByRole('button', { name: 'Announce' }));
    expect(region('assertive', container)).toHaveTextContent('Could not save');
    expect(region('polite', container)?.textContent).toBe('');
  });

  it('re-announces the same text by changing it slightly', async () => {
    const user = userEvent.setup();
    const { container } = render(
      <AnnouncerProvider>
        <Announce text="Copied" />
      </AnnouncerProvider>,
    );
    const button = screen.getByRole('button', { name: 'Announce' });
    await user.click(button);
    const first = region('polite', container)?.textContent;
    await user.click(button);
    const second = region('polite', container)?.textContent;
    expect(first).toBe('Copied');
    expect(second).not.toBe(first);
    expect(second?.trim()).toBe('Copied');
  });

  it('clears on demand and after `clearAfter`', async () => {
    vi.useFakeTimers();
    try {
      const { container } = render(
        <AnnouncerProvider clearAfter={1000}>
          <Announce text="Uploaded" />
        </AnnouncerProvider>,
      );
      act(() => screen.getByRole('button', { name: 'Announce' }).click());
      expect(region('polite', container)).toHaveTextContent('Uploaded');
      act(() => {
        vi.advanceTimersByTime(1000);
      });
      expect(region('polite', container)?.textContent).toBe('');
      act(() => screen.getByRole('button', { name: 'Announce' }).click());
      act(() => screen.getByRole('button', { name: 'Clear' }).click());
      expect(region('polite', container)?.textContent).toBe('');
    } finally {
      vi.useRealTimers();
    }
  });

  it('falls back to page-level regions on <body> without a provider', async () => {
    const user = userEvent.setup();
    render(<Announce text="Filters applied" />);
    await user.click(screen.getByRole('button', { name: 'Announce' }));
    const global = document.querySelector('[data-mn-announcer="global"][aria-live="polite"]');
    expect(global).toHaveTextContent('Filters applied');
    expect(global?.parentElement).toBe(document.body);
    await user.click(screen.getByRole('button', { name: 'Clear' }));
    expect(global?.textContent).toBe('');
  });

  it('empties the page-level region a few seconds after speaking', () => {
    vi.useFakeTimers();
    try {
      render(<Announce text="Saved" />);
      act(() => screen.getByRole('button', { name: 'Announce' }).click());
      const global = document.querySelector('[data-mn-announcer="global"][aria-live="polite"]');
      expect(global).toHaveTextContent('Saved');
      act(() => {
        vi.advanceTimersByTime(7000);
      });
      expect(global?.textContent).toBe('');
    } finally {
      vi.useRealTimers();
    }
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AnnouncerProvider>
        <LiveRegion>Ready</LiveRegion>
      </AnnouncerProvider>,
    );
    expect(html).toContain('mn-live-region');
    expect(html).toContain('aria-live="assertive"');
  });
});
