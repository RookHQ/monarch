import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  EscalationPolicyEditor,
  validateEscalationPolicy,
  type EscalationLevel,
  type EscalationPolicyEditorProps,
  type EscalationTarget,
} from './index.js';

const TARGETS: EscalationTarget[] = [
  { value: 'oncall', label: 'On-call rota' },
  { value: 'chat', label: 'Team chat', description: 'Chat channel' },
  { value: 'lead', label: 'Team lead' },
];
const START: EscalationLevel[] = [
  { id: 'a', afterMinutes: 15, targets: ['oncall'] },
  { id: 'b', afterMinutes: 60, targets: ['chat', 'lead'] },
];

function Harness({
  initial = START,
  onLevels,
  ...props
}: Partial<EscalationPolicyEditorProps> & {
  initial?: EscalationLevel[];
  onLevels?: (l: EscalationLevel[]) => void;
}) {
  const [levels, setLevels] = useState(initial);
  return (
    <EscalationPolicyEditor
      targets={TARGETS}
      {...props}
      value={levels}
      onChange={(l) => {
        setLevels(l);
        onLevels?.(l);
      }}
    />
  );
}

describe('EscalationPolicyEditor', () => {
  it('renders the ladder with summaries and labelled controls, without axe violations', async () => {
    render(<Harness description="Each level fires only if nobody has acknowledged yet." />);
    const list = screen.getByRole('list', { name: 'Escalation levels' });
    expect(within(list).getAllByRole('listitem')).toHaveLength(2);
    const l2 = screen.getByRole('group', { name: 'Level 2' });
    expect(l2).toHaveTextContent('After 1h, notify Team chat, Team lead');
    expect(within(l2).getByRole('spinbutton', { name: 'After' })).toBeInTheDocument();
    expect(within(l2).getByRole('group', { name: 'Notify' })).toBeInTheDocument();
    expect(within(l2).getByRole('checkbox', { name: /Team lead/ })).toBeChecked();
    await expectNoA11yViolations();
  });

  it('toggles targets in target order and edits the time', async () => {
    const user = userEvent.setup();
    const onLevels = vi.fn();
    render(<Harness onLevels={onLevels} />);
    const l1 = screen.getByRole('group', { name: 'Level 1' });
    const lead = within(l1).getByRole('checkbox', { name: /Team lead/ });
    lead.focus();
    await user.keyboard(' ');
    await user.click(within(l1).getByRole('checkbox', { name: /Team chat/ }));
    expect(onLevels.mock.lastCall?.[0][0].targets).toEqual(['oncall', 'chat', 'lead']);
    const after = within(l1).getByRole('spinbutton', { name: 'After' });
    await user.clear(after);
    await user.type(after, '30m{Enter}');
    expect(onLevels.mock.lastCall?.[0][0].afterMinutes).toBe(30);
  });

  it('adds a later level and removes levels within the limits', async () => {
    const user = userEvent.setup();
    const onLevels = vi.fn();
    render(<Harness onLevels={onLevels} maxLevels={3} minLevels={2} />);
    expect(screen.getByRole('button', { name: 'Remove level 1' })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Add level' }));
    expect(onLevels.mock.lastCall?.[0][2]).toMatchObject({ afterMinutes: 75, targets: [] });
    expect(screen.getByText('A policy can have up to 3 levels.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Remove level 3' }));
    expect(onLevels.mock.lastCall?.[0]).toHaveLength(2);
  });

  it('shows problems when asked and warns without targets', () => {
    const levels: EscalationLevel[] = [
      { id: 'a', afterMinutes: 30, targets: ['oncall'] },
      { id: 'b', afterMinutes: 20, targets: ['chat'] },
      { id: 'c', afterMinutes: 90, targets: [] },
    ];
    const { rerender } = render(
      <EscalationPolicyEditor value={levels} onChange={() => {}} targets={TARGETS} showErrors />,
    );
    expect(screen.getByRole('group', { name: 'Level 2' })).toHaveAccessibleDescription(
      'This level must come later than the one before it.',
    );
    expect(screen.getByRole('group', { name: 'Level 3' })).toHaveTextContent('notify nobody yet');
    rerender(<EscalationPolicyEditor value={levels} onChange={() => {}} targets={[]} />);
    expect(screen.getByRole('note')).toHaveTextContent('There is nothing to notify yet.');
  });

  it('validates cumulative and interval timing', () => {
    expect(validateEscalationPolicy(START)).toEqual({});
    const swapped = [START[1]!, START[0]!];
    expect(validateEscalationPolicy(swapped)).toEqual({ a: 'order' });
    expect(validateEscalationPolicy(swapped, 'interval')).toEqual({});
  });

  it('renders on the server', () => {
    const html = renderToString(
      <EscalationPolicyEditor value={START} onChange={() => {}} targets={TARGETS} />,
    );
    expect(html).toContain('mn-escalation-policy-editor');
  });
});
