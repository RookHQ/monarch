/**
 * Locale-aware formatting and parsing for calendar dates and wall-clock times,
 * built on `Intl` only. Every formatter formats a UTC midnight with
 * `timeZone: 'UTC'`, so the output is the calendar date itself and never
 * shifts with the runtime's time zone. Pure and SSR-safe.
 *
 * `locale` is optional everywhere; `undefined` means the runtime default. For
 * server rendering pass an explicit locale, so server and browser agree.
 */
import {
  daysInMonth,
  isIsoDate,
  isoToUtcDate,
  toIsoDate,
  type IsoDate,
  type Weekday,
} from './iso.js';

const cache = new Map<string, Intl.DateTimeFormat>();

/** A cached `Intl.DateTimeFormat` (grids format dozens of labels per render). */
export function dateFormatter(
  locale: string | undefined,
  options: Intl.DateTimeFormatOptions,
): Intl.DateTimeFormat {
  const key = `${locale ?? ''}|${JSON.stringify(options)}`;
  let f = cache.get(key);
  if (!f) {
    f = new Intl.DateTimeFormat(locale, { timeZone: 'UTC', ...options });
    cache.set(key, f);
  }
  return f;
}

/** Formats an ISO date in the locale, for example `'Wednesday, 4 November 2026'`. */
export function formatIsoDate(
  iso: IsoDate,
  locale?: string,
  options: Intl.DateTimeFormatOptions = { dateStyle: 'medium' },
): string {
  return dateFormatter(locale, options).format(isoToUtcDate(iso));
}

/** The full accessible name of a day: weekday, day, month and year. */
export function formatFullDate(iso: IsoDate, locale?: string): string {
  return formatIsoDate(iso, locale, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });
}

/** `'November 2026'` in the locale. */
export function formatMonthTitle(iso: IsoDate, locale?: string): string {
  return formatIsoDate(iso, locale, { month: 'long', year: 'numeric' });
}

/** The day-of-month numeral in the locale's digits. */
export function formatDayNumber(iso: IsoDate, locale?: string): string {
  return formatIsoDate(iso, locale, { day: 'numeric' });
}

/** Weekday names starting from `weekStartsOn`. */
export function weekdayNames(
  locale: string | undefined,
  weekStartsOn: Weekday,
  width: 'long' | 'short' | 'narrow' = 'short',
): string[] {
  const f = dateFormatter(locale, { weekday: width });
  // 2023-01-01 was a Sunday.
  return Array.from({ length: 7 }, (_, i) =>
    f.format(new Date(Date.UTC(2023, 0, 1 + ((weekStartsOn + i) % 7)))),
  );
}

/** Month names, January first. */
export function monthNames(locale: string | undefined, width: 'long' | 'short' = 'long'): string[] {
  const f = dateFormatter(locale, { month: width });
  return Array.from({ length: 12 }, (_, i) => f.format(new Date(Date.UTC(2023, i, 1))));
}

// CLDR firstDay by region (supplemental weekData). Everything else starts on Monday.
const SUNDAY_REGIONS = new Set(
  'AG AS BD BR BS BT BW BZ CA CO DM DO ET GT GU HK HN ID IL IN JM JP KE KH KR LA MH MM MO MT MX MZ NI NP PA PE PH PK PR PT PY SA SG SV TH TT TW UM US VE VI WS YE ZA ZW'.split(
    ' ',
  ),
);
const SATURDAY_REGIONS = new Set('AE AF BH DJ DZ EG IQ IR JO KW LY OM QA SD SY'.split(' '));

interface LocaleWithWeekInfo extends Intl.Locale {
  getWeekInfo?: () => { firstDay: number };
  weekInfo?: { firstDay: number };
}

function resolvedLocale(locale?: string): string {
  return new Intl.DateTimeFormat(locale).resolvedOptions().locale;
}

/** The first day of the week for a locale (0 = Sunday), from `Intl.Locale` week info or CLDR data. */
export function localeWeekStart(locale?: string): Weekday {
  try {
    const loc = new Intl.Locale(resolvedLocale(locale)) as LocaleWithWeekInfo;
    const info = loc.getWeekInfo?.() ?? loc.weekInfo;
    if (info && info.firstDay >= 1 && info.firstDay <= 7) return (info.firstDay % 7) as Weekday;
    const region = loc.maximize().region ?? '';
    if (region === 'MV') return 5;
    if (SATURDAY_REGIONS.has(region)) return 6;
    if (SUNDAY_REGIONS.has(region)) return 0;
  } catch {
    // An unknown locale falls through to Monday, the ISO 8601 default.
  }
  return 1;
}

export type DateField = 'day' | 'month' | 'year';

/** The order of day, month and year in the locale's numeric date (`['month', 'day', 'year']` in en-US). */
export function dateFieldOrder(locale?: string): DateField[] {
  const order: DateField[] = [];
  for (const p of numericFormatter(locale).formatToParts(new Date(Date.UTC(2026, 11, 31))))
    if (p.type === 'day' || p.type === 'month' || p.type === 'year') order.push(p.type);
  return order.length === 3 ? order : ['year', 'month', 'day'];
}

