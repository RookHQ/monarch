import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  ActionEditor,
  createAction,
  validateAction,
  validateActions,
  type ActionEditorProps,
  type ActionTypeDefinition,
  type RuleAction,
} from './index.js';

const TEAMS = [
  { value: 't1', label: 'Support' },
  { value: 't2', label: 'Billing' },
];

const TYPES: ActionTypeDefinition[] = [
  {
    value: 'set_team',
    label: 'Set team',
    params: [{ key: 'teamId', label: 'Team', type: 'select', options: TEAMS, required: true }],
  },
  {
    value: 'add_tags',
    label: 'Add tags',
    defaults: { tags: [] },
    params: [{ key: 'tags', label: 'Tags', type: 'tags', required: true }],
  },
  {
    value: 'notify',
    label: 'Notify people',
    group: 'Messages',
    description: 'Sends an in-app notification.',
    defaults: { to: 'assignee', message: '' },
    params: [
      {
        key: 'to',
        label: 'Notify',
        type: 'select',
        required: true,
        options: [
          { value: 'assignee', label: 'Assignee' },
          { value: 'user', label: 'A specific person' },
        ],
      },
      {
        key: 'userId',
        label: 'Person',
        type: 'select',
        options: [{ value: 'u1', label: 'Jo Park' }],
        required: true,
        visibleWhen: (p) => p.to === 'user',
      },
      { key: 'message', label: 'Message', type: 'textarea', required: true },
      { key: 'urgent', label: 'Mark as urgent', type: 'boolean' },
    ],
    validate: (p) =>
      typeof p.message === 'string' && p.message.length > 200
        ? 'Keep the message under 200 characters.'
        : null,
  },
  {
    value: 'external',
    label: 'Create an external issue',
    disabled: true,
    disabledReason: 'The connection is not set up.',
  },
];

const START: RuleAction[] = [
  { id: 'a', type: 'set_team', params: { teamId: 't2' } },
  { id: 'b', type: 'notify', params: { to: 'assignee', message: 'New request' } },
];

function Harness({
  onActions,
  initial = START,
  ...props
}: Partial<ActionEditorProps> & { onActions?: (a: RuleAction[]) => void; initial?: RuleAction[] }) {
  const [value, setValue] = useState<RuleAction[]>(initial);
  return (
    <ActionEditor
      actionTypes={TYPES}
      {...props}
      value={value}
      onChange={(a) => {
        setValue(a);
        onActions?.(a);
      }}
    />
  );
}

