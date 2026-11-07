import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { AiPlanPart } from '../ai-conversation/index.js';
import { AiPlan } from './index.js';

const part: AiPlanPart = {
  type: 'plan',
  steps: [
    { id: 's1', label: 'Collect last quarter’s figures', state: 'todo' },
    { id: 's2', label: 'Compare them with the forecast', state: 'todo' },
    { id: 's3', label: 'Draft a short summary', state: 'todo' },
  ],
};

const inputs = () => screen.getAllByRole('textbox') as HTMLInputElement[];
const values = () => inputs().map((i) => i.value);

describe('AiPlan', () => {
  it('renders a labelled, editable plan with human-in-the-loop copy', async () => {
    const ref = createRef<HTMLDivElement>();
    render(<AiPlan ref={ref} part={part} data-testid="root" className="extra" />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-plan', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'Proposed plan' })).toHaveAccessibleDescription(
      /Nothing runs until then\./,
    );
    expect(values()).toEqual(part.steps.map((s) => s.label));
    expect(screen.getByRole('textbox', { name: 'Step 2' })).toBeInTheDocument();
    expect(root).toHaveAttribute('data-decision', 'pending');
    await expectNoA11yViolations();
  });

  it('edits a label inline and reports the draft', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<AiPlan part={part} onChange={onChange} />);
    const first = screen.getByRole('textbox', { name: 'Step 1' });
    await user.clear(first);
    await user.type(first, 'Collect figures');
    expect(first).toHaveValue('Collect figures');
    expect(onChange).toHaveBeenLastCalledWith([
      { id: 's1', label: 'Collect figures', state: 'todo' },
      part.steps[1],
      part.steps[2],
    ]);
  });

  it('reorders with the move buttons and from the keyboard handle', async () => {
    const user = userEvent.setup();
    render(<AiPlan part={part} />);
    await user.click(screen.getByRole('button', { name: 'Move Draft a short summary up' }));
    expect(values()[1]).toBe('Draft a short summary');
    const handle = screen.getByRole('button', { name: 'Reorder Draft a short summary' });
    handle.focus();
    await user.keyboard(' {ArrowUp} ');
    expect(values()[0]).toBe('Draft a short summary');
  });

  it('adds a step with the button and with Enter, focusing the new field', async () => {
    const user = userEvent.setup();
    render(<AiPlan part={part} />);
    await user.click(screen.getByRole('button', { name: 'Add step' }));
    expect(inputs()).toHaveLength(4);
    expect(document.activeElement).toBe(inputs()[3]);
    await user.keyboard('Share it with the team');
    await user.click(inputs()[0]!);
    await user.keyboard('{Enter}');
    expect(inputs()).toHaveLength(5);
    expect(document.activeElement).toBe(inputs()[1]);
    expect(values()[4]).toBe('Share it with the team');
  });

  it('removes steps, announcing it and moving focus to a neighbour', async () => {
    const user = userEvent.setup();
    render(<AiPlan part={part} />);
    await user.click(screen.getByRole('button', { name: 'Remove Compare them with the forecast' }));
    expect(values()).toEqual(['Collect last quarter’s figures', 'Draft a short summary']);
    expect(document.activeElement).toBe(inputs()[0]);
    expect(screen.getByText('Removed Compare them with the forecast')).toBeInTheDocument();
    // Backspace in an empty field removes it too.
    await user.clear(inputs()[1]!);
    await user.keyboard('{Backspace}');
    expect(inputs()).toHaveLength(1);
  });

  it('approves the cleaned steps, then becomes a read-only list and takes focus', async () => {
    const user = userEvent.setup();
    const onApprove = vi.fn();
    render(<AiPlan part={part} onApprove={onApprove} />);
    await user.click(screen.getByRole('button', { name: 'Add step' })); // left empty
    await user.click(screen.getByRole('button', { name: 'Approve plan' }));
    expect(onApprove).toHaveBeenCalledWith(part.steps);
    const group = screen.getByRole('group', { name: 'Proposed plan' });
    expect(group).toHaveAttribute('data-decision', 'approved');
    expect(document.activeElement).toBe(group);
    expect(screen.queryAllByRole('textbox')).toHaveLength(0);
    expect(screen.queryByRole('button')).toBeNull();
    const list = within(group).getByRole('list');
    expect(within(list).getAllByRole('listitem')).toHaveLength(3);
    expect(group).toHaveTextContent('Plan approved.');
    await expectNoA11yViolations();
  });

  it('rejects', async () => {
    const user = userEvent.setup();
    const onReject = vi.fn();
    render(<AiPlan part={part} onReject={onReject} />);
    await user.click(screen.getByRole('button', { name: 'Reject' }));
    expect(onReject).toHaveBeenCalled();
    expect(screen.getByRole('group')).toHaveAttribute('data-decision', 'rejected');
    expect(screen.getByRole('group')).toHaveTextContent('Plan rejected.');
  });

  it('disables Approve while every step is empty and says why', async () => {
    const user = userEvent.setup();
    render(
      <AiPlan part={{ type: 'plan', steps: [{ id: 'x', label: 'Only step', state: 'todo' }] }} />,
    );
    await user.click(screen.getByRole('button', { name: 'Remove Only step' }));
    expect(document.activeElement).toBe(screen.getByRole('button', { name: 'Add step' }));
    expect(screen.getByRole('button', { name: 'Approve plan' })).toBeDisabled();
    expect(screen.getByText('Add at least one step to approve the plan.')).toBeInTheDocument();
  });

  it('is read-only when not editable and follows a controlled decision with live states', () => {
    const live: AiPlanPart = {
      type: 'plan',
      steps: [
        { id: 's1', label: 'Collect', state: 'done' },
        { id: 's2', label: 'Compare', state: 'active' },
      ],
    };
    const { rerender } = render(<AiPlan part={part} editable={false} />);
    expect(screen.queryAllByRole('textbox')).toHaveLength(0);
    expect(screen.getByRole('button', { name: 'Approve plan' })).toBeInTheDocument();
    rerender(<AiPlan part={live} decision="approved" />);
    expect(screen.getAllByRole('listitem')[1]).toHaveAttribute('aria-current', 'step');
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('replaces the draft when the model sends a new plan, but not on its own echo', async () => {
    const user = userEvent.setup();
    function Host() {
      const [p, setP] = useState(part);
      return (
        <>
          <AiPlan part={p} onChange={(steps) => setP({ type: 'plan', steps })} />
          <button
            type="button"
            onClick={() =>
              setP({ type: 'plan', steps: [{ id: 'n', label: 'New plan step', state: 'todo' }] })
            }
          >
            New plan
          </button>
        </>
      );
    }
    render(<Host />);
    await user.type(inputs()[0]!, '!');
    expect(values()[0]).toBe('Collect last quarter’s figures!');
    await user.click(screen.getByRole('button', { name: 'New plan' }));
    expect(values()).toEqual(['New plan step']);
  });

  it('translates its strings', () => {
    render(
      <AiPlan
        part={part}
        messages={{ title: 'Plan proposé', approve: 'Approuver', reject: 'Refuser' }}
      />,
    );
    expect(screen.getByRole('group', { name: 'Plan proposé' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Approuver' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiPlan part={part} />);
    expect(html).toContain('mn-ai-plan');
    expect(html).toContain('Proposed plan');
  });
});