const numericFormatter = (locale?: string) =>
  dateFormatter(locale, { day: '2-digit', month: '2-digit', year: 'numeric' });

/** The numeric short date the pickers show and accept, for example `'04/11/2026'` in en-GB. */
export function formatNumericDate(iso: IsoDate, locale?: string): string {
  return numericFormatter(locale).format(isoToUtcDate(iso));
}

/**
 * A typing hint for the locale's numeric date, for example `'DD/MM/YYYY'`
 * (en-GB), `'MM/DD/YYYY'` (en-US) or `'DD.MM.YYYY'` (de). The letters are
 * passed in so products can translate them.
 */
export function dateFormatHint(
  locale?: string,
  letters: { day: string; month: string; year: string } = {
    day: 'DD',
    month: 'MM',
    year: 'YYYY',
  },
): string {
  return numericFormatter(locale)
    .formatToParts(new Date(Date.UTC(2026, 11, 31)))
    .map((p) =>
      p.type === 'day' || p.type === 'month' || p.type === 'year'
        ? letters[p.type]
        : p.type === 'literal'
          ? p.value.replace(/[‎‏؜]/g, '')
          : '',
    )
    .join('');
}

const digitCache = new Map<string, Map<string, string>>();

/** Maps the locale's native digits (and Western Arabic digits) to ASCII. */
function normaliseDigits(text: string, locale?: string): string {
  const key = locale ?? '';
  let map = digitCache.get(key);
  if (!map) {
    map = new Map();
    const nf = new Intl.NumberFormat(locale, { useGrouping: false });
    for (let i = 0; i < 10; i += 1) map.set(nf.format(i), String(i));
    // Arabic-Indic and extended Arabic-Indic, Devanagari and Bengali digits, always accepted.
    for (const base of [0x0660, 0x06f0, 0x0966, 0x09e6])
      for (let i = 0; i < 10; i += 1) map.set(String.fromCharCode(base + i), String(i));
    digitCache.set(key, map);
  }
  let out = '';
  for (const ch of text) out += map.get(ch) ?? ch;
  return out;
}

const fold = (s: string) => s.toLocaleLowerCase().normalize('NFD').replace(/[̀-ͯ.]/g, '');

export interface ParseDateOptions {
  locale?: string | undefined;
  /** Supplies the year when only day and month are typed, and the century for two-digit years. Default: today. */
  referenceDate?: IsoDate | undefined;
}

/**
 * Parses a typed date in the locale's order: `31/12/2026` (en-GB),
 * `12/31/2026` (en-US), `31.12.26`, `31 Dec 2026`, `Dec 31`, `31122026`, and
 * ISO `2026-12-31` in every locale. Weekday names are ignored. Returns `null`
 * when the text is not a real date.
 */
export function parseDateInput(text: string, options: ParseDateOptions = {}): IsoDate | null {
  const { locale } = options;
  const input = normaliseDigits(text.trim(), locale);
  if (!input) return null;
  if (/^\d{4}-\d{2}-\d{2}$/.test(input)) return isIsoDate(input) ? input : null;

  const reference = options.referenceDate ?? toIsoDate(new Date().getFullYear(), 1, 1);
  const referenceYear = Number(reference.slice(0, 4));
  const order = dateFieldOrder(locale);
  const tokens = input.split(/[^\p{L}\p{N}]+/u).filter(Boolean);

  let monthByName: number | null = null;
  const numbers: string[] = [];
  const months = [
    ...monthNames(locale, 'long'),
    ...monthNames(locale, 'short'),
    ...monthNames('en', 'long'),
    ...monthNames('en', 'short'),
  ].map(fold);
  const weekdays = [
    ...weekdayNames(locale, 0, 'long'),
    ...weekdayNames(locale, 0, 'short'),
    ...weekdayNames('en', 0, 'long'),
    ...weekdayNames('en', 0, 'short'),
  ].map(fold);

  for (const token of tokens) {
    // A number, or a number glued to a suffix ("4th", "4日", "2026년"), counts as the number.
    const num = /^(\d+)\p{L}*$/u.exec(token)?.[1];
    if (num !== undefined) {
      numbers.push(num);
      continue;
    }
    const t = fold(token);
    if (weekdays.includes(t)) continue;
    const exact = months.indexOf(t);
    const hit = exact >= 0 ? exact : t.length >= 3 ? months.findIndex((m) => m.startsWith(t)) : -1;
    if (hit < 0 || monthByName !== null) return null;
    monthByName = (hit % 12) + 1;
  }

  const fields = new Map<DateField, string>();
  const [first, second, third] = numbers;

  if (monthByName !== null) {
    // Month given by name: the numbers are the day and, optionally, the year
    // (a three- or four-digit number, or else the one in the year's position).
    fields.set('month', String(monthByName));
    if (first === undefined || third !== undefined) return null;
    if (second === undefined) {
      if (first.length > 2) return null;
      fields.set('day', first);
    } else {
      const yearFirst =
        first.length > 2 || (second.length <= 2 && order.indexOf('year') < order.indexOf('day'));
      fields.set('year', yearFirst ? first : second);
      fields.set('day', yearFirst ? second : first);
    }
  } else if (first !== undefined && second === undefined) {
    // Digits typed without separators: 31122026, 311226, 3112.
    if (![4, 6, 8].includes(first.length)) return null;
    const yearWidth = first.length === 8 ? 4 : first.length === 6 ? 2 : 0;
    let i = 0;
    for (const field of order) {
      const width = field === 'year' ? yearWidth : 2;
      if (width) fields.set(field, first.slice(i, i + width));
      i += width;
    }
  } else if (first !== undefined && second !== undefined && third !== undefined) {
    if (numbers.length > 3) return null;
    // A leading four-digit number is a year: 2026/12/31 reads as Y-M-D in every locale.
    const seq: DateField[] = first.length === 4 ? ['year', 'month', 'day'] : order;
    seq.forEach((f, i) => fields.set(f, numbers[i] ?? ''));
  } else if (first !== undefined && second !== undefined) {
    order.filter((f) => f !== 'year').forEach((f, i) => fields.set(f, numbers[i] ?? ''));
  } else {
    return null;
  }

  const dayText = fields.get('day');
  const monthText = fields.get('month');
  const yearText = fields.get('year');
  if (!dayText || !monthText) return null;
  const day = Number(dayText);
  const month = Number(monthText);
  let year = yearText === undefined ? referenceYear : Number(yearText);
  if (yearText !== undefined && yearText.length === 3) return null;
  if (yearText !== undefined && yearText.length <= 2) {
    // Two-digit year: the century that puts it within 50 years of the reference year.
    year += Math.floor(referenceYear / 100) * 100;
    if (year > referenceYear + 49) year -= 100;
    else if (year < referenceYear - 50) year += 100;
  }
  if (month < 1 || month > 12 || year < 1 || year > 9999) return null;
  if (day < 1 || day > daysInMonth(year, month)) return null;
  return toIsoDate(year, month, day);
}

