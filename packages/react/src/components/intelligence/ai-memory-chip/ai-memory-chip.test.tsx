import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiMemoryChip, shortMemoryText, type AiMemory } from './index.js';

const memory: AiMemory = {
  id: 'm1',
  text: 'Prefers summaries under 200 words',
  entity: { kind: 'person', name: 'Ada Lovelace' },
};

describe('shortMemoryText', () => {
  it('collapses whitespace and truncates long text', () => {
    expect(shortMemoryText('  a \n b ')).toBe('a b');
    expect(shortMemoryText('x'.repeat(60), 10)).toBe(`${'x'.repeat(9)}…`);
  });
});

describe('AiMemoryChip', () => {
  it('renders "Remembered:" with the entity, forwards props and the ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(<AiMemoryChip ref={ref} memory={memory} data-testid="root" className="extra" />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-memory-chip', 'extra');
    expect(root).toHaveAttribute('data-variant', 'chip');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'Memory' })).toHaveTextContent(
      'Remembered: Prefers summaries under 200 words · Ada Lovelace',
    );
    expect(root.querySelector('.mn-ai-memory-chip__icon svg')).not.toBeNull();
    expect(screen.queryByRole('button')).toBeNull();
    await expectNoA11yViolations();
  });

  it('row variant has no prefix and shows leading and meta content', () => {
    render(
      <AiMemoryChip
        memory={memory}
        variant="row"
        leading={<input type="checkbox" aria-label="Pick" />}
        meta="Updated today"
      />,
    );
    const group = screen.getByRole('group', { name: 'Memory' });
    expect(group).toHaveTextContent('Prefers summaries under 200 words');
    expect(group).not.toHaveTextContent('Remembered:');
    expect(group).not.toHaveTextContent('Ada Lovelace');
    expect(group).toHaveTextContent('Updated today');
    expect(screen.getByRole('checkbox', { name: 'Pick' })).toBeInTheDocument();
  });

  it('forgets through a named button', async () => {
    const user = userEvent.setup();
    const onForget = vi.fn();
    render(<AiMemoryChip memory={memory} onForget={onForget} />);
    await user.click(
      screen.getByRole('button', { name: 'Forget memory: Prefers summaries under 200 words' }),
    );
    expect(onForget).toHaveBeenCalledWith('m1');
  });

  it('edits inline: Enter saves the trimmed text and focus returns to Edit', async () => {
    const user = userEvent.setup();
    const onEdit = vi.fn();
    render(<AiMemoryChip memory={memory} onEdit={onEdit} />);
    const edit = screen.getByRole('button', { name: /Edit memory/ });
    await user.click(edit);
    const field = screen.getByRole('textbox', { name: 'Memory' });
    expect(field).toHaveFocus();
    expect(field).toHaveAccessibleDescription('Enter to save, Escape to cancel');
    await expectNoA11yViolations();
    await user.clear(field);
    await user.type(field, '  Prefers bullet points  {Enter}');
    expect(onEdit).toHaveBeenCalledWith('m1', 'Prefers bullet points');
    expect(screen.queryByRole('textbox')).toBeNull();
    expect(screen.getByRole('button', { name: /Edit memory/ })).toHaveFocus();
  });

  it('Escape cancels, Shift+Enter adds a line, empty and unchanged text are not saved', async () => {
    const user = userEvent.setup();
    const onEdit = vi.fn();
    render(<AiMemoryChip memory={memory} onEdit={onEdit} />);
    await user.click(screen.getByRole('button', { name: /Edit memory/ }));
    await user.type(screen.getByRole('textbox'), '{Escape}');
    expect(screen.queryByRole('textbox')).toBeNull();
    expect(screen.getByRole('button', { name: /Edit memory/ })).toHaveFocus();

    await user.keyboard('{Enter}');
    const field = screen.getByRole('textbox');
    await user.clear(field);
    expect(field).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();
    await user.type(field, '{Enter}');
    expect(screen.getByRole('textbox')).toBeInTheDocument();
    await user.type(field, 'a{Shift>}{Enter}{/Shift}b');
    expect(field).toHaveValue('a\nb');
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onEdit).not.toHaveBeenCalled();

    await user.click(screen.getByRole('button', { name: /Edit memory/ }));
    await user.click(screen.getByRole('button', { name: 'Save' }));
    expect(onEdit).not.toHaveBeenCalled();
  });

  it('localises strings', () => {
    render(
      <AiMemoryChip
        memory={memory}
        onForget={() => {}}
        messages={{ label: 'Erinnerung', prefix: 'Gemerkt:', forget: () => 'Vergessen' }}
      />,
    );
    expect(screen.getByRole('group', { name: 'Erinnerung' })).toHaveTextContent('Gemerkt:');
    expect(screen.getByRole('button', { name: 'Vergessen' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiMemoryChip memory={memory} onEdit={() => {}} />);
    expect(html).toContain('mn-ai-memory-chip');
    expect(html).toContain('Remembered:');
  });
});
