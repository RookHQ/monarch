'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph, EyeGlyph, PlusGlyph } from '../../../internal/icons.js';
import { useControllable } from '../../../internal/use-controllable.js';
import { Button } from '../../actions/button/index.js';
import { Avatar } from '../../identity/avatar/index.js';
import { Popover, PopoverContent, PopoverTrigger } from '../../overlays/popover/index.js';
import { AiAgentAvatar } from '../ai-agent-avatar/index.js';

/** Whether a participant is a person or an AI agent. */
export type AiSharedSessionKind = 'person' | 'agent';

/** What a person may do. Owners manage the queue, editors send, viewers only watch. */
export type AiSharedSessionRole = 'owner' | 'editor' | 'viewer';

/** How present a participant is: here and engaged, here but quiet, or stepped away. */
export type AiSharedSessionPresence = 'active' | 'idle' | 'away';

/** What a participant is doing right now. */
export type AiSharedSessionActivity = 'typing' | 'speaking' | 'reviewing';

/** One person or agent in the shared conversation. */
export interface AiSharedSessionParticipant {
  /** Stable id: the React key, and what `answering`, `queue` and the callbacks refer to. */
  id: string;
  /** Display name. */
  name: string;
  /** Avatar image URL. Initials are shown without it. */
  avatar?: string | undefined;
  /** A person or an AI agent. Agents get the square agent picture and their own group. */
  kind: AiSharedSessionKind;
  /** What a person may do. Ignored for agents. Default `editor`. */
  role?: AiSharedSessionRole | undefined;
  /** How present they are. */
  presence: AiSharedSessionPresence;
  /** What they are doing right now, if anything. */
  activity?: AiSharedSessionActivity | undefined;
  /** Marks the person looking at this screen. Their role decides what they can do here. */
  you?: boolean | undefined;
}

/** Which agent is answering now, and whose request it is. */
export interface AiSharedSessionAnswering {
  /** The agent that is answering. */
  participantId: string;
  /** The person whose request is being answered. */
  forParticipantId: string;
  /** A short description of the request, e.g. "Summarise the Q3 notes". */
  label?: string | undefined;
}

/** A request waiting for its turn. */
export interface AiSharedSessionRequest {
  /** Stable id, passed back from the queue callbacks. */
  id: string;
  /** Who asked. */
  participantId: string;
  /** The start of the request, shown as one line. */
  preview: string;
}

/** The part of the session the status line is built from. */
export interface AiSharedSessionState {
  /** Everyone here. */
  participants: readonly AiSharedSessionParticipant[];
  /** The answer in progress, if any. */
  answering?: AiSharedSessionAnswering | null | undefined;
  /** Requests waiting, first in line first. */
  queue?: readonly AiSharedSessionRequest[] | undefined;
}

/** Describes one queue move, passed to `onReorder` beside the new order. */
export interface AiSharedSessionMove {
  /** The request that moved. */
  id: string;
  /** Its old index. */
  from: number;
  /** Its new index. */
  to: number;
}

