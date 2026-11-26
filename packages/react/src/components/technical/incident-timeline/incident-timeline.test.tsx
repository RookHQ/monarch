import { describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  IncidentTimeline,
  formatIncidentTime,
  resolveIncidentStage,
  type IncidentUpdate,
} from './index.js';

const UPDATES: IncidentUpdate[] = [
  {
    id: 'u1',
    stage: 'investigating',
    body: 'Requests to the API are failing for some customers.\nWe are looking into it.',
    time: '2026-03-02T09:05:00Z',
    automated: true,
  },
  {
    id: 'u2',
    stage: 'identified',
    body: 'A configuration change caused the errors. Rolling it back.',
    time: '2026-03-02T09:20:00Z',
    author: 'Ana Lopez',
  },
  {
    id: 'u3',
    stage: 'resolved',
    body: 'Error rates are back to normal.',
    time: '2026-03-02T10:00:00Z',
  },
];

describe('IncidentTimeline', () => {
  it('renders an ordered, named list with stage, time and text', async () => {
    render(<IncidentTimeline updates={UPDATES} locale="en-GB" />);
    const list = screen.getByRole('list', { name: 'Incident updates' });
    expect(list.tagName).toBe('OL');
    const items = within(list).getAllByRole('listitem');
    expect(items).toHaveLength(3);
    expect(items[0]).toHaveTextContent('Investigating');
    expect(items[0]).toHaveAttribute('data-tone', 'danger');
    expect(items[0]!.querySelector('time')).toHaveAttribute('datetime', '2026-03-02T09:05:00.000Z');
    expect(items[0]!.querySelector('time')).toHaveTextContent('09:05');
    expect(items[0]!.querySelector('time')).toHaveTextContent('UTC');
    expect(items[2]).toHaveAttribute('data-tone', 'success');
    await expectNoA11yViolations();
  });

  it('tags automated updates and shows authors', () => {
    render(<IncidentTimeline updates={UPDATES} />);
    const [auto, person] = screen.getAllByRole('listitem');
    expect(within(auto!).getByText('Automated')).toBeInTheDocument();
    expect(auto).toHaveAttribute('data-automated');
    expect(within(person!).queryByText('Automated')).toBeNull();
    expect(within(person!).getByText('Ana Lopez')).toBeInTheDocument();
  });

  it('reflects the latest stage on the root', () => {
    const ref = createRef<HTMLDivElement>();
    render(<IncidentTimeline ref={ref} updates={UPDATES} />);
    expect(ref.current).toHaveClass('mn-incident-timeline');
    expect(ref.current).toHaveAttribute('data-stage', 'resolved');
    expect(ref.current).toHaveAttribute('data-tone', 'success');
  });

  it('accepts product stages, overrides and translated messages', () => {
    render(
      <IncidentTimeline
        updates={[
          { id: 'a', stage: 'escalated', body: 'Paged the on-call lead.', time: 0 },
          { id: 'b', stage: 'POST_MORTEM', body: 'Write-up published.', time: 0 },
        ]}
        stages={{ escalated: { label: 'Escalated', tone: 'warning' } }}
        messages={{ label: 'Updates' }}
      />,
    );
    const items = within(screen.getByRole('list', { name: 'Updates' })).getAllByRole('listitem');
    expect(items[0]).toHaveTextContent('Escalated');
    expect(items[0]).toHaveAttribute('data-tone', 'warning');
    expect(items[1]).toHaveTextContent('Post mortem');
    expect(items[1]).toHaveAttribute('data-tone', 'neutral');
  });

  it('shows an empty message and a footer slot', () => {
    render(
      <IncidentTimeline
        updates={[]}
        messages={{ empty: 'Nothing yet' }}
        footer={<button type="button">Post update</button>}
      />,
    );
    expect(screen.getByText('Nothing yet')).toBeInTheDocument();
    expect(screen.queryByRole('list')).toBeNull();
    expect(screen.getByRole('button', { name: 'Post update' })).toBeInTheDocument();
  });

  it('is a polite live region only when asked', () => {
    const { rerender } = render(<IncidentTimeline updates={UPDATES} />);
    expect(screen.getByRole('list')).not.toHaveAttribute('aria-live');
    rerender(<IncidentTimeline updates={UPDATES} live />);
    expect(screen.getByRole('list')).toHaveAttribute('aria-live', 'polite');
  });

  it('formats times in UTC by default and in a given zone', () => {
    expect(formatIncidentTime('2026-03-02T09:05:00Z', { locale: 'en-GB' })).toContain('09:05');
    expect(
      formatIncidentTime('2026-03-02T09:05:00Z', { locale: 'en-GB', timeZone: 'Asia/Kolkata' }),
    ).toContain('14:35');
    expect(formatIncidentTime('not a date')).toBe('');
    expect(resolveIncidentStage('MONITORING')).toEqual({ label: 'Monitoring', tone: 'info' });
  });

  it('renders on the server with the same text', () => {
    const html = renderToString(<IncidentTimeline updates={UPDATES} locale="en-GB" />);
    expect(html).toContain('mn-incident-timeline');
    expect(html).toContain('09:05');
    expect(html).toContain('Automated');
  });
});
