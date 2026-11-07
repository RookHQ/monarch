import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiPromptInput } from '../ai-prompt-input/index.js';
import { AiContextChips, type AiContextChip } from '../ai-context-chips/index.js';
import {
  AiMentionContext,
  aiMentionToContextChip,
  useAiMentionContext,
  type AiMentionItem,
  type AiMentionSearch,
} from './index.js';

const ITEMS: AiMentionItem[] = [
  { id: 'u1', kind: 'person', label: 'Ana Lopez', description: 'Design lead' },
  { id: 'd1', kind: 'doc', label: 'Quarterly plan', description: 'Docs / Planning', tokens: 4200 },
  { id: 'r1', kind: 'record', label: 'Account 1042', tokens: 300 },
  { id: 'a1', kind: 'app', label: 'Calendar', disabled: true },
];

const localSearch: AiMentionSearch = (q) =>
  ITEMS.filter((i) => i.label.toLowerCase().includes(q.toLowerCase()));

function Harness({
  search = localSearch,
  onSelect,
}: {
  search?: AiMentionSearch;
  onSelect?: (item: AiMentionItem) => unknown;
}) {
  const [context, setContext] = useState<AiContextChip[]>([]);
  const mention = useAiMentionContext({
    search,
    debounceMs: 0,
    exclude: context.map((c) => c.id),
    onSelect: (item) => {
      setContext((list) => [...list, aiMentionToContextChip(item)]);
      return onSelect?.(item);
    },
  });
  return (
    <>
      <AiContextChips
        items={context}
        onRemove={(id) => setContext((l) => l.filter((c) => c.id !== id))}
      />
      <AiPromptInput label="Message the assistant" onSubmit={() => {}} {...mention} />
    </>
  );
}

describe('aiMentionToContextChip', () => {
  it('maps kinds and keeps tokens and description', () => {
    expect(aiMentionToContextChip(ITEMS[1] as AiMentionItem)).toEqual({
      id: 'd1',
      kind: 'file',
      label: 'Quarterly plan',
      tokens: 4200,
      description: 'Docs / Planning',
    });
    expect(aiMentionToContextChip(ITEMS[0] as AiMentionItem).kind).toBe('person');
    expect(aiMentionToContextChip(ITEMS[2] as AiMentionItem)).not.toHaveProperty('description');
  });
});

describe('AiMentionContext', () => {
  it('groups results by kind in a named listbox', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiMentionContext
        ref={ref}
        data-testid="root"
        items={ITEMS}
        activeId="d1"
        onSelect={() => {}}
      />,
    );
    expect(ref.current).toBe(screen.getByTestId('root'));
    expect(ref.current).toHaveClass('mn-ai-mention-context');
    const list = screen.getByRole('listbox', { name: 'Add context' });
    const groups = within(list).getAllByRole('group');
    expect(groups).toHaveLength(4);
    expect(within(list).getByRole('group', { name: 'Documents' })).toHaveTextContent(
      'Quarterly plan',
    );
    expect(within(list).getByRole('group', { name: 'People' })).toHaveTextContent('Ana Lopez');
    const doc = screen.getByRole('option', { name: 'Quarterly plan' });
    expect(doc).toHaveAccessibleDescription('Docs / Planning');
    expect(doc).toHaveAttribute('aria-selected', 'true');
    expect(doc).toHaveTextContent('4.2K tokens');
    expect(screen.getByRole('option', { name: 'Calendar' })).toHaveAttribute(
      'aria-disabled',
      'true',
    );
    await expectNoA11yViolations();
  });

  it('shows loading, empty and error states with retry', async () => {
    const onRetry = vi.fn();
    const { rerender } = render(
      <AiMentionContext items={[]} status="loading" onSelect={() => {}} />,
    );
    expect(screen.getAllByText('Searching…').length).toBeGreaterThan(0);
    rerender(<AiMentionContext items={[]} query="zz" onSelect={() => {}} />);
    expect(screen.getAllByText('Nothing matches “zz”').length).toBeGreaterThan(0);
    rerender(<AiMentionContext items={[]} status="error" onRetry={onRetry} onSelect={() => {}} />);
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(onRetry).toHaveBeenCalled();
  });
});

