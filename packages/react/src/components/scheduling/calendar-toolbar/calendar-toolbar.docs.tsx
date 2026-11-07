import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { CalendarToolbar, type CalendarToolbarView } from '@rookcom/monarch-react/scheduling';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const MONTHS = ['October 2026', 'November 2026', 'December 2026'];

function Demo() {
  const [view, setView] = useState<CalendarToolbarView>('month');
  const [i, setI] = useState(1);
  return (
    <div style={{ width: '100%' }}>
      <CalendarToolbar
        title={MONTHS[i]}
        view={view}
        onViewChange={setView}
        onToday={() => setI(1)}
        todayDisabled={i === 1}
        onPrevious={() => setI((n) => Math.max(0, n - 1))}
        onNext={() => setI((n) => Math.min(MONTHS.length - 1, n + 1))}
        previousDisabled={i === 0}
        nextDisabled={i === MONTHS.length - 1}
        actions={
          <Button variant="primary" size="sm">
            New event
          </Button>
        }
      />
    </div>
  );
}

export default {
  id: 'calendar-toolbar',
  title: 'CalendarToolbar',
  summary: 'Calendar header: Today, previous and next, range title, view switcher and actions.',
  sections: {
    purpose: {
      summary:
        'One calendar header so navigation is identical in every app. Needed by: Calendar, Stride (issue calendar), Chat (meetings), Recruit (interviews) and Tickets (calendar panel), which each keep a copy of the calendar kit’s CalendarToolbar today.',
      useWhen: ['Above CalendarMonthView, TimeGrid, AgendaList or a TeamCalendar.'],
      avoidWhen: [
        'Choosing a single date in a form: use DatePicker.',
        'General page actions without date navigation: use Toolbar or PageHeader.',
      ],
    },
    designPrinciples: {
      items: [
        'Callbacks only: the product owns the date, the range and the title text.',
        'Arrow buttons are named after the view (“Next week”), so the unit is never ambiguous.',
        'The title is a polite live region, so moving announces the new range.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<div>` (ref). The view switcher appears only with `onViewChange` and more than one view.',
        '`views` accepts product view ids beside the built-in ones; label and unit them with `messages.views` and `messages.units`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (calendar kit `CalendarToolbar`): `onPrev` → `onPrevious`; `todayLabel` → `messages.today`; `viewLabels` → `messages.views`; `busy` (a node) → `busy` (boolean, shows a Spinner); `titleAs` now defaults to `h2` (pass `h1` where it heads the page); `view`, `views`, `onViewChange`, `title`, `onToday`, `onNext`, `datePicker`, `actions` are unchanged.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'titleAs',
          values: ['h1', 'h2', 'h3'],
          description: 'Heading level of the title. Default `h2`.',
        },
        {
          prop: 'onViewChange',
          values: ['set', 'unset'],
          description: 'Shows or hides the SegmentedControl view switcher.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Busy',
          attribute: 'data-busy',
          description: 'A small labelled spinner after the title.',
        },
        {
          name: 'Today disabled',
          description: '`todayDisabled` when the range already shows today.',
        },
        {
          name: 'Edge of range',
          description: '`previousDisabled` / `nextDisabled` at a booking window’s ends.',
        },
      ],
    },
    interaction: {
      items: [
        'Today, previous and next call their callbacks; the product updates `title`.',
        'Choosing a view calls `onViewChange`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Today, previous, next, date picker, view switcher, actions.' },
        { keys: 'Enter / Space', action: 'Activate a button.' },
        { keys: 'Arrow keys', action: 'Move and select within the view switcher.' },
      ],
    },
    accessibility: {
      role: 'heading (title), radiogroup (view switcher)',
      name: 'Arrows are “Previous {unit}” / “Next {unit}”; the switcher is “Calendar view”.',
      items: [
        'The title has `aria-live="polite"` and `aria-atomic`.',
        'The busy spinner is a status named “Loading”.',
        'Arrows mirror in right-to-left pages.',
      ],
    },
    responsive: {
      items: [
        'A container query moves the title to its own line under 30rem and puts the switcher and actions on the next line.',
        'Nothing scrolls sideways at 390px.',
      ],
    },
    theme: {
      items: ['Built from Button, IconButton, SegmentedControl and Spinner; every theme applies.'],
    },
    examples: {
      items: [
        { title: 'Month view with actions', stack: true, render: () => <Demo /> },
        {
          title: 'Without a view switcher, busy',
          stack: true,
          render: () => (
            <CalendarToolbar
              title="2 – 8 November 2026"
              view="week"
              busy
              onToday={() => {}}
              onPrevious={() => {}}
              onNext={() => {}}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Format the title with Intl in the product’s locale.',
        'Disable Today when today is already shown.',
      ],
      dont: ['Do not put filters inside the title; use `actions`.'],
    },
    related: {
      items: [
        { id: 'calendar-month-view', reason: 'Month grid below the toolbar.' },
        { id: 'time-grid', reason: 'Day and week grids.' },
        { id: 'agenda-list', reason: 'Agenda view.' },
        { id: 'segmented-control', reason: 'The view switcher.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'spinner', 'segmented-control'],
    },
    testing: {
      unit: [
        'Names after the view, live title, switcher and actions; axe clean.',
        'Today, previous and next callbacks.',
        'Arrow keys switch views.',
        'Custom views, labels, units and heading level; switcher hidden without `onViewChange`.',
        'Busy spinner and disabled states; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
