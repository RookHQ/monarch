import { useState } from 'react';
import { Button, Field, FieldLabel, Input } from '@rookcom/monarch-react';
import {
  BookingFlow,
  useBookingFlow,
  type BookingFlowStep,
  type SlotPickerSlot,
} from '@rookcom/monarch-react/scheduling';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const SERVICES = [
  { id: 'intro', name: 'Introductory call', minutes: 20 },
  { id: 'consult', name: 'Consultation', minutes: 45 },
];

function slotsFor(day: string): SlotPickerSlot[] {
  if (day.endsWith('-07') || day.endsWith('-08')) return [];
  return ['09:00', '10:00', '11:30', '14:00'].map((t) => ({ start: `${day}T${t}:00Z` }));
}

function ServiceOptions({ value, onPick }: { value: string | null; onPick: (id: string) => void }) {
  const flow = useBookingFlow();
  return (
    <div style={{ display: 'grid', gap: 8 }}>
      {SERVICES.map((s) => (
        <Button
          key={s.id}
          aria-pressed={value === s.id}
          onClick={() => {
            onPick(s.id);
            flow.next();
          }}
        >
          {s.name} · {s.minutes} min
        </Button>
      ))}
    </div>
  );
}

function Demo({ layout }: { layout: 'steps' | 'list' }) {
  const [service, setService] = useState<string | null>(null);
  const [day, setDay] = useState<string | null>('2026-11-05');
  const [slot, setSlot] = useState<string | null>(null);
  const [name, setName] = useState('');
  const chosen = SERVICES.find((s) => s.id === service);
  const steps: BookingFlowStep[] = [
    {
      id: 'service',
      label: 'Service',
      title: 'Choose a service',
      content: <ServiceOptions value={service} onPick={setService} />,
      summary: chosen?.name,
      hideContinue: true,
    },
    {
      id: 'time',
      label: 'Date and time',
      title: 'Choose a date and time',
      description: chosen ? `${chosen.name} · ${chosen.minutes} min` : undefined,
      slotPicker: {
        day,
        onDayChange: (d: string) => {
          setDay(d);
          setSlot(null);
        },
        slots: day ? slotsFor(day) : [],
        value: slot,
        onValueChange: setSlot,
        today: '2026-11-02',
        min: '2026-11-02',
        timeZone: 'Europe/London',
        locale: 'en-GB',
        layout: 'stacked',
      },
      summary: slot ? `${slot.slice(0, 10)}, ${slot.slice(11, 16)}` : undefined,
      complete: slot !== null,
    },
    {
      id: 'details',
      label: 'Your details',
      content: (
        <Field required>
          <FieldLabel>Name</FieldLabel>
          <Input value={name} onChange={(e) => setName(e.target.value)} autoComplete="name" />
        </Field>
      ),
      summary: name || undefined,
      complete: name.trim() !== '',
    },
    {
      id: 'review',
      label: 'Review',
      title: 'Review your booking',
      content: <p style={{ margin: 0 }}>Check the summary, then confirm.</p>,
    },
  ];
  return (
    <div style={{ width: '100%', maxWidth: 880 }}>
      <BookingFlow
        layout={layout}
        steps={steps}
        onSubmit={() => new Promise((r) => setTimeout(r, 600))}
        confirmation={
          <div>
            <h2 style={{ margin: 0 }}>You are booked</h2>
            <p>Reference AB12CD. The details are on their way.</p>
          </div>
        }
      />
    </div>
  );
}

