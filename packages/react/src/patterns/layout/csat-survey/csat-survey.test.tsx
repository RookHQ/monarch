import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { CSATSurvey } from './index.js';

describe('CSATSurvey', () => {
  it('asks the question with a named Good / Bad group', async () => {
    render(<CSATSurvey context="Request #1042 · Billing question" />);
    expect(
      screen.getByRole('region', { name: 'How would you rate the support you received?' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1 })).toBeInTheDocument();
    const group = screen.getByRole('group', { name: 'Your rating' });
    expect(group).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Good' })).toHaveAttribute('aria-pressed', 'false');
    expect(
      screen.getByText('Choose an answer above. You can add a comment afterwards.'),
    ).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('instant: saves the rating at once, then the comment', async () => {
    const user = userEvent.setup();
    let resolve!: () => void;
    const onRate = vi.fn(() => new Promise<void>((r) => (resolve = r)));
    const onComment = vi.fn(() => Promise.resolve());
    render(<CSATSurvey onRate={onRate} onComment={onComment} />);
    await user.click(screen.getByRole('button', { name: 'Good' }));
    expect(onRate).toHaveBeenCalledWith('good', '');
    expect(screen.getByText('Saving your answer…')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Bad' })).toBeDisabled();
    resolve();
    expect(await screen.findByText('Thanks, your answer was recorded.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Good' })).toHaveAttribute('aria-pressed', 'true');

    await user.type(
      screen.getByRole('textbox', { name: /Anything you’d like to add/ }),
      ' Quick reply ',
    );
    await user.click(screen.getByRole('button', { name: 'Save comment' }));
    expect(onComment).toHaveBeenCalledWith('Quick reply', 'good');
    expect(await screen.findByText('Thanks, your comment was saved.')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('rolls back and explains a failed save', async () => {
    const user = userEvent.setup();
    const onRate = vi.fn(() => Promise.reject(new Error('This survey has closed.')));
    render(<CSATSurvey onRate={onRate} rating="good" />);
    await user.click(screen.getByRole('button', { name: 'Bad' }));
    expect(await screen.findByText('This survey has closed.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Good' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('form mode: rating and comment are sent together, then thanks', async () => {
    const user = userEvent.setup();
    const onRate = vi.fn();
    render(<CSATSurvey mode="form" scale="five" headingAs="h2" onRate={onRate} />);
    const send = screen.getByRole('button', { name: 'Send feedback' });
    expect(send).toBeDisabled();
    const four = screen.getByRole('radio', { name: '4 Good' });
    await user.click(four);
    expect(onRate).not.toHaveBeenCalled();
    await user.type(screen.getByRole('textbox'), 'Helpful');
    await user.click(send);
    expect(onRate).toHaveBeenCalledWith(4, 'Helpful');
    await waitFor(() =>
      expect(screen.getByRole('status')).toHaveTextContent('Thanks for your feedback.'),
    );
  });

  it('locked: choices disabled, the note says why, no comment box', () => {
    render(<CSATSurvey rating="bad" locked />);
    expect(screen.getByRole('button', { name: 'Good' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Bad' })).toHaveAttribute('aria-pressed', 'true');
    expect(
      screen.getByText('Thanks for your feedback. This answer can no longer be changed.'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
  });

  it('loading, load error and submitted states', async () => {
    const { rerender } = render(<CSATSurvey loading />);
    expect(screen.getByRole('region', { name: 'Loading survey' })).toHaveAttribute(
      'aria-busy',
      'true',
    );
    rerender(<CSATSurvey error="This survey link isn’t valid." />);
    expect(
      screen.getByRole('heading', { name: 'We couldn’t load this survey' }),
    ).toBeInTheDocument();
    expect(screen.getByText('This survey link isn’t valid.')).toBeInTheDocument();
    rerender(<CSATSurvey submitted />);
    expect(screen.getByRole('status')).toHaveTextContent('Thanks for your feedback.');
    await expectNoA11yViolations();
  });

  it('prefills the saved comment and translates', () => {
    render(
      <CSATSurvey
        rating="good"
        comment="Great"
        messages={{ question: '¿Qué tal te atendimos?', good: 'Bien', bad: 'Mal' }}
      />,
    );
    expect(screen.getByRole('heading', { name: '¿Qué tal te atendimos?' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Bien' })).toBeInTheDocument();
    expect(screen.getByRole('textbox')).toHaveValue('Great');
  });

  it('renders on the server', () => {
    const html = renderToString(<CSATSurvey rating="good" />);
    expect(html).toContain('mn-csat');
    expect(html).toContain('How would you rate the support you received?');
  });
});
