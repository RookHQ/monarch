import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldError, FieldLabel } from '../form-field/index.js';
import { RadioCardGroup, type RadioCardOption } from './index.js';

const Icon = () => <svg data-testid="icon" viewBox="0 0 16 16" />;

const plans: RadioCardOption[] = [
  {
    value: 'standard',
    title: 'Standard',
    description: 'Arrives in 3 to 5 working days.',
    meta: '€4.90',
    icon: <Icon />,
  },
  { value: 'express', title: 'Express', description: 'Arrives the next working day.' },
  { value: 'pickup', title: 'Pick up in store', disabled: true },
];

describe('RadioCardGroup', () => {
  it('is a labelled radiogroup of cards named by title and described by meta and description', async () => {
    render(<RadioCardGroup label="Delivery" options={plans} defaultValue="standard" />);
    expect(screen.getByRole('radiogroup', { name: 'Delivery' })).toHaveClass('mn-radio-card-group');
    const standard = screen.getByRole('radio', { name: 'Standard' });
    expect(standard).toBeChecked();
    expect(standard).toHaveAccessibleDescription('€4.90 Arrives in 3 to 5 working days.');
    expect(standard.closest('label')).toHaveAttribute('data-state', 'checked');
    expect(screen.getByTestId('icon').parentElement).toHaveAttribute('aria-hidden', 'true');
    expect(screen.getByRole('radio', { name: 'Pick up in store' })).toBeDisabled();
    await expectNoA11yViolations();
  });

  it('selects when any part of the card is clicked', async () => {
    const onValueChange = vi.fn();
    render(<RadioCardGroup label="Delivery" options={plans} onValueChange={onValueChange} />);
    const user = userEvent.setup();
    await user.click(screen.getByText('Arrives the next working day.'));
    expect(screen.getByRole('radio', { name: 'Express' })).toBeChecked();
    expect(onValueChange).toHaveBeenCalledWith('express');
    expect(screen.getByText('Express').closest('label')).toHaveAttribute('data-state', 'checked');
  });

  it('moves and selects with arrow keys, skipping disabled cards', async () => {
    render(<RadioCardGroup label="Delivery" options={plans} defaultValue="standard" />);
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('radio', { name: 'Standard' })).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('radio', { name: 'Express' })).toBeChecked();
    await user.keyboard('{ArrowDown}');
    // Pick up is disabled, so focus wraps to the first card.
    expect(screen.getByRole('radio', { name: 'Standard' })).toHaveFocus();
    expect(screen.getByRole('radio', { name: 'Standard' })).toBeChecked();
  });

  it('works in a Field and submits under `name`', async () => {
    let submitted: FormData | null = null;
    render(
      <form
        aria-label="Checkout"
        onSubmit={(event) => {
          event.preventDefault();
          submitted = new FormData(event.currentTarget);
        }}
      >
        <Field invalid required>
          <FieldLabel>Delivery</FieldLabel>
          <RadioCardGroup name="delivery" options={plans} columns={2} defaultValue="express" />
          <FieldError>Choose a delivery option.</FieldError>
        </Field>
      </form>,
    );
    const group = screen.getByRole('radiogroup', { name: 'Delivery' });
    expect(group).toHaveAttribute('data-columns', '2');
    expect(group).toHaveAttribute('aria-invalid', 'true');
    expect(group).toHaveAccessibleDescription('Choose a delivery option.');
    fireEvent.submit(screen.getByRole('form', { name: 'Checkout' }));
    expect(submitted!.get('delivery')).toBe('express');
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<RadioCardGroup label="Delivery" options={plans} />);
    expect(html).toContain('mn-radio-card-group__card');
    expect(html).toContain('role="radiogroup"');
  });
});