/** Every visible and assistive-technology string. English defaults. */
export interface AiSharedSessionMessages {
  /** Accessible name of the whole bar. */
  label: string;
  /** Accessible name of the avatar stack, e.g. "3 people and 1 agent here". */
  here: (people: number, agents: number) => string;
  /** Accessible name of the button that opens the full list. */
  showAll: (total: number, formatted: string) => string;
  /** Name of the full list. */
  listTitle: string;
  /** Heading of the people group. */
  people: string;
  /** Heading of the agents group. */
  agents: string;
  /** Marks your own name, e.g. "Maya (you)". */
  nameWithYou: (name: string) => string;
  /** Said for agents where a person would have a role. */
  agentKind: string;
  /** Role names. */
  roles: Record<AiSharedSessionRole, string>;
  /** Presence words, always shown beside the presence shape. */
  presence: Record<AiSharedSessionPresence, string>;
  /** Activity words used in the list and in avatar names. */
  activities: Record<AiSharedSessionActivity, string>;
  /** Accessible name of one face in the stack, from its name and details. */
  participant: (name: string, details: readonly string[]) => string;
  /** Status line for each activity; `names` is one or two names, or `several(count)`. */
  activityLine: Record<AiSharedSessionActivity, (names: string, count: number) => string>;
  /** Stands in for the names when more than two people share an activity. */
  several: (count: number, formatted: string) => string;
  /** Status line while an agent answers someone else's request. */
  answering: (agent: string, requester: string, label: string | undefined) => string;
  /** Status line while an agent answers your own request. */
  answeringYou: (agent: string, label: string | undefined) => string;
  /** Used when the answering agent is not in `participants`. */
  assistant: string;
  /** Used when a requester is not in `participants`. */
  someone: string;
  /** Status line for the queue, e.g. "2 requests waiting". */
  queueCount: (count: number, formatted: string) => string;
  /** Status line when nothing is happening. */
  quiet: string;
  /** Text of the button that shows or hides the queue. */
  queueToggle: (count: number, formatted: string) => string;
  /** Accessible name of the queue list. */
  queueTitle: string;
  /** Withdraw button (your own request). */
  withdraw: string;
  /** Remove button (owner). */
  remove: string;
  /** Move up button (owner). */
  moveUp: string;
  /** Move down button (owner). */
  moveDown: string;
  /** Accessible name of a queue button; must contain `action`, the visible text. */
  requestAction: (action: string, requester: string, position: number) => string;
  /** Announced after a request is moved. */
  moved: (requester: string, position: number, total: number) => string;
  /** Invite button. */
  invite: string;
  /** Follow toggle (pressed while following). */
  follow: string;
  /** Accessible name of a Follow toggle; must contain the visible text. */
  followLabel: (name: string) => string;
  /** Read-only notice shown to viewers. */
  viewerNotice: string;
  /** Request access button. */
  requestAccess: string;
  /** Shown in place of the button once access has been requested. */
  accessRequested: string;
}

const withLabel = (text: string, label: string | undefined) => (label ? `${text}: ${label}` : text);

export const AI_SHARED_SESSION_MESSAGES: AiSharedSessionMessages = {
  label: 'Shared session',
  here: (people, agents) =>
    [
      people ? `${people} ${people === 1 ? 'person' : 'people'}` : '',
      agents ? `${agents} ${agents === 1 ? 'agent' : 'agents'}` : '',
    ]
      .filter(Boolean)
      .join(' and ') + ' here',
  showAll: (_total, formatted) => `Show all ${formatted} participants`,
  listTitle: 'In this session',
  people: 'People',
  agents: 'Agents',
  nameWithYou: (name) => `${name} (you)`,
  agentKind: 'AI agent',
  roles: { owner: 'Owner', editor: 'Editor', viewer: 'Viewer' },
  presence: { active: 'Active', idle: 'Idle', away: 'Away' },
  activities: { typing: 'Typing', speaking: 'Speaking', reviewing: 'Reviewing' },
  participant: (name, details) => [name, ...details].join(', '),
  activityLine: {
    typing: (names, count) => `${names} ${count === 1 ? 'is' : 'are'} typing…`,
    speaking: (names, count) => `${names} ${count === 1 ? 'is' : 'are'} speaking`,
    reviewing: (names, count) => `${names} ${count === 1 ? 'is' : 'are'} reviewing`,
  },
  several: (_count, formatted) => `${formatted} people`,
  answering: (agent, requester, label) =>
    withLabel(`${agent} is answering ${requester}’s request`, label),
  answeringYou: (agent, label) => withLabel(`${agent} is answering your request`, label),
  assistant: 'Assistant',
  someone: 'Someone',
  queueCount: (count, formatted) => `${formatted} ${count === 1 ? 'request' : 'requests'} waiting`,
  quiet: 'No requests in progress',
  queueToggle: (_count, formatted) => `Queue (${formatted})`,
  queueTitle: 'Waiting requests',
  withdraw: 'Withdraw',
  remove: 'Remove',
  moveUp: 'Move up',
  moveDown: 'Move down',
  requestAction: (action, requester, position) =>
    `${action}: request ${position}, from ${requester}`,
  moved: (requester, position, total) =>
    `${requester}’s request moved to position ${position} of ${total}`,
  invite: 'Invite',
  follow: 'Follow',
  followLabel: (name) => `Follow ${name}`,
  viewerNotice: 'You can watch but not send',
  requestAccess: 'Request access',
  accessRequested: 'Access requested',
};

