import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ScoreRing } from './index.js';

describe('ScoreRing', () => {
  it('is a meter with the value as text in the centre', async () => {
    const { container } = render(<ScoreRing data-testid="root" value={78.4} />);
    const meter = screen.getByRole('meter', { name: 'Score' });
    expect(meter).toHaveClass('mn-score-ring');
    expect(meter).toHaveAttribute('aria-valuenow', '78.4');
    expect(meter).toHaveAttribute('aria-valuetext', '78 out of 100');
    expect(container.querySelector('.mn-score-ring__value')).toHaveTextContent('78');
    expect(container.querySelector('.mn-score-ring__max')).toHaveTextContent('/ 100');
    expect(meter).toHaveAttribute('data-tone', 'info');
    await expectNoA11yViolations();
  });

  it('takes its name from the visible label', async () => {
    render(<ScoreRing value={91} label="Visibility" description="This week" />);
    expect(screen.getByRole('meter', { name: 'Visibility' })).toHaveAttribute(
      'data-tone',
      'success',
    );
    expect(screen.getByText('This week')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('clamps, applies thresholds and fixed tones', () => {
    const { rerender } = render(<ScoreRing value={140} />);
    expect(screen.getByRole('meter')).toHaveAttribute('aria-valuenow', '100');
    rerender(<ScoreRing value={-5} />);
    expect(screen.getByRole('meter')).toHaveAttribute('data-tone', 'danger');
    rerender(<ScoreRing value={45} />);
    expect(screen.getByRole('meter')).toHaveAttribute('data-tone', 'warning');
    rerender(<ScoreRing value={45} tone="accent" />);
    expect(screen.getByRole('meter')).toHaveAttribute('data-tone', 'accent');
    rerender(
      <ScoreRing
        value={3}
        max={5}
        thresholds={[
          { min: 4, tone: 'success' },
          { min: 0, tone: 'warning' },
        ]}
        formatValue={(v) => v.toFixed(1)}
      />,
    );
    const meter = screen.getByRole('meter');
    expect(meter).toHaveAttribute('data-tone', 'warning');
    expect(meter).toHaveAttribute('aria-valuetext', '3.0 out of 5');
  });

  it('shows an empty state for null', async () => {
    render(<ScoreRing value={null} label="Health" />);
    const img = screen.getByRole('img', { name: 'Health No score yet' });
    expect(img).toHaveAttribute('data-empty');
    expect(img.querySelector('.mn-score-ring__arc')).toBeNull();
    await expectNoA11yViolations();
  });

  it('honours aria-label and messages', () => {
    render(
      <ScoreRing
        value={50}
        aria-label="Answer engine score"
        messages={{ valueText: (v, max) => `${v} von ${max}` }}
        size={48}
      />,
    );
    const meter = screen.getByRole('meter', { name: 'Answer engine score' });
    expect(meter).toHaveAttribute('aria-valuetext', '50 von 100');
    expect(meter.querySelector('.mn-score-ring__max')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<ScoreRing value={62} label="Score" />);
    expect(html).toContain('mn-score-ring');
    expect(html).toContain('role="meter"');
  });
});
