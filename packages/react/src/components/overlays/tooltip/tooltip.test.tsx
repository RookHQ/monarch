import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import { Tooltip, TooltipProvider } from './index.js';

describe('Tooltip', () => {
  it('appears on keyboard focus and describes the trigger', async () => {
    render(
      <TooltipProvider delayDuration={0}>
        <Tooltip content="Copy link">
          <Button>Copy</Button>
        </Tooltip>
      </TooltipProvider>,
    );
    const user = userEvent.setup();
    await user.tab();
    const tip = await screen.findByRole('tooltip');
    expect(tip).toHaveTextContent('Copy link');
    expect(screen.getByRole('button', { name: 'Copy' })).toHaveAccessibleDescription('Copy link');
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <TooltipProvider>
        <Tooltip content="tip">
          <button type="button">t</button>
        </Tooltip>
      </TooltipProvider>,
    );
    expect(html).toContain('<button');
  });
});