describe('ActionEditor', () => {
  it('renders each action as a named group with labelled parameters, without axe violations', async () => {
    render(<Harness />);
    expect(screen.getByRole('list', { name: 'Actions' })).toBeInTheDocument();
    const a1 = screen.getByRole('group', { name: 'Action 1' });
    expect(within(a1).getByRole('combobox', { name: 'Action' })).toHaveValue('set_team');
    expect(within(a1).getByRole('combobox', { name: /Team/ })).toHaveValue('t2');
    const a2 = screen.getByRole('group', { name: 'Action 2' });
    expect(within(a2).getByRole('combobox', { name: 'Action' })).toHaveAccessibleDescription(
      'Sends an in-app notification.',
    );
    expect(within(a2).getByRole('textbox', { name: /Message/ })).toHaveValue('New request');
    expect(within(a2).queryByRole('combobox', { name: /Person/ })).toBeNull();
    expect(
      within(a2).getByRole('option', {
        name: 'Create an external issue (The connection is not set up.)',
      }),
    ).toBeDisabled();
    await expectNoA11yViolations();
  });

  it('changing the type starts from its defaults; dependent parameters appear', async () => {
    const user = userEvent.setup();
    const onActions = vi.fn();
    render(<Harness onActions={onActions} />);
    const a1 = screen.getByRole('group', { name: 'Action 1' });
    await user.selectOptions(within(a1).getByRole('combobox', { name: 'Action' }), 'add_tags');
    expect(onActions.mock.lastCall?.[0][0]).toEqual({
      id: 'a',
      type: 'add_tags',
      params: { tags: [] },
    });
    const a2 = screen.getByRole('group', { name: 'Action 2' });
    await user.selectOptions(within(a2).getByRole('combobox', { name: /Notify/ }), 'user');
    expect(within(a2).getByRole('combobox', { name: /Person/ })).toBeInTheDocument();
    await user.click(within(a2).getByRole('checkbox', { name: 'Mark as urgent' }));
    expect(onActions.mock.lastCall?.[0][1].params).toEqual({
      to: 'user',
      message: 'New request',
      urgent: true,
    });
  });

  it('adds, removes and reorders actions', async () => {
    const user = userEvent.setup();
    const onActions = vi.fn();
    render(<Harness onActions={onActions} maxActions={3} />);
    await user.click(screen.getByRole('button', { name: 'Add action' }));
    expect(onActions.mock.lastCall?.[0][2]).toMatchObject({ type: 'set_team', params: {} });
    expect(screen.getByText('You can add up to 3 actions.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Remove action 3' }));
    await user.click(screen.getByRole('button', { name: 'Move Action 2, Notify people up' }));
    expect(onActions.mock.lastCall?.[0].map((a: RuleAction) => a.id)).toEqual(['b', 'a']);
  });

  it('shows missing parameters and the type check only when asked', () => {
    const actions: RuleAction[] = [
      { id: 'x', type: 'set_team', params: {} },
      { id: 'y', type: 'notify', params: { to: 'assignee', message: 'x'.repeat(201) } },
    ];
    const { rerender } = render(
      <ActionEditor value={actions} onChange={() => {}} actionTypes={TYPES} />,
    );
    expect(screen.queryByText('Fill in “Team”.')).toBeNull();
    rerender(<ActionEditor value={actions} onChange={() => {}} actionTypes={TYPES} showErrors />);
    const team = screen.getByRole('combobox', { name: /Team/ });
    expect(team).toHaveAttribute('aria-invalid', 'true');
    expect(team).toHaveAccessibleDescription('Fill in “Team”.');
    expect(screen.getByText('Keep the message under 200 characters.')).toBeInTheDocument();
  });

  it('warns on an action whose type is unavailable or unknown', () => {
    render(
      <ActionEditor
        value={[
          { id: 'x', type: 'external', params: {} },
          { id: 'y', type: 'gone', params: {} },
        ]}
        onChange={() => {}}
        actionTypes={TYPES}
      />,
    );
    expect(
      screen.getByText(/This action cannot run right now. The connection is not set up./),
    ).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'gone (not available)' })).toBeInTheDocument();
  });

  it('renders a custom parameter editor and an unordered list', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    const custom: ActionTypeDefinition = {
      value: 'formula',
      label: 'Formula',
      render: (params, set) => (
        <button type="button" onClick={() => set({ ...params, expr: '1 + 1' })}>
          Fill formula
        </button>
      ),
    };
    render(
      <ActionEditor
        ordered={false}
        value={[{ id: 'f', type: 'formula', params: {} }]}
        onChange={onChange}
        actionTypes={[custom]}
      />,
    );
    expect(screen.queryByRole('button', { name: /Reorder/ })).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Fill formula' }));
    expect(onChange).toHaveBeenCalledWith([
      { id: 'f', type: 'formula', params: { expr: '1 + 1' } },
    ]);
  });

  it('helpers create and validate actions', () => {
    expect(createAction('n', TYPES[2])).toEqual({
      id: 'n',
      type: 'notify',
      params: { to: 'assignee', message: '' },
    });
    expect(
      validateAction({ id: 'n', type: 'notify', params: { to: 'user', message: 'Hi' } }, TYPES),
    ).toEqual(['Fill in “Person”.']);
    expect(validateActions(START, TYPES)).toEqual({});
  });

  it('shows the empty text and renders on the server', () => {
    render(<ActionEditor value={[]} onChange={() => {}} actionTypes={TYPES} />);
    expect(screen.getByText('No actions yet.')).toBeInTheDocument();
    const html = renderToString(
      <ActionEditor value={START} onChange={() => {}} actionTypes={TYPES} />,
    );
    expect(html).toContain('mn-action-editor');
  });
});
