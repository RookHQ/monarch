'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type FormEvent,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { Button } from '../../../components/actions/button/index.js';
import {
  Sheet,
  SheetBody,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from '../../../components/overlays/sheet/index.js';
import { Spinner } from '../../../components/feedback/spinner/index.js';
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from '../../../components/navigation/tabs/index.js';
import { Textarea } from '../../../components/forms/text-field/index.js';
import { cx } from '../../../internal/cx.js';
import { ExternalGlyph } from '../../../internal/shell/icons.js';
import {
  BookGlyph,
  MailGlyph,
  PulseGlyph,
  SendGlyph,
} from '../../../internal/shell/panel-icons.js';
import {
  SUPPORT_MESSAGE_MAX,
  useSupportChat,
  type SupportChatClient,
  type SupportChatEntry,
} from './support-panel.client.js';

/** A way to reach support besides chat: email, phone, a contact form. */
export interface SupportContact {
  label: string;
  href: string;
  /** e.g. "support@example.com" or "Mon–Fri, 9–6". */
  description?: string | undefined;
  /** Decorative. Default: a mail glyph. */
  icon?: ReactNode;
  /** Open in a new tab. Default: true for http(s) links. */
  external?: boolean | undefined;
}

/** The status page link, with an optional current state supplied by the product. */
export interface SupportStatus {
  href: string;
  /** Link text. Default "System status". */
  label?: string | undefined;
  /** The current state, if the product knows it. Shown as a coloured dot plus `text`. */
  tone?: 'operational' | 'degraded' | 'outage' | undefined;
  /** e.g. "All systems normal". Required with `tone`, since colour alone says nothing. */
  text?: string | undefined;
}

export interface SupportPanelMessages {
  title: string;
  description: string;
  close: string;
  chatTab: string;
  helpTab: string;
  connecting: string;
  conversation: string;
  emptyChat: string;
  you: string;
  support: string;
  composerLabel: string;
  composerPlaceholder: string;
  send: string;
  sending: string;
  notSent: string;
  resend: string;
  typing: (name?: string) => string;
  offline: string;
  loadFailed: string;
  timedOut: string;
  sessionEnded: string;
  unavailable: string;
  retry: string;
  articles: string;
  contact: string;
  status: string;
  helpCenter: string;
  newTab: string;
}

const DEFAULT_MESSAGES: SupportPanelMessages = {
  title: 'Help & support',
  description: 'Chat with the support team.',
  close: 'Close Help & support',
  chatTab: 'Chat',
  helpTab: 'Help',
  connecting: 'Connecting to support…',
  conversation: 'Conversation with support',
  emptyChat: 'Ask a question and the team will reply here.',
  you: 'You',
  support: 'Support',
  composerLabel: 'Message',
  composerPlaceholder: 'Write a message…',
  send: 'Send',
  sending: 'Sending…',
  notSent: 'Not sent.',
  resend: 'Try again',
  typing: (name) => (name ? `${name} is typing…` : 'Support is typing…'),
  offline: 'The team is away. Leave a message and they’ll reply here.',
  loadFailed: 'We can’t load chat right now.',
  timedOut: 'Chat is taking too long to load.',
  sessionEnded: 'Your session ended. Sign in again to keep chatting.',
  unavailable: 'Chat isn’t available here.',
  retry: 'Try again',
  articles: 'Help articles',
  contact: 'Other ways to reach us',
  status: 'System status',
  helpCenter: 'Help Center',
  newTab: 'opens in a new tab',
};

export interface SupportPanelProps {
  /** Open (controlled). Wire to the rail's `support.open`. */
  open?: boolean | undefined;
  defaultOpen?: boolean | undefined;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Panel id, for the rail's `support.panelId` (`aria-controls`). */
  id?: string | undefined;
  /** The product's Support Chat API client. With it, the panel has a Chat tab. */
  client?: SupportChatClient | undefined;
  /** Your own chat surface instead of the built-in one (e.g. a widget host element). */
  chat?: ReactNode;
  /** Help articles (search, popular articles): the Help tab's first section. */
  articles?: ReactNode;
  /** Other ways to reach support. */
  contacts?: SupportContact[] | undefined;
  /** The status page. */
  status?: SupportStatus | undefined;
  /** "Help Center" link in the footer. */
  helpHref?: string | undefined;
  /** Give up connecting after this long. Default 15000 ms; 0 waits forever. */
  connectTimeout?: number | undefined;
  /** Edge the panel opens from. Default `left` (beside the rail). */
  side?: 'left' | 'right' | undefined;
  /**
   * Distance from the edge from 48rem up, so the panel opens beside the rail
   * instead of over it, e.g. `var(--mn-layout-rail-collapsed)`. Default 0.
   */
  inset?: string | undefined;
  /** Formats message times. Default: the browser's short time. */
  formatTime?: ((date: Date) => string) | undefined;
  /** The tab shown first. Default `chat` when there is one. */
  defaultTab?: 'chat' | 'help' | undefined;
  className?: string | undefined;
  /** Interface text, for translation. */
  messages?: Partial<SupportPanelMessages> | undefined;
}

function external(href: string): boolean {
  return /^https?:\/\//i.test(href);
}

function NewTabLink({
  href,
  className,
  newTab,
  external: ext = external(href),
  children,
}: {
  href: string;
  className: string;
  newTab: string;
  external?: boolean | undefined;
  children: ReactNode;
}) {
  return (
    <a
      href={href}
      className={className}
      {...(ext ? { target: '_blank', rel: 'noopener noreferrer' } : {})}
    >
      {children}
      {ext ? <span className="mn-visually-hidden"> ({newTab})</span> : null}
    </a>
  );
}

const defaultTime = (d: Date) => {
  try {
    return new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: '2-digit' }).format(d);
  } catch {
    return d.toISOString().slice(11, 16);
  }
};

