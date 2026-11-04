import { useMemo, useState } from 'react';
import {
  SupportPanel,
  type SupportChatClient,
  type SupportChatEvent,
} from '@rookcom/monarch-react/shell';
import { Button } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

/** Docs only: an in-memory client that answers every message after a moment. */
function useDemoClient(): SupportChatClient {
  return useMemo(() => {
    const listeners = new Set<(e: SupportChatEvent) => void>();
    let n = 0;
    const emit = (e: SupportChatEvent) => listeners.forEach((fn) => fn(e));
    return {
      connect: async () => ({
        messages: [
          {
            id: 'm1',
            author: 'system',
            text: 'Conversation started',
            createdAt: '2026-11-04T09:58:00Z',
          },
          {
            id: 'm2',
            author: 'agent',
            authorName: 'Sam',
            text: 'Hi! What can we help you with today?',
            createdAt: '2026-11-04T09:58:00Z',
          },
        ],
        availability: { online: true, expectedReply: 'Typically replies in a few minutes' },
      }),
      send: async (text) => {
        n += 1;
        setTimeout(() => emit({ type: 'typing', typing: true, name: 'Sam' }), 400);
        setTimeout(
          () =>
            emit({
              type: 'message',
              message: {
                id: `r${n}`,
                author: 'agent',
                authorName: 'Sam',
                text: 'Thanks, looking into it now.',
                createdAt: new Date().toISOString(),
              },
            }),
          1600,
        );
        return { id: `v${n}`, author: 'visitor', text, createdAt: new Date().toISOString() };
      },
      subscribe: (fn) => {
        listeners.add(fn);
        return () => listeners.delete(fn);
      },
    };
  }, []);
}

const utcTime = (d: Date) => `${d.toISOString().slice(11, 16)} UTC`;

function Basic() {
  const client = useDemoClient();
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button
        aria-expanded={open}
        aria-controls={open ? 'support-demo' : undefined}
        onClick={() => setOpen(true)}
      >
        Help &amp; support
      </Button>
      <SupportPanel
        id="support-demo"
        open={open}
        onOpenChange={setOpen}
        client={client}
        formatTime={utcTime}
        articles={
          <ul style={{ margin: 0, paddingLeft: 'var(--mn-space-5)', lineHeight: 1.9 }}>
            <li>
              <a href="#getting-started">Getting started</a>
            </li>
            <li>
              <a href="#invite-your-team">Invite your team</a>
            </li>
            <li>
              <a href="#billing-questions">Billing questions</a>
            </li>
          </ul>
        }
        contacts={[
          {
            label: 'Email support',
            href: 'mailto:support@example.com',
            description: 'support@example.com',
          },
        ]}
        status={{
          href: 'https://status.example.com',
          tone: 'operational',
          text: 'All systems normal',
        }}
        helpHref="https://help.example.com"
      />
    </>
  );
}

function HelpOnly() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button variant="ghost" onClick={() => setOpen(true)}>
        Get help
      </Button>
      <SupportPanel
        open={open}
        onOpenChange={setOpen}
        contacts={[
          {
            label: 'Email support',
            href: 'mailto:support@example.com',
            description: 'support@example.com',
          },
        ]}
        status={{ href: 'https://status.example.com' }}
        messages={{ description: 'Find an answer or get in touch.' }}
      />
    </>
  );
}

