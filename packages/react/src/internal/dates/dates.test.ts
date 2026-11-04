import { describe, expect, it } from 'vitest';
import {
  addDays,
  addMonths,
  addYears,
  clampIso,
  compareIso,
  dateFieldOrder,
  dateFormatHint,
  dateToIso,
  dayOfWeek,
  daysBetween,
  daysInMonth,
  endOfMonth,
  endOfWeek,
  formatFullDate,
  formatMonthTitle,
  formatNumericDate,
  formatTime,
  isIsoDate,
  isoWeekNumber,
  isWithin,
  localeHourCycle,
  localeWeekStart,
  minutesToIsoTime,
  monthGrid,
  monthsBetween,
  parseDateInput,
  parseIsoDate,
  parseIsoDateTime,
  parseTimeInput,
  startOfMonth,
  startOfWeek,
  toIsoDate,
  toIsoDateTime,
  todayIso,
  weekdayNames,
} from './index.js';

describe('ISO date maths', () => {
  it('validates real calendar dates only', () => {
    expect(isIsoDate('2026-11-04')).toBe(true);
    expect(isIsoDate('2024-02-29')).toBe(true);
    expect(isIsoDate('2026-02-29')).toBe(false);
    expect(isIsoDate('2026-13-01')).toBe(false);
    expect(isIsoDate('2026-1-01')).toBe(false);
    expect(isIsoDate('2026-11-04T10:00')).toBe(false);
    expect(isIsoDate(null)).toBe(false);
    expect(parseIsoDate('2026-11-04')).toEqual({ year: 2026, month: 11, day: 4 });
  });

  it('builds dates with rollover, including years below 100', () => {
    expect(toIsoDate(2026, 13, 1)).toBe('2027-01-01');
    expect(toIsoDate(2026, 3, 0)).toBe('2026-02-28');
    expect(toIsoDate(45, 6, 1)).toBe('0045-06-01');
    expect(daysInMonth(2024, 2)).toBe(29);
    expect(daysInMonth(1900, 2)).toBe(28);
  });

  it('adds days across months, years and DST changes without drift', () => {
    expect(addDays('2026-12-31', 1)).toBe('2027-01-01');
    expect(addDays('2026-03-01', -1)).toBe('2026-02-28');
    // European and US daylight-saving weekends.
    expect(addDays('2026-03-28', 1)).toBe('2026-03-29');
    expect(addDays('2026-03-29', 1)).toBe('2026-03-30');
    expect(addDays('2026-11-01', 1)).toBe('2026-11-02');
    expect(daysBetween('2026-03-01', '2026-04-01')).toBe(31);
    expect(daysBetween('2026-04-01', '2026-03-01')).toBe(-31);
  });

  it('adds months and years, clamping the day', () => {
    expect(addMonths('2026-01-31', 1)).toBe('2026-02-28');
    expect(addMonths('2024-01-31', 1)).toBe('2024-02-29');
    expect(addMonths('2026-03-31', -1)).toBe('2026-02-28');
    expect(addMonths('2026-11-15', 14)).toBe('2028-01-15');
    expect(addYears('2024-02-29', 1)).toBe('2025-02-28');
    expect(monthsBetween('2026-11-30', '2027-01-01')).toBe(2);
  });

  it('compares, clamps and checks bounds', () => {
    expect(compareIso('2026-01-01', '2026-01-02')).toBeLessThan(0);
    expect(compareIso('2026-01-02', '2026-01-02')).toBe(0);
    expect(clampIso('2026-01-01', '2026-02-01', '2026-03-01')).toBe('2026-02-01');
    expect(clampIso('2026-04-01', '2026-02-01', '2026-03-01')).toBe('2026-03-01');
    expect(isWithin('2026-02-10', '2026-02-01', null)).toBe(true);
    expect(isWithin('2026-01-10', '2026-02-01', undefined)).toBe(false);
  });

  it('knows months and weeks', () => {
    expect(startOfMonth('2026-11-04')).toBe('2026-11-01');
    expect(endOfMonth('2026-02-04')).toBe('2026-02-28');
    expect(dayOfWeek('2026-11-04')).toBe(3);
    expect(startOfWeek('2026-11-04', 1)).toBe('2026-11-02');
    expect(startOfWeek('2026-11-04', 0)).toBe('2026-11-01');
    expect(startOfWeek('2026-11-04', 6)).toBe('2026-10-31');
    expect(endOfWeek('2026-11-04', 1)).toBe('2026-11-08');
  });

  it('computes ISO 8601 week numbers at year edges', () => {
    expect(isoWeekNumber('2026-01-01')).toBe(1);
    expect(isoWeekNumber('2027-01-01')).toBe(53);
    expect(isoWeekNumber('2024-12-30')).toBe(1);
    expect(isoWeekNumber('2026-11-04')).toBe(45);
  });

  it('lays out a month grid from any week start', () => {
    const mon = monthGrid('2026-11-04', 1);
    expect(mon[0]![0]).toBe('2026-10-26');
    expect(mon.at(-1)!.at(-1)).toBe('2026-12-06');
    expect(mon.every((w) => w.length === 7)).toBe(true);
    const sun = monthGrid('2026-02-10', 0);
    expect(sun).toHaveLength(4);
    expect(sun[0]![0]).toBe('2026-02-01');
    expect(monthGrid('2026-02-10', 0, true)).toHaveLength(6);
  });

  it('reads today and dates in a time zone without depending on the runtime zone', () => {
    const now = new Date('2026-11-04T20:00:00Z');
    expect(todayIso({ now, timeZone: 'UTC' })).toBe('2026-11-04');
    expect(todayIso({ now, timeZone: 'Asia/Kolkata' })).toBe('2026-11-05');
    expect(dateToIso(now, 'America/Los_Angeles')).toBe('2026-11-04');
    expect(todayIso()).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });

  it('handles date-time strings', () => {
    expect(parseIsoDateTime('2026-11-04T14:30')).toEqual({ date: '2026-11-04', minutes: 870 });
    expect(parseIsoDateTime('2026-11-04T14:30:00')).toEqual({ date: '2026-11-04', minutes: 870 });
    expect(parseIsoDateTime('2026-11-04T24:00')).toBeNull();
    expect(parseIsoDateTime('2026-02-30T10:00')).toBeNull();
    expect(toIsoDateTime('2026-11-04', 545)).toBe('2026-11-04T09:05');
    expect(minutesToIsoTime(-15)).toBe('23:45');
  });

  it('throws on malformed input instead of producing a wrong date', () => {
    expect(() => addDays('04/11/2026', 1)).toThrow(RangeError);
  });
});

