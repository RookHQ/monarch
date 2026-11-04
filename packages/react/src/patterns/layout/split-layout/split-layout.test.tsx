import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { SplitLayout } from './index.js';

describe('SplitLayout', () => {
  it('labels both panes and shows a back button when asked', async () => {
    const onBack = vi.fn();
    render(
      <SplitLayout
        listLabel="Tickets"
        detailLabel="Ticket"
        list={<p>list</p>}
        detail={<p>detail</p>}
        detailOpen
        onBack={onBack}
      />,
    );
    expect(screen.getByRole('region', { name: 'Tickets' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Ticket' })).toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole('button', { name: 'Back to list' }));
    expect(onBack).toHaveBeenCalledOnce();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<SplitLayout list={<p>l</p>} detail={<p>d</p>} />);
    expect(html).toContain('mn-split');
  });
});