describe('useAiMentionContext', () => {
  it('searches on @, picks with the keyboard and produces a chip', async () => {
    const user = userEvent.setup();
    const search = vi.fn(localSearch);
    render(<Harness search={search} />);
    const box = screen.getByRole('textbox', { name: 'Message the assistant' });
    await user.click(box);
    await user.keyboard('Read @');
    const list = await screen.findByRole('listbox', { name: 'Add context' });
    expect(search).toHaveBeenLastCalledWith(
      '',
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    );
    expect(box).toHaveAttribute('aria-controls', list.id);
    // Display order: documents first.
    expect(box).toHaveAttribute(
      'aria-activedescendant',
      screen.getByRole('option', { name: 'Quarterly plan' }).id,
    );
    await user.keyboard('{ArrowDown}');
    expect(box).toHaveAttribute(
      'aria-activedescendant',
      screen.getByRole('option', { name: 'Ana Lopez' }).id,
    );
    await user.keyboard('{Enter}');
    expect(screen.queryByRole('listbox')).toBeNull();
    expect(box).toHaveValue('Read ');
    expect(screen.getByRole('list', { name: 'Context' })).toHaveTextContent('Person: Ana Lopez');
    expect(box).toHaveFocus();

    // Already-added items are excluded from the next search. (The input restores the caret on the next frame.)
    await new Promise((resolve) => requestAnimationFrame(resolve));
    await user.keyboard('@');
    await screen.findByRole('listbox');
    expect(screen.queryByRole('option', { name: 'Ana Lopez' })).toBeNull();
    await user.keyboard('acc');
    await screen.findByRole('option', { name: 'Account 1042' });
    await user.keyboard('{Tab}');
    expect(screen.getByRole('list', { name: 'Context' })).toHaveTextContent('Record: Account 1042');
  });

  it('inserts returned text and picks with the pointer', async () => {
    const user = userEvent.setup();
    render(<Harness onSelect={(item) => `@${item.label} `} />);
    const box = screen.getByRole('textbox');
    await user.click(box);
    await user.keyboard('@qu');
    await user.click(await screen.findByRole('option', { name: 'Quarterly plan' }));
    expect(box).toHaveValue('@Quarterly plan ');
    expect(box).toHaveFocus();
  });

  it('shows an error and retries', async () => {
    const user = userEvent.setup();
    let fail = true;
    const search: AiMentionSearch = async (q) => {
      if (fail) throw new Error('down');
      return localSearch(q, { signal: new AbortController().signal });
    };
    render(<Harness search={search} />);
    await user.click(screen.getByRole('textbox'));
    await user.keyboard('@');
    const retry = await screen.findByRole('button', { name: 'Retry' });
    expect(screen.getByRole('textbox')).not.toHaveAttribute('aria-controls');
    fail = false;
    await user.click(retry);
    expect(await screen.findByRole('listbox')).toBeInTheDocument();
  });

  it('aborts the previous search as the query changes', async () => {
    const user = userEvent.setup();
    const signals: AbortSignal[] = [];
    const search: AiMentionSearch = (q, { signal }) => {
      signals.push(signal);
      return new Promise((resolve) => setTimeout(() => resolve(localSearch(q, { signal })), 20));
    };
    render(<Harness search={search} />);
    await user.click(screen.getByRole('textbox'));
    await user.keyboard('@a');
    await screen.findByRole('listbox');
    expect(signals.length).toBeGreaterThan(1);
    expect(signals.slice(0, -1).every((s) => s.aborted)).toBe(true);
  });
});

it('renders on the server', () => {
  expect(renderToString(<AiMentionContext items={ITEMS} onSelect={() => {}} />)).toContain(
    'mn-ai-mention-context',
  );
  expect(renderToString(<Harness />)).toContain('aria-autocomplete="list"');
});