export type HourCycle = '12' | '24';

/** Whether the locale writes times with a 12-hour clock (`'12'`) or a 24-hour one (`'24'`). */
export function localeHourCycle(locale?: string): HourCycle {
  const o = new Intl.DateTimeFormat(locale, { hour: 'numeric' }).resolvedOptions();
  if (o.hourCycle) return o.hourCycle === 'h11' || o.hourCycle === 'h12' ? '12' : '24';
  return o.hour12 ? '12' : '24';
}

/** Formats minutes after midnight as a wall-clock time: `'2:30 PM'` or `'14:30'`. */
export function formatTime(minutes: number, locale?: string, hourCycle?: HourCycle): string {
  const cycle = hourCycle ?? localeHourCycle(locale);
  return dateFormatter(locale, {
    hour: cycle === '12' ? 'numeric' : '2-digit',
    minute: '2-digit',
    hourCycle: cycle === '12' ? 'h12' : 'h23',
  }).format(new Date(Date.UTC(2023, 0, 1, 0, minutes)));
}

/** The locale's AM and PM markers, folded for matching. */
function dayPeriods(locale?: string): { am: string[]; pm: string[] } {
  const f = dateFormatter(locale, { hour: 'numeric', hourCycle: 'h12' });
  const marker = (h: number) =>
    fold(
      f.formatToParts(new Date(Date.UTC(2023, 0, 1, h))).find((p) => p.type === 'dayPeriod')
        ?.value ?? '',
    ).replace(/\s/g, '');
  return {
    am: ['a', 'am', marker(1)].filter(Boolean),
    pm: ['p', 'pm', marker(13)].filter(Boolean),
  };
}

/**
 * Parses a typed wall-clock time into minutes after midnight: `14:30`,
 * `2:30 pm`, `2pm`, `1430`, `9.15`, `09:00:00`. Returns `null` when invalid.
 */
export function parseTimeInput(text: string, locale?: string): number | null {
  const input = fold(normaliseDigits(text.trim(), locale)).replace(/\s+/g, '');
  if (!input) return null;
  const m = /^(\d{1,4})(?:[:.h](\d{2}))?(?::(\d{2}))?(\p{L}*)$/u.exec(input);
  if (!m) return null;
  let hours: number;
  let minutes: number;
  const head = m[1] ?? '';
  if (m[2] === undefined && head.length > 2) {
    hours = Number(head.slice(0, head.length - 2));
    minutes = Number(head.slice(-2));
  } else {
    hours = Number(head);
    minutes = m[2] === undefined ? 0 : Number(m[2]);
  }
  const suffix = m[4] ?? '';
  if (suffix) {
    const { am, pm } = dayPeriods(locale);
    const isPm = pm.includes(suffix);
    if (!isPm && !am.includes(suffix)) return null;
    if (hours < 1 || hours > 12) return null;
    hours = (hours % 12) + (isPm ? 12 : 0);
  }
  if (hours > 23 || minutes > 59) return null;
  return hours * 60 + minutes;
}
