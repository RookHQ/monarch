import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AvailabilityToggle } from './index.js';

function Controlled({ onChange }: { onChange?: (v: boolean) => void }) {
  const [available, setAvailable] = useState(true);
  return (
    <AvailabilityToggle
      available={available}
      showHint
      onAvailableChange={(v) => {
        setAvailable(v);
        onChange?.(v);
      }}
    />
  );
}

describe('AvailabilityToggle', () => {
  it('is a named switch with the status and hint', async () => {
    render(<Controlled />);
    const sw = screen.getByRole('switch', { name: 'Available for new work' });
    expect(sw).toBeChecked();
    expect(sw).toHaveAccessibleDescription('Automatic assignment can give you new work.');
    expect(screen.getByText('Available')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('toggles with the keyboard and reports the new value', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<Controlled onChange={onChange} />);
    await user.tab();
    const sw = screen.getByRole('switch');
    expect(sw).toHaveFocus();
    await user.keyboard(' ');
    expect(onChange).toHaveBeenCalledWith(false);
    expect(sw).not.toBeChecked();
    expect(screen.getByText('Away')).toBeInTheDocument();
    expect(sw).toHaveAccessibleDescription(
      'Automatic assignment skips you. You can still work on what you have.',
    );
  });

  it('is disabled and busy while pending', () => {
    render(<AvailabilityToggle available={false} pending />);
    const sw = screen.getByRole('switch');
    expect(sw).toBeDisabled();
    expect(sw).toHaveAttribute('aria-busy', 'true');
  });

  it('renders nothing until availability is known', () => {
    const { container } = render(<AvailabilityToggle available={null} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('stops key events from reaching a surrounding menu', async () => {
    const user = userEvent.setup();
    const outer = vi.fn();
    render(
      // eslint-disable-next-line jsx-a11y/no-static-element-interactions -- stands in for a menu
      <div onKeyDown={outer}>
        <AvailabilityToggle available inMenu onAvailableChange={() => {}} />
      </div>,
    );
    await user.tab();
    await user.keyboard(' ');
    expect(outer).not.toHaveBeenCalled();
  });

  it('translates', () => {
    render(
      <AvailabilityToggle available messages={{ label: 'Disponible', available: 'Présent' }} />,
    );
    expect(screen.getByRole('switch', { name: 'Disponible' })).toBeInTheDocument();
    expect(screen.getByText('Présent')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<AvailabilityToggle available showHint />);
    expect(html).toContain('mn-availability-toggle');
    expect(html).toContain('Available');
  });
});
