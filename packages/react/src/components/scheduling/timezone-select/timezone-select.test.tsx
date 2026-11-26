import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel } from '../../forms/form-field/index.js';
import {
  TimezoneSelect,
  canonicalTimezone,
  formatUtcOffset,
  getTimezoneInfo,
  listTimezones,
} from './index.js';

/** The option's label, without its description. */
const labelOf = (o: Element) => o.querySelector('.mn-menu__text')?.firstChild?.textContent;

// A January and a July instant, to see daylight saving time.
const JAN = Date.UTC(2026, 0, 15, 12);
const JUL = Date.UTC(2026, 6, 15, 12);
const ZONES = [
  'UTC',
  'Asia/Kolkata',
  'America/New_York',
  'Europe/London',
  'Asia/Kathmandu',
  'America/Los_Angeles',
];

describe('timezone helpers', () => {
  it('formats offsets', () => {
    expect(formatUtcOffset(330)).toBe('UTC+05:30');
    expect(formatUtcOffset(-480)).toBe('UTC-08:00');
    expect(formatUtcOffset(0)).toBe('UTC+00:00');
    expect(formatUtcOffset(345)).toBe('UTC+05:45');
  });

  it('computes DST-aware offsets, city and long name for an instant', () => {
    expect(getTimezoneInfo('America/New_York', { now: JAN }).offsetMinutes).toBe(-300);
    expect(getTimezoneInfo('America/New_York', { now: JUL }).offsetMinutes).toBe(-240);
    const kolkata = getTimezoneInfo('Asia/Kolkata', { now: JUL });
    expect(kolkata).toMatchObject({ city: 'Kolkata', region: 'Asia', offsetLabel: 'UTC+05:30' });
    expect(kolkata.longName).toBe('India Standard Time');
    expect(getTimezoneInfo('America/Argentina/Buenos_Aires', { now: JUL }).city).toBe(
      'Buenos Aires',
    );
  });

  it('falls back for unknown zones and lists zones with UTC', () => {
    expect(getTimezoneInfo('Mars/Olympus', { now: JUL })).toMatchObject({
      offsetMinutes: 0,
      city: 'Olympus',
    });
    const zones = listTimezones();
    expect(zones).toContain('UTC');
    expect(zones).toContain('Asia/Kolkata');
  });

  it('uses the static list when Intl.supportedValuesOf is missing', () => {
    const original = (Intl as { supportedValuesOf?: unknown }).supportedValuesOf;
    (Intl as { supportedValuesOf?: unknown }).supportedValuesOf = undefined;
    try {
      const zones = listTimezones();
      expect(zones.length).toBeGreaterThan(60);
      expect(zones[0]).toBe('UTC');
    } finally {
      (Intl as { supportedValuesOf?: unknown }).supportedValuesOf = original;
    }
  });
});

