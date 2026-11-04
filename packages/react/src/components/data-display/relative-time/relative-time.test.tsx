import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { hydrateRoot } from 'react-dom/client';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { RelativeTime, formatRelativeTime, relativeTimeParts } from './index.js';

const NOW = Date.UTC(2026, 10, 3, 12, 0, 0);
const MIN = 60_000;
const HOUR = 60 * MIN;
const DAY = 24 * HOUR;

afterEach(() => {
  vi.useRealTimers();
});

describe('relativeTimeParts / formatRelativeTime', () => {
  it('picks the largest unit that fits, after rounding', () => {
    expect(relativeTimeParts(NOW - 30_000, NOW)).toEqual({ value: -30, unit: 'second' });
    expect(relativeTimeParts(NOW - 5 * MIN, NOW)).toEqual({ value: -5, unit: 'minute' });
    // 59.6 minutes rounds to 60, which rolls over to 1 hour.
    expect(relativeTimeParts(NOW - 59.6 * MIN, NOW)).toEqual({ value: -1, unit: 'hour' });
    expect(relativeTimeParts(NOW + 2 * DAY, NOW)).toEqual({ value: 2, unit: 'day' });
    expect(relativeTimeParts(NOW - 14 * DAY, NOW)).toEqual({ value: -2, unit: 'week' });
    expect(relativeTimeParts(NOW - 400 * DAY, NOW)).toEqual({ value: -1, unit: 'year' });
  });

  it('formats with Intl.RelativeTimeFormat, numeric and style options', () => {
    expect(formatRelativeTime(NOW - 3 * HOUR, { now: NOW, locale: 'en' })).toBe('3 hours ago');
    expect(formatRelativeTime(NOW - DAY, { now: NOW, locale: 'en' })).toBe('yesterday');
    expect(formatRelativeTime(NOW - DAY, { now: NOW, locale: 'en', numeric: 'always' })).toBe(
      '1 day ago',
    );
    expect(formatRelativeTime(NOW, { now: NOW, locale: 'en' })).toBe('now');
    expect(formatRelativeTime(NOW + 2 * DAY, { now: NOW, locale: 'en' })).toBe('in 2 days');
    expect(formatRelativeTime(NOW - 3 * MIN, { now: NOW, locale: 'en', format: 'short' })).toBe(
      '3 min. ago',
    );
    expect(formatRelativeTime(NOW - 3 * HOUR, { now: NOW, locale: 'de' })).toBe('vor 3 Stunden');
  });
});

