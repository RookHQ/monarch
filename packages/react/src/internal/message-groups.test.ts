import { describe, expect, it } from 'vitest';
import { continuesMessageGroup, messageRowLayout } from './message-groups.js';

const msg = (id: string, time: string, authorId?: string | null) => ({ id, time, authorId });
const labels = { today: 'Today', yesterday: 'Yesterday' };
const now = '2026-11-04T18:00:00Z';

describe('message grouping', () => {
  it('groups the same author on the same day within the window', () => {
    const a = msg('a', '2026-11-04T10:00:00Z', 'u1');
    expect(continuesMessageGroup(a, msg('b', '2026-11-04T10:04:00Z', 'u1'), undefined, 'UTC')).toBe(
      true,
    );
    expect(continuesMessageGroup(a, msg('b', '2026-11-04T10:05:00Z', 'u1'), undefined, 'UTC')).toBe(
      false,
    );
    expect(continuesMessageGroup(a, msg('b', '2026-11-04T10:01:00Z', 'u2'))).toBe(false);
    expect(continuesMessageGroup(a, msg('b', '2026-11-04T09:59:00Z', 'u1'))).toBe(false);
    expect(continuesMessageGroup(msg('a', a.time, null), msg('b', a.time, null))).toBe(false);
    expect(continuesMessageGroup(undefined, a)).toBe(false);
    expect(continuesMessageGroup(a, msg('b', '2026-11-04T10:20:00Z', 'u1'), 30 * 60_000)).toBe(
      true,
    );
  });

  it('does not group across midnight in the display zone', () => {
    const a = msg('a', '2026-11-04T23:58:00Z', 'u1');
    const b = msg('b', '2026-11-05T00:01:00Z', 'u1');
    expect(continuesMessageGroup(a, b, undefined, 'UTC')).toBe(false);
    expect(continuesMessageGroup(a, b, undefined, 'Asia/Kolkata')).toBe(true);
  });

  it('lays out day separators, the unread divider and grouping', () => {
    const a = msg('a', '2026-11-03T10:00:00Z', 'u1');
    const b = msg('b', '2026-11-04T10:00:00Z', 'u1');
    const c = msg('c', '2026-11-04T10:01:00Z', 'u1');
    const opts = { timeZone: 'UTC', now, labels, locale: 'en-GB' };
    expect(messageRowLayout(undefined, a, opts)).toEqual({
      newDay: true,
      dayLabel: 'Yesterday',
      unread: false,
      grouped: false,
    });
    expect(messageRowLayout(a, b, opts)).toMatchObject({ newDay: true, dayLabel: 'Today' });
    expect(messageRowLayout(b, c, opts)).toEqual({
      newDay: false,
      dayLabel: '',
      unread: false,
      grouped: true,
    });
    expect(messageRowLayout(b, c, { ...opts, firstUnreadId: 'c' })).toEqual({
      newDay: false,
      dayLabel: '',
      unread: true,
      grouped: false,
    });
  });
});
