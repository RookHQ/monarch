import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  QueryBuilder,
  createQueryGroup,
  evaluateQuery,
  flattenQueryRules,
  validateQuery,
  type QueryBuilderProps,
  type QueryField,
  type QueryGroup,
} from './index.js';

const FIELDS: QueryField[] = [
  {
    value: 'status',
    label: 'Status',
    type: 'select',
    options: [
      { value: 'open', label: 'Open' },
      { value: 'closed', label: 'Closed' },
    ],
  },
  { value: 'title', label: 'Title', type: 'text' },
  { value: 'count', label: 'Count', type: 'number' },
];

const START: QueryGroup = {
  id: 'root',
  combinator: 'and',
  rules: [{ id: 'r1', field: 'status', operator: 'eq', value: 'open' }],
  groups: [
    {
      id: 'g1',
      combinator: 'or',
      rules: [{ id: 'r2', field: 'title', operator: 'contains', value: 'report' }],
      groups: [],
    },
  ],
};

function Harness({
  onQuery,
  initial = START,
  ...props
}: Partial<QueryBuilderProps> & { onQuery?: (q: QueryGroup) => void; initial?: QueryGroup }) {
  const [value, setValue] = useState<QueryGroup>(initial);
  return (
    <QueryBuilder
      fields={FIELDS}
      {...props}
      value={value}
      onChange={(q) => {
        setValue(q);
        onQuery?.(q);
      }}
    />
  );
}

