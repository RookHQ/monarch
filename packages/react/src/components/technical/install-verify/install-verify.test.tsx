import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { InstallVerify, type InstallSnippet } from './index.js';

const NOW = '2026-01-10T12:00:00Z';
const snippets: InstallSnippet[] = [
  {
    id: 'html',
    label: 'HTML',
    code: '<script src="https://cdn.example.com/widget.js" async></script>',
    description: 'Paste before the closing body tag.',
  },
  { id: 'react', label: 'React', code: "import { Widget } from 'example-widget';" },
];

function mockClipboard() {
  const spy = vi.fn(() => Promise.resolve());
  Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText: spy } });
  Object.defineProperty(document, 'execCommand', { configurable: true, value: () => false });
  return spy;
}

describe('InstallVerify', () => {
  it('shows install and verify halves with the first snippet selected', async () => {
    render(
      <InstallVerify
        snippets={snippets}
        status="waiting"
        targets={[{ label: 'www.example.com' }]}
        data-testid="root"
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-install-verify');
    expect(root).toHaveAttribute('data-status', 'waiting');
    expect(screen.getByRole('heading', { name: 'Install' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Verify' })).toBeInTheDocument();
    expect(screen.getByRole('tablist', { name: 'Platform' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'HTML' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('textbox', { name: 'HTML snippet' })).toHaveValue(snippets[0]!.code);
    expect(screen.getByText('Waiting for your site…').closest('[role="status"]')).not.toBeNull();
    expect(screen.getByText(/open any page of www.example.com/)).toBeInTheDocument();
    expect(screen.getByRole('list', { name: 'Websites' })).toHaveTextContent('not seen yet');
    await expectNoA11yViolations();
  });

  it('switches snippets with the arrow keys', async () => {
    const user = userEvent.setup();
    const onSnippetChange = vi.fn();
    render(
      <InstallVerify
        snippets={snippets}
        status="waiting"
        part="install"
        onSnippetChange={onSnippetChange}
      />,
    );
    await user.tab();
    expect(screen.getByRole('tab', { name: 'HTML' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('tab', { name: 'React' })).toHaveFocus();
    expect(onSnippetChange).toHaveBeenCalledWith('react');
    expect(screen.getByRole('textbox', { name: 'React snippet' })).toBeInTheDocument();
    expect(screen.queryByRole('heading')).toBeNull();
  });

  it('copies the snippet and the developer instructions', async () => {
    const user = userEvent.setup();
    const writeText = mockClipboard();
    render(
      <InstallVerify
        snippets={snippets.slice(0, 1)}
        status="waiting"
        part="install"
        developerInstructions="Add the snippet to every page."
        developerEmailHref="mailto:?subject=Install"
      />,
    );
    expect(screen.queryByRole('tablist')).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Copy HTML snippet' }));
    expect(writeText).toHaveBeenLastCalledWith(snippets[0]!.code);
    await user.click(screen.getByRole('button', { name: 'Copy instructions for your developer' }));
    expect(writeText).toHaveBeenLastCalledWith('Add the snippet to every page.');
    expect(screen.getByRole('link', { name: 'Email them' })).toHaveAttribute(
      'href',
      'mailto:?subject=Install',
    );
  });

  it('calls onConnected once and names the hosts it was seen on', async () => {
    const onConnected = vi.fn();
    const targets = [
      { label: 'www.example.com', lastSeenAt: '2026-01-10T11:55:00Z' },
      { label: 'shop.example.com' },
    ];
    const { rerender } = render(
      <InstallVerify
        snippets={snippets}
        status="waiting"
        part="verify"
        targets={targets}
        now={NOW}
        onConnected={onConnected}
      />,
    );
    expect(onConnected).not.toHaveBeenCalled();
    rerender(
      <InstallVerify
        snippets={snippets}
        status="connected"
        part="verify"
        targets={targets}
        now={NOW}
        onConnected={onConnected}
      />,
    );
    rerender(
      <InstallVerify
        snippets={snippets}
        status="connected"
        part="verify"
        targets={targets}
        now={NOW}
        onConnected={() => onConnected()}
      />,
    );
    expect(onConnected).toHaveBeenCalledTimes(1);
    expect(screen.getByText('Connected on www.example.com')).toBeInTheDocument();
    expect(screen.getByRole('list', { name: 'Websites' })).toHaveTextContent('seen 5 minutes ago');
    await expectNoA11yViolations();
  });

  it('shows the unavailable state and a test link', async () => {
    render(
      <InstallVerify
        snippets={snippets}
        status="unavailable"
        part="verify"
        testHref="https://example.com/try"
        testHint="Runs the real widget."
      />,
    );
    expect(screen.getByText('Live verification isn’t available')).toBeInTheDocument();
    const link = screen.getByRole('link', { name: 'Open a test page (opens in a new tab)' });
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    await expectNoA11yViolations();
  });

  it('disables the test action', () => {
    render(
      <InstallVerify
        snippets={snippets}
        status="waiting"
        part="verify"
        testHref="https://example.com/try"
        testDisabled
      />,
    );
    expect(screen.getByRole('button', { name: 'Open a test page' })).toBeDisabled();
    expect(screen.queryByRole('link')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <InstallVerify snippets={snippets} status="connected" connectedOn={['www.example.com']} />,
    );
    expect(html).toContain('mn-install-verify');
    expect(html).toContain('Connected on www.example.com');
  });
});
