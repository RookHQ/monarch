import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import { LogDrawer, httpStatusTone, type LogDrawerProps } from './index.js';

const NOW = new Date('2026-03-02T12:00:00Z');
const request = {
  method: 'post',
  path: '/v1/orders',
  status: 429,
  errorCode: 'rate_limited',
  id: 'req_7f3a2c',
  at: '2026-03-02T11:58:00Z',
  durationMs: 84,
};

function Demo(props: Partial<LogDrawerProps>) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button onClick={() => setOpen(true)}>Open request</Button>
      <LogDrawer
        open={open}
        onOpenChange={setOpen}
        request={request}
        now={NOW}
        fields={[
          { label: 'Credential', value: 'Staging key' },
          {
            label: 'Origin',
            value: 'https://shop.example.com',
            mono: true,
            copyValue: 'https://shop.example.com',
          },
          { label: 'Region', value: null },
        ]}
        {...props}
      />
    </>
  );
}

function mockClipboard() {
  const spy = vi.fn(() => Promise.resolve());
  Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText: spy } });
  Object.defineProperty(document, 'execCommand', { configurable: true, value: () => false });
  return spy;
}

describe('LogDrawer', () => {
  it('opens as a named dialog with the request summary and fields', async () => {
    const user = userEvent.setup();
    render(<Demo description="Request bodies are not stored." />);
    await user.click(screen.getByRole('button', { name: 'Open request' }));
    const drawer = screen.getByRole('dialog', { name: 'Request details' });
    expect(drawer).toHaveClass('mn-log-drawer');
    expect(drawer).toHaveAccessibleDescription('Request bodies are not stored.');
    expect(drawer).toHaveAttribute('data-status-tone', 'warning');
    expect(within(drawer).getByText('POST')).toBeInTheDocument();
    expect(within(drawer).getByText('/v1/orders')).toBeInTheDocument();
    expect(within(drawer).getByText('429')).toBeInTheDocument();
    expect(within(drawer).getByText('rate_limited')).toBeInTheDocument();
    expect(within(drawer).getByText('84 ms')).toBeInTheDocument();
    expect(within(drawer).getByText('Staging key')).toBeInTheDocument();
    expect(within(drawer).getByText('Not set')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('copies the request ID and other copyable fields', async () => {
    const user = userEvent.setup();
    const writeText = mockClipboard();
    render(<Demo />);
    await user.click(screen.getByRole('button', { name: 'Open request' }));
    await user.click(screen.getByRole('button', { name: 'Copy request ID' }));
    expect(writeText).toHaveBeenCalledWith('req_7f3a2c');
    await user.click(screen.getByRole('button', { name: 'Copy Origin' }));
    expect(writeText).toHaveBeenLastCalledWith('https://shop.example.com');
  });

  it('closes with Escape and the close button and returns focus', async () => {
    const user = userEvent.setup();
    render(<Demo />);
    const trigger = screen.getByRole('button', { name: 'Open request' });
    await user.click(trigger);
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(trigger).toHaveFocus();
    await user.click(trigger);
    await user.click(screen.getByRole('button', { name: 'Close' }));
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('shows loading and error states', async () => {
    const user = userEvent.setup();
    const onRetry = vi.fn();
    const { rerender } = render(<LogDrawer open loading />);
    expect(screen.getByText('Loading details')).toBeInTheDocument();
    expect(screen.queryByText('Request ID')).toBeNull();
    rerender(<LogDrawer open error onRetry={onRetry} />);
    expect(screen.getByText('The details did not load.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalled();
  });

  it('takes a title, extra content and messages', async () => {
    render(
      <LogDrawer
        open
        title="Delivery details"
        request={{ status: 200 }}
        messages={{ status: 'Response' }}
      >
        <pre>{'{ "ok": true }'}</pre>
      </LogDrawer>,
    );
    expect(screen.getByRole('dialog', { name: 'Delivery details' })).toBeInTheDocument();
    expect(screen.getByText('Response')).toBeInTheDocument();
    expect(screen.getByText('{ "ok": true }')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('maps HTTP status classes to tones', () => {
    expect(httpStatusTone(204)).toBe('success');
    expect(httpStatusTone(301)).toBe('info');
    expect(httpStatusTone(404)).toBe('warning');
    expect(httpStatusTone(503)).toBe('danger');
    expect(httpStatusTone(101)).toBe('neutral');
  });

  it('renders on the server', () => {
    expect(() => renderToString(<LogDrawer open request={request} now={NOW} />)).not.toThrow();
    expect(renderToString(<Demo />)).toContain('Open request');
  });
});
