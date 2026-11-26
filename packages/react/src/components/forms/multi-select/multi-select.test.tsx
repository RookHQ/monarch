import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel } from '../form-field/index.js';
import { MultiSelect, type MultiSelectOption } from './index.js';

const labels: MultiSelectOption[] = [
  { value: 'bug', label: 'Bug' },
  { value: 'design', label: 'Design' },
  { value: 'docs', label: 'Documentation' },
  { value: 'perf', label: 'Performance' },
];

const chips = () => screen.queryAllByRole('listitem').map((li) => li.textContent);

describe('MultiSelect', () => {
  it('is a labelled combobox with a multi-selectable listbox; search, Enter and click pick', async () => {
    const onValueChange = vi.fn();
    render(
      <Field>
        <FieldLabel>Labels</FieldLabel>
        <MultiSelect options={labels} defaultValue={['bug']} onValueChange={onValueChange} />
      </Field>,
    );
    const input = screen.getByRole('combobox', { name: 'Labels' });
    expect(chips()).toEqual(['Bug']);
    const user = userEvent.setup();
    await user.type(input, 'doc');
    const listbox = await screen.findByRole('listbox', { name: 'Labels' });
    expect(listbox).toHaveAttribute('aria-multiselectable', 'true');
    expect(screen.getAllByRole('option')).toHaveLength(1);
    await user.keyboard('{Enter}');
    expect(onValueChange).toHaveBeenLastCalledWith(['bug', 'docs']);
    expect(input).toHaveValue('');
    expect(input).toHaveAttribute('aria-expanded', 'true');
    await user.click(screen.getByRole('option', { name: /Design/ }));
    expect(onValueChange).toHaveBeenLastCalledWith(['bug', 'docs', 'design']);
    expect(chips()).toEqual(['Bug', 'Documentation', 'Design']);
    await expectNoA11yViolations();
  });

  it('moves with arrow keys, closes with Escape and keeps focus on the input', async () => {
    render(<MultiSelect aria-label="Labels" options={labels} />);
    const input = screen.getByRole('combobox', { name: 'Labels' });
    const user = userEvent.setup();
    await user.click(input);
    await user.keyboard('{ArrowDown}');
    expect(input).toHaveAttribute('aria-expanded', 'true');
    await user.keyboard('{End}{Enter}');
    expect(chips()).toEqual(['Performance']);
    await user.keyboard('{Escape}');
    expect(input).toHaveAttribute('aria-expanded', 'false');
    expect(input).toHaveFocus();
  });

  it('removes chips with their button and with Backspace', async () => {
    function Controlled() {
      const [value, setValue] = useState<string[]>(['bug', 'design', 'docs']);
      return (
        <MultiSelect aria-label="Labels" options={labels} value={value} onValueChange={setValue} />
      );
    }
    render(<Controlled />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Remove Design' }));
    expect(chips()).toEqual(['Bug', 'Documentation']);
    expect(screen.getByRole('combobox')).toHaveFocus();
    await user.keyboard('{Backspace}');
    expect(chips()).toEqual(['Bug']);
  });

  it('disables the other options and shows a count at maxSelections', async () => {
    render(
      <MultiSelect
        aria-label="Labels"
        options={labels}
        defaultValue={['bug']}
        maxSelections={2}
        messages={{ limit: (n, max) => `${n}/${max}` }}
      />,
    );
    const input = screen.getByRole('combobox', { name: 'Labels' });
    expect(input).toHaveAccessibleDescription('1/2');
    const user = userEvent.setup();
    await user.click(input);
    await user.keyboard('{ArrowDown}');
    await user.click(screen.getByRole('option', { name: /Design/ }));
    expect(input).toHaveAccessibleDescription('2/2');
    expect(screen.getByRole('option', { name: /Documentation/ })).toHaveAttribute(
      'aria-disabled',
      'true',
    );
    expect(screen.getByRole('option', { name: /Bug/ })).not.toHaveAttribute(
      'aria-disabled',
      'true',
    );
    await user.click(screen.getByRole('option', { name: /Documentation/ }));
    expect(chips()).toEqual(['Bug', 'Design']);
  });

  it('submits each value under `name`, never the search text', async () => {
    let submitted: FormData | null = null;
    render(
      <form
        aria-label="Filter"
        onSubmit={(event) => {
          event.preventDefault();
          submitted = new FormData(event.currentTarget);
        }}
      >
        <MultiSelect
          aria-label="Labels"
          name="label"
          options={labels}
          defaultValue={['bug', 'perf']}
        />
      </form>,
    );
    await userEvent.setup().type(screen.getByRole('combobox'), 'des');
    fireEvent.submit(screen.getByRole('form', { name: 'Filter' }));
    expect(submitted!.getAll('label')).toEqual(['bug', 'perf']);
    expect([...submitted!.keys()]).toEqual(['label', 'label']);
  });

  it('has no remove buttons and submits nothing when disabled', () => {
    const { container } = render(
      <MultiSelect
        aria-label="Labels"
        name="label"
        disabled
        options={labels}
        defaultValue={['bug']}
      />,
    );
    expect(screen.queryByRole('button', { name: /Remove/ })).toBeNull();
    expect(container.querySelector('input[type="hidden"]')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <MultiSelect aria-label="Labels" options={labels} defaultValue={['bug']} maxSelections={3} />,
    );
    expect(html).toContain('mn-multi-select');
    expect(html).toContain('1 of 3 selected');
  });
});