/**
 * The rail's "Help & support" panel: a side sheet with in-app support chat
 * (through the product's Support Chat API client), help articles, contact
 * options and the status page. It is a modal dialog (Sheet): focus moves in,
 * Escape closes, focus returns. The conversation is kept while the panel is
 * closed. Monarch makes no network calls; the client does.
 */
export const SupportPanel = forwardRef<HTMLDivElement, SupportPanelProps>(function SupportPanel(
  {
    open: openProp,
    defaultOpen = false,
    onOpenChange,
    id,
    client,
    chat,
    articles,
    contacts,
    status,
    helpHref,
    connectTimeout = 15_000,
    side = 'left',
    inset,
    formatTime = defaultTime,
    defaultTab,
    className,
    messages: messagesProp,
  },
  ref,
) {
  const messages = useMemo(() => ({ ...DEFAULT_MESSAGES, ...messagesProp }), [messagesProp]);
  const autoId = useId();
  const panelId = id ?? `${autoId}-support`;
  const [innerOpen, setInnerOpen] = useState(defaultOpen);
  const open = openProp ?? innerOpen;
  const setOpen = (next: boolean) => {
    if (openProp === undefined) setInnerOpen(next);
    onOpenChange?.(next);
  };

  // Sheet returns focus to a SheetTrigger; the rail item is not one, so the
  // element focused before opening is remembered and focused again on close.
  const returnTo = useRef<HTMLElement | null>(null);

  const hasChat = Boolean(chat || client);
  const hasHelp = Boolean(articles || contacts?.length || status);
  const [tab, setTab] = useState<'chat' | 'help'>(defaultTab ?? (hasChat ? 'chat' : 'help'));
  const activeTab = hasChat && hasHelp ? tab : hasChat ? 'chat' : 'help';

  const chatCtl = useSupportChat(chat ? undefined : client, { timeout: connectTimeout });
  const { phase } = chatCtl.state;
  // Connect on first open (never before): nothing is fetched until the person asks for help.
  const { connect } = chatCtl;
  useEffect(() => {
    if (open && client && !chat && phase === 'idle') connect();
  }, [open, client, chat, phase, connect]);
  // Kept here, outside the sheet, so a half-written message survives closing the panel.
  const [draft, setDraft] = useState('');

  const chatView =
    chat ??
    (client ? (
      <SupportChat
        ctl={chatCtl}
        messages={messages}
        formatTime={formatTime}
        visible={open && activeTab === 'chat'}
        draft={draft}
        onDraftChange={setDraft}
      />
    ) : null);

  const help = (
    <div className="mn-support-panel__help">
      {articles ? (
        <section className="mn-support-panel__section" aria-labelledby={`${panelId}-articles`}>
          <h3 id={`${panelId}-articles`} className="mn-support-panel__section-title">
            {messages.articles}
          </h3>
          {articles}
        </section>
      ) : null}
      {contacts?.length ? (
        <section className="mn-support-panel__section" aria-labelledby={`${panelId}-contact`}>
          <h3 id={`${panelId}-contact`} className="mn-support-panel__section-title">
            {messages.contact}
          </h3>
          <ul className="mn-support-panel__contacts">
            {contacts.map((c) => (
              <li key={`${c.label}\u0000${c.href}`}>
                <NewTabLink
                  href={c.href}
                  className="mn-support-panel__contact"
                  newTab={messages.newTab}
                  external={c.external}
                >
                  <span className="mn-support-panel__contact-icon" aria-hidden>
                    {c.icon ?? <MailGlyph />}
                  </span>
                  <span className="mn-support-panel__contact-text">
                    <span className="mn-support-panel__contact-label">{c.label}</span>
                    {c.description ? (
                      <span className="mn-support-panel__contact-description">{c.description}</span>
                    ) : null}
                  </span>
                </NewTabLink>
              </li>
            ))}
          </ul>
        </section>
      ) : null}
    </div>
  );

  const footer =
    status || helpHref ? (
      <div className="mn-support-panel__footer">
        {status ? (
          <NewTabLink
            href={status.href}
            className="mn-support-panel__footer-link"
            newTab={messages.newTab}
          >
            {status.tone ? (
              <span className="mn-support-panel__status-dot" data-tone={status.tone} aria-hidden />
            ) : (
              <PulseGlyph />
            )}
            <span>
              {`${status.label ?? messages.status}${status.tone && status.text ? `: ${status.text}` : ''}`}
            </span>
          </NewTabLink>
        ) : null}
        {helpHref ? (
          <NewTabLink
            href={helpHref}
            className="mn-support-panel__footer-link"
            newTab={messages.newTab}
          >
            <BookGlyph />
            <span>{messages.helpCenter}</span>
            {external(helpHref) ? <ExternalGlyph /> : null}
          </NewTabLink>
        ) : null}
      </div>
    ) : null;

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetContent
        ref={ref}
        id={panelId}
        side={side}
        size="md"
        closeLabel={messages.close}
        className={cx('mn-support-panel', className)}
        style={inset ? ({ '--mn-support-panel-inset': inset } as CSSProperties) : undefined}
        data-tab={activeTab}
        footer={footer}
        onOpenAutoFocus={() => {
          const active = document.activeElement;
          returnTo.current =
            active instanceof HTMLElement && active !== document.body ? active : null;
        }}
        onCloseAutoFocus={(e) => {
          e.preventDefault();
          const back = returnTo.current;
          returnTo.current = null;
          if (back?.isConnected) {
            back.focus();
            return;
          }
          // Opened from a drawer that has closed since, or from code: the visible rail item.
          Array.from(document.querySelectorAll<HTMLElement>('[data-rook-support-trigger]'))
            .find((el) => el.getClientRects().length > 0)
            ?.focus();
        }}
      >
        <SheetHeader>
          <SheetTitle>{messages.title}</SheetTitle>
          <SheetDescription>{messages.description}</SheetDescription>
        </SheetHeader>
        {hasChat && hasHelp ? (
          <Tabs
            value={activeTab}
            onValueChange={(v) => setTab(v === 'help' ? 'help' : 'chat')}
            className="mn-support-panel__tabs"
          >
            <TabsList className="mn-support-panel__tablist">
              <TabsTrigger value="chat">{messages.chatTab}</TabsTrigger>
              <TabsTrigger value="help">{messages.helpTab}</TabsTrigger>
            </TabsList>
            {/* Chat stays mounted on the Help tab so a draft and the live stream survive. */}
            <TabsContent
              value="chat"
              forceMount
              hidden={activeTab !== 'chat'}
              className="mn-support-panel__pane"
            >
              {chatView}
            </TabsContent>
            <TabsContent value="help" className="mn-support-panel__pane" data-scroll="">
              {help}
            </TabsContent>
          </Tabs>
        ) : hasChat ? (
          <div className="mn-support-panel__pane">{chatView}</div>
        ) : (
          <SheetBody>{help}</SheetBody>
        )}
      </SheetContent>
    </Sheet>
  );
});

