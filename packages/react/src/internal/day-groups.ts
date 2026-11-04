/**
 * Calendar-day helpers shared by the collaboration feeds (MessageTimeline,
 * NotificationCenter, ActivityTimeline). Days are compared in the viewer's
 * time zone, or in `timeZone` when one is given, so "Today" means the same
 * thing as the clock on the reader's screen.
 */

/** Anything `new Date()` accepts. Mirrors `DateInput` in RelativeTime. */
export type DayInput = Date | number | string;

export interface DayOptions {
  /** IANA time zone. Default: the viewer's zone. */
  timeZone?: string | undefined;
  /** BCP 47 locale for labels. Default: the runtime locale. */
  locale?: string | undefined;
  /** Reference time for "Today" and "Yesterday". Default: the current time. */
  now?: DayInput | undefined;
}

export interface DayLabels {
  today: string;
  yesterday: string;
}

export const DEFAULT_DAY_LABELS: DayLabels = { today: 'Today', yesterday: 'Yesterday' };

function toDate(input: DayInput): Date {
  return input instanceof Date ? input : new Date(input);
}

/** `YYYY-MM-DD` of the moment in the given zone: a stable key for grouping. */
export function dayKey(input: DayInput, timeZone?: string): string {
  const date = toDate(input);
  if (Number.isNaN(date.getTime())) return 'invalid';
  const parts = new Intl.DateTimeFormat('en-CA', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    ...(timeZone ? { timeZone } : {}),
  }).formatToParts(date);
  const get = (type: string) => parts.find((p) => p.type === type)?.value ?? '00';
  return `${get('year')}-${get('month')}-${get('day')}`;
}

/** Whether two moments fall on the same calendar day. */
export function sameDay(a: DayInput, b: DayInput, timeZone?: string): boolean {
  return dayKey(a, timeZone) === dayKey(b, timeZone);
}

/** The key of the day before `key` (`YYYY-MM-DD`), in plain calendar arithmetic. */
function previousDayKey(key: string): string {
  const [y, m, d] = key.split('-').map(Number) as [number, number, number];
  const prev = new Date(Date.UTC(y, m - 1, d - 1));
  return prev.toISOString().slice(0, 10);
}

/**
 * "Today", "Yesterday", a weekday and date for this year ("Monday, 3 March"),
 * or a full date for other years.
 */
export function dayLabel(
  input: DayInput,
  options: DayOptions = {},
  labels: DayLabels = DEFAULT_DAY_LABELS,
): string {
  const { timeZone, locale, now = Date.now() } = options;
  const date = toDate(input);
  if (Number.isNaN(date.getTime())) return '';
  const key = dayKey(date, timeZone);
  const today = dayKey(now, timeZone);
  if (key === today) return labels.today;
  if (key === previousDayKey(today)) return labels.yesterday;
  const sameYear = key.slice(0, 4) === today.slice(0, 4);
  return new Intl.DateTimeFormat(locale, {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
    ...(sameYear ? {} : { year: 'numeric' }),
    ...(timeZone ? { timeZone } : {}),
  }).format(date);
}

export interface DayGroup<T> {
  /** `YYYY-MM-DD` in the display zone. */
  key: string;
  /** The first item's moment, for labelling. */
  date: Date;
  items: T[];
}

/**
 * Splits items into consecutive runs of the same day, keeping their order.
 * Items do not need to be sorted, but a day that appears twice in an unsorted
 * list gets two groups.
 */
export function groupByDay<T>(
  items: readonly T[],
  getDate: (item: T) => DayInput,
  timeZone?: string,
): DayGroup<T>[] {
  const groups: DayGroup<T>[] = [];
  for (const item of items) {
    const date = toDate(getDate(item));
    const key = dayKey(date, timeZone);
    const last = groups[groups.length - 1];
    if (last && last.key === key) last.items.push(item);
    else groups.push({ key, date, items: [item] });
  }
  return groups;
}
