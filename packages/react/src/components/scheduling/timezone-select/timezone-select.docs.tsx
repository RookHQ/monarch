import { useState } from 'react';
import { Field, FieldDescription, FieldLabel } from '@rookcom/monarch-react';
import { TimezoneSelect } from '@rookcom/monarch-react/scheduling';
import type { ComponentDocs } from '../../../internal/component-docs.js';

// A fixed instant so the examples (and their screenshots) show the same offsets all year.
const NOW = Date.UTC(2026, 0, 15, 12);

function WorkspaceZone() {
  const [zone, setZone] = useState<string | null>('Asia/Kolkata');
  return (
    <Field style={{ width: '100%', maxWidth: 320 }}>
      <FieldLabel>Workspace time zone</FieldLabel>
      <TimezoneSelect
        value={zone}
        onValueChange={setZone}
        now={NOW}
        detectedZone="Asia/Kolkata"
        pinned={['Europe/London', 'America/New_York']}
        placeholder="Search city, country or offset"
      />
      <FieldDescription>Reports and reminders use this zone.</FieldDescription>
    </Field>
  );
}

const COMMON = [
  'UTC',
  'America/Los_Angeles',
  'America/Chicago',
  'America/New_York',
  'Europe/London',
  'Europe/Berlin',
  'Asia/Dubai',
  'Asia/Kolkata',
  'Asia/Singapore',
  'Asia/Tokyo',
  'Australia/Sydney',
];

export default {
  id: 'timezone-select',
  title: 'TimezoneSelect',
  summary:
    'Searchable IANA time zone picker labelled by UTC offset and city, with detected and pinned zones.',
  sections: {
    purpose: {
      summary:
        'Chooses an IANA time zone from every zone the runtime knows (about 420), labelled "(UTC+05:30) Kolkata" and sorted by offset, with search by city, country, offset or abbreviation. Replaces native selects and hard-coded lists of 15 zones. Import from `@rookcom/monarch-react/scheduling`.',
      useWhen: [
        'A person, workspace, schedule or booking page needs a time zone.',
        'A short curated list is enough: pass `zones`.',
      ],
      avoidWhen: [
        'Showing a time in a zone: format it with Intl instead.',
        'Picking a UTC offset rather than a place: offsets change with daylight saving time.',
      ],
    },
    designPrinciples: {
      items: [
        'Zones are places, not offsets: the value is the IANA id, and the label shows the offset for `now` (DST-aware).',
        'The likely answer is first: the device’s zone under "Detected", then the product’s "Pinned" and "Recent" zones.',
        'Same render on server and client: detection runs after mount, and `now` can be passed.',
        'Legacy ids that browsers still report (Asia/Calcutta) select their current name (Asia/Kolkata).',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Built on Combobox and accepts its input props (size, placeholder, invalid, disabled, limit, …). Forwards its ref to the input.',
        'Helpers: `getTimezoneInfo(zone, { now, locale })`, `listTimezones()`, `formatUtcOffset(minutes)`, `canonicalTimezone(zone)`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Without `Intl.supportedValuesOf` (older browsers and Node) the list falls back to about 80 common zones; a value outside the list is still shown.',
      ],
    },
    variants: {
      items: [{ prop: 'size', values: ['sm', 'md', 'lg'], description: 'Height, as Combobox.' }],
    },
    states: {
      items: [
        {
          name: 'Sections',
          attribute: 'role="group"',
          description:
            '"Detected", "Pinned", "Recent" and "All time zones" while nothing is typed.',
        },
        { name: 'Searching', description: 'A flat list of matches in offset order.' },
        { name: 'Empty', attribute: 'role="status"', description: '"No time zones match".' },
        { name: 'Selected / invalid / disabled', description: 'As Combobox.' },
      ],
    },
    interaction: {
      items: [
        'Typing filters by every word: city, IANA id, long name ("India Standard Time"), offset (`+5:30`, `utc-8`, `+0530`) or country and abbreviation for common zones ("india", "us pacific", "bst").',
        'The full list is built only when the field is first focused or opened, so pages with many fields stay fast.',
        'A zone listed under "Detected" or "Pinned" also appears in its place in "All time zones".',
      ],
    },
    keyboard: {
      items: [
        { keys: 'ArrowDown / ArrowUp', action: 'Opens the list, then moves.' },
        {
          keys: 'PageDown / PageUp, Home / End',
          action: 'Moves ten zones, or to the first or last.',
        },
        { keys: 'Enter', action: 'Selects the active zone.' },
        { keys: 'Escape', action: 'Closes and restores the selected label.' },
      ],
    },
    accessibility: {
      role: 'combobox, controlling a listbox',
      name: 'A FieldLabel or `aria-label`.',
      items: [
        'Each option reads its offset, city, IANA id and long name.',
        'Sections are labelled groups.',
        'Inherits Combobox’s APG keyboard and `aria-activedescendant` support.',
      ],
    },
    responsive: {
      items: ['The list matches the field width (at least 12rem); long descriptions wrap.'],
    },
    theme: {
      items: ['Combobox tokens; the input uses tabular numbers so offsets line up.'],
    },
    examples: {
      items: [
        {
          title: 'Detected and pinned zones',
          stack: true,
          render: () => <WorkspaceZone />,
        },
        {
          title: 'Curated list, small',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 260 }}>
              <TimezoneSelect
                aria-label="Sending time zone"
                size="sm"
                zones={COMMON}
                now={NOW}
                defaultValue="Europe/Berlin"
                detectedZone={null}
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Store the IANA id the picker returns, not the offset.',
        'Pin the zones the workspace already uses (offices, teammates).',
        'Pass `now` when the page is server rendered and must match exactly.',
      ],
      dont: [
        'Do not hard-code a short list of zones unless the product truly only supports those.',
        'Do not show offsets without a place; "UTC+01:00" is London in summer and Lagos all year.',
      ],
    },
    related: {
      items: [
        { id: 'combobox', reason: 'The searchable select it is built on.' },
        { id: 'select', reason: 'For a handful of fixed choices.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['combobox'],
      notes: [
        'Uses only `Intl`; no time zone data is bundled beyond the fallback id list and search aliases.',
      ],
    },
    testing: {
      unit: [
        'Offsets formatted; DST-aware offsets, city and long name for an instant; unknown zones fall back.',
        'Static list when `Intl.supportedValuesOf` is missing; legacy ids canonicalised.',
        'Detected, Pinned, Recent and All sections in offset order; device detection after mount.',
        'Search by country, offset and "us pacific"; selects; empty message.',
        'A value outside the list, a legacy value and a custom label.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/timezone-select',
        'Cross-browser axe: /components/timezone-select',
      ],
    },
  },
} satisfies ComponentDocs;
