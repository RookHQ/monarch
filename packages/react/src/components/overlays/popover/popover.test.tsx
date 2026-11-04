import { describe, expect, it } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import { Popover, PopoverContent, PopoverTrigger } from './index.js';

describe('Popover', () => {
  it('opens and closes with Escape, returning focus', async () => {
    render(
      <Popover>
        <PopoverTrigger asChild>
          <Button>Filters</Button>
        </PopoverTrigger>
        <PopoverContent aria-label="Filters">
          <Button>Apply</Button>
        </PopoverContent>
      </Popover>,
    );
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Filters' });
    await user.click(trigger);
    const panel = await screen.findByRole('dialog', { name: 'Filters' });
    expect(panel).toBeInTheDocument();
    await expectNoA11yViolations(panel);
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(trigger).toHaveFocus();
  });

  it('renders on the server (closed)', () => {
    const html = renderToString(
      <Popover>
        <PopoverTrigger asChild>
          <Button>Filters</Button>
        </PopoverTrigger>
        <PopoverContent aria-label="Filters">x</PopoverContent>
      </Popover>,
    );
    expect(html).toContain('aria-haspopup="dialog"');
  });
});
