import { Avatar } from '@rookcom/monarch-react';
import { PresenceDot } from '@rookcom/monarch-react/shell';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const people = [
  { name: 'Ada Lovelace', status: 'online' as const },
  { name: 'Grace Hopper', status: 'away' as const, custom: { emoji: '🍽️', text: 'Lunch' } },
  { name: 'Alan Turing', status: 'busy' as const },
  { name: 'Katherine Johnson', status: 'dnd' as const, custom: { text: 'Focusing' } },
  { name: 'Edsger Dijkstra', status: 'offline' as const },
];

export default {
  id: 'presence-dot',
  title: 'PresenceDot',
  summary:
    'A small dot for online, away, busy, do not disturb or offline, with a text alternative.',
  sections: {
    purpose: {
      summary:
        'Shows whether a person is online, away, busy, in do-not-disturb or offline, next to their name or on their avatar, and reads the same as text. Needed by: Chat (conversation list, member details), Tickets (agent availability on the account avatar).',
      useWhen: [
        'A list row or a custom avatar layout needs a presence indicator that is not an Avatar.',
        'A custom status ("Lunch", "Focusing") should travel with the presence.',
      ],
      avoidWhen: [
        'You render an Avatar: use its `status` prop, which draws the same dot.',
        'The state is a record status (ticket open, job paused): use StatusBadge.',
      ],
    },
    designPrinciples: {
      items: [
        'Data in: the app knows the presence; the dot never subscribes to a presence service.',
        'Never colour alone: the text alternative carries the status, and forced-colours mode gives each status its own shape.',
        'Same colours and shapes as Avatar’s status dot, so a person looks the same everywhere.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the root `<span>`.',
        '`presenceLabel(status, customStatus?, messages?)` returns the text the dot reads, for use in tooltips or row labels.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Chat `PresenceDot`): `userId` + `usePresence()` → pass `status` and `customStatus` from your presence store; the `dnd` value stays `dnd`; render nothing yourself while presence is unknown; `className="absolute -bottom-0.5 -right-0.5"` → `overlay`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'status',
          values: ['online', 'away', 'busy', 'dnd', 'offline'],
          description: 'Success, warning and danger colours; dnd adds a bar; offline is a ring.',
        },
        { prop: 'size', values: ['sm', 'md', 'lg'], description: '8, 10 or 12px.' },
        {
          prop: 'overlay',
          values: ['false', 'true'],
          description: 'Pin to the bottom-end corner of a positioned parent, with a surface ring.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Status', attribute: 'data-status', description: 'The presence shown.' },
        { name: 'Overlay', attribute: 'data-overlay', description: 'Pinned on an avatar corner.' },
        {
          name: 'Decorative',
          attribute: 'aria-hidden="true"',
          description:
            'Nearby text already says the status; the dot is hidden from assistive technology.',
        },
      ],
    },
    interaction: {
      items: [],
      notes: [
        'Not interactive. A native title shows the status on hover unless `showTitle={false}`.',
      ],
    },
    keyboard: {
      items: [],
      notes: ['Not focusable: it is part of the surrounding row or link.'],
    },
    accessibility: {
      role: 'none (inline text)',
      name: 'Visually hidden text such as "Away · Lunch", read as part of the surrounding content.',
      items: [
        'The status is text, so it is announced within the row or link name.',
        'Use `decorative` only when the same words are visible nearby.',
        'Forced colours: online filled, away half-filled, busy square, dnd barred, offline ring.',
      ],
    },
    responsive: {
      items: ['Fixed size; it never wraps or grows.'],
    },
    theme: {
      items: [
        '`--mn-color-success-solid`, `--mn-color-warning-solid`, `--mn-color-danger-solid`, and a `--mn-color-text-subtle` ring for offline, in all four themes.',
        'The overlay ring uses `--mn-color-bg-surface` so the dot stands off the avatar.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Every status, with custom status text',
          render: () => (
            <ul
              style={{
                display: 'grid',
                gap: 'var(--mn-space-2)',
                margin: 0,
                padding: 0,
                listStyle: 'none',
              }}
            >
              {people.map((p) => (
                <li
                  key={p.name}
                  style={{ display: 'flex', alignItems: 'center', gap: 'var(--mn-space-2)' }}
                >
                  <PresenceDot status={p.status} customStatus={p.custom ?? null} />
                  {p.name}
                </li>
              ))}
            </ul>
          ),
        },
        {
          title: 'On an avatar corner',
          render: () => (
            <div style={{ display: 'flex', gap: 'var(--mn-space-4)' }}>
              {people.slice(0, 3).map((p) => (
                <span key={p.name} style={{ position: 'relative', display: 'inline-flex' }}>
                  <Avatar name={p.name} size="md" />
                  <PresenceDot status={p.status} overlay size="sm" />
                </span>
              ))}
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep presence fresh in the app (socket or polling) and pass the current value.',
        'Pass the custom status so screen-reader users hear it too.',
      ],
      dont: [
        'Do not mark it decorative unless the status is written out next to it.',
        'Do not use it for non-person states such as service health.',
      ],
    },
    related: {
      items: [
        { id: 'avatar', reason: 'Has the same dot built in through `status`.' },
        { id: 'my-status', reason: 'The signed-in person’s own presence and status.' },
        { id: 'availability-toggle', reason: 'Lets an agent switch available or away.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Text alternative and data-status for each status; title on hover.',
        'Custom status joins the text; dnd and busy read differently.',
        'Decorative, overlay and size; translation; presenceLabel trims empty parts.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
