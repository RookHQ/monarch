import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiAgentPicker, aiAgentKey, type AiAgentPickerOption } from './index.js';

const agents: AiAgentPickerOption[] = [
  {
    agent: { id: 'research', name: 'Research agent', role: 'Finds sources' },
    description: 'Answers with citations.',
    capabilities: ['Summarises'],
    tools: [{ name: 'Search' }],
  },
  {
    agent: { id: 'writer', name: 'Writing agent', role: 'Drafts text' },
    description: 'Drafts and edits.',
    state: 'working',
  },
  { agent: { id: 'locked', name: 'Billing agent' }, disabled: true },
  { agent: { name: 'Scheduling agent' } },
];

function Controlled(props: { onChange?: (v: string) => void; initial?: string | null }) {
  const [value, setValue] = useState<string | null>(props.initial ?? null);
  return (
    <AiAgentPicker
      agents={agents}
      value={value}
      onChange={(v) => {
        setValue(v);
        props.onChange?.(v);
      }}
    />
  );
}

describe('AiAgentPicker', () => {
  it('renders a labelled radio group of agents, named by their names', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiAgentPicker ref={ref} agents={agents} value="writer" data-testid="root" className="x" />,
    );
    const group = screen.getByRole('radiogroup', { name: 'Choose an agent' });
    expect(group).toBe(ref.current);
    expect(group).toHaveClass('mn-ai-agent-picker', 'x');
    const radios = screen.getAllByRole('radio');
    expect(radios).toHaveLength(4);
    expect(screen.getByRole('radio', { name: 'Writing agent' })).toHaveAttribute(
      'aria-checked',
      'true',
    );
    expect(screen.getByRole('radio', { name: 'Research agent' })).toHaveAccessibleDescription(
      'Answers with citations.',
    );
    expect(screen.getByRole('radio', { name: 'Billing agent' })).toHaveAttribute(
      'aria-disabled',
      'true',
    );
    // Roving tab stop sits on the chosen agent.
    expect(radios.map((r) => r.tabIndex)).toEqual([-1, 0, -1, -1]);
    await expectNoA11yViolations();
  });

  it('chooses on click and ignores disabled agents', async () => {
    const onChange = vi.fn();
    render(<Controlled onChange={onChange} />);
    await userEvent.click(screen.getByRole('radio', { name: 'Billing agent' }));
    expect(onChange).not.toHaveBeenCalled();
    await userEvent.click(screen.getByText('Writing agent'));
    expect(onChange).toHaveBeenCalledWith('writer');
    expect(screen.getByRole('radio', { name: 'Writing agent' })).toHaveAttribute(
      'aria-checked',
      'true',
    );
    expect(screen.getByRole('radio', { name: 'Writing agent' })).toHaveFocus();
  });

  it('moves and chooses with arrow keys, skipping disabled agents and wrapping', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(<Controlled onChange={onChange} />);
    await user.tab();
    expect(screen.getByRole('radio', { name: 'Research agent' })).toHaveFocus();
    await user.keyboard(' ');
    expect(onChange).toHaveBeenLastCalledWith('research');
    await user.keyboard('{ArrowDown}');
    expect(onChange).toHaveBeenLastCalledWith('writer');
    await user.keyboard('{ArrowDown}');
    // Skips the disabled Billing agent; a missing id falls back to the name.
    expect(onChange).toHaveBeenLastCalledWith('Scheduling agent');
    expect(screen.getByRole('radio', { name: 'Scheduling agent' })).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(onChange).toHaveBeenLastCalledWith('research');
    await user.keyboard('{ArrowUp}');
    expect(onChange).toHaveBeenLastCalledWith('Scheduling agent');
    await user.keyboard('{Home}');
    expect(onChange).toHaveBeenLastCalledWith('research');
    await user.keyboard('{End}');
    expect(onChange).toHaveBeenLastCalledWith('Scheduling agent');
    await user.tab();
    expect(document.body).toHaveFocus();
  });

  it('disables the whole group', async () => {
    const onChange = vi.fn();
    render(<AiAgentPicker agents={agents} value={null} disabled onChange={onChange} />);
    await userEvent.click(screen.getByText('Research agent'));
    expect(onChange).not.toHaveBeenCalled();
    expect(screen.getByRole('radiogroup')).toHaveAttribute('aria-disabled', 'true');
  });

  it('shows an empty message, a grid layout and translated strings', () => {
    const { container } = render(
      <AiAgentPicker
        agents={[]}
        value={null}
        layout="grid"
        messages={{ label: 'Agents', empty: 'Nothing here' }}
      />,
    );
    expect(screen.getByRole('radiogroup', { name: 'Agents' })).toHaveTextContent('Nothing here');
    expect(container.firstElementChild).toHaveAttribute('data-layout', 'grid');
  });

  it('uses aria-labelledby when given', () => {
    render(
      <>
        <h2 id="h">Pick a helper</h2>
        <AiAgentPicker agents={agents} value={null} aria-labelledby="h" />
      </>,
    );
    expect(screen.getByRole('radiogroup', { name: 'Pick a helper' })).toBeInTheDocument();
  });

  it('keys agents by id, falling back to name', () => {
    expect(aiAgentKey({ id: 'a', name: 'A' })).toBe('a');
    expect(aiAgentKey({ name: 'B' })).toBe('B');
  });

  it('renders on the server', () => {
    const html = renderToString(<AiAgentPicker agents={agents} value="research" />);
    expect(html).toContain('mn-ai-agent-picker');
    expect(html).toContain('role="radiogroup"');
  });
});
