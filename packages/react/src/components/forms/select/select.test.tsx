import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { OverlayStage } from '../../../internal/overlay-stage.js';
import { Field, FieldLabel } from '../form-field/index.js';
import { Select, SelectContent, SelectItem, SelectTrigger } from './index.js';

describe('Select', () => {
  it('trigger inside an invalid required Field', () => {
    render(
      <Field invalid required>
        <FieldLabel>S</FieldLabel>
        <Select>
          <SelectTrigger placeholder="Pick" />
          <SelectContent>
            <SelectItem value="a">A</SelectItem>
          </SelectContent>
        </Select>
      </Field>,
    );
    const t = screen.getByRole('combobox', { name: /S/ });
    expect(t).toHaveAttribute('aria-invalid', 'true');
    expect(t).toHaveAttribute('aria-required', 'true');
    expect(t).not.toHaveAttribute('required');
  });

  it('renders a labelled trigger accessibly', async () => {
    render(
      <Select defaultValue="open">
        <SelectTrigger aria-label="Status" placeholder="Status" />
        <SelectContent>
          <SelectItem value="open">Open</SelectItem>
        </SelectContent>
      </Select>,
    );
    expect(screen.getByRole('combobox', { name: 'Status' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <Select>
        <SelectTrigger placeholder="Pick" aria-label="Pick" />
      </Select>,
    );
    expect(html).toContain('mn-select');
  });

  it('outside a stage, defaultOpen opens the real list and onOpenChange is called on close', async () => {
    const changes: boolean[] = [];
    render(
      <Select defaultValue="open" defaultOpen onOpenChange={(o) => changes.push(o)}>
        <SelectTrigger aria-label="Status" />
        <SelectContent>
          <SelectItem value="open">Open</SelectItem>
          <SelectItem value="done">Done</SelectItem>
        </SelectContent>
      </Select>,
    );
    expect(await screen.findByRole('listbox')).toBeInTheDocument();
    expect(document.querySelector('[data-mn-select-picture]')).toBeNull();
    await userEvent.keyboard('{Escape}');
    expect(screen.queryByRole('listbox')).toBeNull();
    expect(changes).toEqual([false]);
  });

  it('open in an overlay stage, shows the list without making the page modal', async () => {
    render(
      <>
        <button type="button">Elsewhere</button>
        <OverlayStage>
          <Select defaultValue="pending" defaultOpen>
            <SelectTrigger aria-label="Status" placeholder="Status" />
            <SelectContent>
              <SelectItem value="open">Open</SelectItem>
              <SelectItem value="pending">Pending</SelectItem>
            </SelectContent>
          </Select>
        </OverlayStage>
      </>,
    );
    const elsewhere = screen.getByRole('button', { name: 'Elsewhere' });
    expect(elsewhere.closest('[aria-hidden]')).toBeNull();
    expect(document.body.contains(document.activeElement)).toBe(true);
    expect(document.activeElement).toBe(document.body);
    const picture = document.querySelector('[data-mn-select-picture]');
    expect(picture?.closest('[data-mn-overlay-stage]')).not.toBeNull();
    expect(picture?.textContent).toBe('OpenPending');
    expect(picture?.querySelector('[data-state="checked"]')?.textContent).toBe('Pending');
    const trigger = screen.getByRole('combobox', { name: 'Status' });
    expect(trigger).toHaveTextContent('Pending');
    await expectNoA11yViolations(document.body);
  });
});
