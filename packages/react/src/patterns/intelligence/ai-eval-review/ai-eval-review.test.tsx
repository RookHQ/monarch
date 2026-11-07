import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiEvalReview, type AiEvalCriterion, type AiEvalItem } from './index.js';

const ITEMS: AiEvalItem[] = [
  {
    id: 'a',
    title: 'Refund policy question',
    prompt: 'How long do refunds take?',
    output: 'Refunds take **5 business days**.',
    reference: 'Five business days.',
  },
  { id: 'b', title: 'Shipping question', output: 'Orders ship within 24 hours.' },
  { id: 'c', output: 'A third answer.' },
];

const RUBRIC: AiEvalCriterion[] = [
  { id: 'accuracy', label: 'Accuracy', description: 'Matches the reference.' },
  { id: 'tone', label: 'Tone', scale: 3 },
];

describe('AiEvalReview', () => {
  it('renders the queue, the current item and the rubric', async () => {
    render(<AiEvalReview items={ITEMS} rubric={RUBRIC} data-testid="root" />);
    expect(screen.getByTestId('root')).toHaveClass('mn-ai-eval-review');
    expect(screen.getByRole('region', { name: 'Review AI outputs' })).toBeInTheDocument();
    const queue = screen.getByRole('list', { name: 'Review queue' });
    expect(within(queue).getAllByRole('button')).toHaveLength(3);
    expect(within(queue).getByRole('button', { current: true })).toHaveTextContent(
      'Refund policy question',
    );
    expect(screen.getByText('0 of 3 reviewed')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Refund policy question' })).toBeInTheDocument();
    expect(screen.getByText('5 business days').tagName).toBe('STRONG');
    expect(screen.getByText('Five business days.')).toBeInTheDocument();
    const accuracy = screen.getByRole('group', { name: 'Accuracy' });
    expect(accuracy).toHaveAccessibleDescription('Matches the reference.');
    expect(within(accuracy).getAllByRole('radio')).toHaveLength(5);
    expect(within(screen.getByRole('group', { name: 'Tone' })).getAllByRole('radio')).toHaveLength(
      3,
    );
    expect(screen.getByText('Score 0 / 8')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('scores, notes and verdicts update the review', async () => {
    const user = userEvent.setup();
    const onReviewChange = vi.fn();
    render(<AiEvalReview items={ITEMS} rubric={RUBRIC} onReviewChange={onReviewChange} />);
    await user.click(screen.getByRole('radio', { name: '4 of 5' }));
    await user.click(screen.getByRole('radio', { name: '2 of 3' }));
    expect(screen.getByText('Score 6 / 8')).toBeInTheDocument();
    await user.type(screen.getByRole('textbox', { name: 'Notes' }), 'Good');
    await user.click(screen.getByRole('radio', { name: 'Pass' }));
    expect(onReviewChange).toHaveBeenLastCalledWith(
      'a',
      { scores: { accuracy: 4, tone: 2 }, notes: 'Good', verdict: 'pass' },
      expect.objectContaining({ a: expect.anything() }),
    );
    expect(screen.getByText('1 of 3 reviewed')).toBeInTheDocument();
    const entry = within(screen.getByRole('list', { name: 'Review queue' })).getByRole('button', {
      current: true,
    });
    expect(entry).toHaveAttribute('data-verdict', 'pass');
    expect(entry).toHaveTextContent('Pass');
  });

  it('moves with Previous / Next (focusing the heading) and keeps each review', async () => {
    const user = userEvent.setup();
    render(<AiEvalReview items={ITEMS} rubric={RUBRIC} />);
    await user.click(screen.getByRole('radio', { name: '3 of 5' }));
    await user.click(screen.getByRole('button', { name: 'Next' }));
    expect(screen.getByRole('heading', { name: 'Shipping question' })).toHaveFocus();
    expect(screen.getByRole('radio', { name: '3 of 5' })).not.toBeChecked();
    await user.click(screen.getByRole('button', { name: 'Previous' }));
    expect(screen.getByRole('radio', { name: '3 of 5' })).toBeChecked();
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
  });

  it('arrow keys move through the queue with selection following focus', async () => {
    const user = userEvent.setup();
    render(<AiEvalReview items={ITEMS} rubric={RUBRIC} />);
    const queue = screen.getByRole('list', { name: 'Review queue' });
    within(queue).getByRole('button', { current: true }).focus();
    await user.keyboard('{ArrowDown}');
    expect(within(queue).getByRole('button', { name: /Shipping question/ })).toHaveFocus();
    expect(screen.getByRole('heading', { name: 'Shipping question' })).toBeInTheDocument();
    await user.keyboard('{End}');
    expect(screen.getByRole('heading', { name: 'Output 3' })).toBeInTheDocument();
    await user.keyboard('{Home}');
    expect(screen.getByRole('heading', { name: 'Refund policy question' })).toBeInTheDocument();
  });

  it('J / K and P / F shortcuts work outside text fields only', async () => {
    const user = userEvent.setup();
    render(<AiEvalReview items={ITEMS} rubric={RUBRIC} advanceOnVerdict />);
    screen.getByRole('button', { name: 'Next' }).focus();
    await user.keyboard('j');
    expect(screen.getByRole('heading', { name: 'Shipping question' })).toHaveFocus();
    await user.keyboard('k');
    expect(screen.getByRole('heading', { name: 'Refund policy question' })).toHaveFocus();
    await user.keyboard('f');
    // advanceOnVerdict moved on to the next item.
    expect(screen.getByRole('heading', { name: 'Shipping question' })).toHaveFocus();
    expect(screen.getByText('1 of 3 reviewed')).toBeInTheDocument();
    await user.click(screen.getByRole('textbox', { name: 'Notes' }));
    await user.keyboard('jp');
    expect(screen.getByRole('textbox', { name: 'Notes' })).toHaveValue('jp');
    expect(screen.getByRole('heading', { name: 'Shipping question' })).toBeInTheDocument();
  });

  it('starts at the first unreviewed item and supports controlled reviews', () => {
    render(
      <AiEvalReview
        items={ITEMS}
        rubric={RUBRIC}
        reviews={{ a: { scores: {}, notes: '', verdict: 'fail' } }}
        messages={{ progress: (r, t) => `${r}/${t}` }}
      />,
    );
    expect(screen.getByRole('heading', { name: 'Shipping question' })).toBeInTheDocument();
    expect(screen.getByText('1/3')).toBeInTheDocument();
  });

  it('shows an empty state with no items', () => {
    render(<AiEvalReview items={[]} rubric={RUBRIC} />);
    expect(screen.getByText('Nothing to review.')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiEvalReview items={ITEMS} rubric={RUBRIC} />);
    expect(html).toContain('mn-ai-eval-review');
    expect(html).toContain('Refund policy question');
  });
});
