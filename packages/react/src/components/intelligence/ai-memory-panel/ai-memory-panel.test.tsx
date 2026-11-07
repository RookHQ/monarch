import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiMemoryPanel, groupAiMemories, type AiMemoryItem } from './index.js';

const MEMORIES: AiMemoryItem[] = [
  {
    id: 'a1',
    text: 'Prefers summaries under 200 words',
    entity: { kind: 'person', name: 'Ada Lovelace' },
    updatedAt: '2026-10-01T10:00:00Z',
  },
  {
    id: 'a2',
    text: 'Works in the Lisbon office',
    entity: { kind: 'person', name: 'Ada Lovelace' },
    updatedAt: '2026-10-05T10:00:00Z',
  },
  {
    id: 'p1',
    text: 'Launch review is every Thursday',
    entity: { kind: 'project', name: 'Atlas' },
    updatedAt: '2026-09-01T10:00:00Z',
  },
  {
    id: 'o1',
    text: 'Fiscal year starts in April',
    entity: { kind: 'org', name: 'Northwind' },
    updatedAt: '2026-08-01T10:00:00Z',
  },
];

const fmt = { formatDate: (d: Date) => d.toISOString().slice(0, 10) };

function Harness({ onForgetMany }: { onForgetMany?: (ids: string[]) => unknown }) {
  const [memories, setMemories] = useState(MEMORIES);
  return (
    <AiMemoryPanel
      memories={memories}
      messages={fmt}
      onEdit={(id, text) =>
        setMemories((all) => all.map((x) => (x.id === id ? { ...x, text } : x)))
      }
      onForget={(id) => setMemories((all) => all.filter((x) => x.id !== id))}
      onForgetMany={async (ids) => {
        await onForgetMany?.(ids);
        setMemories((all) => all.filter((x) => !ids.includes(x.id)));
      }}
    />
  );
}

describe('groupAiMemories', () => {
  it('orders groups by kind then name and items newest first', () => {
    const groups = groupAiMemories(MEMORIES);
    expect(groups.map((g) => g.entity.name)).toEqual(['Ada Lovelace', 'Northwind', 'Atlas']);
    expect(groups[0]?.items.map((x) => x.id)).toEqual(['a2', 'a1']);
  });
});

