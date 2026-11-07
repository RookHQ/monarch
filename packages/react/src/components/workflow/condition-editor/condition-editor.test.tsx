import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  ConditionEditor,
  conditionOperatorsFor,
  createCondition,
  evaluateCondition,
  evaluateConditions,
  validateCondition,
  validateConditions,
  type Condition,
  type ConditionEditorProps,
  type ConditionField,
  type ConditionMatch,
} from './index.js';

const FIELDS: ConditionField[] = [
  {
    value: 'priority',
    label: 'Priority',
    type: 'select',
    options: [
      { value: 'low', label: 'Low' },
      { value: 'high', label: 'High' },
      { value: 'urgent', label: 'Urgent' },
    ],
  },
  { value: 'subject', label: 'Subject', type: 'text' },
  { value: 'hours', label: 'Hours since created', type: 'number', placeholder: 'Hours' },
  { value: 'vip', label: 'From a key account', type: 'boolean' },
  { value: 'tags', label: 'Tags', type: 'tags' },
  {
    value: 'later',
    label: 'Later question',
    type: 'text',
    group: 'Questions',
    disabled: true,
    disabledReason: 'comes later',
  },
];

const START: Condition[] = [
  { id: 'a', field: 'priority', operator: 'eq', value: 'high' },
  { id: 'b', field: 'subject', operator: 'contains', value: 'refund' },
];

function Harness({
  onConditions,
  initial = START,
  ...props
}: Partial<ConditionEditorProps> & {
  onConditions?: (c: Condition[]) => void;
  initial?: Condition[];
}) {
  const [value, setValue] = useState<Condition[]>(initial);
  const [match, setMatch] = useState<ConditionMatch>('all');
  return (
    <ConditionEditor
      fields={FIELDS}
      match={match}
      onMatchChange={setMatch}
      {...props}
      value={value}
      onChange={(c) => {
        setValue(c);
        onConditions?.(c);
      }}
    />
  );
}

