import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Button } from '../components/actions/button/index.js';
import {
  Dialog,
  DialogContent,
  DialogTitle,
  DialogTrigger,
} from '../components/overlays/dialog/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../components/overlays/dropdown-menu/index.js';
import { Popover, PopoverContent, PopoverTrigger } from '../components/overlays/popover/index.js';
import { Sheet, SheetContent, SheetTitle } from '../components/overlays/sheet/index.js';
import { Tooltip, TooltipProvider } from '../components/overlays/tooltip/index.js';
import { OverlayStage, useOverlayStage } from './overlay-stage.js';

function Probe() {
  return <span data-testid="probe">{String(useOverlayStage())}</span>;
}

describe('OverlayStage', () => {
  it('tells components they are staged, and nothing else is', () => {
    render(
      <>
        <Probe />
        <OverlayStage>
          <Probe />
        </OverlayStage>
      </>,
    );
    expect(screen.getAllByTestId('probe').map((p) => p.textContent)).toEqual(['false', 'true']);
  });

  it('renders an open dialog in place, not modal and without taking focus', async () => {
    render(
      <>
        <button type="button">Elsewhere</button>
        <OverlayStage align="center">
          <Dialog defaultOpen>
            <DialogTrigger asChild>
              <Button>Delete project</Button>
            </DialogTrigger>
            <DialogContent size="sm">
              <DialogTitle>Delete project?</DialogTitle>
            </DialogContent>
          </Dialog>
        </OverlayStage>
      </>,
    );
    const stage = document.querySelector('[data-mn-overlay-stage]');
    const dialog = screen.getByRole('dialog', { name: 'Delete project?' });
    expect(stage?.contains(dialog)).toBe(true);
    expect(stage?.getAttribute('data-align')).toBe('center');
    expect(document.querySelector('.mn-overlay')).toBeNull();
    expect(dialog.contains(document.activeElement)).toBe(false);
    // The rest of the page is not hidden from assistive technology.
    expect(screen.getByRole('button', { name: 'Elsewhere' }).closest('[aria-hidden]')).toBeNull();
    // A click elsewhere does not close it.
    await userEvent.click(screen.getByRole('button', { name: 'Elsewhere' }));
    expect(screen.getByRole('dialog')).toBeTruthy();
  });

  it('closes a staged dialog with Escape only from inside the stage', async () => {
    render(
      <>
        <button type="button">Elsewhere</button>
        <OverlayStage>
          <Dialog defaultOpen>
            <DialogContent>
              <DialogTitle>Rename board</DialogTitle>
            </DialogContent>
          </Dialog>
        </OverlayStage>
      </>,
    );
    screen.getByRole('button', { name: 'Elsewhere' }).focus();
    await userEvent.keyboard('{Escape}');
    expect(screen.getByRole('dialog')).toBeTruthy();
    screen.getByRole('button', { name: 'Close' }).focus();
    await userEvent.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('keeps the normal modal dialog outside a stage', () => {
    render(
      <Dialog defaultOpen>
        <DialogContent>
          <DialogTitle>Modal</DialogTitle>
        </DialogContent>
      </Dialog>,
    );
    const dialog = screen.getByRole('dialog');
    expect(dialog.parentElement).toBe(document.body);
    expect(document.querySelector('.mn-overlay')).not.toBeNull();
  });

  it('renders a staged menu, popover, sheet and tooltip in place', () => {
    render(
      <TooltipProvider>
        <OverlayStage>
          <DropdownMenu defaultOpen>
            <DropdownMenuTrigger asChild>
              <Button>Actions</Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent>
              <DropdownMenuItem>Rename</DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
          <Popover defaultOpen>
            <PopoverTrigger asChild>
              <Button>Filters</Button>
            </PopoverTrigger>
            <PopoverContent aria-label="Filters">Only mine</PopoverContent>
          </Popover>
          <Sheet defaultOpen>
            <SheetContent side="bottom">
              <SheetTitle>Share link</SheetTitle>
            </SheetContent>
          </Sheet>
          <Tooltip content="Copy link" defaultOpen>
            <Button>Copy</Button>
          </Tooltip>
        </OverlayStage>
      </TooltipProvider>,
    );
    const stage = document.querySelector('[data-mn-overlay-stage]');
    expect(stage?.contains(screen.getByRole('menu'))).toBe(true);
    expect(stage?.contains(screen.getByText('Only mine'))).toBe(true);
    expect(stage?.contains(screen.getByRole('dialog', { name: 'Share link' }))).toBe(true);
    expect(stage?.querySelector('.mn-tooltip')?.textContent).toContain('Copy link');
    expect(stage?.contains(document.activeElement)).toBe(false);
  });
});
