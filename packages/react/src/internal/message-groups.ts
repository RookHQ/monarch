/**
 * The grouping rule and day separators shared by the message lists
 * (MessageTimeline and VirtualMessageList): when a message continues the
 * previous one's group, and when a row starts a new day.
 */
import { dayKey, dayLabel, sameDay, type DayInput, type DayLabels } from './day-groups.js';

/** Default window for grouping consecutive messages from one author: 5 minutes. */
export const MESSAGE_GROUP_WINDOW_MS = 5 * 60_000;

/** The fields the grouping rule reads from a message. */
export interface GroupableMessage {
  id: string;
  time: DayInput;
  authorId?: string | null | undefined;
}

const ms = (input: DayInput) => (input instanceof Date ? input : new Date(input)).getTime();

/**
 * Whether `next` continues `previous`'s group: same author (both set), same
 * day in `timeZone`, and sent within `windowMs` after it.
 */
export function continuesMessageGroup(
  previous: GroupableMessage | undefined,
  next: GroupableMessage,
  windowMs: number = MESSAGE_GROUP_WINDOW_MS,
  timeZone?: string | undefined,
): boolean {
  if (!previous || previous.authorId == null || next.authorId == null) return false;
  if (previous.authorId !== next.authorId) return false;
  if (!sameDay(previous.time, next.time, timeZone)) return false;
  const gap = ms(next.time) - ms(previous.time);
  return gap >= 0 && gap < windowMs;
}

export interface MessageRowOptions {
  /** The first unread message: it gets a "New messages" divider and never groups. */
  firstUnreadId?: string | null | undefined;
  windowMs?: number | undefined;
  timeZone?: string | undefined;
  locale?: string | undefined;
  /** Reference time for "Today" and "Yesterday". */
  now?: DayInput | null | undefined;
  labels: DayLabels;
}

export interface MessageRowLayout {
  /** The row starts a new day: render a day separator before it. */
  newDay: boolean;
  /** The day separator's text, or `''` when `newDay` is false. */
  dayLabel: string;
  /** The row is the first unread message: render the unread divider before it. */
  unread: boolean;
  /** The row continues the previous message's group: render it compact. */
  grouped: boolean;
}

/** What to draw before and for one message row, given the message before it. */
export function messageRowLayout(
  previous: GroupableMessage | undefined,
  item: GroupableMessage,
  options: MessageRowOptions,
): MessageRowLayout {
  const { firstUnreadId, windowMs, timeZone, locale, now, labels } = options;
  const newDay = !previous || dayKey(previous.time, timeZone) !== dayKey(item.time, timeZone);
  const unread = firstUnreadId != null && item.id === firstUnreadId;
  const grouped = !newDay && !unread && continuesMessageGroup(previous, item, windowMs, timeZone);
  return {
    newDay,
    dayLabel: newDay ? dayLabel(item.time, { now, locale, timeZone }, labels) : '',
    unread,
    grouped,
  };
}