function SupportChat({
  ctl,
  messages,
  formatTime,
  visible,
  draft,
  onDraftChange: setDraft,
}: {
  ctl: ReturnType<typeof useSupportChat>;
  messages: SupportPanelMessages;
  formatTime: (d: Date) => string;
  visible: boolean;
  draft: string;
  onDraftChange: (draft: string) => void;
}) {
  const { state, send, resend, connect, markRead } = ctl;
  const logRef = useRef<HTMLDivElement>(null);
  const composerRef = useRef<HTMLTextAreaElement>(null);
  const composerId = useId();

  // Newest at the bottom, kept in view as messages arrive.
  useEffect(() => {
    const el = logRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [state.entries.length, state.typing]);

  useEffect(() => {
    if (visible && state.phase === 'ready') markRead();
  }, [visible, state.phase, state.entries, markRead]);

  const submit = (e?: FormEvent) => {
    e?.preventDefault();
    if (state.phase !== 'ready' || !draft.trim()) return;
    send(draft);
    setDraft('');
    composerRef.current?.focus();
  };

  const onKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault();
      submit();
    }
  };

  if (state.phase === 'idle' || state.phase === 'connecting') {
    return (
      <div className="mn-support-panel__state" role="status">
        <Spinner size="md" />
        <span>{messages.connecting}</span>
      </div>
    );
  }

  if (state.phase === 'error' || state.phase === 'ended') {
    const text =
      state.reason === 'session'
        ? messages.sessionEnded
        : state.reason === 'unavailable'
          ? messages.unavailable
          : state.reason === 'timeout'
            ? messages.timedOut
            : messages.loadFailed;
    return (
      <div className="mn-support-panel__state" data-error="">
        <p className="mn-support-panel__state-text" role="alert">
          {text}
        </p>
        {state.reason !== 'unavailable' ? (
          <Button variant="secondary" size="sm" onClick={connect}>
            {messages.retry}
          </Button>
        ) : null}
      </div>
    );
  }

  const offline = state.availability && !state.availability.online;
  const note = offline ? messages.offline : state.availability?.expectedReply;

  return (
    <div className="mn-support-panel__chat">
      {note ? <p className="mn-support-panel__availability">{note}</p> : null}
      <div
        ref={logRef}
        className="mn-support-panel__log"
        role="log"
        aria-live="polite"
        aria-label={messages.conversation}
        // A scrolling history must be reachable by keyboard (WCAG 2.1.1).
        // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
        tabIndex={0}
      >
        {state.entries.length === 0 ? (
          <p className="mn-support-panel__empty">{messages.emptyChat}</p>
        ) : (
          <ol className="mn-support-panel__messages">
            {state.entries.map((m) => (
              <Entry
                key={m.id}
                m={m}
                messages={messages}
                formatTime={formatTime}
                onResend={resend}
              />
            ))}
          </ol>
        )}
      </div>
      <p className="mn-support-panel__typing" role="status">
        {state.typing ? messages.typing(state.typing.name) : null}
      </p>
      <form className="mn-support-panel__composer" onSubmit={submit}>
        <label htmlFor={composerId} className="mn-visually-hidden">
          {messages.composerLabel}
        </label>
        <Textarea
          ref={composerRef}
          id={composerId}
          rows={2}
          value={draft}
          maxLength={SUPPORT_MESSAGE_MAX}
          placeholder={messages.composerPlaceholder}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={onKeyDown}
          className="mn-support-panel__input"
        />
        <Button
          type="submit"
          variant="primary"
          size="sm"
          leadingIcon={<SendGlyph />}
          disabled={!draft.trim()}
        >
          {messages.send}
        </Button>
      </form>
    </div>
  );
}