describe('RelativeTime', () => {
  it('renders a <time datetime> with the relative text and an absolute tooltip', () => {
    render(<RelativeTime date={NOW - 3 * HOUR} now={NOW} locale="en-GB" timeZone="UTC" />);
    const time = screen.getByText('3 hours ago');
    expect(time.tagName).toBe('TIME');
    expect(time).toHaveAttribute('datetime', new Date(NOW - 3 * HOUR).toISOString());
    const tip = screen.getByRole('tooltip');
    expect(tip).toHaveTextContent('3 Nov 2026, 09:00');
    expect(time).toHaveAttribute('aria-describedby', tip.id);
  });

  it('accepts ISO strings and Date objects', () => {
    const iso = new Date(NOW - 2 * DAY).toISOString();
    const { rerender } = render(<RelativeTime date={iso} now={NOW} locale="en" />);
    expect(screen.getByText('2 days ago')).toBeInTheDocument();
    rerender(<RelativeTime date={new Date(NOW - 5 * MIN)} now={new Date(NOW)} locale="en" />);
    expect(screen.getByText('5 minutes ago')).toBeInTheDocument();
  });

  it('is reachable by keyboard and Escape dismisses the tooltip', async () => {
    const { container } = render(
      <>
        <button type="button">Before</button>
        <RelativeTime date={NOW - 3 * HOUR} now={NOW} locale="en" />
      </>,
    );
    const user = userEvent.setup();
    await user.tab();
    await user.tab();
    const time = screen.getByText('3 hours ago');
    expect(time).toHaveFocus();
    const root = container.querySelector('.mn-relative-time')!;
    expect(root).not.toHaveAttribute('data-dismissed');
    await user.keyboard('{Escape}');
    expect(root).toHaveAttribute('data-dismissed');
    await user.tab({ shift: true });
    expect(root).not.toHaveAttribute('data-dismissed');
  });

  it('tooltip={false} renders no tab stop and no tooltip', () => {
    render(<RelativeTime date={NOW - 3 * HOUR} now={NOW} locale="en" tooltip={false} />);
    expect(screen.getByText('3 hours ago')).not.toHaveAttribute('tabindex');
    expect(screen.queryByRole('tooltip')).toBeNull();
  });

  it('switches to the absolute date after absoluteAfter', () => {
    render(
      <RelativeTime
        date={NOW - 10 * DAY}
        now={NOW}
        locale="en-GB"
        timeZone="UTC"
        absoluteFormat={{ dateStyle: 'medium' }}
        absoluteAfter={7 * DAY}
      />,
    );
    expect(screen.getByText('24 Oct 2026')).toBeInTheDocument();
    expect(screen.queryByRole('tooltip')).toBeNull();
  });

  it('formats the absolute time in the given time zone', () => {
    render(
      <RelativeTime
        date={NOW}
        now={NOW}
        locale="en-GB"
        timeZone="Asia/Kolkata"
        absoluteFormat={{ timeStyle: 'short' }}
      />,
    );
    expect(screen.getByRole('tooltip')).toHaveTextContent('17:30');
  });

  it('updates live as time passes', () => {
    vi.useFakeTimers({ now: NOW });
    render(<RelativeTime date={NOW - 2 * MIN} locale="en" live />);
    expect(screen.getByText('2 minutes ago')).toBeInTheDocument();
    for (let i = 0; i < 6; i++) {
      act(() => {
        vi.advanceTimersByTime(30_000);
      });
    }
    expect(screen.getByText('5 minutes ago')).toBeInTheDocument();
  });

  it('does not tick without live', () => {
    vi.useFakeTimers({ now: NOW });
    render(<RelativeTime date={NOW - 2 * MIN} locale="en" />);
    act(() => {
      vi.advanceTimersByTime(10 * MIN);
    });
    expect(screen.getByText('2 minutes ago')).toBeInTheDocument();
  });

  it('renders a dash for an invalid date', () => {
    const { container } = render(<RelativeTime date="not a date" />);
    expect(container.querySelector('time')).toHaveAttribute('data-invalid');
    expect(container).toHaveTextContent('—');
  });

  it('forwards the ref and props to the <time> element', () => {
    let node: HTMLTimeElement | null = null;
    render(
      <RelativeTime
        ref={(n) => {
          node = n;
        }}
        data-testid="t"
        date={NOW}
        now={NOW}
        className="extra"
      />,
    );
    expect(node).toBe(screen.getByTestId('t'));
    expect(screen.getByTestId('t').parentElement).toHaveClass('mn-relative-time', 'extra');
  });

  it('calls consumer key handlers', () => {
    const onKeyDown = vi.fn();
    render(<RelativeTime date={NOW} now={NOW} onKeyDown={onKeyDown} />);
    fireEvent.keyDown(screen.getByText('now'), { key: 'a' });
    expect(onKeyDown).toHaveBeenCalled();
  });

  it('has no axe violations', async () => {
    render(
      <main>
        <p>
          Updated <RelativeTime date={NOW - 3 * HOUR} now={NOW} locale="en" />
        </p>
        <p>
          <RelativeTime date={NOW - 3 * DAY} now={NOW} locale="en" tooltip={false} />
        </p>
      </main>,
    );
    await expectNoA11yViolations();
  });

  it('renders on the server, identically when now is injected', () => {
    const el = <RelativeTime date={NOW - 3 * HOUR} now={NOW} locale="en" timeZone="UTC" />;
    const html = renderToString(el);
    expect(html).toContain('mn-relative-time');
    expect(html).toContain('3 hours ago');
    expect(html).toContain(`dateTime="${new Date(NOW - 3 * HOUR).toISOString()}"`);
  });

  it('hydrates without errors when the client clock differs from the server', async () => {
    vi.useFakeTimers({ now: NOW, toFake: ['Date'] });
    const html = renderToString(<RelativeTime date={NOW - 3 * HOUR} locale="en" />);
    expect(html).toContain('3 hours ago');
    vi.setSystemTime(NOW + 2 * HOUR);
    const container = document.createElement('div');
    container.innerHTML = html;
    document.body.appendChild(container);
    const errors = vi.spyOn(console, 'error').mockImplementation(() => {});
    const recoverable = vi.fn();
    await act(async () => {
      hydrateRoot(container, <RelativeTime date={NOW - 3 * HOUR} locale="en" />, {
        onRecoverableError: recoverable,
      });
    });
    expect(recoverable).not.toHaveBeenCalled();
    expect(errors).not.toHaveBeenCalled();
    expect(container).toHaveTextContent('5 hours ago');
    errors.mockRestore();
    container.remove();
  });
});