type RecordKeys = 'roles' | 'presence' | 'activities' | 'activityLine';

/** The `messages` prop: any string may be replaced, and the keyed groups may be partial. */
export type AiSharedSessionMessagesInput = Partial<Omit<AiSharedSessionMessages, RecordKeys>> & {
  roles?: Partial<AiSharedSessionMessages['roles']> | undefined;
  presence?: Partial<AiSharedSessionMessages['presence']> | undefined;
  activities?: Partial<AiSharedSessionMessages['activities']> | undefined;
  activityLine?: Partial<AiSharedSessionMessages['activityLine']> | undefined;
};

/** Merges a partial `messages` prop with the English defaults. */
export function resolveAiSharedSessionMessages(
  messages?: AiSharedSessionMessagesInput,
): AiSharedSessionMessages {
  const { roles, presence, activities, activityLine, ...rest } = messages ?? {};
  const d = AI_SHARED_SESSION_MESSAGES;
  return {
    ...d,
    ...rest,
    roles: { ...d.roles, ...roles },
    presence: { ...d.presence, ...presence },
    activities: { ...d.activities, ...activities },
    activityLine: { ...d.activityLine, ...activityLine },
  };
}

/** Participants split for display. */
export interface AiSharedSessionGroups {
  /** People, with you first and the rest in the given order. */
  people: AiSharedSessionParticipant[];
  /** Agents, in the given order. */
  agents: AiSharedSessionParticipant[];
  /** The person looking at this screen, when one is marked `you`. */
  you: AiSharedSessionParticipant | undefined;
}

/** Splits participants into people (you first) and agents, keeping the given order otherwise. */
export function groupAiSharedSessionParticipants(
  participants: readonly AiSharedSessionParticipant[],
): AiSharedSessionGroups {
  const people = participants.filter((p) => p.kind === 'person');
  const agents = participants.filter((p) => p.kind === 'agent');
  const you = people.find((p) => p.you);
  return { people: you ? [you, ...people.filter((p) => p !== you)] : people, agents, you };
}

/** One clause of the status line. */
export interface AiSharedSessionStatusPart {
  /** What the clause is about. */
  kind: 'answering' | AiSharedSessionActivity | 'queue';
  /** The clause in plain words. */
  text: string;
  /** Whether it is worth announcing. Typing and speaking are not. */
  announce: boolean;
}

/** The status line, as clauses and as text. */
export interface AiSharedSessionStatus {
  /** The clauses, most important first. */
  parts: AiSharedSessionStatusPart[];
  /** The visible line: every clause joined with " · ". Empty when nothing is happening. */
  text: string;
  /** What a screen reader is told: the clauses worth announcing only. */
  announcement: string;
}

/** Options of the status helpers. */
export interface AiSharedSessionStatusOptions {
  /** Translated strings. */
  messages?: AiSharedSessionMessagesInput | undefined;
  /** Locale for numbers and lists of names. Default: the runtime locale. */
  locale?: string | undefined;
}

const ACTIVITIES: AiSharedSessionActivity[] = ['typing', 'speaking', 'reviewing'];

/**
 * Builds the status line from the session: who the assistant is answering,
 * what other people are doing, and how many requests wait. Your own activity
 * is left out, since you already know it.
 */
