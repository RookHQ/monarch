import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  AiContextChips,
  aiContextChipWeight,
  formatAiContextTokens,
  type AiContextChip,
} from './index.js';

const ITEMS: AiContextChip[] = [
  { id: 'f', kind: 'file', label: 'Q3 plan.pdf', tokens: 1200 },
  { id: 's', kind: 'selection', label: 'Selected text', tokens: 80 },
  { id: 'p', kind: 'page', label: 'Overview', removable: false },
  { id: 'r', kind: 'record', label: 'Account 1042', tokens: 400 },
];

function Controlled({ initial = ITEMS }: { initial?: AiContextChip[] }) {
  const [items, setItems] = useState(initial);
  const input = useRef<HTMLInputElement>(null);
  return (
    <>
      <input ref={input} aria-label="Prompt" />
      <AiContextChips
        items={items}
        fallbackFocusRef={input}
        onRemove={(id) => setItems((list) => list.filter((i) => i.id !== id))}
      />
    </>
  );
}

describe('formatAiContextTokens / aiContextChipWeight', () => {
  it('formats compactly and stays stable', () => {
    expect(formatAiContextTokens(950)).toBe('950');
    expect(formatAiContextTokens(1234)).toBe('1.2K');
    expect(formatAiContextTokens(18000)).toBe('18K');
    expect(formatAiContextTokens(Number.NaN)).toBe('0');
  });

  it('weights by share of the reference', () => {
    expect(aiContextChipWeight(50, 100)).toBe('heavy');
    expect(aiContextChipWeight(20, 100)).toBe('medium');
    expect(aiContextChipWeight(19, 100)).toBe('light');
    expect(aiContextChipWeight(10, 0)).toBe('light');
  });
});

describe('AiContextChips', () => {
  it('lists each chip with its kind, name and tokens, and a total', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiContextChips
        ref={ref}
        data-testid="root"
        className="extra"
        items={ITEMS}
        onRemove={() => {}}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-context-chips', 'extra');
    expect(ref.current).toBe(root);
    const list = screen.getByRole('list', { name: 'Context' });
    const chips = within(list).getAllByRole('listitem');
    expect(chips).toHaveLength(4);
    expect(chips[0]).toHaveTextContent('File: Q3 plan.pdf, 1.2K tokens');
    expect(chips[0]).toHaveAttribute('data-kind', 'file');
    expect(chips[0]).toHaveAttribute('data-weight', 'heavy');
    expect(chips[1]).toHaveAttribute('data-weight', 'light');
    expect(chips[3]).toHaveAttribute('data-weight', 'medium');
    expect(chips[2]).not.toHaveAttribute('data-weight');
    expect(root).toHaveTextContent('≈ 1.7K tokens in context');
    expect(screen.getByRole('button', { name: 'Remove Q3 plan.pdf from context' })).toBeVisible();
    // Not removable: no button.
    expect(within(chips[2] as HTMLElement).queryByRole('button')).toBeNull();
    await expectNoA11yViolations();
  });

  it('weights against a budget when given', () => {
    render(<AiContextChips items={ITEMS} budget={100_000} />);
    for (const chip of screen.getAllByRole('listitem').filter((c) => c.dataset.weight)) {
      expect(chip).toHaveAttribute('data-weight', 'light');
    }
  });

  it('removes from the keyboard, moves focus to the next chip and announces', async () => {
    const user = userEvent.setup();
    render(<Controlled />);
    const first = screen.getByRole('button', { name: 'Remove Q3 plan.pdf from context' });
    first.focus();
    await user.keyboard('{Enter}');
    expect(screen.queryByText('Q3 plan.pdf')).toBeNull();
    expect(screen.getByRole('button', { name: 'Remove Selected text from context' })).toHaveFocus();
    expect(screen.getByRole('status')).toHaveTextContent('Q3 plan.pdf removed from context');
  });

  it('moves focus to the previous chip, then to the fallback', async () => {
    const user = userEvent.setup();
    render(
      <Controlled
        initial={[
          { id: 'a', kind: 'app', label: 'Calendar' },
          { id: 'b', kind: 'person', label: 'Ana Lopez' },
        ]}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Remove Ana Lopez from context' }));
    expect(screen.getByRole('button', { name: 'Remove Calendar from context' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(screen.getByRole('textbox', { name: 'Prompt' })).toHaveFocus();
    expect(screen.queryByRole('list')).toBeNull();
  });

  it('hides the total without tokens, shows empty content and takes messages', () => {
    const onRemove = vi.fn();
    const { rerender } = render(
      <AiContextChips
        data-testid="root"
        items={[{ id: 'a', kind: 'page', label: 'Overview' }]}
        onRemove={onRemove}
        messages={{ label: 'Kontext', kind: { page: 'Seite' }, remove: (l) => `${l} entfernen` }}
      />,
    );
    expect(screen.getByRole('list', { name: 'Kontext' })).toHaveTextContent('Seite: Overview');
    expect(screen.getByTestId('root')).not.toHaveTextContent('in context');
    expect(screen.getByRole('button', { name: 'Overview entfernen' })).toBeInTheDocument();
    rerender(<AiContextChips data-testid="root" items={[]} empty="Nothing attached" />);
    expect(screen.getByTestId('root')).toHaveTextContent('Nothing attached');
    expect(screen.getByTestId('root')).toHaveAttribute('data-empty');
  });

  it('renders on the server', () => {
    const html = renderToString(<AiContextChips items={ITEMS} onRemove={() => {}} />);
    expect(html).toContain('mn-ai-context-chips');
    expect(html).toContain('1.2K tokens');
  });
});
