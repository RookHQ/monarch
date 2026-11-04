import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel } from '../form-field/index.js';
import { Combobox, matchComboboxOption, type ComboboxOption } from './index.js';

const options = [
  { value: 'open', label: 'Open' },
  { value: 'pending', label: 'Pending', description: 'Waiting on customer' },
  { value: 'closed', label: 'Closed' },
  { value: 'archived', label: 'Archived', disabled: true },
];

describe('Combobox', () => {
  it('filters, navigates with the keyboard and selects with Enter', async () => {
    const onValueChange = vi.fn();
    render(
      <Field>
        <FieldLabel>Status</FieldLabel>
        <Combobox options={options} onValueChange={onValueChange} placeholder="Choose" />
      </Field>,
    );
    const input = screen.getByRole('combobox', { name: 'Status' });
    expect(input).toHaveAttribute('aria-expanded', 'false');
    const user = userEvent.setup();
    await user.type(input, 'pe');
    expect(input).toHaveAttribute('aria-expanded', 'true');
    const list = await screen.findByRole('listbox');
    expect(list.querySelectorAll('[role="option"]')).toHaveLength(2); // Open, Pending
    await user.keyboard('{ArrowDown}');
    const active = input.getAttribute('aria-activedescendant');
    expect(active).toBeTruthy();
    expect(document.getElementById(active!)).toHaveTextContent('Pending');
    await user.keyboard('{Enter}');
    expect(onValueChange).toHaveBeenCalledWith('pending');
    await waitFor(() => expect(input).toHaveAttribute('aria-expanded', 'false'));
    expect(input).toHaveValue('Pending');
  });

  it('shows the empty message and closes on Escape, restoring the selection', async () => {
    render(<Combobox aria-label="Status" options={options} defaultValue="open" />);
    const input = screen.getByRole('combobox', { name: 'Status' });
    expect(input).toHaveValue('Open');
    const user = userEvent.setup();
    await user.clear(input);
    await user.type(input, 'zzz');
    expect(await screen.findByRole('status')).toHaveTextContent('No matches');
    await user.keyboard('{Escape}');
    expect(input).toHaveValue('Open');
    await expectNoA11yViolations();
  });

  it('does not select disabled options', async () => {
    const onValueChange = vi.fn();
    render(<Combobox aria-label="Status" options={options} onValueChange={onValueChange} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('combobox'));
    await user.keyboard('{ArrowDown}{End}{Enter}');
    expect(onValueChange).not.toHaveBeenCalled();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <Combobox aria-label="Owner" options={[{ value: 'a', label: 'Ada' }]} defaultValue="a" />,
    );
    expect(html).toContain('role="combobox"');
  });
});

const people: ComboboxOption[] = [
  {
    value: 'ada',
    label: 'Ada Lovelace',
    hint: 'Design',
    group: 'In this team',
    keywords: 'ada@example.com',
  },
  { value: 'grace', label: 'Grace Hopper', hint: 'Platform', group: 'In this team' },
  { value: 'alan', label: 'Alan Turing', group: 'Everyone else' },
  { value: 'katherine', label: 'Katherine Johnson', group: 'Everyone else', description: 'Away' },
];

