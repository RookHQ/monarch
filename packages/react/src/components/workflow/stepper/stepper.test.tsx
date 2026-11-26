import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Stepper, stepStatus, type StepperStep } from './index.js';

const STEPS: StepperStep[] = [
  { id: 'basics', label: 'Basics' },
  { id: 'access', label: 'Access', description: 'Who can join' },
  { id: 'branding', label: 'Branding', optional: true },
  { id: 'review', label: 'Review' },
];

const list = () => screen.getByRole('list');
const step = (name: string) =>
  within(list())
    .getAllByRole('listitem')
    .find((li) => li.textContent?.includes(name)) as HTMLElement;

describe('Stepper', () => {
  it('is an ordered list in a named nav with aria-current="step" on the current step', async () => {
    render(<Stepper steps={STEPS} current="access" />);
    const nav = screen.getByRole('navigation', { name: 'Progress' });
    expect(nav).toHaveAttribute('data-orientation', 'horizontal');
    expect(list().tagName).toBe('OL');
    expect(step('Access')).toHaveAttribute('aria-current', 'step');
    expect(step('Basics')).not.toHaveAttribute('aria-current');
    expect(step('Basics')).toHaveAttribute('data-status', 'complete');
    expect(step('Access')).toHaveAttribute('data-status', 'current');
    expect(step('Review')).toHaveAttribute('data-status', 'upcoming');
    // Status and position are text for assistive technology.
    expect(step('Basics')).toHaveTextContent('Step 1 of 4: Basics (completed)');
    expect(step('Branding')).toHaveTextContent('Optional');
    expect(step('Access')).toHaveTextContent('Who can join');
    await expectNoA11yViolations();
  });

  it('takes the current step by index, and clamps it', () => {
    const { rerender } = render(<Stepper steps={STEPS} current={2} />);
    expect(step('Branding')).toHaveAttribute('aria-current', 'step');
    rerender(<Stepper steps={STEPS} current={99} />);
    expect(step('Review')).toHaveAttribute('aria-current', 'step');
  });

  it('shows errors and out-of-order completion', async () => {
    render(
      <Stepper
        steps={[
          { id: 'a', label: 'Upload', status: 'error' },
          { id: 'b', label: 'Map columns' },
          { id: 'c', label: 'Preview', status: 'complete' },
          { id: 'd', label: 'Import' },
        ]}
        current="b"
      />,
    );
    expect(step('Upload')).toHaveAttribute('data-status', 'error');
    expect(step('Upload')).toHaveTextContent('(needs attention)');
    expect(step('Preview')).toHaveAttribute('data-status', 'complete');
    await expectNoA11yViolations();
  });

  it('makes reached steps buttons only when onStepSelect is given', async () => {
    const user = userEvent.setup();
    const onStepSelect = vi.fn();
    const { rerender } = render(<Stepper steps={STEPS} current="branding" />);
    expect(screen.queryAllByRole('button')).toHaveLength(0);
    rerender(<Stepper steps={STEPS} current="branding" onStepSelect={onStepSelect} />);
    // Completed steps are buttons; the current and future steps are not.
    const buttons = screen.getAllByRole('button');
    expect(buttons.map((b) => b.textContent)).toEqual([
      'Step 1 of 4: Basics (completed)',
      'Step 2 of 4: Access (completed)Who can join',
    ]);
    await user.click(buttons[0] as HTMLElement);
    expect(onStepSelect).toHaveBeenCalledWith(STEPS[0], 0);
    await user.tab();
    expect(buttons[1]).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onStepSelect).toHaveBeenLastCalledWith(STEPS[1], 1);
    await expectNoA11yViolations();
  });

  it('keeps steps up to `furthest` clickable after going back, except disabled ones', async () => {
    const user = userEvent.setup();
    function Wizard() {
      const [current, setCurrent] = useState('basics');
      return (
        <Stepper
          steps={STEPS.map((s) => (s.id === 'branding' ? { ...s, disabled: true } : s))}
          current={current}
          furthest="review"
          onStepSelect={(s) => setCurrent(s.id)}
        />
      );
    }
    render(<Wizard />);
    expect(screen.getAllByRole('button').map((b) => b.textContent)).toEqual([
      // The number in the circle is aria-hidden; the text carries "Step n of m".
      '2Step 2 of 4: AccessWho can join',
      '4Step 4 of 4: Review',
    ]);
    await user.click(screen.getByRole('button', { name: /Review/ }));
    expect(step('Review')).toHaveAttribute('aria-current', 'step');
    expect(step('Basics')).toHaveAttribute('data-status', 'complete');
  });

  it('has a compact "Step n of m" summary and a vertical layout', () => {
    const { container, rerender } = render(
      <Stepper steps={STEPS} current={1} compact="always" aria-label="Setup progress" />,
    );
    const nav = screen.getByRole('navigation', { name: 'Setup progress' });
    expect(nav).toHaveAttribute('data-compact', 'always');
    const summary = container.querySelector('.mn-stepper__summary');
    expect(summary).toHaveTextContent('Step 2 of 4Access');
    // The bar is decorative: the summary text carries the information.
    expect(container.querySelector('.mn-stepper__progress')).toHaveAttribute('aria-hidden', 'true');
    rerender(<Stepper steps={STEPS} current={1} orientation="vertical" />);
    expect(screen.getByRole('navigation')).toHaveAttribute('data-orientation', 'vertical');
  });

  it('translates its messages', () => {
    render(
      <Stepper
        steps={STEPS}
        current={1}
        messages={{ stepOf: (n, t) => `Étape ${n} sur ${t}`, complete: 'terminée' }}
      />,
    );
    expect(step('Basics')).toHaveTextContent('Étape 1 sur 4: Basics (terminée)');
  });

  it('derives statuses with stepStatus', () => {
    expect(stepStatus(0, 1, { id: 'a', label: 'A' })).toBe('complete');
    expect(stepStatus(1, 1, { id: 'a', label: 'A' })).toBe('current');
    expect(stepStatus(2, 1, { id: 'a', label: 'A' })).toBe('upcoming');
    expect(stepStatus(1, 1, { id: 'a', label: 'A', status: 'error' })).toBe('error');
  });

  it('renders on the server', () => {
    const html = renderToString(<Stepper steps={STEPS} current="access" />);
    expect(html).toContain('mn-stepper');
    expect(html).toContain('aria-current="step"');
  });
});
