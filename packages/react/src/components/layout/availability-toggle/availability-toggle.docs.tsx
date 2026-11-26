import { useState } from 'react';
import { AvailabilityToggle } from '@rookcom/monarch-react/shell';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function Basic() {
  const [available, setAvailable] = useState(true);
  return (
    <div style={{ width: '18rem' }}>
      <AvailabilityToggle available={available} onAvailableChange={setAvailable} showHint />
    </div>
  );
}

function Saving() {
  const [available, setAvailable] = useState(false);
  const [pending, setPending] = useState(false);
  return (
    <div style={{ width: '18rem' }}>
      <AvailabilityToggle
        available={available}
        pending={pending}
        onAvailableChange={(next) => {
          setPending(true);
          setTimeout(() => {
            setAvailable(next);
            setPending(false);
          }, 800);
        }}
      />
    </div>
  );
}

export default {
  id: 'availability-toggle',
  title: 'AvailabilityToggle',
  summary: 'An on/off switch for whether automatic assignment may give someone new work.',
  sections: {
    purpose: {
      summary:
        'Lets an agent mark themselves available or away for routing, from the account menu, a sidebar or a home page. Needed by: Tickets (account menu, mobile menu, desk home).',
      useWhen: [
        'Work is assigned automatically (round robin, load balancing) and people must be able to step out of it.',
      ],
      avoidWhen: [
        'Chat-style presence with custom statuses: use MyStatus.',
        'A setting saved with a form: use Switch inside the form.',
      ],
    },
    designPrinciples: {
      items: [
        'Data in, value out: the app saves the change, rolls back on error and shows the toast.',
        'The status word is always visible, so the state is clear without reading the switch.',
        'One switch, one effect: the hint says what being away changes and what it does not.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: ['Forwards its ref to the root `<div>`. Built on Switch.'],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Tickets `AvailabilityToggle`): the `useMyAvailability()` hook stays in the app. Pass `available={available}`, `onAvailableChange={set}`, `pending={pending}`, keep `showHint`, and replace the wrapping `<div onKeyDown={(e) => e.stopPropagation()}>` in the account menu with `inMenu`. `ready === false` maps to `available={null}`. Toast copy stays in the app.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'showHint',
          values: ['false', 'true'],
          description: 'Add a one-line explanation under the status.',
        },
        {
          prop: 'inMenu',
          values: ['false', 'true'],
          description: 'Keep key presses from reaching a surrounding menu.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Available', attribute: 'data-available', description: 'Switch on, green mark.' },
        { name: 'Away', description: 'Switch off, yellow mark.' },
        {
          name: 'Pending',
          attribute: 'data-pending',
          description: 'Saving: the switch is disabled and `aria-busy`.',
        },
        { name: 'Unknown', description: '`available={null}`: renders nothing.' },
      ],
    },
    interaction: {
      items: [
        'Clicking or pressing Space on the switch calls `onAvailableChange` with the new value.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Focus the switch.' },
        { keys: 'Space / Enter', action: 'Toggle available and away.' },
      ],
    },
    accessibility: {
      role: 'switch',
      name: '"Available for new work" (`messages.label`); the hint is its description.',
      items: [
        'The switch announces on and off; the visible status word repeats it for sighted people.',
        'The coloured mark is decorative and has its own filled or hollow shape in forced colours.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; the text wraps and the switch stays at the end.',
        'At least 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Status marks use `--mn-color-success-solid` and `--mn-color-warning-solid`; the switch uses its own accent tokens.',
      ],
    },
    examples: {
      items: [
        { title: 'With a hint', render: () => <Basic /> },
        { title: 'Saving', render: () => <Saving /> },
      ],
    },
    doDont: {
      do: [
        'Update optimistically and roll back with a toast if saving fails.',
        'Show the same availability on the account avatar (Avatar `status`).',
      ],
      dont: [
        'Do not stop people working on what they already have when they are away.',
        'Do not hide the toggle for agents who are subject to routing.',
      ],
    },
    related: {
      items: [
        { id: 'switch', reason: 'The control it uses.' },
        { id: 'my-status', reason: 'Presence with custom status for chat-style apps.' },
        { id: 'presence-dot', reason: 'Shows presence next to a name.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['switch'] },
    testing: {
      unit: [
        'Named switch, status text and hint as description.',
        'Space toggles and reports the value; the hint follows.',
        'Pending disables and sets aria-busy; unknown renders nothing.',
        'inMenu stops key events; translation.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