describe('locale formatting', () => {
  it('formats calendar dates in UTC so they never shift', () => {
    // Punctuation varies between ICU versions; the parts and their order do not.
    expect(formatFullDate('2026-11-04', 'en-GB')).toMatch(/^Wednesday,? 4 November 2026$/);
    expect(formatFullDate('2026-11-04', 'en-US')).toMatch(/^Wednesday, November 4, 2026$/);
    expect(formatMonthTitle('2026-11-04', 'de-DE')).toBe('November 2026');
    expect(formatNumericDate('2026-11-04', 'en-GB')).toBe('04/11/2026');
    expect(formatNumericDate('2026-11-04', 'en-US')).toBe('11/04/2026');
    expect(formatNumericDate('2026-11-04', 'de-DE')).toBe('04.11.2026');
  });

  it('knows the field order and a typing hint', () => {
    expect(dateFieldOrder('en-US')).toEqual(['month', 'day', 'year']);
    expect(dateFieldOrder('en-GB')).toEqual(['day', 'month', 'year']);
    expect(dateFieldOrder('ja-JP')).toEqual(['year', 'month', 'day']);
    expect(dateFormatHint('en-US')).toBe('MM/DD/YYYY');
    expect(dateFormatHint('de-DE')).toBe('DD.MM.YYYY');
    expect(dateFormatHint('fr-FR', { day: 'JJ', month: 'MM', year: 'AAAA' })).toBe('JJ/MM/AAAA');
  });

  it('knows the first day of the week per locale', () => {
    expect(localeWeekStart('en-US')).toBe(0);
    expect(localeWeekStart('en-GB')).toBe(1);
    expect(localeWeekStart('de-DE')).toBe(1);
    expect(localeWeekStart('en-IN')).toBe(0);
    expect(localeWeekStart('ar-EG')).toBe(6);
    expect(localeWeekStart('not a locale')).toBe(1);
  });

  it('names weekdays from the week start', () => {
    expect(weekdayNames('en-GB', 1)).toEqual(['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']);
    expect(weekdayNames('en-US', 0, 'narrow')[0]).toBe('S');
  });

  it('formats times in 12 or 24 hours', () => {
    expect(localeHourCycle('en-US')).toBe('12');
    expect(localeHourCycle('en-GB')).toBe('24');
    expect(formatTime(870, 'en-US')).toBe('2:30 PM');
    expect(formatTime(870, 'en-GB')).toBe('14:30');
    expect(formatTime(0, 'en-US', '12')).toBe('12:00 AM');
    expect(formatTime(5, 'en-US', '24')).toBe('00:05');
  });
});

