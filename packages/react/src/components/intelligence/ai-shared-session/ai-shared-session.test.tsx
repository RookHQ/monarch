import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  AiSharedSession,
  aiSharedSessionStatus,
  aiSharedSessionStatusLine,
  groupAiSharedSessionParticipants,
  type AiSharedSessionParticipant,
  type AiSharedSessionProps,
  type AiSharedSessionRequest,
} from './index.js';

const maya: AiSharedSessionParticipant = {
  id: 'maya',
  name: 'Maya Chen',
  kind: 'person',
  role: 'owner',
  presence: 'active',
};
const leo: AiSharedSessionParticipant = {
  id: 'leo',
  name: 'Leo Park',
  kind: 'person',
  role: 'editor',
  presence: 'idle',
};
const ana: AiSharedSessionParticipant = {
  id: 'ana',
  name: 'Ana Ruiz',
  kind: 'person',
  role: 'viewer',
  presence: 'away',
};
const assistant: AiSharedSessionParticipant = {
  id: 'assistant',
  name: 'Assistant',
  kind: 'agent',
  presence: 'active',
};
const researcher: AiSharedSessionParticipant = {
  id: 'researcher',
  name: 'Research agent',
  kind: 'agent',
  presence: 'idle',
};

/** Everyone, seen by `youId`. */
const seenBy = (
  youId: string,
  extra: Partial<Record<string, Partial<AiSharedSessionParticipant>>> = {},
) =>
  [maya, leo, ana, assistant, researcher].map((p) => ({
    ...p,
    ...extra[p.id],
    you: p.id === youId,
  }));

const queue: AiSharedSessionRequest[] = [
  { id: 'r1', participantId: 'leo', preview: 'Compare the two drafts' },
  { id: 'r2', participantId: 'maya', preview: 'Summarise the meeting notes' },
  { id: 'r3', participantId: 'leo', preview: 'List the open questions' },
];

afterEach(() => {
  vi.useRealTimers();
});

describe('AiSharedSession helpers', () => {
  it('groups participants into people (you first) and agents', () => {
    const groups = groupAiSharedSessionParticipants(seenBy('leo'));
    expect(groups.people.map((p) => p.id)).toEqual(['leo', 'maya', 'ana']);
    expect(groups.agents.map((p) => p.id)).toEqual(['assistant', 'researcher']);
    expect(groups.you?.id).toBe('leo');
    expect(groupAiSharedSessionParticipants([assistant]).you).toBeUndefined();
  });

  it('builds the status line in plain words', () => {
    const participants = seenBy('ana', { maya: { activity: 'typing' } });
    expect(aiSharedSessionStatusLine({ participants }, { locale: 'en' })).toBe(
      'Maya Chen is typing…',
    );
    expect(
      aiSharedSessionStatusLine(
        {
          participants,
          answering: { participantId: 'assistant', forParticipantId: 'maya' },
          queue: queue.slice(0, 2),
        },
        { locale: 'en' },
      ),
    ).toBe(
      'Assistant is answering Maya Chen’s request · Maya Chen is typing… · 2 requests waiting',
    );
    expect(aiSharedSessionStatusLine({ participants: seenBy('ana') })).toBe('');
  });

  it('says "your request", joins two names, counts more, and skips your own activity', () => {
    const typing = { activity: 'typing' as const };
    const status = aiSharedSessionStatus(
      {
        participants: seenBy('maya', { maya: typing, leo: typing, ana: typing }),
        answering: { participantId: 'assistant', forParticipantId: 'maya', label: 'Q3 notes' },
        queue: queue.slice(0, 1),
      },
      { locale: 'en' },
    );
    expect(status.parts.map((p) => p.kind)).toEqual(['answering', 'typing', 'queue']);
    expect(status.parts[0]?.text).toBe('Assistant is answering your request: Q3 notes');
    expect(status.parts[1]?.text).toBe('Leo Park and Ana Ruiz are typing…');
    expect(status.parts[2]?.text).toBe('1 request waiting');
    // Typing is never part of the announcement.
    expect(status.announcement).toBe(
      'Assistant is answering your request: Q3 notes. 1 request waiting',
    );

    const many = aiSharedSessionStatus(
      { participants: seenBy('assistant', { maya: typing, leo: typing, ana: typing }) },
      { locale: 'en' },
    );
    expect(many.text).toBe('3 people are typing…');
    expect(many.announcement).toBe('');
  });

  it('falls back when the agent or the requester is not in the list', () => {
    expect(
      aiSharedSessionStatusLine({
        participants: [],
        answering: { participantId: 'gone', forParticipantId: 'nobody' },
      }),
    ).toBe('Assistant is answering Someone’s request');
  });
});

