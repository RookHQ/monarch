import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel } from '../form-field/index.js';
import type { ComboboxOption } from '../combobox/index.js';
import { EntityPicker } from './index.js';

const projects: ComboboxOption[] = [
  { value: 'apollo', label: 'Apollo', hint: 'APL', group: 'Active' },
  { value: 'borealis', label: 'Borealis', hint: 'BOR', group: 'Active' },
  { value: 'cassini', label: 'Cassini', description: 'Paused since March', group: 'Paused' },
  { value: 'apex', label: 'Data Apex', group: 'Paused', disabled: true },
];

/** The trigger: a select-only combobox named by its label, showing the value as its content. */
function picker(name: string, text: string) {
  const el = screen.getByRole('combobox', { name });
  expect(el).toHaveTextContent(text);
  return el;
}

describe('EntityPicker', () => {
  it('opens from a labelled trigger, searches and picks, then closes and refocuses the trigger', async () => {
    const onValueChange = vi.fn();
    render(
      <Field>
        <FieldLabel>Project</FieldLabel>
        <EntityPicker
          options={projects}
          onValueChange={onValueChange}
          placeholder="Choose a project"
        />
      </Field>,
    );
    const trigger = picker('Project', 'Choose a project');
    expect(trigger).toHaveClass('mn-entity-picker');
    expect(trigger).toHaveAttribute('data-placeholder');
    const user = userEvent.setup();
    await user.click(trigger);
    const dialog = await screen.findByRole('dialog', { name: 'Project' });
    expect(trigger).toHaveAttribute('aria-expanded', 'true');
    expect(trigger).toHaveAttribute('aria-controls', dialog.id);
    const search = within(dialog).getByRole('combobox', { name: 'Search…' });
    expect(search).toHaveFocus();
    expect(within(dialog).getByRole('listbox', { name: 'Project' })).toBeInTheDocument();
    await user.keyboard('bor');
    await user.keyboard('{Enter}');
    expect(onValueChange).toHaveBeenCalledWith('borealis');
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(trigger).toHaveFocus();
    expect(picker('Project', 'Borealis')).toBe(trigger);
    await expectNoA11yViolations();
  });

  it('lists groups and ranks prefix matches first', async () => {
    render(<EntityPicker aria-label="Project" options={projects} defaultOpen />);
    expect(screen.getAllByRole('group')).toHaveLength(2);
    const user = userEvent.setup();
    await user.keyboard('ap');
    const names = screen.getAllByRole('option').map((o) => o.textContent);
    expect(names[0]).toMatch(/^Apollo/);
    expect(names[1]).toMatch(/^Data Apex/);
    await expectNoA11yViolations();
  });

  it('closes on Escape without changing the value', async () => {
    const onValueChange = vi.fn();
    render(
      <EntityPicker
        aria-label="Project"
        options={projects}
        defaultValue="apollo"
        onValueChange={onValueChange}
      />,
    );
    const trigger = picker('Project', 'Apollo');
    const user = userEvent.setup();
    await user.click(trigger);
    await screen.findByRole('dialog');
    await user.keyboard('{ArrowDown}{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(onValueChange).not.toHaveBeenCalled();
    // ArrowDown on the trigger opens it too (APG select-only combobox).
    await user.keyboard('{ArrowDown}');
    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(onValueChange).not.toHaveBeenCalled();
    expect(trigger).toHaveFocus();
  });

  it('multiple: stays open, toggles values and summarises as first +N', async () => {
    function Demo() {
      const [values, setValues] = useState<string[]>(['apollo']);
      return (
        <EntityPicker
          aria-label="Projects"
          multiple
          options={projects}
          values={values}
          onValuesChange={setValues}
        />
      );
    }
    render(<Demo />);
    const user = userEvent.setup();
    await user.click(picker('Projects', 'Apollo'));
    const listbox = await screen.findByRole('listbox');
    expect(listbox).toHaveAttribute('aria-multiselectable', 'true');
    await user.click(screen.getByRole('option', { name: /Borealis/ }));
    await user.click(screen.getByRole('option', { name: /Cassini/ }));
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByRole('option', { name: /Borealis/ })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    await user.keyboard('{Escape}');
    expect(picker('Projects', 'Apollo+2')).toBeInTheDocument();
  });

  it('clear option, footer with close, and a custom trigger', async () => {
    const onValueChange = vi.fn();
    render(
      <EntityPicker
        options={projects}
        defaultValue="apollo"
        onValueChange={onValueChange}
        clearOption={{ label: 'No project' }}
        trigger={<button type="button">Change project</button>}
        footer={({ close, query }) => (
          <button type="button" onClick={close}>
            Manage projects{query ? ` (${query})` : ''}
          </button>
        )}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Change project' }));
    const clear = await screen.findByRole('option', { name: 'No project' });
    expect(clear).toHaveAttribute('aria-selected', 'false');
    await user.keyboard('ca');
    expect(screen.getByRole('button', { name: 'Manage projects (ca)' })).toBeInTheDocument();
    await user.clear(screen.getByRole('combobox'));
    await user.click(screen.getByRole('option', { name: 'No project' }));
    expect(onValueChange).toHaveBeenCalledWith(null);
    await user.click(screen.getByRole('button', { name: 'Change project' }));
    await user.click(await screen.findByRole('button', { name: 'Manage projects' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
  });

  it('async search with create keeps labels for picked records', async () => {
    const onSearch = vi.fn(async (q: string) =>
      q
        ? [{ value: `c-${q}`, label: `Contact ${q}` }]
        : [{ value: 'c-1', label: 'Recent contact' }],
    );
    const onCreate = vi.fn((q: string) => ({ value: 'new', label: q }));
    render(
      <EntityPicker
        aria-label="Contact"
        onSearch={onSearch}
        searchDebounce={0}
        onCreate={onCreate}
        valueOptions={[]}
      />,
    );
    const user = userEvent.setup();
    await user.click(picker('Contact', 'Select…'));
    expect(await screen.findByRole('option', { name: 'Recent contact' })).toBeInTheDocument();
    await user.keyboard('Rae');
    expect(await screen.findByRole('option', { name: 'Contact Rae' })).toBeInTheDocument();
    await user.click(screen.getByRole('option', { name: 'Create “Rae”' }));
    await waitFor(() => expect(picker('Contact', 'Rae')).toBeInTheDocument());
    expect(onCreate).toHaveBeenCalledWith('Rae');
  });

  it('is disabled from the Field and marked invalid', () => {
    render(
      <Field disabled invalid>
        <FieldLabel>Owner</FieldLabel>
        <EntityPicker options={projects} />
      </Field>,
    );
    const trigger = screen.getByRole('combobox', { name: 'Owner' });
    expect(trigger).toBeDisabled();
    expect(trigger).toHaveAttribute('aria-invalid', 'true');
    expect(trigger).toHaveAttribute('data-invalid');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <EntityPicker aria-label="Project" options={projects} defaultValue="apollo" />,
    );
    expect(html).toContain('mn-entity-picker');
    expect(html).toContain('Apollo');
  });
});
