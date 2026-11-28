import { describe, expect, it } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DirectionProvider } from '../../../internal/direction.js';
import { Button } from '../../actions/button/index.js';
import { Popover, PopoverContent, PopoverTrigger } from './index.js';
import { OverlayStage } from '../../../internal/overlay-stage.js';

describe('Popover', () => {
  it('is never modal in an overlay stage, even when asked to be', () => {
    render(
      <>
        <button type="button">Elsewhere</button>
        <OverlayStage>
          <Popover defaultOpen modal>
            <PopoverTrigger asChild>
              <Button>Filters</Button>
            </PopoverTrigger>
            <PopoverContent aria-label="Filters">Only mine</PopoverContent>
          </Popover>
        </OverlayStage>
      </>,
    );
    const stage = document.querySelector('[data-mn-overlay-stage]');
    expect(stage?.contains(screen.getByText('Only mine'))).toBe(true);
    expect(screen.getByRole('button', { name: 'Elsewhere' }).closest('[aria-hidden]')).toBeNull();
    expect(stage?.contains(document.activeElement)).toBe(false);
  });

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

  it('takes the reading direction of a DirectionProvider into the portalled panel', async () => {
    render(
      <DirectionProvider dir="rtl">
        <Popover defaultOpen>
          <PopoverTrigger asChild>
            <Button>Filter</Button>
          </PopoverTrigger>
          <PopoverContent aria-label="Filters">Body</PopoverContent>
        </Popover>
      </DirectionProvider>,
    );
    expect(await screen.findByRole('dialog', { name: 'Filters' })).toHaveAttribute('dir', 'rtl');
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