export function aiSharedSessionStatus(
  session: AiSharedSessionState,
  options: AiSharedSessionStatusOptions = {},
): AiSharedSessionStatus {
  const m = resolveAiSharedSessionMessages(options.messages);
  const number = new Intl.NumberFormat(options.locale);
  const list = new Intl.ListFormat(options.locale, { style: 'long', type: 'conjunction' });
  const byId = new Map(session.participants.map((p) => [p.id, p]));
  const parts: AiSharedSessionStatusPart[] = [];

  const { answering, queue } = session;
  if (answering) {
    const agent = byId.get(answering.participantId)?.name ?? m.assistant;
    const requester = byId.get(answering.forParticipantId);
    const label = answering.label || undefined;
    parts.push({
      kind: 'answering',
      text: requester?.you
        ? m.answeringYou(agent, label)
        : m.answering(agent, requester?.name ?? m.someone, label),
      announce: true,
    });
  }
  for (const activity of ACTIVITIES) {
    const names = session.participants
      .filter((p) => p.activity === activity && !p.you)
      .map((p) => p.name);
    if (!names.length) continue;
    const who =
      names.length > 2 ? m.several(names.length, number.format(names.length)) : list.format(names);
    parts.push({
      kind: activity,
      text: m.activityLine[activity](who, names.length),
      announce: activity === 'reviewing',
    });
  }
  if (queue?.length) {
    parts.push({
      kind: 'queue',
      text: m.queueCount(queue.length, number.format(queue.length)),
      announce: true,
    });
  }

  return {
    parts,
    text: parts.map((p) => p.text).join(' · '),
    announcement: parts
      .filter((p) => p.announce)
      .map((p) => p.text)
      .join('. '),
  };
}

/** The visible status line as one string, e.g. "Maya is typing… · 2 requests waiting". */
export function aiSharedSessionStatusLine(
  session: AiSharedSessionState,
  options?: AiSharedSessionStatusOptions,
): string {
  return aiSharedSessionStatus(session, options).text;
}

export interface AiSharedSessionProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** Everyone in the session, people and agents, in display order. Mark one person `you`. */
  participants: readonly AiSharedSessionParticipant[];
  /** The answer in progress: which agent, and whose request. */
  answering?: AiSharedSessionAnswering | null | undefined;
  /** Requests waiting for their turn, first in line first. */
  queue?: readonly AiSharedSessionRequest[] | undefined;
  /** Show at most this many avatars before "+N". Default 4. */
  max?: number | undefined;
  /** Whether the full participant list is open (controlled). */
  participantsOpen?: boolean | undefined;
  /** Whether the full participant list starts open (uncontrolled). Default false. */
  defaultParticipantsOpen?: boolean | undefined;
  /** Called when the participant list opens or closes. */
  onParticipantsOpenChange?: ((open: boolean) => void) | undefined;
  /** Whether the queue is expanded (controlled). */
  queueOpen?: boolean | undefined;
  /** Whether the queue starts expanded (uncontrolled). Default false. */
  defaultQueueOpen?: boolean | undefined;
  /** Called when the queue is expanded or collapsed. */
  onQueueOpenChange?: ((open: boolean) => void) | undefined;
  /** Shows a Withdraw button on your own requests; called with the request id. */
  onWithdraw?: ((requestId: string) => void) | undefined;
  /**
   * Shows Move up and Move down on every request when you are the owner. Called
   * with the request ids in their new order and a description of the move.
   */
  onReorder?: ((orderedIds: string[], move: AiSharedSessionMove) => void) | undefined;
  /** Shows a Remove button on other people's requests when you are the owner. */
  onRemove?: ((requestId: string) => void) | undefined;
  /** Shows an Invite button. The product opens its own invite flow. */
  onInvite?: (() => void) | undefined;
  /** Ids of the people you follow (controlled). */
  following?: readonly string[] | undefined;
  /** Ids of the people you follow at first (uncontrolled). */
  defaultFollowing?: readonly string[] | undefined;
  /** Shows a Follow toggle beside every other person; called with their id and the new value. */
  onFollowChange?: ((participantId: string, following: boolean) => void) | undefined;
  /** Shows a Request access button in the viewer notice. */
  onRequestAccess?: (() => void) | undefined;
  /** Set once access was requested: the button is replaced by "Access requested". */
  accessRequested?: boolean | undefined;
  /**
   * Announce status changes (who is being answered, the queue length) through a
   * polite live region. Typing and speaking are never announced. Default true.
   */
  announce?: boolean | undefined;
  /** Shortest time between two announcements, in milliseconds. Default 4000. */
  announceInterval?: number | undefined;
  /** Locale for numbers and lists of names. Default: the runtime locale. */
  locale?: string | undefined;
  /** Replaces any of the English strings; the keyed groups may be partial. */
  messages?: AiSharedSessionMessagesInput | undefined;
}

