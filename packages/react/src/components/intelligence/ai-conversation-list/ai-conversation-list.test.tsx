import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiConversationList, groupAiConversations, type AiConversationSummary } from './index.js';

const NOW = '2026-03-10T12:00:00Z';
const CONVERSATIONS: AiConversationSummary[] = [
  { id: 'a', title: 'Launch checklist', updatedAt: '2026-03-10T09:00:00Z', preview: 'Week 9' },
  { id: 'b', title: 'Hiring plan', updatedAt: '2026-03-10T11:00:00Z' },
  { id: 'c', title: 'Budget review', updatedAt: '2026-03-07T09:00:00Z' },
  { id: 'd', title: 'Team offsite', updatedAt: '2026-02-01T09:00:00Z' },
  { id: 'e', title: 'Roadmap', updatedAt: '2026-01-01T09:00:00Z', pinned: true },
];

function Stateful(props: {
  onDelete?: (id: string) => unknown;
  onRename?: (id: string, title: string) => void;
}) {
  const [list, setList] = useState(CONVERSATIONS);
  return (
    <AiConversationList
      conversations={list}
      activeId="a"
      now={NOW}
      timeZone="UTC"
      onSelect={() => {}}
      onRename={(id, title) => {
        props.onRename?.(id, title);
        setList((l) => l.map((c) => (c.id === id ? { ...c, title } : c)));
      }}
      onPin={(id, pinned) => setList((l) => l.map((c) => (c.id === id ? { ...c, pinned } : c)))}
      onDelete={async (id) => {
        await props.onDelete?.(id);
        setList((l) => l.filter((c) => c.id !== id));
      }}
    />
  );
}

describe('groupAiConversations', () => {
  it('groups into pinned, today, previous 7 days and older, newest first', () => {
    const groups = groupAiConversations(CONVERSATIONS, { now: NOW, timeZone: 'UTC' });
    expect(groups.map((g) => [g.group, g.items.map((c) => c.id)])).toEqual([
      ['pinned', ['e']],
      ['today', ['b', 'a']],
      ['previous7Days', ['c']],
      ['older', ['d']],
    ]);
  });

  it('leaves out empty groups', () => {
    const groups = groupAiConversations([CONVERSATIONS[3]!], { now: NOW, timeZone: 'UTC' });
    expect(groups.map((g) => g.group)).toEqual(['older']);
  });
});

