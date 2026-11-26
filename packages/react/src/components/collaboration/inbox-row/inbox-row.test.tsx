import { describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { hydrateRoot } from 'react-dom/client';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { InboxList, InboxRow, formatCompactAge, type InboxRowProps } from './index.js';

const NOW = new Date('2026-05-10T12:00:00Z');

const BASE: InboxRowProps = {
  title: 'Jordan Kim',
  href: '/inbox/41',
  preview: 'Can I change the delivery address?',
  timestamp: new Date('2026-05-10T11:55:00Z'),
  now: NOW,
};

function renderRow(props: Partial<InboxRowProps> = {}) {
  return render(
    <InboxList aria-label="Open conversations">
      <InboxRow {...BASE} {...props} />
    </InboxList>,
  );
}

describe('formatCompactAge', () => {
  it('shortens ages', () => {
    expect(formatCompactAge(NOW, NOW)).toBe('now');
    expect(formatCompactAge(new Date('2026-05-10T11:55:00Z'), NOW)).toBe('5m');
    expect(formatCompactAge(new Date('2026-05-10T09:00:00Z'), NOW)).toBe('3h');
    expect(formatCompactAge(new Date('2026-05-08T12:00:00Z'), NOW)).toBe('2d');
    expect(formatCompactAge(new Date('2026-04-20T12:00:00Z'), NOW)).toBe('2w');
    expect(formatCompactAge(new Date('2026-01-12T12:00:00Z'), NOW, 'en-GB')).toBe('12 Jan');
    expect(formatCompactAge(new Date('2025-01-12T12:00:00Z'), NOW, 'en-GB')).toBe('12 Jan 2025');
  });
});

describe('InboxRow', () => {
  it('is a list item with one link naming the conversation, unread state and time', async () => {
    const ref = createRef<HTMLLIElement>();
    render(
      <InboxList aria-label="Open conversations">
        <InboxRow ref={ref} {...BASE} unread iconLabel="via email" current />
      </InboxList>,
    );
    const list = screen.getByRole('list', { name: 'Open conversations' });
    const item = within(list).getByRole('listitem');
    expect(ref.current).toBe(item);
    expect(item).toHaveAttribute('data-unread', 'true');
    const link = within(item).getByRole('link');
    expect(link).toHaveAttribute('href', '/inbox/41');
    expect(link).toHaveAttribute('aria-current', 'true');
    expect(link).toHaveAccessibleName(/^Jordan Kim\s*, via email\s*, Unread\s*, .*Can I change/);
    expect(screen.getByText('5m')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('shows an unread count capped at 99+', () => {
    renderRow({ unread: 140 });
    expect(screen.getByText('99+')).toBeInTheDocument();
    expect(screen.getByRole('link')).toHaveAccessibleName(/140 unread/);
  });

  it('opens in place on a plain click but leaves modifier clicks to the browser', async () => {
    const user = userEvent.setup();
    const onOpen = vi.fn();
    renderRow({ onOpen });
    await user.click(screen.getByRole('link'));
    expect(onOpen).toHaveBeenCalledTimes(1);
    const event = new MouseEvent('click', { bubbles: true, cancelable: true, ctrlKey: true });
    screen.getByRole('link').dispatchEvent(event);
    expect(onOpen).toHaveBeenCalledTimes(1);
    expect(event.defaultPrevented).toBe(false);
  });

  it('selects with the checkbox, shift-click and while selecting', async () => {
    const user = userEvent.setup();
    const onSelectedChange = vi.fn();
    const onOpen = vi.fn();
    const { rerender } = renderRow({ onSelectedChange, onOpen });
    const box = screen.getByRole('checkbox', { name: 'Select Jordan Kim' });
    expect(box).toHaveAttribute('aria-checked', 'false');
    await user.click(box);
    expect(onSelectedChange).toHaveBeenLastCalledWith(true, { shiftKey: false });
    await user.keyboard('{Shift>}');
    await user.click(screen.getByRole('link'));
    await user.keyboard('{/Shift}');
    expect(onSelectedChange).toHaveBeenLastCalledWith(true, { shiftKey: true });
    rerender(
      <InboxList aria-label="Open conversations">
        <InboxRow
          {...BASE}
          selected
          selecting
          onSelectedChange={onSelectedChange}
          onOpen={onOpen}
        />
      </InboxList>,
    );
    expect(screen.getByRole('checkbox')).toHaveAttribute('aria-checked', 'true');
    await user.click(screen.getByRole('link'));
    expect(onSelectedChange).toHaveBeenLastCalledWith(false, { shiftKey: false });
    expect(onOpen).not.toHaveBeenCalled();
    await user.tab({ shift: true });
    expect(screen.getByRole('checkbox')).toHaveFocus();
    await user.keyboard(' ');
    expect(onSelectedChange).toHaveBeenCalledTimes(4);
    await expectNoA11yViolations();
  });

  it('selects on long-press with touch and swallows the following click', () => {
    vi.useFakeTimers();
    try {
      const onSelectedChange = vi.fn();
      const onOpen = vi.fn();
      renderRow({ onSelectedChange, onOpen });
      const link = screen.getByRole('link');
      const down = new Event('pointerdown', { bubbles: true });
      Object.defineProperty(down, 'pointerType', { value: 'touch' });
      fireEvent(link, down);
      act(() => {
        vi.advanceTimersByTime(600);
      });
      expect(onSelectedChange).toHaveBeenCalledWith(true, { shiftKey: false });
      fireEvent.click(link);
      expect(onOpen).not.toHaveBeenCalled();
    } finally {
      vi.useRealTimers();
    }
  });

  it('opens on the next press when a long-press was never followed by a click', () => {
    vi.useFakeTimers();
    try {
      const onSelectedChange = vi.fn();
      const onOpen = vi.fn();
      renderRow({ onSelectedChange, onOpen });
      const link = screen.getByRole('link');
      const touch = () => {
        const down = new Event('pointerdown', { bubbles: true });
        Object.defineProperty(down, 'pointerType', { value: 'touch' });
        fireEvent(link, down);
      };
      touch();
      act(() => {
        vi.advanceTimersByTime(600);
      });
      expect(onSelectedChange).toHaveBeenCalledTimes(1);
      // No click arrives for the long-press. The next tap is an ordinary one.
      fireEvent.pointerUp(link);
      touch();
      fireEvent.pointerUp(link);
      fireEvent.click(link);
      expect(onOpen).toHaveBeenCalledTimes(1);
    } finally {
      vi.useRealTimers();
    }
  });

  it('shows no time for a timestamp that does not parse, instead of throwing', () => {
    renderRow({ timestamp: 'not a date' });
    expect(screen.getByRole('link')).toBeInTheDocument();
    expect(document.querySelector('time')).toBeNull();
  });

  it('renders badges, trailing content and no avatar on request', () => {
    const { container } = renderRow({
      avatar: null,
      badges: <span>Open</span>,
      trailing: <span>#41</span>,
      messages: { unread: () => 'New' },
      unread: true,
    });
    expect(container.querySelector('.mn-avatar')).toBeNull();
    expect(screen.getByText('Open')).toBeInTheDocument();
    expect(screen.getByText('#41')).toBeInTheDocument();
    expect(screen.getByRole('link')).toHaveAccessibleName(/New/);
  });

  it('renders on the server', () => {
    const html = renderToString(
      <InboxList aria-label="Inbox">
        <InboxRow {...BASE} onSelectedChange={() => {}} />
      </InboxList>,
    );
    expect(html).toContain('mn-inbox-row');
    expect(html).toContain('href="/inbox/41"');
  });

  it('keeps the age ticking without an injected now', () => {
    vi.useFakeTimers({ now: new Date('2026-05-10T12:00:00Z'), toFake: ['setTimeout', 'Date'] });
    try {
      const { now: _now, ...live } = BASE;
      render(
        <InboxList aria-label="Inbox">
          <InboxRow {...live} timeZone="UTC" locale="en-GB" />
        </InboxList>,
      );
      expect(screen.getByText('5m')).toBeInTheDocument();
      // Each tick re-arms the next one after React commits.
      for (let i = 0; i < 2; i++) {
        act(() => {
          vi.advanceTimersByTime(30_500);
        });
      }
      expect(screen.getByText('6m')).toBeInTheDocument();
    } finally {
      vi.useRealTimers();
    }
  });

  it('hydrates the server render without a mismatch when the zones differ', async () => {
    let machineZone = 'America/New_York';
    const original = Intl.DateTimeFormat.prototype.resolvedOptions;
    const zone = vi
      .spyOn(Intl.DateTimeFormat.prototype, 'resolvedOptions')
      .mockImplementation(function (this: Intl.DateTimeFormat) {
        return { ...original.call(this), timeZone: machineZone };
      });
    const error = vi.spyOn(console, 'error').mockImplementation(() => {});
    const { now: _now, ...live } = BASE;
    const ui = (
      <InboxList aria-label="Inbox">
        <InboxRow {...live} timestamp="2026-05-10T23:30:00Z" />
      </InboxList>
    );
    const container = document.createElement('div');
    try {
      const html = renderToString(ui);
      // UTC on the server, whatever the server's own zone is.
      expect(html).toContain('May 10, 2026');
      machineZone = 'Asia/Kolkata';
      container.innerHTML = html;
      document.body.append(container);
      let root: ReturnType<typeof hydrateRoot> | undefined;
      await act(async () => {
        root = hydrateRoot(container, ui);
      });
      expect(error).not.toHaveBeenCalled();
      // The viewer's zone after hydration: 23:30 UTC is 11 May in Kolkata.
      expect(container.querySelector('time')?.getAttribute('title')).toContain('May 11');
      act(() => root?.unmount());
    } finally {
      container.remove();
      zone.mockRestore();
      error.mockRestore();
    }
  });
});
