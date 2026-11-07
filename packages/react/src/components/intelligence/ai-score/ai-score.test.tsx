import { describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiScore, aiScoreBand, aiScoreBandRanges, type AiScoreBand } from './index.js';

const factors = [
  { label: 'Opened the last 3 emails', impact: 12, detail: 'In the past week' },
  { label: 'No meeting booked', impact: -8 },
  { label: 'Company size', impact: 0 },
];

describe('aiScoreBand', () => {
  it('picks the band with the highest min at or below the value', () => {
    expect(aiScoreBand(0)?.label).toBe('Low');
    expect(aiScoreBand(39)?.label).toBe('Low');
    expect(aiScoreBand(40)?.label).toBe('Medium');
    expect(aiScoreBand(70)?.label).toBe('High');
    expect(aiScoreBand(100)?.label).toBe('High');
  });

  it('sorts bands and computes ranges', () => {
    const bands: AiScoreBand[] = [
      { min: 50, label: 'B' },
      { min: 0, label: 'A' },
    ];
    expect(aiScoreBandRanges(bands).map((b) => [b.label, b.min, b.max])).toEqual([
      ['A', 0, 49],
      ['B', 50, 100],
    ]);
    expect(aiScoreBand(10, [{ min: 20, label: 'Only' }])?.label).toBe('Only');
  });
});

describe('AiScore', () => {
  it('renders a named group with the value as text in the ring centre', async () => {
    const ref = createRef<HTMLDivElement>();
    const { container } = render(
      <AiScore ref={ref} data-testid="root" className="extra" value={72} label="Lead score" />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-score', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'Lead score' })).toBe(root);
    expect(root).toHaveAttribute('data-variant', 'ring');
    expect(root).toHaveAttribute('data-tone', 'success');
    const center = container.querySelector('.mn-ai-score__center')!;
    expect(center).toHaveTextContent('72');
    expect(center).toHaveTextContent('out of 100');
    expect(container.querySelector('svg.mn-ai-score__svg')).toHaveAttribute('aria-hidden');
    expect(container.querySelector('.mn-ai-score__arc')).toHaveAttribute(
      'stroke-dasharray',
      '72 100',
    );
    await expectNoA11yViolations();
  });

  it('shows the band label as text, never colour alone', () => {
    const { container } = render(<AiScore value={25} />);
    const band = container.querySelector('.mn-ai-score__band')!;
    expect(band).toHaveTextContent('Low');
    expect(band).toHaveAttribute('data-tone', 'danger');
  });

  it('identifies the score as AI-generated', () => {
    render(<AiScore value={50} />);
    expect(screen.getByRole('group', { name: 'Score' })).toHaveTextContent('AI score');
  });

  it('clamps out-of-range values', () => {
    const { container, rerender } = render(<AiScore value={140} />);
    expect(container.querySelector('.mn-ai-score__number')).toHaveTextContent('100');
    rerender(<AiScore value={-5} />);
    expect(container.querySelector('.mn-ai-score__number')).toHaveTextContent('0');
  });

  it('shows "No score yet" and no band for null', () => {
    const { container } = render(<AiScore value={null} />);
    expect(container).toHaveTextContent('No score yet');
    expect(container.querySelector('.mn-ai-score__band')).toBeNull();
    expect(container.querySelector('.mn-ai-score__arc')).toBeNull();
    expect(container.querySelector('.mn-ai-score')).toHaveAttribute('data-empty');
  });

  it('renders the bar variant with the value as text and band ticks', async () => {
    const { container } = render(<AiScore value={55} variant="bar" label="Health" />);
    expect(container.querySelector('.mn-ai-score__bar')).toHaveAttribute('aria-hidden');
    expect(container.querySelector('.mn-ai-score__fill')).toHaveStyle({ inlineSize: '55%' });
    expect(container.querySelectorAll('.mn-ai-score__tick')).toHaveLength(2);
    expect(container.querySelector('.mn-ai-score__bar-text')).toHaveTextContent('55');
    expect(container).toHaveTextContent('Medium');
    await expectNoA11yViolations();
  });

  it('renders factors as a data table with signed impacts', async () => {
    render(<AiScore value={64} factors={factors} rationale="Engaged but no meeting yet." />);
    const table = screen.getByRole('table', { name: 'What affects this score' });
    const rows = within(table).getAllByRole('row');
    expect(rows).toHaveLength(4);
    expect(within(rows[1]!).getByRole('rowheader')).toHaveTextContent('Opened the last 3 emails');
    expect(rows[1]).toHaveTextContent('In the past week');
    expect(rows[1]).toHaveTextContent('+12');
    expect(rows[1]).toHaveAttribute('data-direction', 'up');
    expect(rows[2]).toHaveTextContent('-8');
    expect(rows[2]).toHaveAttribute('data-direction', 'down');
    expect(rows[3]).toHaveTextContent('0');
    expect(rows[3]).toHaveAttribute('data-direction', 'none');
    expect(screen.getByText('Engaged but no meeting yet.')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('explains the bands in a legend table that marks the current band', async () => {
    const user = userEvent.setup();
    const { container } = render(<AiScore value={45} />);
    const details = container.querySelector('details')!;
    expect(details).not.toHaveAttribute('open');
    await user.click(screen.getByText('How to read this score'));
    expect(details).toHaveAttribute('open');
    const rows = within(details).getAllByRole('row');
    expect(rows.map((r) => r.textContent)).toEqual([
      'BandRange',
      'Low0–39',
      'Medium (this score)40–69',
      'High70–100',
    ]);
    await expectNoA11yViolations();
  });

  it('hides the legend with showBands={false}', () => {
    const { container } = render(<AiScore value={45} showBands={false} />);
    expect(container.querySelector('details')).toBeNull();
  });

  it('uses custom bands and messages', () => {
    render(
      <AiScore
        value={90}
        bands={[
          { min: 0, label: 'Faible', tone: 'neutral' },
          { min: 80, label: 'Fort', tone: 'ai' },
        ]}
        messages={{ label: 'Note', outOf: (m) => `sur ${m}`, aiGenerated: 'Note IA' }}
      />,
    );
    const group = screen.getByRole('group', { name: 'Note' });
    expect(group).toHaveTextContent('Fort');
    expect(group).toHaveTextContent('sur 100');
    expect(group).toHaveTextContent('Note IA');
    expect(group).toHaveAttribute('data-tone', 'ai');
  });

  it('renders on the server', () => {
    const html = renderToString(<AiScore value={72} factors={factors} variant="bar" />);
    expect(html).toContain('mn-ai-score');
    expect(html).toContain('High');
    expect(html).toContain('+12');
  });
});