describe('AiConversationList', () => {
  it('renders a navigation landmark with headed groups and the active conversation', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <AiConversationList
        ref={ref}
        data-testid="root"
        className="extra"
        conversations={CONVERSATIONS}
        activeId="a"
        now={NOW}
        timeZone="UTC"
        onSelect={() => {}}
        onNew={() => {}}
        onRename={() => {}}
        onDelete={() => {}}
        onPin={() => {}}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-conversation-list', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('navigation', { name: 'Conversation history' })).toBe(root);
    expect(screen.getAllByRole('heading', { level: 3 }).map((h) => h.textContent)).toEqual([
      'Pinned',
      'Today',
      'Previous 7 days',
      'Older',
    ]);
    const today = screen.getByRole('region', { name: 'Today' });
    const rows = within(today)
      .getAllByRole('button')
      .filter((b) => !b.getAttribute('aria-haspopup'));
    expect(rows.map((r) => r.textContent)).toEqual(['Hiring plan', 'Launch checklistWeek 9']);
    const active = screen.getByRole('button', { name: 'Launch checklist' });
    expect(active).toHaveAttribute('aria-current', 'page');
    expect(active).toHaveAccessibleDescription('Week 9');
    expect(active).toHaveAttribute('aria-keyshortcuts', 'F2 Delete');
    expect(
      screen.getByRole('button', { name: 'More actions for “Launch checklist”' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'New conversation' })).toBeInTheDocument();
    expect(screen.getByRole('searchbox', { name: 'Search conversations' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('selects with click and moves between rows with arrows, Home and End', async () => {
    const onSelect = vi.fn();
    const user = userEvent.setup();
    render(
      <AiConversationList
        conversations={CONVERSATIONS}
        now={NOW}
        timeZone="UTC"
        searchable={false}
        onSelect={onSelect}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Budget review' }));
    expect(onSelect).toHaveBeenCalledWith('c');
    screen.getByRole('button', { name: 'Roadmap' }).focus();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('button', { name: 'Hiring plan' })).toHaveFocus();
    await user.keyboard('{End}');
    expect(screen.getByRole('button', { name: 'Team offsite' })).toHaveFocus();
    await user.keyboard('{ArrowUp}');
    expect(screen.getByRole('button', { name: 'Budget review' })).toHaveFocus();
    await user.keyboard('{Home}');
    expect(screen.getByRole('button', { name: 'Roadmap' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onSelect).toHaveBeenLastCalledWith('e');
    expect(screen.queryByRole('button', { name: /More actions/ })).toBeNull();
  });

  it('filters titles and previews, announcing the count, and shows no-results', async () => {
    const user = userEvent.setup();
    render(
      <AiConversationList
        conversations={CONVERSATIONS}
        now={NOW}
        timeZone="UTC"
        onSelect={() => {}}
      />,
    );
    await user.type(screen.getByRole('searchbox'), 'week');
    expect(screen.getAllByRole('listitem')).toHaveLength(1);
    expect(screen.getByRole('status')).toHaveTextContent('1 conversation');
    await user.type(screen.getByRole('searchbox'), 'zzz');
    expect(screen.getByText('No conversations match “weekzzz”.')).toBeInTheDocument();
  });

  it('delegates search to onSearch without filtering', async () => {
    const onSearch = vi.fn();
    const user = userEvent.setup();
    render(
      <AiConversationList
        conversations={CONVERSATIONS}
        now={NOW}
        onSelect={() => {}}
        onSearch={onSearch}
      />,
    );
    await user.type(screen.getByRole('searchbox'), 'ab');
    expect(onSearch).toHaveBeenLastCalledWith('ab');
    expect(screen.getAllByRole('listitem')).toHaveLength(5);
  });

  it('shows an empty state', () => {
    render(<AiConversationList conversations={[]} onSelect={() => {}} />);
    expect(screen.getByText('No conversations yet.')).toBeInTheDocument();
  });

  it('renames inline from the menu, saving with Enter and restoring focus', async () => {
    const onRename = vi.fn();
    const user = userEvent.setup();
    render(<Stateful onRename={onRename} />);
    await user.click(screen.getByRole('button', { name: 'More actions for “Hiring plan”' }));
    await user.click(await screen.findByRole('menuitem', { name: /Rename/ }));
    const input = await screen.findByRole('textbox', { name: 'Conversation title' });
    await waitFor(() => expect(input).toHaveFocus());
    expect(input).toHaveValue('Hiring plan');
    await user.clear(input);
    await user.type(input, 'Hiring plan Q3{Enter}');
    expect(onRename).toHaveBeenCalledWith('b', 'Hiring plan Q3');
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Hiring plan Q3' })).toHaveFocus(),
    );
  });

  it('renames with F2 and cancels with Escape', async () => {
    const onRename = vi.fn();
    const user = userEvent.setup();
    render(<Stateful onRename={onRename} />);
    screen.getByRole('button', { name: 'Budget review' }).focus();
    await user.keyboard('{F2}');
    expect(screen.getByRole('textbox', { name: 'Conversation title' })).toHaveFocus();
    await user.keyboard('x{Escape}');
    expect(onRename).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Budget review' })).toHaveFocus();
  });

  it('pins and unpins from the menu', async () => {
    const user = userEvent.setup();
    render(<Stateful />);
    await user.click(screen.getByRole('button', { name: 'More actions for “Budget review”' }));
    await user.click(await screen.findByRole('menuitem', { name: 'Pin' }));
    expect(
      within(screen.getByRole('region', { name: 'Pinned' })).getByRole('button', {
        name: 'Budget review',
      }),
    ).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'More actions for “Roadmap”' }));
    await user.click(await screen.findByRole('menuitem', { name: 'Unpin' }));
    expect(screen.getByRole('region', { name: 'Older' })).toHaveTextContent('Roadmap');
  });

  it('confirms before deleting and focuses the next conversation', async () => {
    const onDelete = vi.fn();
    const user = userEvent.setup();
    render(<Stateful onDelete={onDelete} />);
    screen.getByRole('button', { name: 'Hiring plan' }).focus();
    await user.keyboard('{Delete}');
    const dialog = await screen.findByRole('alertdialog', { name: 'Delete “Hiring plan”?' });
    expect(onDelete).not.toHaveBeenCalled();
    await user.click(within(dialog).getByRole('button', { name: 'Delete conversation' }));
    expect(onDelete).toHaveBeenCalledWith('b');
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
    expect(screen.queryByRole('button', { name: 'Hiring plan' })).toBeNull();
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Launch checklist' })).toHaveFocus(),
    );
  });

  it('deletes nothing when the confirmation is cancelled from the menu', async () => {
    const onDelete = vi.fn();
    const user = userEvent.setup();
    render(<Stateful onDelete={onDelete} />);
    await user.click(screen.getByRole('button', { name: 'More actions for “Team offsite”' }));
    await user.click(await screen.findByRole('menuitem', { name: /Delete/ }));
    const dialog = await screen.findByRole('alertdialog', { name: 'Delete “Team offsite”?' });
    await user.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
    expect(onDelete).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Team offsite' })).toBeInTheDocument();
  });

  it('is localised', () => {
    render(
      <AiConversationList
        conversations={CONVERSATIONS}
        now={NOW}
        timeZone="UTC"
        headingLevel={2}
        onSelect={() => {}}
        messages={{ label: 'Verlauf', groups: { today: 'Heute' } }}
      />,
    );
    expect(screen.getByRole('navigation', { name: 'Verlauf' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Heute' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Pinned' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiConversationList conversations={CONVERSATIONS} now={NOW} onSelect={() => {}} />,
    );
    expect(html).toContain('mn-ai-conversation-list');
    expect(html).toContain('Launch checklist');
  });
});
