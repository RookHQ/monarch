import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldDescription, FieldError, FieldLabel } from '../form-field/index.js';
import { RatingPicker, type RatingOption } from './index.js';

const scale: RatingOption[] = [
  { value: 1, label: 'Below' },
  { value: 2, label: 'Partly meets' },
  { value: 3, label: 'Meets' },
  { value: 4, label: 'Exceeds' },
  { value: 5, label: 'Outstanding' },
];

describe('RatingPicker', () => {
  it('is a named radio group with one radio per value and its label', async () => {
    render(<RatingPicker aria-label="Overall rating" options={scale} data-testid="root" />);
    const group = screen.getByRole('radiogroup', { name: 'Overall rating' });
    expect(group).toHaveClass('mn-rating-picker');
    expect(group).toHaveAttribute('data-variant', 'scale');
    const radios = screen.getAllByRole('radio');
    expect(radios).toHaveLength(5);
    expect(screen.getByRole('radio', { name: '3 Meets' })).toHaveAttribute('aria-checked', 'false');
    // With no value, only the first radio is in the tab order.
    expect(radios.map((r) => r.tabIndex)).toEqual([0, -1, -1, -1, -1]);
    await expectNoA11yViolations();
  });

  it('defaults to a 1–5 numeric scale and supports a custom range', () => {
    const { rerender } = render(<RatingPicker aria-label="Score" />);
    expect(screen.getAllByRole('radio').map((r) => r.textContent)).toEqual([
      '1',
      '2',
      '3',
      '4',
      '5',
    ]);
    rerender(<RatingPicker aria-label="Score" min={0} max={10} />);
    expect(screen.getAllByRole('radio')).toHaveLength(11);
  });

  it('selects on click and reports the value', async () => {
    const onValueChange = vi.fn();
    render(<RatingPicker aria-label="Rating" options={scale} onValueChange={onValueChange} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('radio', { name: '4 Exceeds' }));
    expect(onValueChange).toHaveBeenLastCalledWith(4);
    expect(screen.getByRole('radio', { name: '4 Exceeds' })).toHaveAttribute(
      'aria-checked',
      'true',
    );
    // Clicking the selected value again does nothing unless clearable.
    await user.click(screen.getByRole('radio', { name: '4 Exceeds' }));
    expect(onValueChange).toHaveBeenCalledTimes(1);
  });

  it('arrow keys move focus and selection, wrapping; Home and End jump', async () => {
    const onValueChange = vi.fn();
    render(
      <RatingPicker
        aria-label="Rating"
        options={scale}
        defaultValue={2}
        onValueChange={onValueChange}
      />,
    );
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('radio', { name: '2 Partly meets' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('radio', { name: '3 Meets' })).toHaveFocus();
    expect(onValueChange).toHaveBeenLastCalledWith(3);
    await user.keyboard('{ArrowLeft}{ArrowUp}');
    expect(onValueChange).toHaveBeenLastCalledWith(1);
    await user.keyboard('{ArrowUp}');
    expect(screen.getByRole('radio', { name: '5 Outstanding' })).toHaveFocus();
    await user.keyboard('{Home}');
    expect(onValueChange).toHaveBeenLastCalledWith(1);
    await user.keyboard('{End}');
    expect(onValueChange).toHaveBeenLastCalledWith(5);
    // Tab leaves the group in one step.
    await user.tab();
    expect(document.body).toHaveFocus();
  });

  it('clearable: activating the selected value or pressing Delete clears it', async () => {
    const onValueChange = vi.fn();
    render(
      <RatingPicker
        aria-label="Rating"
        options={scale}
        defaultValue={3}
        clearable
        onValueChange={onValueChange}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('radio', { name: '3 Meets' }));
    expect(onValueChange).toHaveBeenLastCalledWith(null);
    expect(
      screen.getAllByRole('radio').every((r) => r.getAttribute('aria-checked') === 'false'),
    ).toBe(true);
    // Focus stays on 3 after clearing; the next arrow selects 4.
    await user.keyboard('{ArrowRight}');
    expect(onValueChange).toHaveBeenLastCalledWith(4);
    await user.keyboard('{Delete}');
    expect(onValueChange).toHaveBeenLastCalledWith(null);
  });

  it('controlled: follows the value prop', async () => {
    const onValueChange = vi.fn();
    const { rerender } = render(
      <RatingPicker aria-label="Rating" value={2} onValueChange={onValueChange} />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('radio', { name: '5' }));
    expect(onValueChange).toHaveBeenCalledWith(5);
    expect(screen.getByRole('radio', { name: '2' })).toHaveAttribute('aria-checked', 'true');
    rerender(<RatingPicker aria-label="Rating" value={5} onValueChange={onValueChange} />);
    expect(screen.getByRole('radio', { name: '5' })).toHaveAttribute('aria-checked', 'true');
  });

  it('disabled and read-only do not change the value', async () => {
    const onValueChange = vi.fn();
    const { rerender } = render(
      <RatingPicker aria-label="Rating" defaultValue={2} disabled onValueChange={onValueChange} />,
    );
    const user = userEvent.setup();
    screen.getAllByRole('radio').forEach((r) => expect(r).toBeDisabled());
    rerender(
      <RatingPicker aria-label="Rating" defaultValue={2} readOnly onValueChange={onValueChange} />,
    );
    expect(screen.getByRole('radiogroup')).toHaveAttribute('aria-readonly', 'true');
    await user.click(screen.getByRole('radio', { name: '4' }));
    screen.getByRole('radio', { name: '2' }).focus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('radio', { name: '3' })).toHaveFocus();
    expect(onValueChange).not.toHaveBeenCalled();
    expect(screen.getByRole('radio', { name: '2' })).toHaveAttribute('aria-checked', 'true');
  });

  it('stars variant names each star "n of max" with its label and shows the selected label', async () => {
    render(
      <RatingPicker
        aria-label="Satisfaction"
        variant="stars"
        options={scale}
        defaultValue={4}
        messages={{ starLabel: (v, m) => `${v} von ${m}` }}
      />,
    );
    expect(screen.getByRole('radio', { name: '4 von 5' })).toHaveAttribute('aria-checked', 'true');
    const root = screen.getByRole('radiogroup');
    expect(root.querySelectorAll('[data-filled]')).toHaveLength(4);
    expect(root.querySelector('.mn-rating-picker__caption')).toHaveTextContent('Exceeds');
    await expectNoA11yViolations();
  });

  it('default star names include the label', () => {
    render(<RatingPicker aria-label="Satisfaction" variant="stars" options={scale} />);
    expect(screen.getByRole('radio', { name: '2 of 5, Partly meets' })).toBeInTheDocument();
  });

  it('takes its label, description and error from a Field', async () => {
    render(
      <Field invalid required>
        <FieldLabel>Overall rating</FieldLabel>
        <RatingPicker options={scale} />
        <FieldDescription>Pick the closest match.</FieldDescription>
        <FieldError>Choose a rating.</FieldError>
      </Field>,
    );
    const group = screen.getByRole('radiogroup', { name: 'Overall rating' });
    expect(group).toHaveAccessibleDescription('Pick the closest match. Choose a rating.');
    expect(group).toHaveAttribute('aria-invalid', 'true');
    expect(group).toHaveAttribute('aria-required', 'true');
    await expectNoA11yViolations();
  });

  it('submits through a hidden input when named', () => {
    const { container } = render(
      <form>
        <RatingPicker aria-label="Rating" name="rating" defaultValue={3} />
      </form>,
    );
    const form = container.querySelector('form')!;
    expect(new FormData(form).get('rating')).toBe('3');
  });

  it('forwards the ref and extra props', () => {
    let node: HTMLDivElement | null = null;
    render(
      <RatingPicker
        ref={(el) => {
          node = el;
        }}
        aria-label="Rating"
        data-testid="root"
        className="extra"
      />,
    );
    expect(node).toBe(screen.getByTestId('root'));
    expect(screen.getByTestId('root')).toHaveClass('mn-rating-picker', 'extra');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <RatingPicker aria-label="Rating" options={scale} defaultValue={3} />,
    );
    expect(html).toContain('mn-rating-picker');
    expect(html).toContain('role="radiogroup"');
    expect(html).toContain('aria-checked="true"');
  });
});
