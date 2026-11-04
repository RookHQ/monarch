import { EventChip } from '@rookcom/monarch-react/scheduling';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'event-chip',
  title: 'EventChip',
  summary: 'One event in a calendar view: colour, title, time and status, as a button or link.',
  sections: {
    purpose: {
      summary:
        'Shows one event the same way in every calendar view (month cell, agenda row, time grid block) and opens it. Needed by: calendar.',
      useWhen: [
        'An event must appear inside CalendarMonthView, AgendaList or TimeGrid; those views render it for you.',
        'A custom view (a list of upcoming bookings, a day summary in a card) needs events that look and read like the calendar’s.',
      ],
      avoidWhen: [
        'The item is not a time-bound event: use Badge or StatusBadge for labels and states.',
        'You need a full event card with actions: put those in the popover or page the chip opens.',
      ],
    },
    designPrinciples: {
      items: [
        'The product colour is only a marker, a bar and a light tint; text stays on Monarch text tokens, so any calendar colour stays readable in every theme.',
        'Status is never colour alone: tentative is hollow or dashed, cancelled and declined are muted and struck through, and the status word is part of the accessible name (and visible text in `bar` and `block`).',
        'The accessible name always reads "title, time, status", even when the visible time is abbreviated (`timeLabel`).',
        'One element, one action: a button by default, a link with `href`, or your router link with `asChild`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<button type="button">` (ref to it), an `<a>` when `href` is set, or the child element with `asChild`.',
        'With plain-text `title`, `time`, `timeLabel` and `detail`, the chip gets a computed `aria-label`; with rich content the name comes from the content instead. Pass `aria-label` to override.',
        '`CalendarEvent` is the data shape the calendar views take: `{ id, title, start, end?, allDay?, color?, status?, detail?, href? }`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['dot', 'bar', 'block'],
          description:
            '`dot` (default): one line, colour dot, start time, title (month cells, popovers). `bar`: tinted row with an accent bar, wraps a detail line (agenda, all-day row). `block`: fills its container, stacked lines (time grid).',
        },
        {
          prop: 'status',
          values: ['confirmed', 'tentative', 'cancelled', 'declined'],
          description: 'Exposed as `data-status`; see States.',
        },
        {
          prop: 'density (block)',
          values: ['regular', 'compact'],
          description: '`compact` puts a short block on one line: start time, then title.',
        },
        {
          prop: 'past',
          values: ['true', 'false'],
          description: 'The event has ended: muted text and a faded marker (`data-past`).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Confirmed',
          attribute: 'data-status="confirmed"',
          description: 'Solid marker or bar and tint.',
        },
        {
          name: 'Tentative',
          attribute: 'data-status="tentative"',
          description:
            'Hollow dot; `bar`/`block` get a lighter tint, an outline and a dashed bar. "Tentative" is in the name.',
        },
        {
          name: 'Cancelled / declined',
          attribute: 'data-status="cancelled" | "declined"',
          description:
            'Muted text, struck-through title, grey marker and surface. The status word is in the name.',
        },
        {
          name: 'Hover',
          description: 'Muted background (`dot`) or a stronger tint (`bar`, `block`).',
        },
        {
          name: 'Focus',
          description:
            'Monarch focus ring on `:focus-visible`; the chip is raised above its neighbours.',
        },
      ],
    },
    interaction: {
      items: [
        'Click, Enter or Space activates the chip (`onClick`), or follows the link with `href` / `asChild`.',
        'It forwards its ref and props, so it can be a PopoverTrigger or DropdownMenuTrigger child directly.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves focus to the chip (the calendar views manage which chips are in the tab order).',
        },
        { keys: 'Enter / Space', action: 'Activates a button chip; Enter follows a link chip.' },
      ],
    },
    accessibility: {
      role: 'button (or link)',
      name: '"Title, time, status, detail", built from the props; `timeLabel` replaces an abbreviated visible time.',
      items: [
        'The marker is decorative (`aria-hidden`).',
        'Status text is always in the name, so screen-reader users get the same information as the visual style.',
        'Verified with axe in jsdom and in Chromium, Firefox and WebKit on /components/event-chip.',
        'In forced-colours mode the bar and block get a system-colour border (dashed when tentative).',
      ],
    },
    responsive: {
      items: [
        'Fills the width it is given and truncates the title with an ellipsis; it never forces its container wider.',
        '`bar` wraps the detail onto its own line on narrow widths.',
      ],
    },
    theme: {
      items: [
        'Text, surfaces and borders are Monarch tokens, so light, dark and more-contrast themes apply automatically.',
        '`color` is mixed into `--mn-color-bg-surface` (14%, 6% when tentative), so the tint follows the theme’s surface.',
        'Without `color` the chip uses the product accent (`--mn-color-accent-solid`).',
        'Transitions are removed under `prefers-reduced-motion: reduce`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Dot chips (month cell)',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 2, width: 220 }}>
              <EventChip title="Stand-up" time="09:00" color="#0f766e" />
              <EventChip title="Design review" time="11:00" status="tentative" color="#7c3aed" />
              <EventChip title="Retrospective" time="16:00" status="cancelled" color="#c2410c" />
              <EventChip title="Vendor call" time="17:30" status="declined" />
            </div>
          ),
          code: `<EventChip title="Stand-up" time="09:00" color="#0f766e" />
<EventChip title="Design review" time="11:00" status="tentative" color="#7c3aed" />
<EventChip title="Retrospective" time="16:00" status="cancelled" />`,
        },
        {
          title: 'Bar chips (agenda, all-day row)',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 4, maxWidth: 360 }}>
              <EventChip
                variant="bar"
                title="Quarterly planning"
                time="10:00 – 11:30"
                detail="Room 4 · 8 guests"
                color="#2563eb"
              />
              <EventChip variant="bar" title="Team offsite" timeLabel="All day" color="#16a34a" />
              <EventChip
                variant="bar"
                title="Onboarding session"
                time="14:00 – 15:00"
                status="tentative"
                color="#db2777"
              />
            </div>
          ),
          code: `<EventChip variant="bar" title="Quarterly planning" time="10:00 – 11:30" detail="Room 4 · 8 guests" color="#2563eb" />
<EventChip variant="bar" title="Team offsite" timeLabel="All day" />`,
        },
        {
          title: 'Block chips (time grid)',
          render: () => (
            <div style={{ display: 'flex', gap: 8 }}>
              <div style={{ width: 160, height: 72 }}>
                <EventChip
                  variant="block"
                  title="Interview"
                  time="13:00 – 14:00"
                  detail="Video call"
                  color="#0891b2"
                />
              </div>
              <div style={{ width: 160, height: 24 }}>
                <EventChip
                  variant="block"
                  density="compact"
                  title="Check-in"
                  time="15:00"
                  timeLabel="15:00 – 15:15"
                  color="#0891b2"
                />
              </div>
              <div style={{ width: 160, height: 72 }}>
                <EventChip variant="block" title="1:1" time="09:00 – 09:30" past />
              </div>
            </div>
          ),
          code: `<EventChip variant="block" title="Interview" time="13:00 – 14:00" detail="Video call" color="#0891b2" />
<EventChip variant="block" density="compact" title="Check-in" time="15:00" timeLabel="15:00 – 15:15" />`,
        },
        {
          title: 'As a link or popover trigger',
          stack: true,
          render: () => (
            <div style={{ width: 220 }}>
              <EventChip href="#/components/event-chip" title="Launch review" time="10:00" />
            </div>
          ),
          code: `<EventChip href={\`/events/\${event.id}\`} title={event.title} time="10:00" />

<EventChip asChild title={event.title} time="10:00">
  <Link href={\`/events/\${event.id}\`} />
</EventChip>

<PopoverTrigger asChild>
  <EventChip title={event.title} time="10:00" />
</PopoverTrigger>`,
        },
      ],
    },
    doDont: {
      do: [
        'Pass `timeLabel` when the visible time is shorter than the event (a start time in a month cell).',
        'Use the calendar or service colour your product already shows elsewhere.',
        'Translate `statusLabel` along with the rest of the UI.',
      ],
      dont: [
        'Do not put several actions inside a chip; it is one control.',
        'Do not signal status with colour only, for example a red chip for cancelled: set `status`.',
        'Do not make a chip with no time unless it is an all-day event, and then say so in `timeLabel`.',
      ],
    },
    related: {
      items: [
        {
          id: 'calendar-month-view',
          reason: 'Renders `dot` chips in each day and its "+N more" popover.',
        },
        { id: 'agenda-list', reason: 'Renders `bar` chips grouped by day.' },
        {
          id: 'time-grid',
          reason: 'Renders `block` chips laid out by time and `bar` chips in the all-day row.',
        },
        { id: 'status-badge', reason: 'For states of records that are not events.' },
      ],
    },
    dependencies: { packages: ['@radix-ui/react-slot', 'react'], components: [] },
    testing: {
      unit: [
        'Button named "title, time, status"; `data-variant`, `data-status` and the colour variable are set.',
        'Confirmed status is left out of the name; `timeLabel` replaces an abbreviated time.',
        '`bar` and `block` include status and detail in the name; compact density and `past` are exposed.',
        'Click, Enter and Space activate it; the ref is forwarded.',
        '`href` renders a link and `asChild` renders a router link with the chip’s class and content.',
        'No axe violations in jsdom; renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/event-chip',
        'Cross-browser axe: /components/event-chip',
      ],
    },
  },
} satisfies ComponentDocs;
