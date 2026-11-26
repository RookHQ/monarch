import { describe, expect, it } from 'vitest';
import {
  dateToIso,
  defaultZone,
  formatInstant,
  formatInstantTime,
  formatZonedTime,
  instantDay,
  instantToWallTime,
  safeZone,
  sameInstant,
  wallTimeToInstant,
  zonedParts,
  zonedTimeToDate,
  zoneName,
  zoneOffsetMinutes,
} from './index.js';

describe('zone helpers', () => {
  it('reads the day and wall-clock minutes of an instant in a zone', () => {
    expect(zonedParts('2026-11-04T20:00:00Z', 'Asia/Kolkata')).toEqual({
      day: '2026-11-05',
      minutes: 90,
    });
    expect(zonedParts(new Date('2026-11-04T03:00:00Z'), 'America/New_York')).toEqual({
      day: '2026-11-03',
      minutes: 22 * 60,
    });
    expect(zonedParts(Date.UTC(2026, 0, 1, 0, 0), 'UTC')).toEqual({
      day: '2026-01-01',
      minutes: 0,
    });
    expect(instantDay('2026-11-04T20:00:00Z', 'Asia/Kolkata')).toBe('2026-11-05');
  });

  it('falls back to UTC for an unknown zone instead of throwing', () => {
    expect(safeZone('Not/AZone')).toBe('UTC');
    expect(safeZone('Europe/London')).toBe('Europe/London');
    expect(zonedParts('2026-11-04T09:30:00Z', 'Not/AZone')).toEqual({
      day: '2026-11-04',
      minutes: 570,
    });
    expect(wallTimeToInstant('2026-11-04T09:30', 'Not/AZone')).toBe('2026-11-04T09:30:00.000Z');
    expect(typeof defaultZone()).toBe('string');
  });

  it('computes DST-correct offsets', () => {
    expect(zoneOffsetMinutes('Asia/Kolkata', new Date('2026-11-04T00:00:00Z'))).toBe(330);
    expect(zoneOffsetMinutes('Europe/London', new Date('2026-07-01T12:00:00Z'))).toBe(60);
    expect(zoneOffsetMinutes('Europe/London', new Date('2026-12-01T12:00:00Z'))).toBe(0);
  });

  it('converts wall-clock time to an instant and back', () => {
    expect(wallTimeToInstant('2026-11-04T15:00', 'Asia/Kolkata')).toBe('2026-11-04T09:30:00.000Z');
    expect(wallTimeToInstant('2026-07-01T09:00', 'Europe/London')).toBe('2026-07-01T08:00:00.000Z');
    expect(wallTimeToInstant('not a time', 'UTC')).toBeNull();
    expect(zonedTimeToDate('2026-11-04', 900, 'Asia/Kolkata').toISOString()).toBe(
      '2026-11-04T09:30:00.000Z',
    );
    expect(instantToWallTime('2026-11-04T09:30:00Z', 'Asia/Kolkata')).toBe('2026-11-04T15:00');
    expect(instantToWallTime('garbage', 'UTC')).toBeNull();
  });

  it('moves a time in a spring-forward gap forward west of UTC as well', () => {
    // US clocks jump from 02:00 to 03:00 on 2026-03-08; 02:30 does not exist.
    expect(wallTimeToInstant('2026-03-08T02:30', 'America/New_York')).toBe(
      '2026-03-08T07:30:00.000Z',
    );
    expect(instantToWallTime('2026-03-08T07:30:00.000Z', 'America/New_York')).toBe(
      '2026-03-08T03:30',
    );
    // Either side of the gap is unchanged.
    expect(wallTimeToInstant('2026-03-08T01:30', 'America/New_York')).toBe(
      '2026-03-08T06:30:00.000Z',
    );
    expect(wallTimeToInstant('2026-03-08T03:00', 'America/New_York')).toBe(
      '2026-03-08T07:00:00.000Z',
    );
  });

  it('never throws on an unreadable instant or an unknown zone', () => {
    expect(zonedParts('garbage', 'UTC')).toEqual({ day: '1970-01-01', minutes: 0 });
    expect(formatZonedTime('garbage', 'UTC', 'en-GB')).toBe('garbage');
    expect(dateToIso(new Date('2026-11-04T20:00:00Z'), 'Not/AZone')).toBe('2026-11-04');
    expect(dateToIso(new Date(Number.NaN), 'Europe/London')).toBe('1970-01-01');
  });

  it('pushes a time in a spring-forward gap forward and keeps the fall-back hour', () => {
    // Europe/London jumps from 01:00 to 02:00 on 2026-03-29.
    expect(wallTimeToInstant('2026-03-29T01:30', 'Europe/London')).toBe('2026-03-29T01:30:00.000Z');
    expect(instantToWallTime('2026-03-29T01:30:00.000Z', 'Europe/London')).toBe('2026-03-29T02:30');
    // US clocks go back on 2026-11-01; the round trip still lands on 09:00.
    const nine = wallTimeToInstant('2026-11-01T09:00', 'America/New_York');
    expect(nine).toBe('2026-11-01T14:00:00.000Z');
    expect(instantToWallTime(nine ?? '', 'America/New_York')).toBe('2026-11-01T09:00');
  });

  it('compares instants written with different offsets', () => {
    expect(sameInstant('2026-11-04T15:00:00+05:30', '2026-11-04T09:30:00Z')).toBe(true);
    expect(sameInstant('2026-11-04T15:00:00Z', '2026-11-04T09:30:00Z')).toBe(false);
  });

  it('formats instants in a zone', () => {
    expect(formatInstantTime('2026-11-04T09:30:00Z', 'Asia/Kolkata', 'en-US', '12')).toBe(
      '3:00 PM',
    );
    expect(formatInstantTime('2026-11-04T09:30:00Z', 'Asia/Kolkata', 'en-GB', '24')).toBe('15:00');
    expect(formatZonedTime('2026-11-04T09:30:00Z', 'Asia/Kolkata', 'en-GB', 'h23')).toBe('15:00');
    expect(formatZonedTime('2026-11-04T09:30:00Z', 'UTC', 'en-US', 'h12')).toBe('9:30 AM');
    expect(formatZonedTime('2026-11-04T09:30:00Z', 'Not/AZone', 'en-GB', 'h23')).toBe('09:30');
    const full = formatInstant('2026-11-04T09:30:00Z', 'Asia/Kolkata', 'en-GB', '24');
    expect(full).toContain('2026');
    expect(full).toContain('15:00');
    expect(formatInstant('garbage', 'UTC')).toBe('garbage');
    expect(zoneName('Asia/Kolkata', new Date('2026-11-04T00:00:00Z'), 'en-US')).toBe(
      'India Standard Time (Asia/Kolkata)',
    );
  });
});
