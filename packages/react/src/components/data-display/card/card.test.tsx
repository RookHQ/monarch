import { createRef } from 'react';
import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from './index.js';

describe('Card', () => {
  it('forwards refs from every part', () => {
    const card = createRef<HTMLDivElement>();
    const header = createRef<HTMLDivElement>();
    const title = createRef<HTMLHeadingElement>();
    const description = createRef<HTMLParagraphElement>();
    const content = createRef<HTMLDivElement>();
    const footer = createRef<HTMLDivElement>();
    render(
      <Card ref={card}>
        <CardHeader ref={header}>
          <CardTitle ref={title}>T</CardTitle>
          <CardDescription ref={description}>D</CardDescription>
        </CardHeader>
        <CardContent ref={content}>C</CardContent>
        <CardFooter ref={footer}>F</CardFooter>
      </Card>,
    );
    expect(card.current).toHaveClass('mn-card');
    expect(header.current).toHaveClass('mn-card__header');
    expect(title.current?.tagName).toBe('H3');
    expect(description.current?.tagName).toBe('P');
    expect(content.current).toHaveClass('mn-card__content');
    expect(footer.current).toHaveClass('mn-card__footer');
  });

  it('renders accessibly with an h3 title by default', async () => {
    render(
      <main>
        <Card>
          <CardHeader>
            <CardTitle>Open tickets</CardTitle>
          </CardHeader>
          <CardContent>128</CardContent>
        </Card>
      </main>,
    );
    expect(screen.getByRole('heading', { name: 'Open tickets', level: 3 })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <Card>
        <CardContent>Body</CardContent>
      </Card>,
    );
    expect(html).toContain('mn-card');
  });
});