function Entry({
  m,
  messages,
  formatTime,
  onResend,
}: {
  m: SupportChatEntry;
  messages: SupportPanelMessages;
  formatTime: (d: Date) => string;
  onResend: (clientMessageId: string) => void;
}) {
  const mine = m.author === 'visitor';
  const date = m.createdAt instanceof Date ? m.createdAt : new Date(m.createdAt);
  const valid = !Number.isNaN(date.getTime());
  const who = mine ? messages.you : (m.authorName ?? messages.support);
  return (
    <li className="mn-support-panel__message" data-author={m.author} data-pending={m.pending}>
      <span className="mn-support-panel__meta">
        {m.author === 'system' ? null : <span className="mn-support-panel__author">{who}</span>}
        {valid && !m.pending ? (
          <time className="mn-support-panel__time" dateTime={date.toISOString()}>
            {formatTime(date)}
          </time>
        ) : null}
      </span>
      <span className="mn-support-panel__bubble">{m.text}</span>
      {m.pending === 'sending' ? (
        <span className="mn-support-panel__pending">{messages.sending}</span>
      ) : m.pending === 'failed' && m.clientMessageId ? (
        <span className="mn-support-panel__pending" data-failed="">
          {messages.notSent}{' '}
          <Button size="xs" variant="ghost" onClick={() => onResend(m.clientMessageId as string)}>
            {messages.resend}
          </Button>
        </span>
      ) : null}
    </li>
  );
}