describe('parseDateInput', () => {
  const gb = { locale: 'en-GB', referenceDate: '2026-11-04' };
  const us = { locale: 'en-US', referenceDate: '2026-11-04' };

  it('reads numeric dates in the locale order', () => {
    expect(parseDateInput('04/11/2026', gb)).toBe('2026-11-04');
    expect(parseDateInput('11/04/2026', us)).toBe('2026-11-04');
    expect(parseDateInput('4.11.2026', { locale: 'de-DE' })).toBe('2026-11-04');
    expect(parseDateInput('2026/11/04', { locale: 'ja-JP' })).toBe('2026-11-04');
    expect(parseDateInput(' 4-11-2026 ', gb)).toBe('2026-11-04');
  });

  it('accepts ISO and a leading four-digit year in every locale', () => {
    expect(parseDateInput('2026-11-04', us)).toBe('2026-11-04');
    expect(parseDateInput('2026/11/04', us)).toBe('2026-11-04');
  });

  it('reads month names and ignores weekday names', () => {
    expect(parseDateInput('4 Nov 2026', gb)).toBe('2026-11-04');
    expect(parseDateInput('November 4, 2026', us)).toBe('2026-11-04');
    expect(parseDateInput('Wed, 4 November 2026', gb)).toBe('2026-11-04');
    expect(parseDateInput('4 nov', gb)).toBe('2026-11-04');
    expect(parseDateInput('4. März 2026', { locale: 'de-DE' })).toBe('2026-03-04');
    expect(parseDateInput('4 marz 2026', { locale: 'de-DE' })).toBe('2026-03-04');
  });

  it('fills a missing year and expands two-digit years near the reference', () => {
    expect(parseDateInput('4/11', gb)).toBe('2026-11-04');
    expect(parseDateInput('04/11/26', gb)).toBe('2026-11-04');
    expect(parseDateInput('04/11/80', gb)).toBe('1980-11-04');
    expect(parseDateInput('04/11/75', gb)).toBe('2075-11-04');
  });

  it('reads digits typed without separators', () => {
    expect(parseDateInput('04112026', gb)).toBe('2026-11-04');
    expect(parseDateInput('110426', us)).toBe('2026-11-04');
    expect(parseDateInput('0411', gb)).toBe('2026-11-04');
  });

  it('reads native digits', () => {
    expect(parseDateInput('٠٤/١١/٢٠٢٦', { locale: 'ar-EG', referenceDate: '2026-01-01' })).toBe(
      '2026-11-04',
    );
  });

  it('rejects impossible or ambiguous text', () => {
    expect(parseDateInput('', gb)).toBeNull();
    expect(parseDateInput('31/02/2026', gb)).toBeNull();
    expect(parseDateInput('13/13/2026', gb)).toBeNull();
    expect(parseDateInput('soon', gb)).toBeNull();
    expect(parseDateInput('4 Nov Dec', gb)).toBeNull();
    expect(parseDateInput('1/2/3/4', gb)).toBeNull();
    expect(parseDateInput('04/11/202', gb)).toBeNull();
  });
});

describe('parseTimeInput', () => {
  it('reads 24-hour and 12-hour times', () => {
    expect(parseTimeInput('14:30')).toBe(870);
    expect(parseTimeInput('2:30 pm')).toBe(870);
    expect(parseTimeInput('2:30 P.M.')).toBe(870);
    expect(parseTimeInput('2pm')).toBe(840);
    expect(parseTimeInput('12am')).toBe(0);
    expect(parseTimeInput('12 pm')).toBe(720);
    expect(parseTimeInput('1430')).toBe(870);
    expect(parseTimeInput('9.15')).toBe(555);
    expect(parseTimeInput('09:00:00')).toBe(540);
    expect(parseTimeInput('9h30')).toBe(570);
  });

  it('rejects invalid times', () => {
    expect(parseTimeInput('')).toBeNull();
    expect(parseTimeInput('24:00')).toBeNull();
    expect(parseTimeInput('10:60')).toBeNull();
    expect(parseTimeInput('13pm')).toBeNull();
    expect(parseTimeInput('noonish')).toBeNull();
  });
});
