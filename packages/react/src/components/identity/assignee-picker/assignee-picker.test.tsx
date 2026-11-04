import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel } from '../../forms/form-field/index.js';
import { AssigneePicker, type AssigneePerson } from './index.js';

const people: AssigneePerson[] = [
  { id: 'u1', name: 'Ada Lovelace', email: 'ada@example.com', hint: '3 open' },
  { id: 'u2', name: 'Grace Hopper', email: 'grace@example.com', status: 'online' },
  { id: 'u3', name: 'Alan Turing', email: 'alan@example.com' },
  { id: 'u4', name: 'Katherine Johnson', email: 'katherine@example.com' },
];

/** The trigger: a select-only combobox named by its label, showing the value as its content. */
function picker(name: string, text: string) {
  const el = screen.getByRole('combobox', { name });
  expect(el).toHaveTextContent(text);
  return el;
}

describe('AssigneePicker', () => {
  it('lists Unassigned, then you, then everyone with avatars; search matches email', async () => {
    const onValueChange = vi.fn();
    render(
      <Field>
        <FieldLabel>Assignee</FieldLabel>
        <AssigneePicker people={people} currentUserId="u3" onValueChange={onValueChange} />
      </Field>,
    );
    const trigger = picker('Assignee', 'Unassigned');
    expect(trigger).toHaveClass('mn-assignee-picker', 'mn-entity-picker');
    const user = userEvent.setup();
    await user.click(trigger);
    const names = (await screen.findAllByRole('option')).map((o) => o.textContent);
    expect(names[0]).toBe('Unassigned');
    expect(names[1]).toMatch(/^AT.*Alan Turing \(you\)/);
    expect(names).toHaveLength(5);
    await user.keyboard('grace@');
    expect(screen.getAllByRole('option')).toHaveLength(1);
    await user.keyboard('{Enter}');
    expect(onValueChange).toHaveBeenCalledWith('u2');
    await waitFor(() => expect(picker('Assignee', 'Grace Hopper')).toBeInTheDocument());
    await expectNoA11yViolations();
  });

  it('assigns to me from the footer and hides the shortcut once I am assigned', async () => {
    function Demo() {
      const [v, setV] = useState<string | null>(null);
      return (
        <AssigneePicker
          aria-label="Owner"
          people={people}
          currentUserId="u1"
          value={v}
          onValueChange={setV}
        />
      );
    }
    render(<Demo />);
    const user = userEvent.setup();
    await user.click(picker('Owner', 'Unassigned'));
    await user.click(await screen.findByRole('button', { name: 'Assign to me' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    const trigger = picker('Owner', 'Ada Lovelace (you)');
    await user.click(trigger);
    await screen.findByRole('dialog');
    expect(screen.queryByRole('button', { name: 'Assign to me' })).toBeNull();
    // Unassigned clears.
    await user.click(screen.getByRole('option', { name: 'Unassigned' }));
    await waitFor(() => expect(picker('Owner', 'Unassigned')).toBeInTheDocument());
  });

  it('multiple: stays open and summarises as an avatar stack with a count', async () => {
    const onValuesChange = vi.fn();
    render(
      <AssigneePicker
        aria-label="Reviewers"
        multiple
        people={people}
        defaultValues={['u1', 'u2']}
        onValuesChange={onValuesChange}
        maxAvatars={2}
      />,
    );
    const trigger = picker('Reviewers', '2 people');
    expect(trigger.querySelector('.mn-avatar-group')).not.toBeNull();
    const user = userEvent.setup();
    await user.click(trigger);
    await user.click(await screen.findByRole('option', { name: /Alan Turing/ }));
    expect(onValuesChange).toHaveBeenLastCalledWith(['u1', 'u2', 'u3']);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    await user.keyboard('{Escape}');
    expect(picker('Reviewers', '3 people')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('searches people on a server and keeps the picked person’s name', async () => {
    const onSearch = vi.fn(async (q: string) =>
      people.filter((p) => p.name.toLowerCase().includes(q.toLowerCase())),
    );
    render(
      <AssigneePicker
        aria-label="Owner"
        onSearch={onSearch}
        searchDebounce={0}
        allowUnassigned={false}
      />,
    );
    const user = userEvent.setup();
    await user.click(picker('Owner', 'Select…'));
    await user.keyboard('kath');
    await user.click(await screen.findByRole('option', { name: /Katherine Johnson/ }));
    await waitFor(() => expect(picker('Owner', 'Katherine Johnson')).toBeInTheDocument());
    expect(screen.queryByRole('option', { name: 'Unassigned' })).toBeNull();
  });

  it('uses valuePeople for a value outside the list, and translated messages', () => {
    render(
      <AssigneePicker
        aria-label="Owner"
        people={[]}
        value="u9"
        valuePeople={[{ id: 'u9', name: 'Mary Jackson' }]}
        messages={{ unassigned: 'Sin asignar' }}
      />,
    );
    expect(picker('Owner', 'Mary Jackson')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AssigneePicker aria-label="Owner" people={people} defaultValue="u2" />,
    );
    expect(html).toContain('mn-assignee-picker');
    expect(html).toContain('Grace Hopper');
  });
});
