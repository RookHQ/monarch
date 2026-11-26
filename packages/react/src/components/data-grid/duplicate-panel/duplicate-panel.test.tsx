import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DuplicatePanel, type DuplicateMatch } from './index.js';

const MATCHES: DuplicateMatch[] = [
  {
    id: '1',
    title: 'Example Ltd',
    kind: 'Client',
    reasons: ['Same email', 'same name'],
    href: '/clients/1',
    meta: 'Archived',
  },
  {
    id: '2',
    title: 'Example Group',
    kind: 'Open lead',
    reasons: ['Same company domain'],
    score: 0.82,
    href: 'javascript:alert(1)',
  },
];

describe('DuplicatePanel', () => {
  it('announces likely duplicates with why they matched', async () => {
    const { rerender } = render(<DuplicatePanel data-testid="p" matches={[]} />);
    const panel = screen.getByTestId('p');
    expect(panel).not.toBeVisible();
    expect(panel).toHaveAttribute('role', 'status');
    rerender(
      <DuplicatePanel data-testid="p" matches={MATCHES} footer="You can still create it." />,
    );
    expect(panel).toBeVisible();
    expect(panel).toHaveTextContent('Possible duplicates');
    expect(panel).toHaveTextContent('Client · Same email, same name');
    expect(panel).toHaveTextContent('Open lead · Same company domain · 82% match');
    expect(panel).toHaveTextContent('You can still create it.');
    const open = screen.getByRole('link', { name: 'Open Example Ltd (opens in a new tab)' });
    expect(open).toHaveAttribute('target', '_blank');
    expect(open).toHaveAttribute('rel', 'noopener noreferrer');
    // Unsafe links are dropped.
    expect(screen.getAllByRole('link')).toHaveLength(1);
    await expectNoA11yViolations();
  });

  it('merges and dismisses by keyboard', async () => {
    const user = userEvent.setup();
    const onMerge = vi.fn();
    function Demo() {
      const [matches, setMatches] = useState(MATCHES);
      return (
        <DuplicatePanel
          variant="panel"
          matches={matches}
          onMerge={onMerge}
          onDismiss={(m) => setMatches((all) => all.filter((x) => x.id !== m.id))}
          openInNewTab={false}
        />
      );
    }
    render(<Demo />);
    expect(
      screen.getByRole('heading', { name: 'Possible duplicates', level: 3 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Open Example Ltd' })).not.toHaveAttribute('target');
    await user.tab();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Merge with Example Ltd' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onMerge).toHaveBeenCalledWith(MATCHES[0]);
    await user.click(screen.getByRole('button', { name: 'Example Group is not a duplicate' }));
    const list = screen.getByRole('list');
    expect(within(list).getAllByRole('listitem')).toHaveLength(1);
    expect(screen.getByRole('heading')).toHaveTextContent('Possible duplicate');
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<DuplicatePanel matches={MATCHES} />);
    expect(html).toContain('mn-duplicate-panel');
    expect(html).toContain('Example Ltd');
  });
});