describe('TimezoneSelect', () => {
  it('labels by offset and city, sorted by offset, with detected and pinned sections', async () => {
    render(
      <Field>
        <FieldLabel>Time zone</FieldLabel>
        <TimezoneSelect
          zones={ZONES}
          now={JUL}
          detectedZone="Asia/Kolkata"
          pinned={['Europe/London']}
        />
      </Field>,
    );
    const input = screen.getByRole('combobox', { name: 'Time zone' });
    const user = userEvent.setup();
    await user.click(input);
    await user.keyboard('{ArrowDown}');
    const groups = await screen.findAllByRole('group');
    expect(groups.map((g) => g.firstChild?.textContent)).toEqual([
      'Detected',
      'Pinned',
      'All time zones',
    ]);
    expect(within(groups[0]!).getByRole('option')).toHaveTextContent('(UTC+05:30) Kolkata');
    const all = within(groups[2]!)
      .getAllByRole('option')
      .map((o) => labelOf(o));
    expect(all).toEqual([
      '(UTC-07:00) Los Angeles',
      '(UTC-04:00) New York',
      '(UTC+00:00) UTC',
      '(UTC+01:00) London',
      '(UTC+05:30) Kolkata',
      '(UTC+05:45) Kathmandu',
    ]);
    await expectNoA11yViolations();
  });

  it('searches by city, offset, abbreviation or country, without sections', async () => {
    const onValueChange = vi.fn();
    render(
      <TimezoneSelect
        aria-label="Time zone"
        zones={ZONES}
        now={JUL}
        detectedZone={null}
        onValueChange={onValueChange}
      />,
    );
    const input = screen.getByRole('combobox');
    const user = userEvent.setup();
    await user.type(input, 'india');
    expect(screen.getAllByRole('option').map((o) => labelOf(o))).toEqual(['(UTC+05:30) Kolkata']);
    await user.clear(input);
    await user.type(input, '+5:45');
    expect(screen.getAllByRole('option').map((o) => labelOf(o))).toEqual(['(UTC+05:45) Kathmandu']);
    await user.clear(input);
    await user.type(input, 'us pacific');
    expect(screen.queryAllByRole('group')).toHaveLength(0);
    await user.keyboard('{Enter}');
    expect(onValueChange).toHaveBeenCalledWith('America/Los_Angeles');
    expect(input).toHaveValue('(UTC-07:00) Los Angeles');
    await user.clear(input);
    await user.type(input, 'nowhere');
    expect(screen.getByRole('status')).toHaveTextContent('No time zones match');
  });

  it('shows a value that is not in the list and a description with the IANA id', async () => {
    render(
      <TimezoneSelect
        aria-label="Time zone"
        zones={['UTC']}
        now={JAN}
        value="Asia/Tokyo"
        detectedZone={null}
      />,
    );
    const input = screen.getByRole('combobox');
    expect(input).toHaveValue('(UTC+09:00) Tokyo');
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Show options' }));
    const option = await screen.findByRole('option', { name: /Tokyo/ });
    expect(option).toHaveAttribute('aria-selected', 'true');
    expect(option).toHaveTextContent('Asia/Tokyo · Japan Standard Time');
  });

  it('selects the current entry for a legacy id', () => {
    render(<TimezoneSelect aria-label="Time zone" zones={ZONES} now={JAN} value="Asia/Calcutta" />);
    expect(screen.getByRole('combobox')).toHaveValue('(UTC+05:30) Kolkata');
    expect(canonicalTimezone('Europe/Kiev')).toBe('Europe/Kyiv');
    expect(canonicalTimezone('Etc/UTC')).toBe('UTC');
    expect(listTimezones()).not.toContain('Asia/Calcutta');
  });

  it('detects the device zone after mount and lists recent zones', async () => {
    render(
      <TimezoneSelect
        aria-label="Time zone"
        zones={ZONES}
        now={JUL}
        recent={['America/New_York']}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Show options' }));
    const device = Intl.DateTimeFormat().resolvedOptions().timeZone;
    await waitFor(() =>
      expect(screen.getAllByRole('group')[0]?.firstChild?.textContent).toBe('Detected'),
    );
    expect(within(screen.getAllByRole('group')[0]!).getByRole('option').textContent).toContain(
      canonicalTimezone(device),
    );
    expect(screen.getAllByRole('group').map((g) => g.firstChild?.textContent)).toContain('Recent');
  });

  it('uses a custom label format', async () => {
    render(
      <TimezoneSelect
        aria-label="Time zone"
        zones={ZONES}
        now={JUL}
        defaultValue="Europe/London"
        formatLabel={(z) => `${z.city} — ${z.offsetLabel}`}
      />,
    );
    expect(screen.getByRole('combobox')).toHaveValue('London — UTC+01:00');
  });

  it('renders on the server with a fixed now', () => {
    const html = renderToString(
      <TimezoneSelect aria-label="Time zone" now={JAN} defaultValue="America/New_York" />,
    );
    expect(html).toContain('mn-timezone-select');
    expect(html).toContain('(UTC-05:00) New York');
  });
});
