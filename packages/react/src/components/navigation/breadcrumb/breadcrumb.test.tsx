import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Breadcrumb } from './index.js';

describe('Breadcrumb', () => {
  it('marks the current page and collapses the middle', async () => {
    render(
      <Breadcrumb
        items={[
          { label: 'Home', href: '/' },
          { label: 'Projects', href: '/p' },
          { label: 'Rook', href: '/p/rook' },
          { label: 'Sprint 4', href: '/p/rook/4' },
          { label: 'Task 12' },
        ]}
        maxVisible={4}
      />,
    );
    expect(screen.getByRole('navigation', { name: 'Breadcrumb' })).toBeInTheDocument();
    expect(screen.getByText('Task 12')).toHaveAttribute('aria-current', 'page');
    const more = screen.getByRole('button', { name: /show 2 more levels/i });
    await userEvent.setup().click(more);
    expect(await screen.findByRole('menuitem', { name: 'Projects' })).toBeInTheDocument();
  });

  it('has no axe violations', async () => {
    render(<Breadcrumb items={[{ label: 'Home', href: '/' }, { label: 'Deals' }]} />);
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <Breadcrumb items={[{ label: 'Home', href: '/' }, { label: 'Deals' }]} />,
    );
    expect(html).toContain('mn-breadcrumb');
  });
});
