import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  ApprovalChainEditor,
  summarizeApprovalChain,
  validateApprovalChain,
  type ApprovalApproverType,
  type ApprovalChainEditorProps,
  type ApprovalChainStep,
} from './index.js';

const TYPES: ApprovalApproverType[] = [
  { value: 'manager', label: 'Reporting manager' },
  { value: 'head', label: 'Department head' },
  {
    value: 'role',
    label: 'Anyone with a role',
    targetLabel: 'Role',
    targets: [
      { value: 'finance', label: 'Finance team' },
      { value: 'hr', label: 'People team' },
    ],
  },
];

const START: ApprovalChainStep[] = [
  { id: 'a', approver: 'manager' },
  { id: 'b', approver: 'role', target: 'finance', label: 'Finance check' },
];

function Harness({
  onSteps,
  ...props
}: Partial<ApprovalChainEditorProps> & { onSteps?: (s: ApprovalChainStep[]) => void }) {
  const [steps, setSteps] = useState<ApprovalChainStep[]>(props.value ? [...props.value] : START);
  return (
    <ApprovalChainEditor
      approverTypes={TYPES}
      {...props}
      value={steps}
      onChange={(s) => {
        setSteps(s);
        onSteps?.(s);
      }}
    />
  );
}

describe('ApprovalChainEditor', () => {
  it('renders each step as a named group with pickers, without axe violations', async () => {
    render(<Harness description="Steps run in order." />);
    expect(screen.getByRole('group', { name: 'Steps, in order' })).toHaveClass(
      'mn-approval-chain-editor',
    );
    const step2 = screen.getByRole('group', { name: 'Step 2' });
    expect(within(step2).getByRole('combobox', { name: 'Approver' })).toHaveValue('role');
    expect(within(step2).getByRole('combobox', { name: 'Role' })).toHaveValue('finance');
    expect(within(step2).getByRole('textbox', { name: 'Label (optional)' })).toHaveValue(
      'Finance check',
    );
    expect(screen.getByText('Steps run in order.')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('changes the approver and drops a target the new type does not use', async () => {
    const user = userEvent.setup();
    const onSteps = vi.fn();
    render(<Harness onSteps={onSteps} />);
    const step2 = screen.getByRole('group', { name: 'Step 2' });
    await user.selectOptions(within(step2).getByRole('combobox', { name: 'Approver' }), 'head');
    expect(onSteps).toHaveBeenLastCalledWith([
      { id: 'a', approver: 'manager' },
      { id: 'b', approver: 'head', label: 'Finance check' },
    ]);
    expect(within(step2).queryByRole('combobox', { name: 'Role' })).toBeNull();
  });

  it('adds and removes steps within the limits', async () => {
    const user = userEvent.setup();
    render(<Harness value={[{ id: 'a', approver: 'manager' }]} maxSteps={2} />);
    expect(screen.getByRole('button', { name: 'Remove step 1' })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Add step' }));
    expect(screen.getAllByRole('group', { name: /^Step / })).toHaveLength(2);
    expect(screen.queryByRole('button', { name: 'Add step' })).toBeNull();
    expect(screen.getByText('A chain can have up to 2 steps.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Remove step 2' }));
    expect(screen.getAllByRole('group', { name: /^Step / })).toHaveLength(1);
  });

  it('reorders with the move buttons and the keyboard handle', async () => {
    const user = userEvent.setup();
    const onSteps = vi.fn();
    render(<Harness onSteps={onSteps} />);
    await user.click(
      screen.getByRole('button', {
        name: 'Move Step 2, Finance check (Anyone with a role: Finance team) up',
      }),
    );
    expect(onSteps.mock.lastCall?.[0].map((s: ApprovalChainStep) => s.id)).toEqual(['b', 'a']);
    const handle = screen.getByRole('button', {
      name: 'Reorder Step 1, Finance check (Anyone with a role: Finance team)',
    });
    handle.focus();
    await user.keyboard(' ');
    await user.keyboard('{ArrowDown}');
    await user.keyboard(' ');
    expect(onSteps.mock.lastCall?.[0].map((s: ApprovalChainStep) => s.id)).toEqual(['a', 'b']);
  });

  it('shows a target error only when asked', () => {
    const steps = [{ id: 'x', approver: 'role' }];
    const { rerender } = render(
      <ApprovalChainEditor value={steps} onChange={() => {}} approverTypes={TYPES} />,
    );
    expect(screen.queryByText('Choose who decides this step.')).toBeNull();
    rerender(
      <ApprovalChainEditor value={steps} onChange={() => {}} approverTypes={TYPES} showErrors />,
    );
    expect(screen.getByRole('combobox', { name: 'Role' })).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByText('Choose who decides this step.')).toBeInTheDocument();
  });

  it('validates and summarises chains', () => {
    expect(validateApprovalChain(START, TYPES)).toEqual({});
    expect(validateApprovalChain([{ id: 'x', approver: 'role' }], TYPES)).toEqual({ x: 'target' });
    expect(summarizeApprovalChain(START, TYPES)).toBe(
      'Reporting manager → Finance check (Anyone with a role: Finance team)',
    );
  });

  it('disables every control', () => {
    render(<Harness disabled />);
    expect(screen.getByRole('button', { name: 'Add step' })).toBeDisabled();
    expect(screen.getAllByRole('combobox')[0]).toBeDisabled();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <ApprovalChainEditor value={START} onChange={() => {}} approverTypes={TYPES} />,
    );
    expect(html).toContain('mn-approval-chain-editor');
    expect(html).toContain('Finance check');
  });
});