describe('AiMemoryPanel', () => {
  it('groups memories under named headings and lists', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <AiMemoryPanel
        ref={ref}
        memories={MEMORIES}
        messages={fmt}
        onEdit={() => {}}
        onForget={() => {}}
        onForgetMany={() => {}}
        data-testid="root"
      />,
    );
    const root = screen.getByRole('region', { name: 'Memories' });
    expect(root).toHaveClass('mn-ai-memory-panel');
    expect(ref.current).toBe(root);
    expect(screen.getAllByRole('heading', { level: 3 }).map((h) => h.textContent)).toEqual([
      'Ada LovelacePerson · 2 memories',
      'NorthwindOrganization · 1 memory',
      'AtlasProject · 1 memory',
    ]);
    const ada = screen.getByRole('list', { name: /Ada Lovelace/ });
    const items = within(ada).getAllByRole('listitem');
    expect(items[0]).toHaveTextContent('Works in the Lisbon office');
    expect(items[0]).toHaveTextContent('Updated 2026-10-05');
    expect(screen.getByRole('searchbox', { name: 'Search memories' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Forget selected' })).toBeDisabled();
    await expectNoA11yViolations();
  });

  it('filters by text and entity name, announces matches and shows no-match text', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    const search = screen.getByRole('searchbox', { name: 'Search memories' });
    await user.type(search, 'north');
    expect(screen.getAllByRole('heading').map((h) => h.textContent)).toEqual([
      'NorthwindOrganization · 1 memory',
    ]);
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('1 memory matches'));
    await user.clear(search);
    await user.type(search, 'zzz');
    expect(screen.getByText('No memories match “zzz”.')).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: 'Select all shown memories' })).toBeDisabled();
  });

  it('edits a memory in place and announces it', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.click(
      screen.getByRole('button', { name: 'Edit memory: Works in the Lisbon office' }),
    );
    const field = screen.getByRole('textbox', { name: 'Memory' });
    expect(field).toHaveFocus();
    await user.clear(field);
    await user.type(field, 'Works in the Porto office{Enter}');
    expect(screen.getByText('Works in the Porto office')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Edit memory: Works in the Porto office' }),
    ).toHaveFocus();
    expect(screen.getByRole('status')).toHaveTextContent('Memory updated');
  });

  it('forgets one memory, announces it and moves focus to the next row', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.click(
      screen.getByRole('button', { name: 'Forget memory: Works in the Lisbon office' }),
    );
    expect(screen.queryByText('Works in the Lisbon office')).toBeNull();
    expect(screen.getByRole('status')).toHaveTextContent('Memory forgotten');
    expect(
      screen.getByRole('checkbox', { name: 'Select memory: Prefers summaries under 200 words' }),
    ).toHaveFocus();
  });

  it('selects by row, group and all, with tri-state checkboxes', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    const group = screen.getByRole('checkbox', { name: 'Select all memories about Ada Lovelace' });
    const all = screen.getByRole('checkbox', { name: 'Select all shown memories' });
    await user.click(
      screen.getByRole('checkbox', { name: 'Select memory: Works in the Lisbon office' }),
    );
    expect(group).toHaveAttribute('aria-checked', 'mixed');
    expect(all).toHaveAttribute('aria-checked', 'mixed');
    expect(screen.getByRole('button', { name: 'Forget selected (1)' })).toBeEnabled();
    group.focus();
    await user.keyboard(' ');
    expect(group).toHaveAttribute('aria-checked', 'true');
    await user.click(all);
    expect(all).toHaveAttribute('aria-checked', 'true');
    expect(screen.getByRole('button', { name: 'Forget selected (4)' })).toBeEnabled();
    await user.click(all);
    expect(screen.getByRole('button', { name: 'Forget selected' })).toBeDisabled();
  });

  it('bulk forget asks for confirmation; cancel keeps everything', async () => {
    const user = userEvent.setup();
    const onForgetMany = vi.fn();
    render(<Harness onForgetMany={onForgetMany} />);
    await user.click(
      screen.getByRole('checkbox', { name: 'Select all memories about Ada Lovelace' }),
    );
    await user.click(screen.getByRole('button', { name: 'Forget selected (2)' }));
    const dialog = await screen.findByRole('alertdialog', { name: 'Forget 2 memories?' });
    await expectNoA11yViolations();
    await user.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
    expect(onForgetMany).not.toHaveBeenCalled();
    expect(screen.getByText('Works in the Lisbon office')).toBeInTheDocument();
  });

  it('bulk forget confirmed: calls onForgetMany, announces, clears selection and focuses search', async () => {
    const user = userEvent.setup();
    const onForgetMany = vi.fn();
    render(<Harness onForgetMany={onForgetMany} />);
    await user.click(
      screen.getByRole('checkbox', { name: 'Select all memories about Ada Lovelace' }),
    );
    await user.click(
      screen.getByRole('checkbox', { name: 'Select memory: Fiscal year starts in April' }),
    );
    await user.click(screen.getByRole('button', { name: 'Forget selected (3)' }));
    const dialog = await screen.findByRole('alertdialog', { name: 'Forget 3 memories?' });
    await user.click(within(dialog).getByRole('button', { name: 'Forget' }));
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
    expect(onForgetMany).toHaveBeenCalledWith(['a1', 'a2', 'o1']);
    expect(screen.getAllByRole('heading').map((h) => h.textContent)).toEqual([
      'AtlasProject · 1 memory',
    ]);
    expect(screen.getByRole('status')).toHaveTextContent('3 memories forgotten');
    expect(screen.getByRole('button', { name: 'Forget selected' })).toBeDisabled();
    await waitFor(() => expect(screen.getByRole('searchbox')).toHaveFocus());
  });

  it('shows an empty state and localises strings', () => {
    render(
      <AiMemoryPanel
        memories={[]}
        onEdit={() => {}}
        onForget={() => {}}
        onForgetMany={() => {}}
        messages={{ label: 'Gedächtnis', empty: 'Noch nichts gemerkt.' }}
      />,
    );
    expect(screen.getByRole('region', { name: 'Gedächtnis' })).toHaveTextContent(
      'Noch nichts gemerkt.',
    );
    expect(screen.queryByRole('searchbox')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiMemoryPanel
        memories={MEMORIES}
        onEdit={() => {}}
        onForget={() => {}}
        onForgetMany={() => {}}
      />,
    );
    expect(html).toContain('mn-ai-memory-panel');
    expect(html).toContain('Ada Lovelace');
  });
});
