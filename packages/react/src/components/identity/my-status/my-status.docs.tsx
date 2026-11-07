import { MyStatus } from '@rookcom/monarch-react/shell';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const now = new Date('2026-03-02T10:00:00Z');

export default {
  id: 'my-status',
  title: 'MyStatus',
  summary:
    'The signed-in person’s avatar, presence and custom status, opening their status settings.',
  sections: {
    purpose: {
      summary:
        'Shows people how others see them (presence and custom status) and gives one place to change it, at the top of a sidebar. Needed by: Chat (conversation sidebar).',
      useWhen: [
        'An app with presence where people set a custom status or do-not-disturb.',
        'The top of a sidebar or a panel header that opens status or preference settings.',
      ],
      avoidWhen: [
        'The account menu (profile, sign out): the RookRail and IdentityBar account menus already show the person.',
        'Showing someone else’s presence: use PresenceDot or Avatar `status`.',
      ],
    },
    designPrinciples: {
      items: [
        'One target: the whole row is a single link or button, never nested controls.',
        'Expired statuses disappear on their own; the app does not need to clear them first.',
        'Name first, then presence, then status, so screen-reader users hear who before what.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the `<a>` (with `href`) or the `<button>`.',
        'Built on Avatar and PresenceDot.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`customStatus.until` takes an ISO string, a Date or epoch milliseconds; compared with `now` (default: the current time).',
        'Migrating from the app copy (Chat `MyStatus`): the component no longer reads `useChatContext()` or fetches prefs. Pass `name` and `avatarUrl` from the member, `presence` from `prefs.presence` (`DND` → `dnd`, `AWAY` → `away`, otherwise `online`), `customStatus={{ emoji: statusEmoji, text: statusText, until: statusUntil }}` and `href={`${base}/preferences`}` with `linkComponent={Link}`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'href / onClick',
          values: ['link', 'button'],
          description: 'A link to a settings page, or a button that opens a status dialog.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Presence', attribute: 'data-presence', description: 'The current presence.' },
        {
          name: 'Has status',
          attribute: 'data-has-status',
          description: 'A custom status is set and not expired; the second line shows it.',
        },
        {
          name: 'No status',
          description:
            'Away, busy and do not disturb are written out; online and offline invite "Set a status".',
        },
      ],
    },
    interaction: {
      items: ['Clicking or pressing Enter follows the link or runs `onClick`.'],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Focus the control.' },
        { keys: 'Enter', action: 'Follow the link or press the button.' },
        { keys: 'Space', action: 'Press the button (button form).' },
      ],
    },
    accessibility: {
      role: 'link, or button without `href`',
      name: '"Your status and settings: <name>, <presence>, <status>" from its text.',
      items: [
        'Named by its content plus a visually hidden prefix, so the visible name is part of the accessible name.',
        'The avatar and dot are decorative; the presence is read as text after the name.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; long names and statuses truncate with an ellipsis.',
        'At least 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Transparent at rest, `--mn-color-bg-muted` on hover, focus ring from `--mn-color-focus-ring`.',
        'Forced colours use ButtonText and Highlight; reduced motion removes the hover transition.',
      ],
    },
    examples: {
      items: [
        {
          title: 'With a custom status, away, and no status',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 'var(--mn-space-1)', width: '16rem' }}>
              <MyStatus
                name="Ada Lovelace"
                presence="online"
                customStatus={{ emoji: '📅', text: 'In a workshop until 3pm' }}
                href="#status"
                now={now}
              />
              <MyStatus name="Grace Hopper" presence="away" href="#status" now={now} />
              <MyStatus name="Alan Turing" presence="online" onClick={() => {}} now={now} />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Open a page or dialog where people set presence, custom status and its end time.',
        'Pass `now` when you render on the server, so the expiry check matches on the client.',
      ],
      dont: ['Do not put other buttons inside it.', 'Do not show it to signed-out visitors.'],
    },
    related: {
      items: [
        { id: 'presence-dot', reason: 'The dot it shows on the avatar.' },
        { id: 'availability-toggle', reason: 'An on/off availability switch for routing.' },
        { id: 'avatar', reason: 'The avatar it shows.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['avatar', 'presence-dot'] },
    testing: {
      unit: [
        'Link named by its text; "Set a status" with no status.',
        'Custom status shown until `until`; away written out without a status.',
        'Button form with Enter and Space; router link component; translation.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