export default {
  id: 'booking-flow',
  title: 'BookingFlow',
  summary: 'Step-by-step booking: service, time, details, review and confirmation.',
  sections: {
    purpose: {
      summary:
        'The public and staff booking journey as a pattern: one step at a time with progress, back and change controls, a summary panel, focus and announcements on every step, and an async submit that ends on a confirmation. Needed by: Calendar (public booking pages, embeds, new appointment), Recruit (candidate self-scheduling).',
      useWhen: [
        'A visitor books an appointment, meeting or interview in a few ordered steps.',
        'Any short ordered journey that ends in one submit and needs a running summary.',
      ],
      avoidWhen: [
        'Long forms with branching sections: use Stepper with your own form.',
        'Just choosing a time: use SlotPicker on its own.',
      ],
    },
    designPrinciples: {
      items: [
        'Steps are data (`{ id, label, title, content, summary, complete }`); the product renders the content and keeps the state.',
        'A step can host a SlotPicker directly (`slotPicker`), so the time step needs no glue.',
        'Each step change moves focus to the new heading and announces “Step 2 of 4: …”, as the app copy did.',
        'Submitting is async with pending and error states; the confirmation replaces the steps only after success.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`step`/`onStepChange` (controlled) or `defaultStep`. `onSubmit` runs on the last step’s button; return a promise to show pending and catch errors.',
        '`useBookingFlow()` gives content `next()`, `back()`, `goTo(id)`, `stepId` and `index`, for options that advance on click.',
        '`completed` (controlled) or the internal state shows `confirmation`. `unavailable` replaces everything when nothing can be booked.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Calendar `booking/BookingExperience`): the fixed `service`/`host`/`time`/`details`/`review` steps become `steps` entries the product builds (leave out the host step when it does not apply); `STEP_LABEL`/`STEP_TITLE` → `label`/`title`; `stepSummary` → `summary`; layout `editorial` → `layout="list"`, the other layouts → `layout="steps"` with `header` and `aside` for the page chrome; `adapter.book` → `onSubmit`; the done screen → `confirmation`; “No appointments are available” → `unavailable`; brand colour → `brandColor`. Fetching, honeypot, uploads and page themes stay in the product.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'layout',
          values: ['steps', 'list'],
          description:
            '`steps`: one step at a time with a progress list and back button. `list`: all steps listed, completed ones summarised with a Change button.',
        },
        {
          prop: 'brandColor',
          values: ['#rrggbb'],
          description:
            'Continue buttons use Button `variant="brand"` with the tenant colour, contrast-corrected per theme.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Current step',
          attribute: 'data-state="current" (item)',
          description: 'Accent number; `aria-current="step"`.',
        },
        {
          name: 'Completed step',
          attribute: 'data-state="done" (item)',
          description: 'Tick; a change button.',
        },
        {
          name: 'Blocked',
          description: 'Continue is disabled while the step’s `complete` is false.',
        },
        {
          name: 'Submitting',
          attribute: 'data-busy',
          description: 'Last button pending; navigation disabled.',
        },
        { name: 'Error', description: 'Danger Alert above the button, referenced by it.' },
        {
          name: 'Done',
          attribute: 'data-state="done" (root)',
          description: 'The confirmation, focused and announced.',
        },
      ],
    },
    interaction: {
      items: [
        'Continue moves to the next step; on the last step it calls `onSubmit`.',
        'Back returns one step; completed steps in the progress list jump back to that step.',
        'Completed steps’ summaries collect in the side panel, after `aside`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Progress links, back, the step content, Continue.' },
        { keys: 'Enter / Space', action: 'Activate Continue, Back and the change buttons.' },
      ],
    },
    accessibility: {
      role: 'navigation (progress), headings, complementary (summary)',
      name: 'Progress is “Booking steps”; the summary panel is “Summary”.',
      items: [
        'The new step’s heading receives focus (`tabIndex=-1`) and a polite status line announces it.',
        'Completed progress items are buttons named “Service, completed. Change”.',
        'Every string is in `messages`.',
      ],
    },
    responsive: {
      items: [
        'A container query puts the summary beside the steps from 52rem; below that it follows them.',
        'The progress list wraps; at 390px the time step stacks its month above the times.',
      ],
    },
    theme: {
      items: [
        'Accent for the current step and Continue (or the tenant brand colour); every theme via tokens.',
        'Forced colours: current step number in Highlight.',
      ],
    },
    examples: {
      items: [
        { title: 'Steps layout', stack: true, render: () => <Demo layout="steps" /> },
        { title: 'List layout', stack: true, render: () => <Demo layout="list" /> },
        {
          title: 'Nothing to book',
          render: () => (
            <BookingFlow
              steps={[]}
              unavailable={<p style={{ margin: 0 }}>No appointments are available right now.</p>}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Leave out steps that do not apply instead of skipping them.',
        'Give each completed step a short `summary`.',
        'Check the slot again on the server when submitting, and show the conflict as the error.',
      ],
      dont: [
        'Do not move focus yourself on step change; the pattern does it.',
        'Do not put more than one submit in the flow.',
      ],
    },
    related: {
      items: [
        { id: 'slot-picker', reason: 'The time step.' },
        { id: 'stepper', reason: 'Generic multi-step forms.' },
        { id: 'reschedule-dialog', reason: 'Changing a booking afterwards.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'alert', 'slot-picker'] },
    testing: {
      unit: [
        'First step with progress and step count; axe clean.',
        'Content advances with `useBookingFlow().next()`; heading focused; announcement; summary panel.',
        '`complete: false` blocks Continue; SlotPicker step.',
        'Back and change buttons report `onStepChange` and focus the heading.',
        'Submit pending, error then confirmation and announcement.',
        'List layout; unavailable; hook outside throws; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
