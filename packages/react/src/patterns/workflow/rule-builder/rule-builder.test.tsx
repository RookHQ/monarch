import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { ActionTypeDefinition } from '../../../components/workflow/action-editor/index.js';
import type { ConditionField } from '../../../components/workflow/condition-editor/index.js';
import {
  RuleBuilder,
  createRuleDraft,
  validateRule,
  type RuleBuilderProps,
  type RuleDraft,
  type RuleTrigger,
} from './index.js';

const TRIGGERS: RuleTrigger[] = [
  { value: 'created', label: 'Request created' },
  { value: 'updated', label: 'Request updated' },
];
const FIELDS: ConditionField[] = [
  { value: 'subject', label: 'Subject', type: 'text' },
  {
    value: 'priority',
    label: 'Priority',
    type: 'select',
    options: [
      { value: 'low', label: 'Low' },
      { value: 'high', label: 'High' },
    ],
  },
];
const TYPES: ActionTypeDefinition[] = [
  {
    value: 'set_priority',
    label: 'Set priority',
    params: [
      {
        key: 'priority',
        label: 'Priority',
        type: 'select',
        required: true,
        options: [
          { value: 'low', label: 'Low' },
          { value: 'high', label: 'High' },
        ],
      },
    ],
  },
];

function Harness({
  initial = createRuleDraft(),
  onRule,
  ...props
}: Partial<RuleBuilderProps> & { initial?: RuleDraft; onRule?: (r: RuleDraft) => void }) {
  const [rule, setRule] = useState(initial);
  return (
    <RuleBuilder
      triggers={TRIGGERS}
      fields={FIELDS}
      actionTypes={TYPES}
      {...props}
      value={rule}
      onChange={(r) => {
        setRule(r);
        onRule?.(r);
      }}
    />
  );
}

describe('RuleBuilder', () => {
  it('renders named when / if / then sections, without axe violations', async () => {
    render(<Harness onSubmit={() => {}} onCancel={() => {}} showStopProcessing />);
    expect(screen.getByRole('textbox', { name: /Name/ })).toBeRequired();
    expect(screen.getByRole('region', { name: 'When' })).toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'When' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'If' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Then' })).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: /Rule is on/ })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: /Stop processing/ })).not.toBeChecked();
    expect(screen.getByRole('heading', { level: 3, name: 'If' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('blocks saving, lists problems and moves focus to the summary', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<Harness onSubmit={onSubmit} />);
    await user.click(screen.getByRole('button', { name: 'Save rule' }));
    expect(onSubmit).not.toHaveBeenCalled();
    const summary = await screen.findByText('Not saved yet');
    await waitFor(() => expect(summary.closest('.mn-rule-builder__summary')).toHaveFocus());
    expect(screen.getByText('Give the rule a name.', { selector: 'li' })).toBeInTheDocument();
    expect(screen.getByText('Pick at least one trigger.', { selector: 'li' })).toBeInTheDocument();
    expect(screen.getByText('Add at least one action.', { selector: 'li' })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: /Name/ })).toHaveAttribute('aria-invalid', 'true');
    await expectNoA11yViolations();
  });

  it('builds a rule from the keyboard and saves it', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<Harness onSubmit={onSubmit} />);
    await user.type(screen.getByRole('textbox', { name: /Name/ }), 'Escalate refunds');
    await user.click(screen.getByRole('checkbox', { name: 'Request created' }));
    await user.click(screen.getByRole('button', { name: 'Add condition' }));
    await user.type(screen.getByRole('textbox', { name: 'Condition 1 value' }), 'refund');
    await user.click(screen.getByRole('button', { name: 'Add action' }));
    const action = screen.getByRole('group', { name: 'Action 1' });
    await user.selectOptions(within(action).getByRole('combobox', { name: /Priority/ }), 'high');
    await user.click(screen.getByRole('button', { name: 'Save rule' }));
    expect(onSubmit).toHaveBeenCalledTimes(1);
    const saved = onSubmit.mock.calls[0]![0] as RuleDraft;
    expect(saved).toMatchObject({
      name: 'Escalate refunds',
      enabled: true,
      triggers: ['created'],
      match: 'all',
      conditions: [{ field: 'subject', operator: 'eq', value: 'refund' }],
      actions: [{ type: 'set_priority', params: { priority: 'high' } }],
    });
  });

  it('shows a pending state and the error from a failed save', async () => {
    const user = userEvent.setup();
    let fail: (e: Error) => void = () => {};
    const onSubmit = vi.fn(() => new Promise((_r, reject) => (fail = reject)));
    render(
      <Harness
        onSubmit={onSubmit}
        initial={createRuleDraft({
          name: 'Ready',
          triggers: ['created'],
          actions: [{ id: 'a', type: 'set_priority', params: { priority: 'low' } }],
        })}
      />,
    );
    const save = screen.getByRole('button', { name: 'Save rule' });
    await user.click(save);
    expect(save).toHaveAttribute('aria-busy', 'true');
    fail(new Error('A rule with this name already exists.'));
    expect(await screen.findByText('A rule with this name already exists.')).toBeInTheDocument();
  });

  it('supports a single trigger picker, no triggers, extra options and cancel', async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();
    const { rerender } = render(
      <Harness triggerMode="single" onCancel={onCancel} showDescription={false}>
        <p>Extra setting</p>
      </Harness>,
    );
    expect(screen.getByRole('combobox', { name: /Trigger/ })).toBeInTheDocument();
    expect(screen.queryByRole('textbox', { name: /Description/ })).toBeNull();
    expect(screen.getByText('Extra setting')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onCancel).toHaveBeenCalled();
    rerender(<Harness triggers={undefined} />);
    expect(screen.queryByRole('region', { name: 'When' })).toBeNull();
  });

  it('validateRule reports every part', () => {
    expect(validateRule(createRuleDraft(), { fields: FIELDS, actionTypes: TYPES })).toEqual({
      name: true,
      triggers: true,
      actions: true,
    });
    expect(
      validateRule(
        createRuleDraft({
          name: 'x',
          conditions: [{ id: 'c', field: 'subject', operator: 'eq', value: '' }],
          actions: [{ id: 'a', type: 'set_priority', params: {} }],
        }),
        { fields: FIELDS, actionTypes: TYPES, requireTrigger: false },
      ),
    ).toEqual({ conditions: ['c'], incompleteActions: ['a'] });
  });

  it('renders on the server', () => {
    const html = renderToString(
      <RuleBuilder
        value={createRuleDraft({ name: 'Rule' })}
        onChange={() => {}}
        triggers={TRIGGERS}
        fields={FIELDS}
        actionTypes={TYPES}
      />,
    );
    expect(html).toContain('mn-rule-builder');
  });
});
