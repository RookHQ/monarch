import { RelativeTime } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

// A fixed reference time keeps the examples (and their visual baselines) stable.
const NOW = Date.UTC(2026, 10, 3, 12, 0, 0);
const MIN = 60_000;
const HOUR = 60 * MIN;
const DAY = 24 * HOUR;

export default {
  id: 'relative-time',
  title: 'RelativeTime',
  summary:
    'A localised "3 hours ago" in a <time> element, with the absolute time on hover and focus.',
  sections: {
    purpose: {
      summary:
        'Shows when something happened (or will happen) relative to now, in the reader’s language, while keeping the exact moment one hover or Tab away and machine-readable in `datetime`.',
      useWhen: [
        'Activity feeds, inbox rows, comments and “last synced” labels where recency matters more than the exact time.',
        'A status that should keep itself current (`live`), such as “Updated 2 minutes ago”.',
      ],
      avoidWhen: [
        'The exact date is what the reader needs (invoices, contracts, due dates): format the date itself.',
        'A countdown that must be precise to the second: build it on a timer and an `aria-live` policy of your own.',
      ],
    },
    designPrinciples: {
      items: [
        'The exact time is never only in a `title`: the tooltip opens on hover and on keyboard focus and is linked with `aria-describedby`.',
        'Localised by `Intl.RelativeTimeFormat` and `Intl.DateTimeFormat`; no hand-written “ago” strings.',
        'Server rendering is deterministic when you pass `now`; without it, the client corrects the text after hydration instead of warning.',
        'Live updates are quiet: the text changes without a live region, so screen readers are not interrupted every minute.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native attributes go to the `<time>` element; `className` goes to the `mn-relative-time` wrapper that also holds the tooltip.',
        '`formatRelativeTime(date, options)` and `relativeTimeParts(date, now)` are exported for places that need the string, such as table exports or `aria-label`s.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: ['`date` and `now` accept a Date, epoch milliseconds or an ISO 8601 string.'],
    },
    variants: {
      items: [
        {
          prop: 'format',
          values: ['long', 'short', 'narrow'],
          description:
            '“3 minutes ago”, “3 min. ago”, “3m ago” (exact text depends on the locale).',
        },
        {
          prop: 'numeric',
          values: ['auto', 'always'],
          description: '`auto` writes “yesterday” and “now”; `always` writes “1 day ago”.',
        },
        {
          prop: 'tooltip',
          values: ['true', 'false'],
          description: 'Absolute time on hover and focus (a tab stop), or plain text.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Relative', description: 'Default: the largest unit that fits, after rounding.' },
        {
          name: 'Absolute',
          description:
            'Past `absoluteAfter` milliseconds the absolute date is shown and the tooltip is dropped.',
        },
        {
          name: 'Tooltip dismissed',
          attribute: 'data-dismissed',
          description: 'Set by Escape while focused; cleared on blur or when the pointer leaves.',
        },
        {
          name: 'Invalid date',
          attribute: 'data-invalid',
          description: 'An unparseable `date` renders an em dash and no `datetime`.',
        },
      ],
    },
    interaction: {
      items: [
        'Hovering the text or the tooltip keeps the tooltip open; the pointer can move onto it (WCAG 1.4.13).',
        'With `live`, the text re-renders every 5 s for the first minute, then every 30 s, 5 min and 1 h as it ages; a number sets a fixed interval.',
        'Passing `now` freezes the reference time, which turns `live` off.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Focuses the timestamp and shows the absolute time (when `tooltip`).',
        },
        { keys: 'Escape', action: 'Hides the tooltip without moving focus.' },
      ],
    },
    accessibility: {
      role: 'time (with a role="tooltip" description)',
      name: 'The relative text; the absolute time is its description through `aria-describedby`.',
      items: [
        '`datetime` carries the ISO 8601 instant for machines and assistive technology.',
        'The tooltip shows on `:focus-visible`, not only on hover, and can be dismissed with Escape.',
        'Use `tooltip={false}` in dense lists to avoid a tab stop per row when the exact time is available elsewhere.',
      ],
    },
    responsive: {
      items: [
        'The text does not wrap. The tooltip is at most 18rem wide and never wider than the viewport minus 2rem.',
        'The tooltip is positioned with CSS above the text; inside a container that clips overflow it can be cut off, so give such containers room or use `tooltip={false}`.',
      ],
    },
    theme: {
      items: [
        'The tooltip uses the inverse surface tokens, like Tooltip, so it reads in light, dark and more-contrast themes.',
        'The dotted underline uses the current text colour at reduced opacity.',
        'The tooltip fade is removed under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Relative and absolute',
          stack: true,
          render: () => (
            <>
              <div>
                Edited <RelativeTime date={NOW - 3 * MIN} now={NOW} locale="en-GB" timeZone="UTC" />
              </div>
              <div>
                Synced{' '}
                <RelativeTime date={NOW - 5 * HOUR} now={NOW} locale="en-GB" timeZone="UTC" />
              </div>
              <div>
                Renews <RelativeTime date={NOW + 2 * DAY} now={NOW} locale="en-GB" timeZone="UTC" />
              </div>
              <div>
                Created{' '}
                <RelativeTime
                  date={NOW - 40 * DAY}
                  now={NOW}
                  locale="en-GB"
                  timeZone="UTC"
                  absoluteAfter={7 * DAY}
                  absoluteFormat={{ dateStyle: 'medium' }}
                />
              </div>
            </>
          ),
          code: `<RelativeTime date={comment.createdAt} />
<RelativeTime date={invoice.renewsAt} locale="en-GB" timeZone="Europe/London" />
<RelativeTime date={doc.createdAt} absoluteAfter={7 * 86_400_000}
  absoluteFormat={{ dateStyle: 'medium' }} />`,
        },
        {
          title: 'Short, narrow and live',
          stack: true,
          render: () => (
            <>
              <div>
                <RelativeTime
                  date={NOW - 3 * MIN}
                  now={NOW}
                  locale="en"
                  format="short"
                  tooltip={false}
                />
              </div>
              <div>
                <RelativeTime
                  date={NOW - 3 * MIN}
                  now={NOW}
                  locale="en"
                  format="narrow"
                  tooltip={false}
                />
              </div>
              <div>
                <RelativeTime
                  date={NOW - DAY}
                  now={NOW}
                  locale="en"
                  numeric="always"
                  tooltip={false}
                />
              </div>
            </>
          ),
          code: `<RelativeTime date={row.updatedAt} format="short" tooltip={false} />
<RelativeTime date={row.updatedAt} format="narrow" tooltip={false} />
<RelativeTime date={lastSync} live />   {/* keeps itself current */}`,
        },
        {
          title: 'Server rendering',
          render: () => (
            <div>
              Localised: <RelativeTime date={NOW - 3 * HOUR} now={NOW} locale="de" timeZone="UTC" />
            </div>
          ),
          code: `// Server component: pass the same "now" to the client for identical HTML.
const now = Date.now();
<RelativeTime date={post.publishedAt} now={now} locale={locale} timeZone={user.timeZone} />`,
        },
      ],
    },
    doDont: {
      do: [
        'Pass the viewer’s `timeZone` when you know it, so the tooltip shows their local time.',
        'Use `absoluteAfter` for old items, where “5 months ago” is less useful than the date.',
        'Pass `now` when rendering on the server if the HTML must be identical to the hydrated result.',
      ],
      dont: [
        'Do not put essential information only in the tooltip.',
        'Do not wrap a live RelativeTime in an `aria-live` region; it would announce every tick.',
        'Do not build “x ago” strings by hand next to it; use `formatRelativeTime`.',
      ],
    },
    related: {
      items: [
        { id: 'tooltip', reason: 'Rich supplementary content on hover and focus.' },
        { id: 'activity-timeline', reason: 'Planned feed that shows a RelativeTime per event.' },
        { id: 'description-list', reason: 'Label and value pairs such as “Last updated”.' },
        { id: 'date-picker', reason: 'Planned input for choosing a date rather than showing one.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Unit selection rounds before rolling over (59.6 min reads “1 hour ago”).',
        'Formats with `Intl.RelativeTimeFormat`, including `numeric`, `format` and other locales.',
        'Renders `<time datetime>` and an absolute tooltip linked by `aria-describedby`.',
        'Accepts ISO strings, epoch numbers and Date objects.',
        'Is a tab stop; Escape dismisses the tooltip and blur resets it.',
        '`tooltip={false}`, `absoluteAfter`, `timeZone` and invalid dates.',
        'Live updates tick with fake timers; no ticking without `live`.',
        'Forwards the ref and calls consumer handlers.',
        'Server rendering with `now`, and hydration with a different client clock without errors.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/relative-time'],
    },
  },
} satisfies ComponentDocs;