describe('ConditionEditor', () => {
  it('renders labelled rows and the all / any switch, without axe violations', async () => {
    render(<Harness />);
    const list = screen.getByRole('list', { name: 'Conditions' });
    expect(list.closest('.mn-condition-editor')).not.toBeNull();
    const row1 = screen.getByRole('group', { name: 'Condition 1' });
    expect(within(row1).getByRole('combobox', { name: 'Condition 1 field' })).toHaveValue(
      'priority',
    );
    expect(within(row1).getByRole('combobox', { name: 'Condition 1 operator' })).toHaveValue('eq');
    expect(within(row1).getByRole('combobox', { name: 'Condition 1 value' })).toHaveValue('high');
    expect(screen.getByRole('radiogroup', { name: 'Match' })).toBeInTheDocument();
    expect(
      within(row1).getByRole('option', { name: 'Later question (comes later)' }),
    ).toBeDisabled();
    await expectNoA11yViolations();
  });

  it('changing the field resets the operator and value to fit it', async () => {
    const user = userEvent.setup();
    const onConditions = vi.fn();
    render(<Harness onConditions={onConditions} />);
    await user.selectOptions(screen.getByRole('combobox', { name: 'Condition 1 field' }), 'hours');
    expect(onConditions).toHaveBeenLastCalledWith([
      { id: 'a', field: 'hours', operator: 'eq', value: null },
      START[1],
    ]);
    const value = screen.getByRole('spinbutton', { name: 'Condition 1 value' });
    await user.type(value, '24');
    expect(onConditions.mock.lastCall?.[0][0].value).toBe(24);
  });

  it('operators without a value hide the value input; list operators keep the value', async () => {
    const user = userEvent.setup();
    const onConditions = vi.fn();
    render(<Harness onConditions={onConditions} />);
    const op = screen.getByRole('combobox', { name: 'Condition 1 operator' });
    await user.selectOptions(op, 'in');
    expect(onConditions.mock.lastCall?.[0][0]).toEqual({
      id: 'a',
      field: 'priority',
      operator: 'in',
      value: ['high'],
    });
    // A multiple-choice value and a tags value are accessible too.
    await user.selectOptions(screen.getByRole('combobox', { name: 'Condition 2 field' }), 'tags');
    await expectNoA11yViolations();
    await user.selectOptions(op, 'is_empty');
    expect(screen.queryByRole('combobox', { name: 'Condition 1 value' })).toBeNull();
    expect(onConditions.mock.lastCall?.[0][0]).toEqual({
      id: 'a',
      field: 'priority',
      operator: 'is_empty',
    });
  });

  it('adds and removes conditions, and switches all / any from the keyboard', async () => {
    const user = userEvent.setup();
    render(<Harness initial={[]} maxConditions={2} />);
    expect(screen.getByText('No conditions. This applies every time.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Add condition' }));
    await user.click(screen.getByRole('button', { name: 'Add condition' }));
    expect(screen.getAllByRole('group', { name: /^Condition \d$/ })).toHaveLength(2);
    expect(screen.queryByRole('button', { name: 'Add condition' })).toBeNull();
    expect(screen.getByText('You can add up to 2 conditions.')).toBeInTheDocument();
    const all = screen.getByRole('radio', { name: 'all' });
    all.focus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('radio', { name: 'any' })).toBeChecked();
    await user.click(screen.getByRole('button', { name: 'Remove condition 2' }));
    expect(screen.getAllByRole('group', { name: /^Condition \d$/ })).toHaveLength(1);
  });

  it('shows problems only when asked, tied to the control', () => {
    const bad: Condition[] = [{ id: 'x', field: 'subject', operator: 'eq', value: '' }];
    const { rerender } = render(
      <ConditionEditor value={bad} onChange={() => {}} fields={FIELDS} />,
    );
    expect(screen.queryByText('Enter a value.')).toBeNull();
    rerender(<ConditionEditor value={bad} onChange={() => {}} fields={FIELDS} showErrors />);
    const input = screen.getByRole('textbox', { name: 'Condition 1 value' });
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAccessibleDescription('Enter a value.');
  });

  it('keeps a stored field or option that no longer exists visible', () => {
    render(
      <ConditionEditor
        value={[
          { id: 'x', field: 'gone', operator: 'eq', value: 'a' },
          { id: 'y', field: 'priority', operator: 'eq', value: 'old' },
        ]}
        onChange={() => {}}
        fields={FIELDS}
        showErrors
      />,
    );
    expect(screen.getByRole('option', { name: 'gone (removed)' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: '(removed option)' })).toBeInTheDocument();
    expect(screen.getByText('Choose a field.')).toBeInTheDocument();
    expect(
      screen.getByText('The chosen value no longer exists. Pick another.'),
    ).toBeInTheDocument();
  });

  it('helpers: operators, creation, validation and evaluation', () => {
    expect(conditionOperatorsFor(FIELDS[3])).toEqual(['is_true', 'is_false']);
    expect(createCondition('n', FIELDS[4])).toEqual({
      id: 'n',
      field: 'tags',
      operator: 'contains',
      value: [],
    });
    expect(
      validateCondition({ id: 'n', field: 'hours', operator: 'gt', value: Number.NaN }, FIELDS),
    ).toBe('number');
    expect(validateConditions(START, FIELDS)).toEqual({});
    expect(
      evaluateCondition(
        { id: 'n', field: 'subject', operator: 'contains', value: 'REFUND' },
        'Refund please',
      ),
    ).toBe(true);
    expect(evaluateCondition({ id: 'n', field: 'hours', operator: 'gte', value: 24 }, 30)).toBe(
      true,
    );
    expect(
      evaluateCondition({ id: 'n', field: 'tags', operator: 'contains', value: ['vip'] }, [
        'new',
        'vip',
      ]),
    ).toBe(true);
    expect(
      evaluateCondition({ id: 'n', field: 'priority', operator: 'nin', value: ['low'] }, 'high'),
    ).toBe(true);
    expect(evaluateCondition({ id: 'n', field: 'vip', operator: 'is_true' }, true)).toBe(true);
    expect(evaluateConditions(START, { priority: 'high', subject: 'Order' }, 'all')).toBe(false);
    expect(evaluateConditions(START, { priority: 'high', subject: 'Order' }, 'any')).toBe(true);
    expect(evaluateConditions([], {})).toBe(true);
  });

  it('disables every control', () => {
    render(<Harness disabled />);
    expect(screen.getByRole('combobox', { name: 'Condition 1 field' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Remove condition 1' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Add condition' })).toBeDisabled();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <ConditionEditor value={START} onChange={() => {}} fields={FIELDS} />,
    );
    expect(html).toContain('mn-condition-editor');
    expect(html).toContain('Priority');
  });
});
