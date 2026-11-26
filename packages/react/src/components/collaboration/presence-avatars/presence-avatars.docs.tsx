import { PresenceAvatars, type PresencePerson } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const PEOPLE: PresencePerson[] = [
  { id: 'u1', name: 'Priya Raman', activity: 'Editing' },
  { id: 'u2', name: 'Tom Becker', activity: 'Viewing' },
  { id: 'u3', name: 'Lee Park', status: 'away' },
  {
    id: 'u4',
    name: 'Ana Silva',
    status: 'busy',
    customStatus: { emoji: '📅', text: 'In a meeting' },
  },
  { id: 'u5', name: 'Ben Okafor', status: 'dnd' },
  { id: 'u6', name: 'Sam Ito' },
  { id: 'u7', name: 'Mia Novak', status: 'offline' },
];

export default {
  id: 'presence-avatars',
  title: 'PresenceAvatars',
  summary:
    'Who is here now: an overlapping avatar stack with presence dots and a "+N" button that lists everyone with their status and activity.',
  sections: {
    purpose: {
      summary:
        'Shows the people currently viewing or editing a document, board or call, with their presence, so collaborators know who else is here. Needed by: Rook Chat (DetailsPanel and ConversationSidebar overlay `PresenceDot` on avatars).',
      useWhen: [
        'A shared document, board or record header: who has it open right now.',
        'A call or room: who joined.',
      ],
      avoidWhen: [
        'A fixed list of members or assignees without live presence: use AvatarGroup.',
        "One person's presence next to their name: use PresenceDot or Avatar `status`.",
        'Who is typing: use TypingIndicator.',
      ],
    },
    designPrinciples: {
      items: [
        'Presence is never colour alone: each person is named with their presence ("Away · In a meeting") and activity, and PresenceDot gives each status its own shape in forced colours.',
        'A "+1" never replaces a face: if only one person would be hidden, they are shown.',
        'The "+N" list shows everyone, so one place answers "who is here".',
        'Data in, nothing subscribed: the product decides presence and order.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`status` defaults to `online`, since people listed are usually here now; pass `away`, `busy`, `dnd` or `offline` as needed.',
        'With `onPersonClick` each face (and each row in the list) is a button, for example to jump to their cursor.',
        'Presence words come from PresenceDot (`presenceMessages` to translate them).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from an AvatarGroup with hand-placed dots: each `<Avatar name src status>` child becomes a `people` entry (`id`, `name`, `avatarSrc`, `status`); `max` keeps its meaning; `overflowLabel` becomes `messages.more`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['xs', 'sm', 'md', 'lg', 'xl'],
          description:
            'Avatar size (default `sm`); the dot scales with it. Exposed as `data-size`.',
        },
        {
          prop: 'max',
          values: ['number'],
          description: 'Faces shown before the "+N" button (default 4).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Away / offline',
          attribute: 'data-status on the item',
          description: 'The face is slightly faded as well as the dot changing.',
        },
        {
          name: 'Interactive',
          attribute: 'data-interactive',
          description:
            'Faces are buttons; on touch screens they stop overlapping so each is a 44px target.',
        },
        {
          name: 'List open',
          description: 'A popover lists everyone with name, presence and activity.',
        },
        { name: 'Focus', description: 'Faces and the "+N" button show the Monarch focus ring.' },
      ],
    },
    interaction: {
      items: [
        'Hovering a face shows its name, presence and activity as a native title.',
        'The "+N" button opens a popover with everyone; Escape or a click outside closes it and returns focus.',
        'With `onPersonClick`, clicking a face or a list row calls it (and closes the list).',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves to each face button (with `onPersonClick`) and the "+N" button.',
        },
        { keys: 'Enter / Space', action: 'Activates a face, or opens the list.' },
        { keys: 'Escape', action: 'Closes the list and returns focus to "+N".' },
      ],
    },
    accessibility: {
      role: 'group containing a list of people (images, or buttons with `onPersonClick`)',
      name: 'The group "{count} people here"; each person "Priya Raman, Online, Editing". A face button also says the initials it shows ("Priya Raman, PR, Online, Editing"), so its name contains its visible text.',
      items: [
        'Avatars and dots inside each face are decorative; the face carries one combined name.',
        'The "+N" button is named "Show 3 more"; its popover and list are named "Everyone here".',
        'Every string is translatable through `messages` and `presenceMessages`.',
      ],
    },
    responsive: {
      items: [
        'An inline stack sized by the avatars; fits a 390px header.',
        'The popover list scrolls past 20rem and stays within the viewport.',
        'Overlap uses logical margins, so the stack mirrors in right-to-left.',
      ],
    },
    theme: {
      items: [
        'Avatar and PresenceDot tokens; rings use the surface (stack) and raised (popover) colours.',
        'Forced colours: PresenceDot switches to shapes, the "+N" button gets a border, focus uses Highlight.',
        'The popover fade follows reduced motion through Popover.',
      ],
    },
    examples: {
      items: [
        { title: 'In a document header', render: () => <PresenceAvatars people={PEOPLE} /> },
        {
          title: 'Clickable, larger',
          render: () => (
            <PresenceAvatars
              people={PEOPLE.slice(0, 5)}
              size="md"
              max={3}
              onPersonClick={() => {}}
            />
          ),
        },
        {
          title: 'Just one person',
          render: () => <PresenceAvatars people={PEOPLE.slice(0, 1)} />,
        },
      ],
    },
    doDont: {
      do: [
        'Put the current person last or leave them out; others are the news.',
        'Pass `activity` when people do different things (editing, viewing).',
        'Remove people who left instead of marking them offline, unless "recently here" matters.',
      ],
      dont: [
        'Use it for static membership lists.',
        'Rely on the dot colour to explain presence.',
        'Show more than a handful of faces before "+N".',
      ],
    },
    related: {
      items: [
        { id: 'avatar', reason: 'The faces, and AvatarGroup for static lists.' },
        { id: 'presence-dot', reason: 'The status dot on each face.' },
        { id: 'typing-indicator', reason: 'Who is typing right now.' },
        { id: 'comment-thread', reason: 'Discussion on the same document.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['avatar', 'popover', 'presence-dot'],
    },
    testing: {
      unit: [
        'Group name with count; each person named with presence, custom status and activity; dots decorative; no axe violations.',
        '"+N" past `max` opens a named list of everyone; Escape returns focus.',
        'Shows the last person instead of "+1".',
        '`onPersonClick` from the stack (keyboard) and from the list (closes it).',
        'Translated messages and presence words; size.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