describe('QueryBuilder', () => {
  it('renders nested groups with accessible names and passes axe', async () => {
    render(<Harness data-testid="root" />);
    expect(screen.getByTestId('root')).toHaveClass('mn-query-builder');
    expect(screen.getByRole('group', { name: 'Query' })).toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'Top group' })).toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'Group 1' })).toHaveAttribute('data-combinator', 'or');
    expect(screen.getByRole('combobox', { name: 'Top group, rule 1 field' })).toHaveValue('status');
    expect(screen.getByRole('textbox', { name: 'Group 1, rule 1 value' })).toHaveValue('report');
    await expectNoA11yViolations();
  });

  it('switches a group combinator with the keyboard', async () => {
    const onQuery = vi.fn();
    const user = userEvent.setup();
    render(<Harness onQuery={onQuery} />);
    const and = screen.getByRole('radio', { name: 'AND', checked: true });
    and.focus();
    await user.keyboard('{ArrowRight}');
    expect(onQuery).toHaveBeenLastCalledWith(expect.objectContaining({ combinator: 'or' }));
  });

  it('adds and removes rules and groups', async () => {
    const onQuery = vi.fn();
    const user = userEvent.setup();
    render(
      <Harness
        onQuery={onQuery}
        createId={(() => {
          let n = 0;
          return () => `n${++n}`;
        })()}
      />,
    );
    const addRule = screen.getAllByRole('button', { name: 'Add rule' });
    await user.click(addRule[0]!);
    let q = onQuery.mock.lastCall![0] as QueryGroup;
    expect(q.rules).toHaveLength(2);
    expect(q.rules[1]).toMatchObject({ id: 'n1', field: 'status' });

    const addGroups = screen.getAllByRole('button', { name: 'Add group' });
    await user.click(addGroups[addGroups.length - 1]!);
    q = onQuery.mock.lastCall![0] as QueryGroup;
    expect(q.groups).toHaveLength(2);
    expect(q.groups[1]).toMatchObject({ id: 'n2', combinator: 'or', rules: [], groups: [] });
    expect(screen.getByRole('group', { name: 'Group 2' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Remove group 1' }));
    q = onQuery.mock.lastCall![0] as QueryGroup;
    expect(q.groups.map((g) => g.id)).toEqual(['n2']);

    await user.click(screen.getByRole('button', { name: 'Remove top group, rule 1' }));
    q = onQuery.mock.lastCall![0] as QueryGroup;
    expect(q.rules.map((r) => r.id)).toEqual(['n1']);
  });

  it('keeps focus in the builder when a group is removed', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.click(screen.getByRole('button', { name: 'Remove group 1' }));
    expect(screen.queryByRole('group', { name: 'Group 1' })).toBeNull();
    expect(screen.getByRole('button', { name: 'Add group' })).toHaveFocus();
  });

  it('never gives a new rule or group an id that the saved query already uses', async () => {
    const user = userEvent.setup();
    const onQuery = vi.fn();
    const empty: QueryGroup = { id: 'root', combinator: 'and', rules: [], groups: [] };
    const first = render(<Harness initial={empty} onQuery={onQuery} />);
    await user.click(screen.getByRole('button', { name: 'Add rule' }));
    await user.click(screen.getByRole('button', { name: 'Add group' }));
    const saved = onQuery.mock.lastCall?.[0] as QueryGroup;
    first.unmount();
    // A later visit: the saved query comes back and the builder counts from one again.
    render(<Harness initial={saved} onQuery={onQuery} />);
    await user.click(screen.getAllByRole('button', { name: 'Add rule' })[0]!);
    await user.click(screen.getAllByRole('button', { name: 'Add group' }).at(-1)!);
    const next = onQuery.mock.lastCall?.[0] as QueryGroup;
    const ids = [
      next.id,
      ...next.rules.map((r) => r.id),
      ...next.groups.flatMap((g) => [g.id, ...g.rules.map((r) => r.id)]),
    ];
    expect(ids).toHaveLength(5);
    expect(new Set(ids).size).toBe(5);
  });

  it('edits a nested rule value', async () => {
    const onQuery = vi.fn();
    const user = userEvent.setup();
    render(<Harness onQuery={onQuery} />);
    const input = screen.getByRole('textbox', { name: 'Group 1, rule 1 value' });
    await user.clear(input);
    await user.type(input, 'audit');
    const q = onQuery.mock.lastCall![0] as QueryGroup;
    expect(q.groups[0]!.rules[0]).toMatchObject({ value: 'audit' });
  });

  it('stops nesting at maxDepth', () => {
    render(<Harness maxDepth={2} />);
    // Top group can add a sub-group; the depth-2 group cannot.
    expect(screen.getAllByRole('button', { name: 'Add group' })).toHaveLength(1);
    expect(screen.getByText('Groups cannot be nested any deeper.')).toBeInTheDocument();
  });

  it('hides sub-groups entirely with maxDepth 1', () => {
    render(<Harness maxDepth={1} initial={createQueryGroup('root')} />);
    expect(screen.queryByRole('button', { name: 'Add group' })).not.toBeInTheDocument();
    expect(screen.getByText('No rules in this group.')).toBeInTheDocument();
  });

  it('shows problems and disables every control', () => {
    const initial: QueryGroup = {
      ...createQueryGroup('root'),
      rules: [{ id: 'x', field: 'title', operator: 'eq', value: '' }],
    };
    const { rerender } = render(<Harness initial={initial} showErrors />);
    expect(screen.getByText('Enter a value.')).toBeInTheDocument();
    rerender(<Harness initial={initial} disabled />);
    expect(screen.getByRole('button', { name: 'Add rule' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Add group' })).toBeDisabled();
  });

  it('accepts translated messages', () => {
    render(
      <Harness
        messages={{ label: 'Filter', group: (p) => `Gruppe ${p.join('.')}`, addRule: 'Regel' }}
      />,
    );
    expect(screen.getByRole('group', { name: 'Filter' })).toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'Gruppe 1.1' })).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Regel' })).toHaveLength(2);
  });

  it('validates and evaluates queries', () => {
    expect(flattenQueryRules(START).map((r) => r.id)).toEqual(['r1', 'r2']);
    expect(validateQuery(START, FIELDS)).toEqual({});
    const broken: QueryGroup = {
      ...START,
      rules: [{ id: 'b', field: 'count', operator: 'gt', value: null }],
    };
    expect(validateQuery(broken, FIELDS)).toEqual({ b: 'value' });
    expect(evaluateQuery(START, { status: 'open', title: 'Weekly report' })).toBe(true);
    expect(evaluateQuery(START, { status: 'open', title: 'Invoice' })).toBe(false);
    expect(evaluateQuery({ ...START, combinator: 'or' }, { status: 'open', title: 'x' })).toBe(
      true,
    );
    expect(evaluateQuery(createQueryGroup('e'), {})).toBe(true);
  });

  it('renders on the server', () => {
    const html = renderToString(<QueryBuilder value={START} onChange={() => {}} fields={FIELDS} />);
    expect(html).toContain('mn-query-builder');
    expect(html).toContain('Top group');
  });
});
