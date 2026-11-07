import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DeviceFrame } from './index.js';

describe('DeviceFrame', () => {
  it('is one named image whose contents are inert', async () => {
    render(
      <DeviceFrame label="Preview of a link page" statusBarTime="9:41">
        <h2>Sample page</h2>
        <button type="button">Sample button</button>
      </DeviceFrame>,
    );
    const frame = screen.getByRole('img', { name: 'Preview of a link page' });
    expect(frame).toHaveClass('mn-device-frame');
    expect(frame).toHaveAttribute('data-device', 'phone');
    expect(frame.style.maxWidth).toBe('390px');
    expect(frame.querySelector('.mn-device-frame__content')).toHaveAttribute('inert');
    // jsdom does not implement inert; browsers keep the sample button out of the tab order.
    await expectNoA11yViolations();
  });

  it('interactive: a named group with focusable, scrollable contents', async () => {
    const user = userEvent.setup();
    render(
      <DeviceFrame label="Live preview" interactive>
        <a href="#one">First link</a>
      </DeviceFrame>,
    );
    expect(screen.getByRole('group', { name: 'Live preview' })).toHaveAttribute('data-interactive');
    await user.tab();
    expect(document.activeElement).toHaveClass('mn-device-frame__content');
    await user.tab();
    expect(screen.getByRole('link', { name: 'First link' })).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('takes a size and a tablet device, and can be flat', () => {
    render(
      <DeviceFrame
        label="Tablet preview"
        device="tablet"
        width={600}
        height={800}
        flat
        data-testid="f"
      />,
    );
    const frame = screen.getByTestId('f');
    expect(frame).toHaveAttribute('data-device', 'tablet');
    expect(frame).toHaveAttribute('data-flat');
    expect(frame.style.maxWidth).toBe('600px');
    expect(frame.querySelector('.mn-device-frame__island')).toBeNull();
    expect(frame.querySelector('.mn-device-frame__buttons')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<DeviceFrame label="Preview">Hello</DeviceFrame>);
    expect(html).toContain('mn-device-frame');
    expect(html).toContain('Hello');
  });
});