export default {
  id: 'support-panel',
  title: 'SupportPanel',
  summary:
    'The rail’s Help & support panel: in-app support chat, help articles, contact options and the status page.',
  sections: {
    purpose: {
      summary:
        'Every Rook app offers help from the rail’s "Help & support" item: a side panel with chat to the support team, help articles and other ways to reach them. SupportPanel is that panel on Sheet. The chat runs through the product’s Support Chat API client; Monarch makes no network calls.',
      useWhen: [
        'Once per app, opened from RookRail’s `support` entry (`support.onOpen`, `open`, `panelId`).',
        'Help-only (no `client`) where chat is not offered yet.',
      ],
      avoidWhen: [
        'Chat between colleagues: that is ChatDock (Rook Chat), bottom-right.',
        'Contextual help on one field or page: use a Tooltip, Popover or inline help text.',
      ],
    },
    designPrinciples: {
      items: [
        'Nothing before it is asked for: the client connects on the first open, never at page load.',
        'Injected, not built in: the product implements `SupportChatClient` over its Support Chat API with its own identity and single-flight session refresh.',
        'Survives closing: the conversation, the draft and the live stream are kept while the panel is closed.',
        'Never empty: connecting, a timeout, a failure, an ended session and an away team each have their own message, with Try again where it helps.',
        'From the rail, beside it: a left sheet that can sit next to the rail (`inset`), never a bottom-right bubble, which belongs to Rook Chat.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the sheet. Built on Sheet and Tabs from `@rookcom/monarch-react`.',
        'Also exported: `useSupportChat` (the chat state machine, for a custom surface on the same client) and `SUPPORT_MESSAGE_MAX` (5000).',
        '`SupportChatClient` is `{ connect({ signal }), send(text, { clientMessageId }), subscribe?(listener), markRead?(messageId) }`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`open` + `onOpenChange` (controlled, as the rail needs) or `defaultOpen`.',
        '`chat` replaces the built-in chat with your own surface (for example a widget host element); `client` is then not used.',
        '`status.tone` needs `status.text`: colour alone says nothing.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'content',
          values: ['chat + help (tabs)', 'chat only', 'help only'],
          description: 'Tabs appear when there is both a chat and help content.',
        },
        {
          prop: 'side',
          values: ['left', 'right'],
          description:
            'Left (default) for the rail; `inset` keeps it clear of the rail from 48rem up.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Connecting',
          description: 'A status while `connect` runs (15 seconds at most by default).',
        },
        { name: 'Ready', description: 'The conversation log, availability line and composer.' },
        {
          name: 'Sending / Not sent',
          attribute: 'data-pending',
          description:
            'A message shows "Sending…" until stored; a failure says "Not sent." with Try again (same client id).',
        },
        { name: 'Typing', description: '"Sam is typing…" in a status line.' },
        { name: 'Away', description: 'Availability offline: the team will reply here later.' },
        {
          name: 'Error / ended',
          description:
            'Timeout, failure, ended session or unavailable: an alert, with Try again except for unavailable.',
        },
      ],
    },
    interaction: {
      items: [
        'Opening connects once; Try again connects again (the previous attempt is aborted).',
        'Enter sends, Shift+Enter adds a line; the composer keeps focus after sending.',
        'Live events add replies (deduplicated by id), typing, availability and the end of a session.',
        'While the chat is visible, the newest support message is reported with `markRead`.',
        'The Help tab holds the articles slot and the contacts; the footer keeps the status page and Help Center one click away.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Escape',
          action: 'Close the panel; focus returns to what opened it (the rail item).',
        },
        { keys: 'Tab / Shift+Tab', action: 'Move within the panel (focus is trapped while open).' },
        { keys: '← / →', action: 'Switch between Chat and Help.' },
        { keys: 'Enter', action: 'Send the message.' },
        { keys: 'Shift+Enter', action: 'New line in the message.' },
      ],
    },
    accessibility: {
      role: 'dialog (modal Sheet) labelled by its title and described by its description',
      name: '"Help & support" (`messages.title`).',
      items: [
        'Focus moves into the panel on open and back to the opener on close, also when the opener is the rail item (not a SheetTrigger); without it, the visible `[data-rook-support-trigger]`.',
        'The conversation is a focusable `role="log"` (polite), so new replies are announced and the history can be scrolled by keyboard.',
        'Errors are alerts; connecting and typing are status messages.',
        'External links say they open in a new tab; the status dot is decorative and the state is in the link text.',
        'Security: Monarch makes no requests. Messages are rendered as text, never as HTML, and are capped at 5000 characters. The client owns identity, tokens and refresh; never pass a secret key to the browser.',
      ],
    },
    responsive: {
      items: [
        'A 28rem side sheet from 48rem up; below that the full screen width, with safe-area insets.',
        'Messages wrap long words; the composer grows to 10rem, then scrolls.',
      ],
    },
    theme: {
      items: [
        'Sheet surface tokens; your messages use the accent solid colours, support’s the muted surface.',
        'The sheet’s slide-in and the contact hover are removed under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Chat, help articles, contacts and status',
          render: () => <Basic />,
          code: `const [open, setOpen] = useState(false);

<RookRail support={{ onOpen: () => setOpen(true), open, panelId: 'support', unread }} … />
<SupportPanel
  id="support"
  open={open}
  onOpenChange={setOpen}
  client={supportClient}          // the product's Support Chat API client
  inset="var(--mn-layout-rail-collapsed)"
  articles={<HelpArticles />}
  contacts={[{ label: 'Email support', href: 'mailto:support@example.com' }]}
  status={{ href: statusUrl, tone: 'operational', text: 'All systems normal' }}
  helpHref={helpCenterUrl}
/>`,
        },
        {
          title: 'Help only',
          render: () => <HelpOnly />,
          code: `<SupportPanel
  contacts={[{ label: 'Email support', href: 'mailto:support@example.com' }]}
  status={{ href: statusUrl }}
/>`,
        },
      ],
    },
    doDont: {
      do: [
        'Open it from the rail and pass `open` and `panelId` back to the rail for `aria-expanded` and `aria-controls`.',
        'Implement `connect` with the abort signal, and `send` idempotently by `clientMessageId`.',
        'Keep the panel mounted for the whole session so the conversation survives navigation.',
      ],
      dont: [
        'Do not put a support bubble bottom-right.',
        'Do not connect before the first open.',
        'Do not show raw API errors in the chat; reject and let the panel explain.',
      ],
    },
    related: {
      items: [
        { id: 'rook-rail', reason: 'Its Help & support entry opens the panel.' },
        { id: 'sheet', reason: 'The side panel it is built on.' },
        { id: 'chat-dock', reason: 'Chat between colleagues.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'sheet', 'spinner', 'tabs', 'text-field'],
    },
    testing: {
      unit: [
        'Connects on first open only; labelled, described dialog; conversation log; availability; markRead.',
        'Enter sends, Shift+Enter is a new line; optimistic then stored.',
        'A failed send is marked and retried with the same client id.',
        'Live replies (deduplicated), typing, away and an ended session.',
        'Timeout aborts and offers Try again; load failure.',
        'Conversation and draft kept across close and reopen; focus returns to the opener.',
        'Chat and Help tabs (arrow keys), articles, contacts, status with text, Help Center.',
        'Own chat surface; help only; defaultOpen, id, inset and translation; SSR when closed.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/support-panel (and the open panel in visual.spec.ts)',
        'Cross-browser axe: /components/support-panel',
      ],
    },
  },
} satisfies ComponentDocs;
