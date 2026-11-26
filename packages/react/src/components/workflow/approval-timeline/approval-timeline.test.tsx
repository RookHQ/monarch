import { describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ApprovalTimeline, type ApprovalTimelineStep } from './index.js';

const NOW = '2026-03-10T12:00:00Z';

const STEPS: ApprovalTimelineStep[] = [
  {
    id: 's',
    label: 'Request',
    status: 'submitted',
    actor: { name: 'Alex Morgan' },
    at: '2026-03-10T09:00:00Z',
  },
  {
    id: '1',
    label: 'Reporting manager',
    status: 'approved',
    actor: { name: 'Sam Lee' },
    at: '2026-03-10T10:00:00Z',
    comment: 'Looks right.',
  },
  { id: '2', label: 'Department head', status: 'skipped', note: 'Same person as the requester.' },
  {
    id: '3',
    label: 'Finance check',
    status: 'pending',
    approvers: [{ name: 'Jo Park' }, { name: 'Ana Ruiz' }],
    delegatedFrom: { name: 'Kim Chen' },
  },
  { id: '4', label: 'Final sign-off', status: 'waiting' },
];

describe('ApprovalTimeline', () => {
  it('lists every step with its status text, people and comment, without axe violations', async () => {
    render(<ApprovalTimeline steps={STEPS} now={NOW} locale="en-GB" />);
    const list = screen.getByRole('list', { name: 'Approval timeline' });
    expect(list).toHaveClass('mn-approval-timeline');
    const items = within(list).getAllByRole('listitem');
    expect(items).toHaveLength(5);
    expect(items[0]).toHaveTextContent('RequestSubmitted');
    expect(items[0]).toHaveTextContent('Alex Morgan');
    expect(items[1]).toHaveAttribute('data-status', 'approved');
    expect(items[1]).toHaveTextContent('Looks right.');
    expect(items[1]).toHaveTextContent('2 hours ago');
    expect(items[2]).toHaveTextContent('Same person as the requester.');
    expect(items[3]).toHaveAttribute('aria-current', 'step');
    expect(items[3]).toHaveTextContent('Decided by Jo Park, Ana Ruiz (standing in for Kim Chen)');
    expect(items[4]).toHaveTextContent('Up next');
    await expectNoA11yViolations();
  });

  it('shows the empty message when there are no steps', () => {
    render(<ApprovalTimeline steps={[]} data-testid="root" />);
    expect(screen.getByTestId('root')).toHaveAttribute('data-empty');
    expect(screen.getByText('Not submitted yet.')).toBeInTheDocument();
  });

  it('translates labels and hides avatars on request', () => {
    const { container } = render(
      <ApprovalTimeline
        steps={[STEPS[1]!]}
        now={NOW}
        showAvatars={false}
        messages={{ label: 'Historique', status: { approved: 'Approuvé' } }}
      />,
    );
    expect(screen.getByRole('list', { name: 'Historique' })).toHaveTextContent('Approuvé');
    expect(container.querySelector('.mn-avatar')).toBeNull();
  });

  it('forwards the ref and extra attributes', () => {
    let node: HTMLElement | null = null;
    render(
      <ApprovalTimeline
        ref={(n) => {
          node = n;
        }}
        steps={STEPS}
        className="extra"
        now={NOW}
      />,
    );
    expect(node).toHaveClass('mn-approval-timeline', 'extra');
  });

  it('renders on the server', () => {
    const html = renderToString(<ApprovalTimeline steps={STEPS} now={NOW} />);
    expect(html).toContain('mn-approval-timeline');
    expect(html).toContain('Finance check');
  });
});
