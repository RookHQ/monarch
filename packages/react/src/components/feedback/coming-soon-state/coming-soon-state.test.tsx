import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import { ComingSoonState } from './index.js';

describe('ComingSoonState', () => {
  it('defaults to a "Coming soon" heading with a decorative icon', async () => {
    render(<ComingSoonState data-testid="root" description="Reports arrive later this year." />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-empty-state', 'mn-coming-soon-state');
    expect(screen.getByRole('heading', { level: 3, name: 'Coming soon' })).toBeInTheDocument();
    expect(root.querySelector('.mn-empty-state__icon')).toHaveAttribute('aria-hidden', 'true');
    expect(root.querySelector('.mn-coming-soon-state__badge')).toBeNull();
    expect(screen.getByText('Reports arrive later this year.')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('puts the badge in the heading name when a feature title is given', () => {
    render(<ComingSoonState title="Status pages" headingLevel="h2" />);
    expect(
      screen.getByRole('heading', { level: 2, name: /^Coming soon ?: Status pages$/ }),
    ).toBeInTheDocument();
  });

  it('can hide the badge and translate the strings', () => {
    const { rerender } = render(<ComingSoonState title="Status pages" badge={false} />);
    expect(screen.getByRole('heading', { name: 'Status pages' })).toBeInTheDocument();
    rerender(
      <ComingSoonState
        title="Statusseiten"
        messages={{ badge: 'Demnächst', title: 'Demnächst' }}
      />,
    );
    expect(
      screen.getByRole('heading', { name: /^Demnächst ?: Statusseiten$/ }),
    ).toBeInTheDocument();
    rerender(<ComingSoonState messages={{ title: 'Bientôt disponible' }} />);
    expect(screen.getByRole('heading', { name: 'Bientôt disponible' })).toBeInTheDocument();
  });

  it('lists planned highlights and renders actions that work', async () => {
    let clicked = 0;
    render(
      <ComingSoonState
        title="Status pages"
        highlights={['Public uptime history', 'Incident updates by email']}
        highlightsLabel="What is planned"
        actions={<Button onClick={() => (clicked += 1)}>Notify me</Button>}
        tip="Expected early next year"
      />,
    );
    const list = screen.getByRole('list', { name: 'What is planned' });
    expect(list.querySelectorAll('li')).toHaveLength(2);
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Notify me' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(clicked).toBe(1);
    expect(screen.getByText('Expected early next year')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('accepts a custom icon, no icon, and forwards the ref', () => {
    let node: HTMLDivElement | null = null;
    const { container, rerender } = render(
      <ComingSoonState
        ref={(el) => {
          node = el;
        }}
        icon={<svg data-testid="custom" />}
      />,
    );
    expect(screen.getByTestId('custom')).toBeInTheDocument();
    expect(node).toBe(container.firstChild);
    rerender(<ComingSoonState icon={null} />);
    expect(container.querySelector('.mn-empty-state__icon')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<ComingSoonState title="Status pages" highlights={['One']} />);
    expect(html).toContain('mn-coming-soon-state');
    expect(html).toContain('Coming soon');
  });
});
