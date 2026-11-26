import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DirectionProvider } from '../../../internal/direction.js';
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

  it('stays hidden while disabled and toggling it never warns', async () => {
    const error = vi.spyOn(console, 'error').mockImplementation(() => {});
    function Demo() {
      const [off, setOff] = useState(true);
      return (
        <TooltipProvider delayDuration={0}>
          <Tooltip content="Copy link" disabled={off}>
            <Button>Copy</Button>
          </Tooltip>
          <button type="button" onClick={() => setOff(!off)}>
            toggle
          </button>
        </TooltipProvider>
      );
    }
    try {
      render(<Demo />);
      const user = userEvent.setup();
      await user.tab();
      expect(screen.queryByRole('tooltip')).toBeNull();
      await user.click(screen.getByRole('button', { name: 'toggle' }));
      await user.click(screen.getByRole('button', { name: 'toggle' }));
      expect(error).not.toHaveBeenCalled();
    } finally {
      error.mockRestore();
    }
  });

  it('takes the reading direction of a DirectionProvider into the portalled bubble', async () => {
    render(
      <DirectionProvider dir="rtl">
        <TooltipProvider>
          <Tooltip content="Archive" open>
            <Button>Go</Button>
          </Tooltip>
        </TooltipProvider>
      </DirectionProvider>,
    );
    await screen.findByRole('tooltip');
    expect(document.querySelector('.mn-tooltip')).toHaveAttribute('dir', 'rtl');
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