type QueueAction = 'up' | 'down' | 'withdraw' | 'remove';

/**
 * The presence and turn-taking bar of an AI conversation that several people
 * and agents share: who is here, who is typing, whose request the assistant
 * is answering, what is waiting, and whether you may send. Data comes in
 * through props and every action goes out through a callback.
 */
export const AiSharedSession = forwardRef<HTMLDivElement, AiSharedSessionProps>(
  function AiSharedSession(
    {
      participants,
      answering,
      queue,
      max = 4,
      participantsOpen,
      defaultParticipantsOpen = false,
      onParticipantsOpenChange,
      queueOpen,
      defaultQueueOpen = false,
      onQueueOpenChange,
      onWithdraw,
      onReorder,
      onRemove,
      onInvite,
      following,
      defaultFollowing,
      onFollowChange,
      onRequestAccess,
      accessRequested = false,
      announce = true,
      announceInterval = 4000,
      locale,
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m = resolveAiSharedSessionMessages(messages);
    const number = new Intl.NumberFormat(locale);
    const queueId = useId();
    const headingId = useId();

    const rootRef = useRef<HTMLDivElement | null>(null);
    const setRoot = useCallback(
      (node: HTMLDivElement | null) => {
        rootRef.current = node;
        if (typeof ref === 'function') ref(node);
        else if (ref) ref.current = node;
      },
      [ref],
    );
    const queueToggleRef = useRef<HTMLButtonElement | null>(null);

    const [listOpen, setListOpen] = useControllable(
      participantsOpen,
      defaultParticipantsOpen,
      onParticipantsOpenChange,
    );
    const [queueShown, setQueueShown] = useControllable(
      queueOpen,
      defaultQueueOpen,
      onQueueOpenChange,
    );
    const [followingInner, setFollowingInner] = useState<readonly string[]>(defaultFollowing ?? []);
    const followed = following ?? followingInner;

    const byId = new Map(participants.map((p) => [p.id, p]));
    const { people, agents, you } = groupAiSharedSessionParticipants(participants);
    const yourRole = you ? (you.role ?? 'editor') : undefined;
    const isOwner = yourRole === 'owner';
    const requests = queue ?? [];

    const status = aiSharedSessionStatus({ participants, answering, queue }, { messages, locale });

    // Announce politely, never the first render and never more often than `announceInterval`.
    const [live, setLive] = useState('');
    const lastText = useRef<string | null>(null);
    const lastAt = useRef(Number.NEGATIVE_INFINITY);
    const { announcement } = status;
    useEffect(() => {
      if (lastText.current === null) {
        lastText.current = announcement;
        return;
      }
      if (!announce || announcement === lastText.current) return;
      if (announcement === '') {
        lastText.current = '';
        setLive('');
        return;
      }
      const say = () => {
        lastText.current = announcement;
        lastAt.current = Date.now();
        setLive(announcement);
      };
      const wait = lastAt.current + announceInterval - Date.now();
      if (wait <= 0) {
        say();
        return;
      }
      // Only the latest text is spoken once the interval has passed.
      const timer = setTimeout(say, wait);
      return () => clearTimeout(timer);
    }, [announcement, announce, announceInterval]);

    // Queue buttons move, disable or disappear when the product applies an action,
    // so focus is put back somewhere sensible once the new queue arrives.
    const pending = useRef<{ id: string; action: QueueAction; from: number } | null>(null);
    const order = requests.map((r) => r.id).join('\u0000');
    useEffect(() => {
      const acted = pending.current;
      const root = rootRef.current;
      if (!acted || !root) return;
      pending.current = null;
      // Only when focus was lost or is still in the bar: never pull it from elsewhere.
      const focused = document.activeElement;
      if (focused && focused !== document.body && !root.contains(focused)) return;
      const buttons = Array.from(
        root.querySelectorAll<HTMLButtonElement>('button[data-request]'),
      ).filter((b) => b.dataset.request === acted.id && !b.disabled);
      const index = requests.findIndex((r) => r.id === acted.id);
      if (index === -1 || !buttons.length) {
        (queueToggleRef.current ?? root).focus();
        return;
      }
      (buttons.find((b) => b.dataset.action === acted.action) ?? buttons[0])?.focus();
      const request = requests[index];
      const moved = (acted.action === 'up' || acted.action === 'down') && index !== acted.from;
      if (request && announce && moved) {
        const asker = byId.get(request.participantId);
        lastAt.current = Date.now();
        setLive(m.moved(asker?.name ?? m.someone, index + 1, requests.length));
      }
      // Runs when the order changes; everything else is read fresh.
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [order]);

    const displayName = (p: AiSharedSessionParticipant) => (p.you ? m.nameWithYou(p.name) : p.name);
    const answeringText = (p: AiSharedSessionParticipant) =>
      answering && answering.participantId === p.id
        ? status.parts.find((part) => part.kind === 'answering')?.text
        : undefined;
    const detailsOf = (p: AiSharedSessionParticipant) =>
      [
        p.kind === 'agent' ? m.agentKind : m.roles[p.role ?? 'editor'],
        m.presence[p.presence],
        p.activity ? m.activities[p.activity] : undefined,
      ].filter((s): s is string => Boolean(s));

    const face = (p: AiSharedSessionParticipant) => (
      <>
        {p.kind === 'agent' ? (
          <AiAgentAvatar
            agent={{ id: p.id, name: p.name, ...(p.avatar ? { avatar: p.avatar } : {}) }}
            state={answering?.participantId === p.id ? 'working' : 'idle'}
            size="sm"
            decorative
          />
        ) : (
          <Avatar name={p.name} src={p.avatar ?? null} seed={p.id} size="sm" decorative />
        )}
        <span
          className="mn-ai-shared-session__mark"
          data-presence={p.presence}
          data-overlay=""
          aria-hidden="true"
        />
      </>
    );

    const limit = Math.max(1, Math.floor(max));
    // A "+1" takes the room of the avatar it hides, so show that avatar instead.
    const visibleCount =
      participants.length - limit === 1
        ? participants.length
        : Math.min(limit, participants.length);
    const hidden = participants.length - visibleCount;

    const setFollow = (id: string, next: boolean) => {
      if (following === undefined) {
        setFollowingInner((before) => (next ? [...before, id] : before.filter((f) => f !== id)));
      }
      onFollowChange?.(id, next);
    };

    const move = (index: number, to: number) => {
      const request = requests[index];
      if (!request || !onReorder || to < 0 || to >= requests.length) return;
      const ids = requests.map((r) => r.id);
      ids.splice(index, 1);
      ids.splice(to, 0, request.id);
      pending.current = { id: request.id, action: to < index ? 'up' : 'down', from: index };
      onReorder(ids, { id: request.id, from: index, to });
    };

    const row = (p: AiSharedSessionParticipant) => {
      const isFollowed = followed.includes(p.id);
      const doing = answeringText(p) ?? (p.activity ? m.activities[p.activity] : undefined);
      return (
        <li
          key={p.id}
          className="mn-ai-shared-session__row"
          data-kind={p.kind}
          data-presence={p.presence}
        >
          <span className="mn-ai-shared-session__face" aria-hidden="true">
            {face(p)}
          </span>
          <span className="mn-ai-shared-session__who">
            <span className="mn-ai-shared-session__name">{displayName(p)}</span>
            <span className="mn-ai-shared-session__details">
              <span className="mn-ai-shared-session__role">
                {p.kind === 'agent' ? m.agentKind : m.roles[p.role ?? 'editor']}
              </span>
              <span className="mn-ai-shared-session__presence">
                <span
                  className="mn-ai-shared-session__mark"
                  data-presence={p.presence}
                  aria-hidden="true"
                />
                {m.presence[p.presence]}
              </span>
              {doing ? <span className="mn-ai-shared-session__doing">{doing}</span> : null}
            </span>
          </span>
          {onFollowChange && p.kind === 'person' && !p.you ? (
            <Button
              size="xs"
              variant="ghost"
              className="mn-ai-shared-session__follow"
              aria-pressed={isFollowed}
              aria-label={m.followLabel(p.name)}
              data-pressed={isFollowed || undefined}
              leadingIcon={<EyeGlyph />}
              onClick={() => setFollow(p.id, !isFollowed)}
            >
              {m.follow}
            </Button>
          ) : null}
        </li>
      );
    };

    const group = (title: string, members: AiSharedSessionParticipant[], key: string) =>
      members.length ? (
        <div
          className="mn-ai-shared-session__group"
          role="group"
          aria-labelledby={`${headingId}-${key}`}
          data-group={key}
        >
          <div id={`${headingId}-${key}`} className="mn-ai-shared-session__group-title">
            {title}
          </div>
          <ul className="mn-ai-shared-session__rows">{members.map(row)}</ul>
        </div>
      ) : null;

    const queueButton = (
      request: AiSharedSessionRequest,
      index: number,
      action: QueueAction,
      text: string,
      onClick: () => void,
      disabled = false,
    ): ReactNode => (
      <Button
        size="xs"
        variant="ghost"
        data-request={request.id}
        data-action={action}
        disabled={disabled}
        aria-label={m.requestAction(
          text,
          byId.get(request.participantId)?.name ?? m.someone,
          index + 1,
        )}
        onClick={onClick}
      >
        {text}
      </Button>
    );

    return (
      <div
        ref={setRoot}
        role="group"
        aria-label={m.label}
        tabIndex={-1}
        {...rest}
        className={cx('mn-ai-shared-session', className)}
        data-role={yourRole}
        data-answering={answering ? '' : undefined}
      >
        <div className="mn-ai-shared-session__bar">
          <div className="mn-ai-shared-session__here">
            <ul
              className="mn-ai-shared-session__stack"
              aria-label={m.here(people.length, agents.length)}
            >
              {participants.slice(0, visibleCount).map((p) => {
                const text = m.participant(displayName(p), detailsOf(p));
                return (
                  <li
                    key={p.id}
                    className="mn-ai-shared-session__item"
                    data-kind={p.kind}
                    data-presence={p.presence}
                  >
                    <span
                      className="mn-ai-shared-session__person"
                      role="img"
                      aria-label={text}
                      title={text}
                    >
                      {face(p)}
                    </span>
                  </li>
                );
              })}
            </ul>
            {participants.length ? (
              <Popover open={listOpen} onOpenChange={setListOpen}>
                <PopoverTrigger asChild>
                  <button
                    type="button"
                    className="mn-ai-shared-session__more"
                    data-overflow={hidden > 0 ? '' : undefined}
                  >
                    {/* The name is hidden text, not aria-label, so it never disagrees with the visible "+3". */}
                    <span className="mn-visually-hidden">
                      {m.showAll(participants.length, number.format(participants.length))}
                    </span>
                    {hidden > 0 ? (
                      <span aria-hidden="true">{`+${number.format(hidden)}`}</span>
                    ) : (
                      <ChevronDownGlyph aria-hidden="true" />
                    )}
                  </button>
                </PopoverTrigger>
                <PopoverContent
                  className="mn-ai-shared-session__popover"
                  aria-label={m.listTitle}
                  side="bottom"
                  align="start"
                  collisionPadding={8}
                >
                  {group(m.people, people, 'people')}
                  {group(m.agents, agents, 'agents')}
                </PopoverContent>
              </Popover>
            ) : null}
          </div>

          <p
            className="mn-ai-shared-session__status"
            data-quiet={status.parts.length ? undefined : ''}
          >
            {status.parts.length
              ? status.parts.map((part, i) => (
                  <span
                    key={part.kind}
                    className="mn-ai-shared-session__clause"
                    data-kind={part.kind}
                  >
                    {i > 0 ? (
                      <span className="mn-ai-shared-session__dot" aria-hidden="true">
                        {' · '}
                      </span>
                    ) : null}
                    {part.text}
                  </span>
                ))
              : m.quiet}
          </p>

          {requests.length || onInvite ? (
            <div className="mn-ai-shared-session__actions">
              {requests.length ? (
                <Button
                  ref={queueToggleRef}
                  size="sm"
                  variant="ghost"
                  className="mn-ai-shared-session__queue-toggle"
                  aria-expanded={queueShown}
                  aria-controls={queueShown ? queueId : undefined}
                  data-open={queueShown ? '' : undefined}
                  trailingIcon={<ChevronDownGlyph />}
                  onClick={() => setQueueShown(!queueShown)}
                >
                  {m.queueToggle(requests.length, number.format(requests.length))}
                </Button>
              ) : null}
              {onInvite ? (
                <Button
                  size="sm"
                  variant="secondary"
                  leadingIcon={<PlusGlyph />}
                  onClick={onInvite}
                >
                  {m.invite}
                </Button>
              ) : null}
            </div>
          ) : null}
        </div>

        {yourRole === 'viewer' ? (
          <div className="mn-ai-shared-session__notice">
            <EyeGlyph className="mn-ai-shared-session__notice-icon" aria-hidden="true" />
            <p className="mn-ai-shared-session__notice-text">{m.viewerNotice}</p>
            {accessRequested ? (
              <span className="mn-ai-shared-session__requested">{m.accessRequested}</span>
            ) : onRequestAccess ? (
              <Button size="sm" variant="secondary" onClick={onRequestAccess}>
                {m.requestAccess}
              </Button>
            ) : null}
          </div>
        ) : null}

        {requests.length && queueShown ? (
          <ol id={queueId} className="mn-ai-shared-session__queue" aria-label={m.queueTitle}>
            {requests.map((request, index) => {
              const asker = byId.get(request.participantId);
              const own = Boolean(you && request.participantId === you.id);
              const withdraw = (): void => {
                pending.current = { id: request.id, action: 'withdraw', from: index };
                onWithdraw?.(request.id);
              };
              const remove = (): void => {
                pending.current = { id: request.id, action: 'remove', from: index };
                onRemove?.(request.id);
              };
              return (
                <li
                  key={request.id}
                  className="mn-ai-shared-session__request"
                  data-own={own ? '' : undefined}
                >
                  <span className="mn-ai-shared-session__request-text">
                    <span className="mn-ai-shared-session__request-name">
                      {asker ? displayName(asker) : m.someone}
                    </span>
                    <span className="mn-ai-shared-session__preview">{request.preview}</span>
                  </span>
                  <span className="mn-ai-shared-session__request-actions">
                    {isOwner && onReorder ? (
                      <>
                        {queueButton(
                          request,
                          index,
                          'up',
                          m.moveUp,
                          () => move(index, index - 1),
                          index === 0,
                        )}
                        {queueButton(
                          request,
                          index,
                          'down',
                          m.moveDown,
                          () => move(index, index + 1),
                          index === requests.length - 1,
                        )}
                      </>
                    ) : null}
                    {own && onWithdraw
                      ? queueButton(request, index, 'withdraw', m.withdraw, withdraw)
                      : isOwner && onRemove
                        ? queueButton(request, index, 'remove', m.remove, remove)
                        : null}
                  </span>
                </li>
              );
            })}
          </ol>
        ) : null}

        {announce ? (
          <span className="mn-visually-hidden" role="status" aria-live="polite" aria-atomic="true">
            {live}
          </span>
        ) : null}
      </div>
    );
  },
);