describe('AiSharedSession', () => {
  it('renders a named group with the avatar stack and the status line', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiSharedSession
        ref={ref}
        data-testid="root"
        className="extra"
        locale="en"
        participants={seenBy('maya', { leo: { activity: 'typing' } })}
        answering={{ participantId: 'assistant', forParticipantId: 'leo' }}
        queue={queue}
        onInvite={() => {}}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-shared-session', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'Shared session' })).toBe(root);
    expect(root).toHaveAttribute('data-role', 'owner');
    expect(root).toHaveAttribute('data-answering');

    const stack = screen.getByRole('list', { name: '3 people and 2 agents here' });
    // Five participants with max 4: four faces and a "+1" would waste the slot, so all five show.
    expect(within(stack).getAllByRole('img')).toHaveLength(5);
    expect(
      within(stack).getByRole('img', { name: 'Maya Chen (you), Owner, Active' }),
    ).toBeInTheDocument();
    expect(
      within(stack).getByRole('img', { name: 'Leo Park, Editor, Idle, Typing' }),
    ).toBeInTheDocument();
    expect(
      within(stack).getByRole('img', { name: 'Assistant, AI agent, Active' }),
    ).toBeInTheDocument();

    expect(root).toHaveTextContent(
      'Assistant is answering Leo Park’s request · Leo Park is typing… · 3 requests waiting',
    );
    expect(screen.getByRole('button', { name: 'Invite' })).toBeInTheDocument();
    // Nothing is announced on first render.
    expect(screen.getByRole('status')).toBeEmptyDOMElement();
    await expectNoA11yViolations();
  });

  it('opens the full list from "+N", grouped, with role and presence in text', async () => {
    const user = userEvent.setup();
    const onOpen = vi.fn();
    render(
      <AiSharedSession
        locale="en"
        max={2}
        participants={seenBy('maya', { leo: { activity: 'reviewing' } })}
        answering={{ participantId: 'assistant', forParticipantId: 'leo' }}
        onParticipantsOpenChange={onOpen}
      />,
    );
    expect(screen.getAllByRole('img')).toHaveLength(2);
    const more = screen.getByRole('button', { name: 'Show all 5 participants' });
    expect(more).toHaveTextContent('+3');
    await user.click(more);
    expect(onOpen).toHaveBeenLastCalledWith(true);

    const dialog = screen.getByRole('dialog', { name: 'In this session' });
    const people = within(dialog).getByRole('group', { name: 'People' });
    const agents = within(dialog).getByRole('group', { name: 'Agents' });
    const rows = within(people).getAllByRole('listitem');
    expect(rows).toHaveLength(3);
    expect(rows[0]).toHaveTextContent('Maya Chen (you)');
    expect(rows[0]).toHaveTextContent('Owner');
    expect(rows[0]).toHaveTextContent('Active');
    expect(rows[1]).toHaveTextContent('Leo Park');
    expect(rows[1]).toHaveTextContent('Editor');
    expect(rows[1]).toHaveTextContent('Idle');
    expect(rows[1]).toHaveTextContent('Reviewing');
    expect(rows[2]).toHaveTextContent('Viewer');
    expect(rows[2]).toHaveTextContent('Away');
    // The presence shape repeats the text; it is never the only signal.
    expect(
      rows[2]?.querySelector('.mn-ai-shared-session__presence [data-presence="away"]'),
    ).not.toBeNull();
    const agentRows = within(agents).getAllByRole('listitem');
    expect(agentRows[0]).toHaveTextContent('Assistant');
    expect(agentRows[0]).toHaveTextContent('AI agent');
    expect(agentRows[0]).toHaveTextContent('Assistant is answering Leo Park’s request');
    await expectNoA11yViolations();

    await user.keyboard('{Escape}');
    expect(onOpen).toHaveBeenLastCalledWith(false);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(more).toHaveFocus();
  });

  it('supports a controlled participant list', async () => {
    const user = userEvent.setup();
    const onOpen = vi.fn();
    const { rerender } = render(
      <AiSharedSession
        participants={seenBy('maya')}
        participantsOpen={false}
        onParticipantsOpenChange={onOpen}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Show all 5 participants' }));
    expect(onOpen).toHaveBeenCalledWith(true);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    rerender(
      <AiSharedSession
        participants={seenBy('maya')}
        participantsOpen
        onParticipantsOpenChange={onOpen}
      />,
    );
    expect(screen.getByRole('dialog', { name: 'In this session' })).toBeInTheDocument();
  });

  it('toggles Follow per person, uncontrolled and controlled', async () => {
    const user = userEvent.setup();
    const onFollow = vi.fn();
    const { rerender } = render(
      <AiSharedSession
        participants={seenBy('maya')}
        defaultParticipantsOpen
        onFollowChange={onFollow}
      />,
    );
    // No toggle for yourself or for agents.
    expect(screen.queryByRole('button', { name: 'Follow Maya Chen' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Follow Assistant' })).not.toBeInTheDocument();
    const follow = screen.getByRole('button', { name: 'Follow Leo Park' });
    expect(follow).toHaveAttribute('aria-pressed', 'false');
    await user.click(follow);
    expect(onFollow).toHaveBeenLastCalledWith('leo', true);
    expect(follow).toHaveAttribute('aria-pressed', 'true');
    follow.focus();
    await user.keyboard('{Enter}');
    expect(onFollow).toHaveBeenLastCalledWith('leo', false);
    expect(follow).toHaveAttribute('aria-pressed', 'false');

    rerender(
      <AiSharedSession
        participants={seenBy('maya')}
        defaultParticipantsOpen
        following={['ana']}
        onFollowChange={onFollow}
      />,
    );
    const ana = screen.getByRole('button', { name: 'Follow Ana Ruiz' });
    expect(ana).toHaveAttribute('aria-pressed', 'true');
    await user.click(ana);
    expect(onFollow).toHaveBeenLastCalledWith('ana', false);
    expect(ana).toHaveAttribute('aria-pressed', 'true');
  });

  it('expands the queue, uncontrolled and controlled', async () => {
    const user = userEvent.setup();
    const onOpen = vi.fn();
    const { rerender } = render(
      <AiSharedSession participants={seenBy('leo')} queue={queue} onQueueOpenChange={onOpen} />,
    );
    const toggle = screen.getByRole('button', { name: 'Queue (3)' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByRole('list', { name: 'Waiting requests' })).not.toBeInTheDocument();
    toggle.focus();
    await user.keyboard('{Enter}');
    expect(onOpen).toHaveBeenLastCalledWith(true);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    const list = screen.getByRole('list', { name: 'Waiting requests' });
    expect(toggle).toHaveAttribute('aria-controls', list.id);
    expect(within(list).getAllByRole('listitem')).toHaveLength(3);
    expect(list).toHaveTextContent('Compare the two drafts');
    await expectNoA11yViolations();

    rerender(
      <AiSharedSession
        participants={seenBy('leo')}
        queue={queue}
        queueOpen={false}
        onQueueOpenChange={onOpen}
      />,
    );
    expect(screen.queryByRole('list', { name: 'Waiting requests' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Queue (3)' }));
    expect(onOpen).toHaveBeenLastCalledWith(true);
    expect(screen.queryByRole('list', { name: 'Waiting requests' })).not.toBeInTheDocument();
  });

  it('lets an editor withdraw only their own requests', async () => {
    const user = userEvent.setup();
    const onWithdraw = vi.fn();
    const onRemove = vi.fn();
    const onReorder = vi.fn();
    render(
      <AiSharedSession
        participants={seenBy('leo')}
        queue={queue}
        defaultQueueOpen
        onWithdraw={onWithdraw}
        onRemove={onRemove}
        onReorder={onReorder}
      />,
    );
    const withdraws = screen.getAllByRole('button', { name: /^Withdraw/ });
    expect(withdraws).toHaveLength(2);
    expect(screen.queryByRole('button', { name: /^Remove/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^Move/ })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Withdraw: request 3, from Leo Park' }));
    expect(onWithdraw).toHaveBeenCalledExactlyOnceWith('r3');
    expect(onRemove).not.toHaveBeenCalled();
  });

  function OwnerQueue(props: Partial<AiSharedSessionProps>) {
    const [items, setItems] = useState(queue);
    return (
      <AiSharedSession
        participants={seenBy('maya')}
        queue={items}
        defaultQueueOpen
        announceInterval={0}
        onReorder={(ids, move) => {
          props.onReorder?.(ids, move);
          setItems(ids.flatMap((id) => items.filter((r) => r.id === id)));
        }}
        onRemove={(id) => {
          props.onRemove?.(id);
          setItems(items.filter((r) => r.id !== id));
        }}
        onWithdraw={(id) => {
          props.onWithdraw?.(id);
          setItems(items.filter((r) => r.id !== id));
        }}
      />
    );
  }

  it('lets the owner reorder with Move up and Move down, keeping focus and announcing', async () => {
    const user = userEvent.setup();
    const onReorder = vi.fn();
    render(<OwnerQueue onReorder={onReorder} />);
    const order = () =>
      within(screen.getByRole('list', { name: 'Waiting requests' }))
        .getAllByRole('listitem')
        .map((li) => li.querySelector('.mn-ai-shared-session__preview')?.textContent);

    expect(
      screen.getByRole('button', { name: 'Move up: request 1, from Leo Park' }),
    ).toBeDisabled();
    expect(
      screen.getByRole('button', { name: 'Move down: request 3, from Leo Park' }),
    ).toBeDisabled();

    await user.click(screen.getByRole('button', { name: 'Move down: request 1, from Leo Park' }));
    expect(onReorder).toHaveBeenLastCalledWith(['r2', 'r1', 'r3'], { id: 'r1', from: 0, to: 1 });
    expect(order()).toEqual([
      'Summarise the meeting notes',
      'Compare the two drafts',
      'List the open questions',
    ]);
    // Focus follows the request that moved.
    expect(
      screen.getByRole('button', { name: 'Move down: request 2, from Leo Park' }),
    ).toHaveFocus();
    expect(screen.getByRole('status')).toHaveTextContent(
      'Leo Park’s request moved to position 2 of 3',
    );

    // Keyboard: moving to the end disables Move down, so focus goes to Move up.
    await user.keyboard('{Enter}');
    expect(onReorder).toHaveBeenLastCalledWith(['r2', 'r3', 'r1'], { id: 'r1', from: 1, to: 2 });
    expect(screen.getByRole('button', { name: 'Move up: request 3, from Leo Park' })).toHaveFocus();
    await user.keyboard(' ');
    expect(onReorder).toHaveBeenLastCalledWith(['r2', 'r1', 'r3'], { id: 'r1', from: 2, to: 1 });
  });

  it('lets the owner remove others and withdraw their own, then returns focus', async () => {
    const user = userEvent.setup();
    const onRemove = vi.fn();
    const onWithdraw = vi.fn();
    render(<OwnerQueue onRemove={onRemove} onWithdraw={onWithdraw} />);
    expect(screen.getAllByRole('button', { name: /^Remove/ })).toHaveLength(2);
    expect(screen.getAllByRole('button', { name: /^Withdraw/ })).toHaveLength(1);

    await user.click(screen.getByRole('button', { name: 'Remove: request 1, from Leo Park' }));
    expect(onRemove).toHaveBeenCalledExactlyOnceWith('r1');
    expect(screen.getByRole('button', { name: 'Queue (2)' })).toHaveFocus();

    await user.click(screen.getByRole('button', { name: 'Withdraw: request 1, from Maya Chen' }));
    expect(onWithdraw).toHaveBeenCalledExactlyOnceWith('r2');
    await user.click(screen.getByRole('button', { name: 'Remove: request 1, from Leo Park' }));
    // The queue is empty, so its button and list are gone and the bar takes focus.
    expect(screen.queryByRole('button', { name: /^Queue/ })).not.toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'Shared session' })).toHaveFocus();
  });

  it('tells viewers they cannot send and offers Request access', async () => {
    const user = userEvent.setup();
    const onRequestAccess = vi.fn();
    const onWithdraw = vi.fn();
    const { rerender } = render(
      <AiSharedSession
        data-testid="root"
        participants={seenBy('ana')}
        queue={queue}
        defaultQueueOpen
        onWithdraw={onWithdraw}
        onReorder={() => {}}
        onRemove={() => {}}
        onRequestAccess={onRequestAccess}
      />,
    );
    expect(screen.getByTestId('root')).toHaveAttribute('data-role', 'viewer');
    expect(screen.getByText('You can watch but not send')).toBeInTheDocument();
    // A viewer sees the queue but has no buttons in it.
    expect(
      within(screen.getByRole('list', { name: 'Waiting requests' })).queryByRole('button'),
    ).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Request access' }));
    expect(onRequestAccess).toHaveBeenCalledOnce();
    await expectNoA11yViolations();

    rerender(
      <AiSharedSession
        participants={seenBy('ana')}
        onRequestAccess={onRequestAccess}
        accessRequested
      />,
    );
    expect(screen.queryByRole('button', { name: 'Request access' })).not.toBeInTheDocument();
    expect(screen.getByText('Access requested')).toBeInTheDocument();

    rerender(<AiSharedSession participants={seenBy('leo')} onRequestAccess={onRequestAccess} />);
    expect(screen.queryByText('You can watch but not send')).not.toBeInTheDocument();
  });

  it('calls onInvite and shows the quiet line when nothing is happening', async () => {
    const user = userEvent.setup();
    const onInvite = vi.fn();
    render(<AiSharedSession participants={seenBy('maya')} onInvite={onInvite} />);
    expect(screen.getByText('No requests in progress')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^Queue/ })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Invite' }));
    expect(onInvite).toHaveBeenCalledOnce();
  });

  it('announces status changes politely, throttled, and never typing', () => {
    vi.useFakeTimers();
    const base = { locale: 'en', announceInterval: 4000 } as const;
    const { rerender } = render(<AiSharedSession {...base} participants={seenBy('maya')} />);
    const status = screen.getByRole('status');
    expect(status).toHaveAttribute('aria-live', 'polite');
    expect(status).toBeEmptyDOMElement();

    // Typing is shown but not announced.
    rerender(
      <AiSharedSession {...base} participants={seenBy('maya', { leo: { activity: 'typing' } })} />,
    );
    expect(screen.getByText('Leo Park is typing…')).toBeInTheDocument();
    expect(status).toBeEmptyDOMElement();

    // The first real change is announced at once.
    const answering = { participantId: 'assistant', forParticipantId: 'leo' };
    rerender(<AiSharedSession {...base} participants={seenBy('maya')} answering={answering} />);
    expect(status).toHaveTextContent('Assistant is answering Leo Park’s request');

    // Changes inside the interval wait, and only the latest one is spoken.
    rerender(
      <AiSharedSession
        {...base}
        participants={seenBy('maya')}
        answering={answering}
        queue={queue.slice(0, 1)}
      />,
    );
    rerender(
      <AiSharedSession
        {...base}
        participants={seenBy('maya')}
        answering={answering}
        queue={queue}
      />,
    );
    expect(status).toHaveTextContent(/^Assistant is answering Leo Park’s request$/);
    act(() => {
      vi.advanceTimersByTime(3900);
    });
    expect(status).toHaveTextContent(/^Assistant is answering Leo Park’s request$/);
    act(() => {
      vi.advanceTimersByTime(200);
    });
    expect(status).toHaveTextContent(
      'Assistant is answering Leo Park’s request. 3 requests waiting',
    );
  });

  it('has no live region when announce is off', () => {
    render(<AiSharedSession participants={seenBy('maya')} announce={false} />);
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('uses translated strings', async () => {
    const user = userEvent.setup();
    render(
      <AiSharedSession
        locale="fr"
        participants={seenBy('ana', { leo: { activity: 'typing' } })}
        queue={queue.slice(0, 2)}
        onInvite={() => {}}
        onRequestAccess={() => {}}
        messages={{
          label: 'Session partagée',
          invite: 'Inviter',
          viewerNotice: 'Vous pouvez regarder mais pas envoyer',
          requestAccess: 'Demander l’accès',
          queueCount: (_count, formatted) => `${formatted} demandes en attente`,
          queueToggle: (_count, formatted) => `File (${formatted})`,
          showAll: (_total, formatted) => `Afficher les ${formatted} participants`,
          listTitle: 'Dans cette session',
          people: 'Personnes',
          roles: { viewer: 'Lecteur' },
          presence: { away: 'Absent' },
          activityLine: { typing: (names) => `${names} écrit…` },
        }}
      />,
    );
    expect(screen.getByRole('group', { name: 'Session partagée' })).toHaveTextContent(
      'Leo Park écrit… · 2 demandes en attente',
    );
    expect(screen.getByRole('button', { name: 'Inviter' })).toBeInTheDocument();
    expect(screen.getByText('Vous pouvez regarder mais pas envoyer')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Demander l’accès' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'File (2)' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Afficher les 5 participants' }));
    const people = within(screen.getByRole('dialog', { name: 'Dans cette session' })).getByRole(
      'group',
      { name: 'Personnes' },
    );
    expect(people).toHaveTextContent('Lecteur');
    expect(people).toHaveTextContent('Absent');
    // Untranslated keys in a partial group keep their defaults.
    expect(people).toHaveTextContent('Owner');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiSharedSession
        locale="en"
        participants={seenBy('ana')}
        answering={{ participantId: 'assistant', forParticipantId: 'maya' }}
        queue={queue}
        defaultQueueOpen
      />,
    );
    expect(html).toContain('mn-ai-shared-session');
    expect(html).toContain('You can watch but not send');
    expect(html).toContain('Compare the two drafts');
  });
});