describe('Combobox: options', () => {
  it('groups options under labelled role="group" headings, in order of first appearance', async () => {
    render(<Combobox aria-label="Owner" options={people} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Show options' }));
    const groups = await screen.findAllByRole('group');
    expect(
      groups.map(
        (g) =>
          g.getAttribute('aria-labelledby') &&
          document.getElementById(g.getAttribute('aria-labelledby')!)?.textContent,
      ),
    ).toEqual(['In this team', 'Everyone else']);
    expect(within(groups[0]!).getAllByRole('option')).toHaveLength(2);
    expect(screen.getByRole('option', { name: /Ada Lovelace/ })).toHaveTextContent('Design');
    await expectNoA11yViolations();
  });

  it('matches hint and keywords with the default filter', () => {
    expect(matchComboboxOption(people[0]!, 'example.com')).toBe(true);
    expect(matchComboboxOption(people[1]!, 'platform')).toBe(true);
    expect(matchComboboxOption(people[2]!, 'zzz')).toBe(false);
  });

  it('ranks prefix matches first with sortByMatch', async () => {
    const countries = [
      { value: 'ar', label: 'Argentina' },
      { value: 'in', label: 'India' },
      { value: 'id', label: 'Indonesia' },
      { value: 'bi', label: 'British Indian Ocean Territory' },
    ];
    render(<Combobox aria-label="Country" options={countries} sortByMatch />);
    const user = userEvent.setup();
    await user.type(screen.getByRole('combobox'), 'in');
    const names = screen.getAllByRole('option').map((o) => o.textContent);
    expect(names).toEqual(['India', 'Indonesia', 'British Indian Ocean Territory', 'Argentina']);
  });

  it('renders custom option content and a leading media slot', async () => {
    render(
      <Combobox
        aria-label="Owner"
        options={[{ value: 'a', label: 'Ada', media: <span data-testid="media">A</span> }]}
        renderOption={(o, s) => `${o.label}${s.selected ? ' ✓' : ''}`}
        defaultValue="a"
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Show options' }));
    expect(await screen.findByRole('option')).toHaveTextContent('Ada ✓');
  });

  it('caps the list with limit and says how many are hidden', async () => {
    const many = Array.from({ length: 30 }, (_, i) => ({ value: `v${i}`, label: `Item ${i}` }));
    render(<Combobox aria-label="Item" options={many} limit={10} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('combobox'));
    await user.keyboard('{ArrowDown}');
    expect(screen.getAllByRole('option')).toHaveLength(10);
    expect(screen.getByRole('status')).toHaveTextContent('Showing 10 of 30');
  });

  it('offers a clear option that is selected while nothing is', async () => {
    const onValueChange = vi.fn();
    render(
      <Combobox
        aria-label="Country"
        options={options}
        defaultValue="open"
        clearOption={{ label: 'No status' }}
        onValueChange={onValueChange}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Show options' }));
    const clear = await screen.findByRole('option', { name: 'No status' });
    expect(clear).toHaveAttribute('aria-selected', 'false');
    await user.click(clear);
    expect(onValueChange).toHaveBeenCalledWith(null);
    expect(screen.getByRole('combobox')).toHaveValue('');
  });
});

describe('Combobox: keyboard', () => {
  it('opens on the selected option and supports PageDown, Home, End and Alt+Up', async () => {
    const many = Array.from({ length: 25 }, (_, i) => ({ value: `v${i}`, label: `Item ${i}` }));
    render(<Combobox aria-label="Item" options={many} defaultValue="v5" />);
    const input = screen.getByRole('combobox');
    const user = userEvent.setup();
    input.focus();
    await user.keyboard('{Alt>}{ArrowDown}{/Alt}');
    const activeText = () =>
      document.getElementById(input.getAttribute('aria-activedescendant')!)?.textContent;
    await waitFor(() => expect(activeText()).toBe('Item 5'));
    await user.keyboard('{PageDown}');
    expect(activeText()).toBe('Item 15');
    await user.keyboard('{PageDown}{PageDown}');
    expect(activeText()).toBe('Item 24');
    await user.keyboard('{PageUp}');
    expect(activeText()).toBe('Item 14');
    await user.keyboard('{Home}');
    expect(activeText()).toBe('Item 0');
    await user.keyboard('{End}');
    expect(activeText()).toBe('Item 24');
    await user.keyboard('{Alt>}{ArrowUp}{/Alt}');
    expect(input).toHaveAttribute('aria-expanded', 'false');
  });

  it('clears with Backspace on an empty input when clearable', async () => {
    const onValueChange = vi.fn();
    render(
      <Combobox
        aria-label="Status"
        options={options}
        defaultValue="open"
        onValueChange={onValueChange}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('combobox'));
    await user.clear(screen.getByRole('combobox'));
    await user.keyboard('{Backspace}');
    expect(onValueChange).toHaveBeenLastCalledWith(null);
  });
});

describe('Combobox: multiple', () => {
  it('toggles values, stays open, shows removable chips and announces changes', async () => {
    const onValuesChange = vi.fn();
    render(
      <Field>
        <FieldLabel>Reviewers</FieldLabel>
        <Combobox
          multiple
          options={people}
          defaultValues={['ada']}
          onValuesChange={onValuesChange}
        />
      </Field>,
    );
    const input = screen.getByRole('combobox', { name: 'Reviewers' });
    expect(screen.getByRole('listitem')).toHaveTextContent('Ada Lovelace');
    const user = userEvent.setup();
    await user.type(input, 'gra');
    const listbox = await screen.findByRole('listbox', { name: 'Reviewers' });
    expect(listbox).toHaveAttribute('aria-multiselectable', 'true');
    await user.keyboard('{Enter}');
    expect(onValuesChange).toHaveBeenLastCalledWith(['ada', 'grace']);
    expect(input).toHaveValue('');
    expect(input).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getAllByRole('listitem')).toHaveLength(2);
    expect(screen.getByText('Grace Hopper added')).toBeInTheDocument();
    // Toggling a selected option removes it.
    await user.click(screen.getByRole('option', { name: /Ada Lovelace/ }));
    expect(onValuesChange).toHaveBeenLastCalledWith(['grace']);
    await expectNoA11yViolations();
  });

  it('removes chips with their button and Backspace on an empty input', async () => {
    function Controlled() {
      const [values, setValues] = useState(['ada', 'grace', 'alan']);
      return (
        <Combobox
          aria-label="Reviewers"
          multiple
          options={people}
          values={values}
          onValuesChange={setValues}
        />
      );
    }
    render(<Controlled />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Remove Grace Hopper' }));
    expect(screen.getAllByRole('listitem').map((li) => li.textContent)).toEqual([
      'Ada Lovelace',
      'Alan Turing',
    ]);
    expect(screen.getByRole('combobox')).toHaveFocus();
    await user.keyboard('{Backspace}');
    expect(screen.getAllByRole('listitem').map((li) => li.textContent)).toEqual(['Ada Lovelace']);
    expect(screen.getByText('Alan Turing removed')).toBeInTheDocument();
  });

  it('does not render remove buttons when disabled', () => {
    render(
      <Combobox
        aria-label="Reviewers"
        multiple
        disabled
        options={people}
        defaultValues={['ada']}
      />,
    );
    expect(screen.queryByRole('button', { name: /Remove/ })).toBeNull();
  });
});

describe('Combobox: async search', () => {
  it('debounces, aborts superseded queries, ignores late answers and keeps the label', async () => {
    const calls: { q: string; signal: AbortSignal; resolve: (o: ComboboxOption[]) => void }[] = [];
    const onSearch = vi.fn(
      (q: string, { signal }: { signal: AbortSignal }) =>
        new Promise<ComboboxOption[]>((resolve) => calls.push({ q, signal, resolve })),
    );
    const onValueChange = vi.fn();
    render(
      <Combobox
        aria-label="City"
        onSearch={onSearch}
        searchDebounce={300}
        onValueChange={onValueChange}
      />,
    );
    const user = userEvent.setup();
    const input = screen.getByRole('combobox');
    await user.click(input);
    await user.keyboard('{ArrowDown}');
    // Opening searches at once with the empty query.
    await waitFor(() => expect(onSearch).toHaveBeenCalledTimes(1));
    expect(onSearch).toHaveBeenLastCalledWith('', expect.anything());
    expect(screen.getByRole('status')).toHaveTextContent('Loading…');
    expect(screen.getByRole('listbox')).toHaveAttribute('aria-busy', 'true');
    await user.type(input, 'par');
    await waitFor(() => expect(onSearch).toHaveBeenCalledTimes(2));
    // Keystrokes inside the debounce window collapse into one call.
    expect(calls.map((c) => c.q)).toEqual(['', 'par']);
    expect(calls[0]!.signal.aborted).toBe(true);
    // A late answer for the superseded query is ignored.
    await act(async () => calls[0]!.resolve([{ value: 'x', label: 'Stale' }]));
    expect(screen.queryByRole('option', { name: 'Stale' })).toBeNull();
    await act(async () =>
      calls[1]!.resolve([{ value: 'paris', label: 'Paris', description: 'France' }]),
    );
    expect(screen.getByRole('listbox')).not.toHaveAttribute('aria-busy');
    await user.click(screen.getByRole('option', { name: /Paris/ }));
    expect(onValueChange).toHaveBeenCalledWith('paris');
    // The label survives even though the next search result may not contain it.
    expect(input).toHaveValue('Paris');
  });

  it('shows an error with a retry, and Enter retries', async () => {
    let fail = true;
    const onSearch = vi.fn(async () => {
      if (fail) throw new Error('offline');
      return [{ value: 'a', label: 'Recovered' }];
    });
    render(<Combobox aria-label="City" onSearch={onSearch} searchDebounce={0} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('combobox'));
    await user.keyboard('{ArrowDown}');
    expect(await screen.findByText('Couldn’t load options.')).toBeInTheDocument();
    fail = false;
    await user.keyboard('{Enter}');
    expect(await screen.findByRole('option', { name: 'Recovered' })).toBeInTheDocument();
    fail = true;
    await user.type(screen.getByRole('combobox'), 'x');
    const retry = await screen.findByRole('button', { name: 'Try again' });
    fail = false;
    await user.click(retry);
    expect(await screen.findByRole('option', { name: 'Recovered' })).toBeInTheDocument();
  });

  it('shows a label from valueOptions when the value is not in the results', () => {
    render(
      <Combobox
        aria-label="City"
        onSearch={async () => []}
        value="paris"
        valueOptions={[{ value: 'paris', label: 'Paris' }]}
      />,
    );
    expect(screen.getByRole('combobox')).toHaveValue('Paris');
  });

  it('waits for minSearchLength', async () => {
    const onSearch = vi.fn(async () => []);
    render(
      <Combobox
        aria-label="City"
        onSearch={onSearch}
        minSearchLength={2}
        searchDebounce={0}
        emptyMessage="Type two letters"
      />,
    );
    const user = userEvent.setup();
    await user.type(screen.getByRole('combobox'), 'p');
    expect(await screen.findByText('Type two letters')).toBeInTheDocument();
    expect(onSearch).not.toHaveBeenCalled();
    await user.type(screen.getByRole('combobox'), 'a');
    await waitFor(() => expect(onSearch).toHaveBeenCalledWith('pa', expect.anything()));
  });
});

describe('Combobox: create', () => {
  it('offers to create the typed text and selects what onCreate returns', async () => {
    const onCreate = vi.fn((q: string) => ({ value: q.toLowerCase(), label: q }));
    const onValueChange = vi.fn();
    render(
      <Combobox
        aria-label="Label"
        options={options}
        onCreate={onCreate}
        onValueChange={onValueChange}
      />,
    );
    const user = userEvent.setup();
    await user.type(screen.getByRole('combobox'), 'Urgent');
    const create = await screen.findByRole('option', { name: 'Create “Urgent”' });
    expect(create).toBeInTheDocument();
    await user.keyboard('{End}{Enter}');
    expect(onCreate).toHaveBeenCalledWith('Urgent');
    await waitFor(() => expect(onValueChange).toHaveBeenCalledWith('urgent'));
    expect(screen.getByRole('combobox')).toHaveValue('Urgent');
  });

  it('hides the create option when a label matches exactly', async () => {
    render(<Combobox aria-label="Label" options={options} onCreate={() => undefined} />);
    const user = userEvent.setup();
    await user.type(screen.getByRole('combobox'), 'open');
    expect(screen.queryByRole('option', { name: /Create/ })).toBeNull();
  });
});

describe('Combobox: inline', () => {
  it('is always open, keeps the text as a search and selects on Enter', async () => {
    const onValueChange = vi.fn();
    render(
      <Combobox
        inline
        aria-label="Search people"
        listLabel="People"
        options={people}
        onValueChange={onValueChange}
      />,
    );
    const input = screen.getByRole('combobox', { name: 'Search people' });
    expect(input).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('listbox', { name: 'People' })).toBeInTheDocument();
    const user = userEvent.setup();
    await user.type(input, 'tur');
    await user.keyboard('{Enter}');
    expect(onValueChange).toHaveBeenCalledWith('alan');
    expect(input).toHaveValue('');
    expect(screen.getByRole('listbox')).toBeInTheDocument();
    await expectNoA11yViolations();
  });
});
