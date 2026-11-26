import { describe, expect, it } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Favicon, faviconHostname, faviconLetter, isSafeFaviconSrc } from './index.js';

describe('Favicon', () => {
  it('is decorative by default and forwards props', async () => {
    const { container } = render(<Favicon data-testid="root" domain="example.com" />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-favicon');
    expect(root).toHaveAttribute('aria-hidden', 'true');
    expect(root).toHaveAttribute('data-state', 'fallback');
    expect(root).toHaveTextContent('E');
    expect(container.querySelector('img')).toBeNull();
    await expectNoA11yViolations();
  });

  it('has an accessible name when labelled', async () => {
    render(
      <Favicon
        domain="https://www.example.org/a"
        src="https://example.org/icon.png"
        label="Example"
      />,
    );
    expect(screen.getByRole('img', { name: 'Example' })).toHaveAttribute('data-state', 'image');
    await expectNoA11yViolations();
  });

  it('builds the source with resolveSrc only when asked', () => {
    const { container } = render(
      <Favicon
        domain="www.Example.com/page"
        size={20}
        resolveSrc={(h, px) => `/icons/${h}?s=${px}`}
      />,
    );
    const img = container.querySelector('img');
    expect(img).toHaveAttribute('src', '/icons/example.com?s=40');
    expect(img).toHaveAttribute('width', '20');
    expect(img).toHaveAttribute('alt', '');
  });

  it('falls back to the letter when the image fails', () => {
    const { container } = render(<Favicon domain="acme.test" src="https://acme.test/x.ico" />);
    fireEvent.error(container.querySelector('img')!);
    expect(container.querySelector('img')).toBeNull();
    expect(container.firstChild).toHaveTextContent('A');
  });

  it('never loads unsafe sources', () => {
    for (const src of [
      'javascript:alert(1)',
      ' java\nscript:alert(1)',
      'data:image/svg+xml;base64,PHN2Zz4=',
      'data:text/html;base64,PGI+',
      'blob:https://x/1',
      'ftp://example.com/a.png',
    ]) {
      const { container, unmount } = render(<Favicon domain="example.com" src={src} />);
      expect(container.querySelector('img')).toBeNull();
      unmount();
    }
  });

  it('uses name for the letter', () => {
    render(<Favicon data-testid="f" name="zeta docs" />);
    expect(screen.getByTestId('f')).toHaveTextContent('Z');
  });

  it('exposes its helpers', () => {
    expect(isSafeFaviconSrc('https://a.test/i.png')).toBe(true);
    expect(isSafeFaviconSrc('/favicon.ico')).toBe(true);
    expect(isSafeFaviconSrc('data:image/png;base64,iVBORw0KGgo=')).toBe(true);
    expect(isSafeFaviconSrc('')).toBe(false);
    expect(faviconHostname('HTTP://WWW.Example.COM:8080/x')).toBe('example.com');
    expect(faviconHostname('not a url')).toBe('');
    expect(faviconLetter('—9lives')).toBe('9');
    expect(faviconLetter('')).toBe('?');
  });

  it('renders on the server', () => {
    const html = renderToString(<Favicon domain="example.com" src="https://example.com/i.png" />);
    expect(html).toContain('mn-favicon');
    expect(html).toContain('src="https://example.com/i.png"');
  });
});
