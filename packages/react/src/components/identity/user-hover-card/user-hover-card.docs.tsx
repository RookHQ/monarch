import { Button } from '@rookcom/monarch-react';
import { UserHoverCard, type UserHoverCardUser } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const PERSON: UserHoverCardUser = {
  id: 'person-1',
  name: 'Sam Rivera',
  handle: '@sam',
  title: 'Design lead',
  status: 'online',
  statusText: 'Working from the north office',
};

export default {
  id: 'user-hover-card',
  title: 'UserHoverCard',
  summary: 'A profile preview that opens on hover or keyboard focus of a person’s name.',
  sections: {
    purpose: {
      summary:
        'Names and avatars appear everywhere (comments, assignees, member lists), and people want to know who someone is without leaving the page. UserHoverCard wraps the existing name link or button and shows the person’s photo, name, handle, title, status, optional details and actions after a short hover, or straight away on keyboard focus. Needed by: Rook ID (members), and every app that shows people in lists or activity feeds (Rook Stride, Rook Tickets, Rook Chat).',
      useWhen: [
        'A person’s name or avatar in a list, comment or activity line, where a quick preview helps.',
        'Mentions in rich text that link to a profile.',
      ],
      avoidWhen: [
        'The only way to reach an action: hover cards are a shortcut, so keep the action on the profile page too.',
        'Touch-first screens where nobody hovers: link to the profile instead.',
        'Long or interactive content such as a form: use Popover or a Dialog.',
      ],
    },
    designPrinciples: {
      items: [
        'The trigger stays the product’s own element: clicking a name still goes where it went.',
        'Opening never steals focus; keyboard users press Tab to reach the card’s actions.',
        'A delay on hover so the card does not flash while the pointer crosses a list.',
        'Data in, no fetching: the product passes the person and can load more on `onOpenChange`.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`user` is `{ name, avatarSrc?, id?, handle?, email?, title?, status?, statusText? }`.',
        'Built on HoverCard (which sits on the Popover surface). Unlike a plain HoverCard it opens at once on keyboard focus, labels the card with `messages.cardLabel` and describes the trigger by the name block only.',
        'Migrating from shadcn/ui (`components/ui/hover-card.tsx` in Rook ID): `<HoverCard openDelay closeDelay><HoverCardTrigger asChild>{link}</HoverCardTrigger><HoverCardContent align sideOffset>…</HoverCardContent></HoverCard>` becomes `<UserHoverCard user={…} openDelay closeDelay align actions={…}>{link}</UserHoverCard>`; the hand-built avatar, name and bio markup inside the content becomes the `user` fields and `details`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'side',
          values: ['top', 'right', 'bottom', 'left'],
          description: 'Which side of the trigger the card opens on. Default `bottom`.',
        },
        {
          prop: 'align',
          values: ['start', 'center', 'end'],
          description: 'Alignment along the trigger. Default `start`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Open',
          attribute: 'data-state="open"',
          description: 'On the card while it is shown (from HoverCard).',
        },
        {
          name: 'Loading',
          attribute: 'data-loading',
          description: 'A "Loading profile" line replaces the details while the product fetches.',
        },
      ],
    },
    interaction: {
      items: [
        'Pointer resting on the trigger for `openDelay` ms (default 500) opens the card.',
        'Moving the pointer from the trigger onto the card keeps it open; leaving both closes it after `closeDelay` ms.',
        'Keyboard focus on the trigger opens it at once; blur closes it unless focus moved into the card.',
        'Touch pointers never open it by hover, so a tap on a link still navigates.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab (onto the trigger)', action: 'Opens the card; focus stays on the trigger.' },
        {
          keys: 'Tab (while open)',
          action:
            'Moves into the card’s actions when it has any; past the last one, focus returns to the trigger and the card closes.',
        },
        { keys: 'Shift+Tab (first action)', action: 'Returns to the trigger.' },
        { keys: 'Escape', action: 'Closes the card and keeps focus on the trigger.' },
      ],
    },
    accessibility: {
      role: 'dialog (non-modal)',
      name: '"Profile of {name}" (`messages.cardLabel`).',
      items: [
        'While open, the trigger is described by the name, handle, title and status lines.',
        'The avatar is decorative because the name is right beside it.',
        'Focus is never moved on open, and never trapped.',
      ],
    },
    responsive: {
      items: [
        'The card is at most 20rem wide and never wider than the viewport minus 2rem.',
        'Long names, handles and titles wrap.',
        'Actions wrap onto several lines and are 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Popover surface: raised background, subtle border, large shadow.',
        'Secondary lines use the muted text colour; the actions row has a subtle top border.',
        'Forced-colors mode draws the card border and divider in system colours.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Name link with actions',
          render: () => (
            <p>
              Assigned to{' '}
              <UserHoverCard
                user={PERSON}
                actions={
                  <>
                    <Button size="sm" variant="primary">
                      Message
                    </Button>
                    <Button size="sm" variant="ghost">
                      View profile
                    </Button>
                  </>
                }
              >
                <a href="#profile">Sam Rivera</a>
              </UserHoverCard>
            </p>
          ),
        },
        {
          title: 'With details',
          render: () => (
            <UserHoverCard
              user={{ name: 'Priya Natarajan', email: 'priya@example.com', title: 'Engineer' }}
              details="Platform team · Local time 14:20"
            >
              <Button variant="ghost">Priya Natarajan</Button>
            </UserHoverCard>
          ),
        },
        {
          title: 'Loading the profile on open',
          render: () => (
            <UserHoverCard user={{ name: 'Alex Kim' }} loading>
              <a href="#alex">Alex Kim</a>
            </UserHoverCard>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Wrap the element that already shows the name, so its click behaviour stays the same.',
        'Keep the card short: who they are, how to reach them.',
        'Fetch extra details on `onOpenChange(true)` and pass `loading` meanwhile.',
      ],
      dont: [
        'Do not put the only copy of an action in the card.',
        'Do not wrap a non-focusable element such as a plain `span`: keyboard users could never open it.',
      ],
    },
    related: {
      items: [
        { id: 'hover-card', reason: 'The general hover preview it is built on.' },
        { id: 'popover', reason: 'The floating surface under HoverCard.' },
        { id: 'avatar', reason: 'The photo with initials fallback and presence dot.' },
        { id: 'member-row', reason: 'A full row for a person in a members list.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['avatar', 'hover-card'] },
    testing: {
      unit: [
        'Opens on keyboard focus without moving focus; Escape closes it; axe passes.',
        'Tab moves into the actions and out again to the trigger.',
        'Hover opens after the delay; moving onto the card keeps it open; leaving closes it.',
        'Loading line and translated messages.',
        'Details and controlled open state.',
        'Keyboard focus opens at once despite the hover delay; a tap does not open it.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
