import { Button } from '@rookcom/monarch-react';
import { EventChip, EventPopover, type CalendarEvent } from '@rookcom/monarch-react/scheduling';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './event-popover.fixture.json';

const EVENT = {
  id: 'e1',
  color: '#2f7d6d',
  ...fixture.data.event,
} as CalendarEvent;

export default {
  id: 'event-popover',
  title: 'EventPopover',
  summary: 'Quick view of a calendar event from its chip: time, status, details and actions.',
  sections: {
    purpose: {
      summary:
        'Shows the essentials of an event without leaving the calendar, with the next actions (open, accept, decline, cancel). Needed by: Calendar (appointment popover), and the calendar views in Stride, Chat and Recruit.',
      useWhen: [
        'A chip in CalendarMonthView, TimeGrid or AgendaList needs a peek before opening the record.',
      ],
      avoidWhen: [
        'Editing the event: open a Dialog or Sheet.',
        'Long content such as notes or attendees lists: open the record page.',
      ],
    },
    designPrinciples: {
      items: [
        'Takes the same `CalendarEvent` as the calendar views, plus props to override or add to it.',
        'Times are shown in a stated zone with its short name, so the reader knows which clock it is.',
        'Actions are the product’s; a function form receives `close` so an action can close the popover first.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The child is the trigger (Radix `asChild`): one focusable element such as an EventChip.',
        'Ref and remaining props go to PopoverContent (`side`, `align`, `collisionPadding`, …).',
        '`status` takes an EventChip status (shown as a Badge) or any node for product statuses.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Calendar `calendar/AppointmentPopover`): `appt` → `event` (map customer name to `title`, service to `subtitle`, colour to `color`), `appt.status` → `status` (a StatusPill node or an EventChip status); `zone`/`timeFormat` → `timeZone`/`hourCycle`; hosts and reference → `details` with icons; cancellation reason and privacy notice → `note`; the Open/Accept/Decline/Cancel buttons → `actions={(close: () => void) => …}` with `onAction` and permission checks staying in the product.',
      ],
    },
    variants: {
      items: [],
      notes: [
        'No variants; the content decides the size. Width is capped at 22rem and the viewport.',
      ],
    },
    states: {
      items: [
        {
          name: 'Status',
          attribute: 'data-status',
          description: 'Badge tone per status; cancelled and declined titles are struck through.',
        },
        { name: 'All day', description: 'Date range and “All day” instead of times.' },
      ],
    },
    interaction: {
      items: [
        'Opens on the trigger’s click; Escape or an outside click closes it and focus returns to the trigger.',
        'Non-modal: the calendar stays usable.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space', action: 'On the trigger, opens the popover.' },
        { keys: 'Tab', action: 'Moves through the actions.' },
        { keys: 'Escape', action: 'Closes and returns focus to the trigger.' },
      ],
    },
    accessibility: {
      role: 'dialog (Radix popover content)',
      name: 'Labelled by the title.',
      items: [
        'Details are a description list; icon rows keep a visually hidden label.',
        'The colour swatch is decorative; the status is text.',
        'Strings are in `messages`; dates and times come from Intl.',
      ],
    },
    responsive: {
      items: ['Width `min(22rem, 100vw - 2rem)`; Radix flips and shifts it to stay on screen.'],
    },
    theme: {
      items: [
        'Popover surface and Badge tones from tokens; the swatch uses the event colour (`--mn-event-color`).',
      ],
    },
    examples: {
      items: [
        {
          title: 'From an event chip',
          stage: true,
          render: () => (
            <EventPopover
              defaultOpen
              event={EVENT}
              timeZone={fixture.data.timeZone}
              locale={fixture.data.locale}
              hourCycle="24"
              details={fixture.data.details}
              actions={(close: () => void) => (
                <>
                  <Button size="sm" onClick={close}>
                    {fixture.data.actions[0]}
                  </Button>
                  <Button size="sm" variant="danger-subtle" onClick={close}>
                    {fixture.data.actions[1]}
                  </Button>
                </>
              )}
            >
              <EventChip
                title={EVENT.title}
                time={fixture.data.chipTime}
                color="#2f7d6d"
                variant="bar"
              />
            </EventPopover>
          ),
        },
        {
          title: 'All day, with a note',
          render: () => (
            <EventPopover
              title="Team offsite"
              start="2026-11-04"
              end="2026-11-06"
              allDay
              status="tentative"
              locale="en-GB"
              note="Venue to be confirmed."
            >
              <Button size="sm">Team offsite</Button>
            </EventPopover>
          ),
        },
      ],
    },
    doDont: {
      do: ['Pass the same zone the calendar uses.', 'Keep actions to two or three.'],
      dont: [
        'Do not put forms in the popover.',
        'Do not show private contact details to people who may not see them.',
      ],
    },
    related: {
      items: [
        { id: 'event-chip', reason: 'The usual trigger.' },
        { id: 'popover', reason: 'The surface.' },
        { id: 'calendar-month-view', reason: 'Wrap chips with `renderEvent`.' },
        { id: 'time-grid', reason: 'Wrap chips with `renderEvent`.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['badge', 'popover'] },
    testing: {
      unit: [
        'Opens from the trigger, named by the title; time, status and details; Escape returns focus; axe clean.',
        'Function actions receive `close`.',
        'All-day range, overrides, icon rows, note and custom status.',
        'Times in the given zone; trigger renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
