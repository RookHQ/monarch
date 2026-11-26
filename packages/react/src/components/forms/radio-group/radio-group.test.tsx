import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldDescription, FieldError, FieldLabel } from '../form-field/index.js';
import { RadioGroup, RadioGroupItem, type RadioOption } from './index.js';

const plans: RadioOption[] = [
  { value: 'starter', label: 'Starter', description: 'One workspace.' },
  { value: 'team', label: 'Team', description: 'Up to 50 people.' },
  { value: 'enterprise', label: 'Enterprise', disabled: true },
  { value: 'custom', label: 'Custom' },
];

describe('RadioGroup', () => {
  it('is a labelled radiogroup of named, described radios', async () => {
    render(<RadioGroup label="Plan" description="Change it at any time." options={plans} />);
    const group = screen.getByRole('radiogroup', { name: 'Plan' });
    expect(group).toHaveAccessibleDescription('Change it at any time.');
    expect(group).toHaveAttribute('data-orientation', 'vertical');
    expect(screen.getAllByRole('radio')).toHaveLength(4);
    expect(screen.getByRole('radio', { name: 'Team' })).toHaveAccessibleDescription(
      'Up to 50 people.',
    );
    expect(screen.getByRole('radio', { name: 'Enterprise' })).toBeDisabled();
    await expectNoA11yViolations();
  });

  it('selects on click and on its label, and reports the value', async () => {
    const onValueChange = vi.fn();
    render(<RadioGroup label="Plan" options={plans} onValueChange={onValueChange} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('radio', { name: 'Team' }));
    expect(screen.getByRole('radio', { name: 'Team' })).toBeChecked();
    expect(onValueChange).toHaveBeenLastCalledWith('team');
    await user.click(screen.getByText('Starter'));
    expect(screen.getByRole('radio', { name: 'Starter' })).toBeChecked();
    expect(screen.getByRole('radio', { name: 'Team' })).not.toBeChecked();
    expect(onValueChange).toHaveBeenLastCalledWith('starter');
  });

  it('has one Tab stop on the checked radio; arrow keys move, select and skip disabled', async () => {
    render(
      <>
        <button type="button">Before</button>
        <RadioGroup label="Plan" options={plans} defaultValue="team" />
        <button type="button">After</button>
      </>,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Before' }));
    await user.tab();
    expect(screen.getByRole('radio', { name: 'Team' })).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    // Enterprise is disabled, so the arrow skips to Custom.
    expect(screen.getByRole('radio', { name: 'Custom' })).toHaveFocus();
    expect(screen.getByRole('radio', { name: 'Custom' })).toBeChecked();
    await user.keyboard('{ArrowUp}');
    expect(screen.getByRole('radio', { name: 'Team' })).toBeChecked();
    await user.tab();
    expect(screen.getByRole('button', { name: 'After' })).toHaveFocus();
  });

  it('Space selects the focused radio when none is checked', async () => {
    render(<RadioGroup label="Plan" options={plans} />);
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('radio', { name: 'Starter' })).toHaveFocus();
    await user.keyboard(' ');
    expect(screen.getByRole('radio', { name: 'Starter' })).toBeChecked();
  });

  it('follows the value prop when controlled', async () => {
    function Controlled() {
      const [value, setValue] = useState('starter');
      return (
        <>
          <RadioGroup label="Plan" options={plans} value={value} onValueChange={setValue} />
          <output>{value}</output>
        </>
      );
    }
    render(<Controlled />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('radio', { name: 'Custom' }));
    expect(screen.getByRole('status')).toHaveTextContent('custom');
    expect(screen.getByRole('radio', { name: 'Custom' })).toBeChecked();
  });

  it('ignores clicks when the value prop does not change', async () => {
    render(<RadioGroup label="Plan" options={plans} value="starter" />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('radio', { name: 'Team' }));
    expect(screen.getByRole('radio', { name: 'Starter' })).toBeChecked();
    expect(screen.getByRole('radio', { name: 'Team' })).not.toBeChecked();
  });

  it('takes its label, description, error, required and invalid from a Field', async () => {
    render(
      <Field invalid required>
        <FieldLabel>Contact method</FieldLabel>
        <FieldDescription>How we reach you about this request.</FieldDescription>
        <RadioGroup
          options={[
            { value: 'email', label: 'Email' },
            { value: 'phone', label: 'Phone' },
          ]}
        />
        <FieldError>Choose a contact method.</FieldError>
      </Field>,
    );
    const group = screen.getByRole('radiogroup', { name: 'Contact method' });
    expect(group).toHaveAccessibleDescription(
      'How we reach you about this request. Choose a contact method.',
    );
    expect(group).toHaveAttribute('aria-required', 'true');
    expect(group).toHaveAttribute('aria-invalid', 'true');
    for (const radio of screen.getAllByRole('radio')) expect(radio).toBeRequired();
    await expectNoA11yViolations();
  });

  it('disables every radio from a disabled Field or prop', () => {
    render(
      <Field disabled>
        <FieldLabel>Size</FieldLabel>
        <RadioGroup
          options={[
            { value: 's', label: 'Small' },
            { value: 'l', label: 'Large' },
          ]}
        />
      </Field>,
    );
    for (const radio of screen.getAllByRole('radio')) expect(radio).toBeDisabled();
  });

  it('submits the selected value with a form under `name`', () => {
    let submitted: FormData | null = null;
    render(
      <form
        aria-label="Settings"
        onSubmit={(event) => {
          event.preventDefault();
          submitted = new FormData(event.currentTarget);
        }}
      >
        <RadioGroup label="Plan" name="plan" options={plans} defaultValue="team" />
        <button type="submit">Save</button>
      </form>,
    );
    fireEvent.submit(screen.getByRole('form', { name: 'Settings' }));
    expect(submitted!.get('plan')).toBe('team');
  });

  it('accepts RadioGroupItem children and a horizontal layout', async () => {
    render(
      <RadioGroup aria-label="Alignment" orientation="horizontal" defaultValue="left">
        <RadioGroupItem value="left" label="Left" />
        <RadioGroupItem value="center" label="Center" />
        <RadioGroupItem value="right" aria-label="Right" />
      </RadioGroup>,
    );
    const group = screen.getByRole('radiogroup', { name: 'Alignment' });
    expect(group).toHaveAttribute('data-orientation', 'horizontal');
    expect(screen.getByRole('radio', { name: 'Left' })).toBeChecked();
    const user = userEvent.setup();
    await user.click(screen.getByRole('radio', { name: 'Right' }));
    expect(screen.getByRole('radio', { name: 'Right' })).toBeChecked();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<RadioGroup label="Plan" options={plans} defaultValue="team" />);
    expect(html).toContain('mn-radio-group');
    expect(html).toContain('role="radiogroup"');
    expect(html).toMatch(/checked="" value="team"/);
  });
});
