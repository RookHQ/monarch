import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiModeSwitcher, type AiMode } from './index.js';

const MODES: AiMode[] = [
  { id: 'ask', label: 'Ask', description: 'Answers questions. Changes nothing.' },
  { id: 'edit', label: 'Edit', description: 'Proposes edits you review before they apply.' },
  { id: 'agent', label: 'Agent', description: 'Runs tools and asks before risky steps.' },
  { id: 'lab', label: 'Lab', disabled: true },
];

function Controlled({ onChange }: { onChange?: (id: string) => void }) {
  const [mode, setMode] = useState('ask');
  return (
    <AiModeSwitcher
      modes={MODES}
      value={mode}
      onChange={(id) => {
        setMode(id);
        onChange?.(id);
      }}
    />
  );
}

describe('AiModeSwitcher', () => {
  it('is a named radio group described by the selected mode', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiModeSwitcher
        ref={ref}
        data-testid="root"
        className="extra"
        modes={MODES}
        value="edit"
        onChange={() => {}}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-mode-switcher', 'extra');
    expect(root).toHaveAttribute('data-mode', 'edit');
    expect(ref.current).toBe(root);
    const group = screen.getByRole('radiogroup', { name: 'Mode' });
    expect(group).toHaveAccessibleDescription('Proposes edits you review before they apply.');
    expect(screen.getByRole('radio', { name: 'Edit' })).toBeChecked();
    expect(screen.getByRole('radio', { name: 'Lab' })).toBeDisabled();
    await expectNoA11yViolations();
  });

  it('moves and selects with the arrow keys and reads the new description', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<Controlled onChange={onChange} />);
    await user.tab();
    expect(screen.getByRole('radio', { name: 'Ask' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(onChange).toHaveBeenLastCalledWith('edit');
    expect(screen.getByRole('radio', { name: 'Edit' })).toHaveFocus();
    expect(screen.getByRole('radio', { name: 'Edit' })).toBeChecked();
    const description = screen.getByText('Proposes edits you review before they apply.');
    expect(description).toHaveAttribute('aria-live', 'polite');
    // Skips the disabled mode and wraps.
    await user.keyboard('{ArrowRight}{ArrowRight}');
    expect(onChange).toHaveBeenLastCalledWith('ask');
    await user.click(screen.getByRole('radio', { name: 'Agent' }));
    expect(onChange).toHaveBeenLastCalledWith('agent');
    expect(screen.getByRole('radiogroup')).toHaveAccessibleDescription(
      'Runs tools and asks before risky steps.',
    );
  });

  it('can hide descriptions, be disabled and take messages', () => {
    render(
      <AiModeSwitcher
        modes={MODES}
        value="ask"
        onChange={() => {}}
        showDescription={false}
        disabled
        messages={{ label: 'Assistant mode' }}
      />,
    );
    const group = screen.getByRole('radiogroup', { name: 'Assistant mode' });
    expect(group).not.toHaveAttribute('aria-describedby');
    expect(group).toHaveAttribute('aria-disabled', 'true');
    expect(screen.queryByText('Answers questions. Changes nothing.')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiModeSwitcher modes={MODES} value="ask" onChange={() => {}} />);
    expect(html).toContain('mn-ai-mode-switcher');
    expect(html).toContain('Answers questions. Changes nothing.');
  });
});
