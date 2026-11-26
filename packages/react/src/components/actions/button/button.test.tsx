import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { contrast, type Hsl } from '@rookcom/monarch-tokens';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button, IconButton } from './index.js';

describe('Button', () => {
  it('defaults to type=button so it never submits a form by accident', () => {
    render(<Button>Save</Button>);
    expect(screen.getByRole('button', { name: 'Save' })).toHaveAttribute('type', 'button');
  });

  it('activates with click, Enter and Space', async () => {
    const onClick = vi.fn();
    render(<Button onClick={onClick}>Save</Button>);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button'));
    await user.keyboard('{Enter}');
    await user.keyboard(' ');
    expect(onClick).toHaveBeenCalledTimes(3);
  });

  it('stays focusable but inert while loading', async () => {
    const onClick = vi.fn();
    render(
      <Button loading onClick={onClick}>
        Save
      </Button>,
    );
    const button = screen.getByRole('button', { name: 'Save' });
    expect(button).not.toBeDisabled();
    expect(button).toHaveAttribute('aria-busy', 'true');
    expect(button).toHaveAttribute('aria-disabled', 'true');
    const user = userEvent.setup();
    await user.tab();
    expect(button).toHaveFocus();
    await user.click(button);
    expect(onClick).not.toHaveBeenCalled();
  });

  it('is disabled natively when disabled', () => {
    render(<Button disabled>Save</Button>);
    expect(screen.getByRole('button')).toBeDisabled();
  });

  it('renders the child element with asChild', () => {
    render(
      <Button asChild variant="primary">
        <a href="/new">New ticket</a>
      </Button>,
    );
    const link = screen.getByRole('link', { name: 'New ticket' });
    expect(link).toHaveClass('mn-button');
    expect(link).toHaveAttribute('data-variant', 'primary');
    expect(link).not.toHaveAttribute('type');
  });

  it('asChild + loading keeps the child element', () => {
    render(
      <Button asChild loading>
        <a href="/x">Go</a>
      </Button>,
    );
    const link = screen.getByRole('link', { name: 'Go' });
    expect(link).toHaveAttribute('aria-busy', 'true');
    expect(link.querySelector('.mn-spinner')).not.toBeNull();
  });

  it('asChild + leadingIcon + trailingIcon', () => {
    render(
      <Button asChild leadingIcon={<svg data-t="l" />} trailingIcon={<svg data-t="t" />}>
        <a href="/x">Go</a>
      </Button>,
    );
    const link = screen.getByRole('link', { name: 'Go' });
    expect(link.querySelectorAll('.mn-button__icon').length).toBe(2);
  });

  it('hides decorative icons from assistive technology', async () => {
    render(<Button leadingIcon={<svg />}>Export</Button>);
    expect(
      screen.getByRole('button', { name: 'Export' }).querySelector('[aria-hidden]'),
    ).not.toBeNull();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<Button variant="primary">Save</Button>);
    expect(html).toContain('mn-button');
  });

  it('exposes success, warning and xs as data attributes', () => {
    render(
      <>
        <Button variant="success">Approve</Button>
        <Button variant="warning" size="xs">
          Flag
        </Button>
      </>,
    );
    expect(screen.getByRole('button', { name: 'Approve' })).toHaveAttribute(
      'data-variant',
      'success',
    );
    const flag = screen.getByRole('button', { name: 'Flag' });
    expect(flag).toHaveAttribute('data-variant', 'warning');
    expect(flag).toHaveAttribute('data-size', 'xs');
  });
});

/** Reads `hsl(h s% l%)` back into an Hsl for contrast checks. */
function parseCss(value: string): Hsl {
  const m = /hsl\(([\d.]+) ([\d.]+)% ([\d.]+)%\)/.exec(value);
  if (!m) throw new Error(`not an hsl() value: ${value}`);
  return { h: Number(m[1]), s: Number(m[2]), l: Number(m[3]) };
}

describe('Button variant="brand"', () => {
  const themes = [
    ['l', 4.5],
    ['d', 4.5],
    ['hl', 7],
    ['hd', 7],
  ] as const;

  it.each(['#facc15', '#0f766e', '#1e3a8a', '#ff5a5f', '#ffffff', '#000000'])(
    'derives readable text for %s in every theme',
    (color) => {
      render(
        <Button variant="brand" brandColor={color}>
          Book
        </Button>,
      );
      const button = screen.getByRole('button', { name: 'Book' });
      expect(button).toHaveAttribute('data-brand');
      for (const [k, target] of themes) {
        const fg = parseCss(button.style.getPropertyValue(`--_brand-${k}-fg`));
        for (const state of ['bg', 'bg-hover', 'bg-active']) {
          const bg = parseCss(button.style.getPropertyValue(`--_brand-${k}-${state}`));
          expect(contrast(fg, bg)).toBeGreaterThanOrEqual(target);
        }
      }
    },
  );

  it('accepts #rgb and keeps caller styles', () => {
    render(
      <Button variant="brand" brandColor="#f80" style={{ minWidth: 120 }}>
        Go
      </Button>,
    );
    const button = screen.getByRole('button', { name: 'Go' });
    expect(button.style.getPropertyValue('--_brand-l-bg')).toMatch(/^hsl\(/);
    expect(button.style.minWidth).toBe('120px');
  });

  it('falls back to the accent for an invalid colour, and ignores brandColor on other variants', () => {
    render(
      <>
        <Button variant="brand" brandColor="teal">
          Fallback
        </Button>
        <Button variant="primary" brandColor="#0f766e">
          Primary
        </Button>
      </>,
    );
    for (const name of ['Fallback', 'Primary']) {
      const button = screen.getByRole('button', { name });
      expect(button).not.toHaveAttribute('data-brand');
      expect(button.getAttribute('style')).toBeNull();
    }
  });

  it('renders the derived colours on the server', () => {
    const html = renderToString(
      <Button variant="brand" brandColor="#0f766e">
        Book
      </Button>,
    );
    expect(html).toContain('--_brand-d-fg');
  });
});

describe('IconButton', () => {
  it('uses the required label as its accessible name', async () => {
    render(<IconButton label="Close panel" icon={<svg />} />);
    expect(screen.getByRole('button', { name: 'Close panel' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('loading shows the spinner only', () => {
    render(<IconButton label="Save" icon={<svg data-t="i" />} loading />);
    const b = screen.getByRole('button', { name: 'Save' });
    expect(b.querySelector('.mn-spinner')).not.toBeNull();
    expect(b.querySelector('[data-t="i"]')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<IconButton label="More" icon={<svg />} />);
    expect(html).toContain('mn-icon-button');
  });
});
