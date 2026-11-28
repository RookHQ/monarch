import {
  AiSharedSession,
  type AiSharedSessionParticipant,
  type AiSharedSessionRequest,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './ai-shared-session.fixture.json';

const participants = fixture.data.participants as AiSharedSessionParticipant[];
const queue = fixture.data.queue as AiSharedSessionRequest[];
const asViewer = participants.map((p) => ({ ...p, you: p.id === 'ana' }));
const asEditor = participants.map((p) => ({ ...p, you: p.id === 'leo' }));

export default {
  id: 'ai-shared-session',
  title: 'AiSharedSession',
  summary:
    'Presence and turn-taking bar for an AI conversation shared by several people and agents.',
  sections: {
    purpose: {
      summary:
        'Shows, in one compact bar, who is in a shared AI conversation, who is typing, whose request the assistant is answering now, what is waiting, and whether you are allowed to send, so nobody talks over the assistant or wonders why their request has not started.',
      useWhen: [
        'Several people work in the same AI conversation at once and take turns asking.',
        'Requests queue up behind the one being answered and people need to see, withdraw or reorder them.',
        'Some people can only watch, and should be told so before they try to type.',
      ],
      avoidWhen: [
        'Only agents take part and you want what each one is doing: use AiMultiAgentRoster.',
        'You only need who is viewing a page, with no AI turn-taking: use PresenceAvatars.',
        'One person talks to one assistant: there is no turn to take, so show nothing.',
      ],
    },
    designPrinciples: {
      items: [
        'One line in plain words says what is happening: "Assistant is answering Leo Park’s request · 2 requests waiting".',
        'People are round avatars and agents are square agent pictures, so an agent is never mistaken for a person.',
        'Presence is a word beside a shape (filled, half, ring), never colour alone.',
        'Every action is an explicit, labelled button. Nothing is sent, withdrawn or reordered without one.',
        'What you may not do is not shown: editors see Withdraw on their own requests only, viewers see no queue buttons.',
        'The component holds no session data and no network code: the product passes the state in and applies each callback.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        'Mark one person in `participants` with `you: true`. Their `role` decides what is shown: `owner` can reorder and remove any request, `editor` can withdraw their own, `viewer` gets the read-only notice.',
        '`aiSharedSessionStatus(session, { messages, locale })` returns the clauses, the visible line and the announcement; `aiSharedSessionStatusLine` returns only the line. Use them to show the same words elsewhere, such as a window title.',
        '`groupAiSharedSessionParticipants(participants)` returns `{ people, agents, you }`, with you first among the people.',
        '`onReorder(orderedIds, move)` gives the request ids in their new order and `{ id, from, to }`. The queue is controlled: apply the change and pass the new `queue`.',
        '`resolveAiSharedSessionMessages` merges a partial `messages` object with `AI_SHARED_SESSION_MESSAGES`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: [
        'No visual variants. What appears depends on your role and on which callbacks you pass.',
      ],
    },
    states: {
      items: [
        {
          name: 'Your role',
          attribute: 'data-role="owner | editor | viewer"',
          description:
            'On the root, from the participant marked `you`. Absent when nobody is marked.',
        },
        {
          name: 'Answering',
          attribute: 'data-answering',
          description:
            'On the root while `answering` is set. The agent’s picture shows its working ring.',
        },
        {
          name: 'Presence',
          attribute: 'data-presence="active | idle | away"',
          description:
            'On each avatar, list row and presence shape. Away people are greyed in the stack.',
        },
        {
          name: 'Queue expanded',
          attribute: 'aria-expanded on the Queue button',
          description: 'The list of waiting requests is shown under the bar.',
        },
        {
          name: 'Own request',
          attribute: 'data-own',
          description: 'On queue rows that are your own requests.',
        },
        {
          name: 'Following',
          attribute: 'aria-pressed on a Follow button',
          description: 'You are following that person.',
        },
        {
          name: 'Quiet',
          attribute: 'data-quiet on the status line',
          description:
            'Nobody is being answered, nobody is active and nothing waits: "No requests in progress".',
        },
      ],
    },
    interaction: {
      items: [
        'The button after the avatars ("+3", or a chevron when everyone fits) opens the full list, grouped into People and Agents, with each one’s role, presence and activity.',
        'Follow, beside every other person in the list, is a toggle; `onFollowChange` gets the person’s id and the new value.',
        'The Queue button expands the waiting requests in order. It appears only while something waits.',
        'Withdraw appears on your own requests; an owner also gets Move up, Move down and Remove on the others.',
        'After a move, focus stays on the request that moved; after a withdraw or remove, focus returns to the Queue button.',
        'Invite calls `onInvite`; the product opens its own invite flow.',
        'Viewers see "You can watch but not send" and, with `onRequestAccess`, a Request access button. Pass `accessRequested` once it was sent.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves between the list button, Queue, Invite and the queue buttons.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused button or toggles Follow.' },
        { keys: 'Escape', action: 'Closes the participant list and returns focus to its button.' },
      ],
      notes: [
        'Reordering needs no drag: Move up and Move down are buttons, disabled at the ends of the queue.',
      ],
    },
    accessibility: {
      role: 'group',
      name: '"Shared session" (`messages.label`)',
      items: [
        'The avatar stack is a list named "3 people and 2 agents here"; each face is named with the person’s name, role or "AI agent", presence and activity.',
        'The full list is a dialog with a People group and an Agents group. Presence is written out beside its shape.',
        'A visually hidden polite status region announces who is being answered and how many requests wait. It never speaks on first render, at most once every `announceInterval` (4 seconds by default), and only the latest text.',
        'Typing and speaking are shown but never announced: they change too often, and speech is already heard.',
        'Queue buttons are named with their request, such as "Move up: request 2, from Leo Park", and a move is confirmed with "… moved to position 1 of 3".',
        'The Queue button uses `aria-expanded` and `aria-controls`; Follow uses `aria-pressed`.',
        'Targets are at least 24px, and 44px on touch screens.',
      ],
    },
    responsive: {
      items: [
        'The bar wraps. Under 30rem (of the screen, or of the AI surface around it) the status line takes its own row below the avatars and buttons.',
        'Queue previews are cut to one line with an ellipsis; their buttons drop under the text when narrow.',
        'The participant list stays inside the viewport and scrolls when long.',
      ],
    },
    theme: {
      items: [
        'Surface, border and text use the neutral Monarch tokens; the answering clause is emphasised by weight, not colour.',
        'Your own requests and a pressed Follow use `--mn-color-ai-subtle` with `--mn-color-ai-strong`.',
        'The active presence shape uses `--mn-color-success-solid`; idle and away use `--mn-color-text-muted`.',
        'In forced colours the shapes are drawn in CanvasText and a pressed Follow uses Highlight.',
        'The only motion is the Queue chevron turning, which is removed under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Owner: answering, typing and a queue to manage',
          stack: true,
          render: () => (
            <AiSharedSession
              participants={participants}
              answering={fixture.data.answering}
              queue={queue}
              defaultQueueOpen
              onInvite={() => {}}
              onFollowChange={() => {}}
              onWithdraw={() => {}}
              onReorder={() => {}}
              onRemove={() => {}}
            />
          ),
        },
        {
          title: 'Editor: can withdraw their own request',
          stack: true,
          render: () => (
            <AiSharedSession
              participants={asEditor}
              answering={{
                participantId: 'assistant',
                forParticipantId: 'leo',
                label: 'Compare the two drafts',
              }}
              queue={queue}
              defaultQueueOpen
              onWithdraw={() => {}}
            />
          ),
        },
        {
          title: 'Viewer: read-only, with Request access',
          stack: true,
          render: () => (
            <AiSharedSession
              participants={asViewer}
              answering={{ participantId: 'assistant', forParticipantId: 'maya' }}
              queue={queue.slice(0, 1)}
              onRequestAccess={() => {}}
            />
          ),
        },
        {
          title: 'Many participants',
          stack: true,
          render: () => (
            <div style={{ maxWidth: '24rem' }}>
              <AiSharedSession participants={participants} max={3} onInvite={() => {}} />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Place it directly above the conversation or the prompt input, where the turn is taken.',
        'Mark exactly one person as `you`, and disable your own prompt input for viewers as well.',
        'Apply queue callbacks straight away, or keep the old queue and explain why the change was refused.',
        'Send presence changes at a calm rate; the bar shows every update but only announces the ones that matter.',
      ],
      dont: [
        'Do not rely on hiding buttons for security: check roles on the server too.',
        'Do not put the whole request text in `preview`; one line is enough to recognise it.',
        'Do not use it for a list of agents with their tasks: that is AiMultiAgentRoster.',
      ],
    },
    related: {
      items: [
        { id: 'ai-multi-agent-roster', reason: 'What each agent in a task is doing.' },
        { id: 'presence-avatars', reason: 'Who is viewing a page, without AI turn-taking.' },
        { id: 'ai-agent-avatar', reason: 'The agent picture used in the stack and the list.' },
        { id: 'ai-prompt-input', reason: 'Where a request is written; disable it for viewers.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'avatar', 'popover', 'ai-agent-avatar'],
    },
    testing: {
      unit: [
        'groupAiSharedSessionParticipants splits people (you first) and agents; the status helpers build the line, say "your request", join two names, count more, skip your own activity and fall back for unknown ids.',
        'Named group, avatar stack with a full name per face, status line, and silence on first render.',
        'The "+N" button opens the grouped list with role and presence in text and closes on Escape; controlled and uncontrolled.',
        'Follow toggles by click and keyboard, controlled and uncontrolled; none for yourself or agents.',
        'The queue expands, controlled and uncontrolled; editors withdraw only their own; owners move up and down (focus follows, the move is announced, ends are disabled) and remove (focus returns).',
        'Viewer notice, Request access and the requested state; Invite; the quiet line.',
        'Announcements are polite, throttled to the latest text and never include typing; `announce={false}` removes the region.',
        'Translated strings with partial groups; server render.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
