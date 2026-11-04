import {
  Badge,
  Button,
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@rookcom/monarch-react';
import { href } from '../lib/router.js';
import { version } from '../lib/manifest.js';
import { products } from '../lib/registry.js';

export function HomePage() {
  return (
    <>
      <h1>One design authority. Every Rook experience.</h1>
      <p className="docs-lead">
        Monarch is the design system behind {products.length} Rook applications: tokens, four
        themes, accessible components and the governance that keeps them consistent. Version{' '}
        {version}.
      </p>
      <p>
        <Button asChild variant="primary">
          <a href={href('/getting-started')}>Get started</a>
        </Button>{' '}
        <Button asChild>
          <a href={href('/components')}>Browse components</a>
        </Button>
      </p>
      <h2 className="docs-h2">Design philosophy</h2>
      <div className="docs-cards">
        {[
          [
            'Effortless',
            'Clean, white and soft-neutral foundations. Generous whitespace. One accent per view.',
          ],
          [
            'Accessible by construction',
            'Every product accent is adjusted at build time until text, rings and states meet WCAG 2.2 AA; the adjustment is written down.',
          ],
          [
            'Recognisably Rook',
            'Rook Red, Blue, Green and Yellow carry meaning; each product keeps its own accent.',
          ],
          [
            'Independent products',
            'Apps ship Monarch inside their bundle. Nothing renders through a Monarch server.',
          ],
          [
            'Measured, not claimed',
            'Adoption and conformance come from scans of real checkouts, never from a hand-edited field.',
          ],
          ['Quiet motion', 'Short, standard easing; everything collapses under reduced motion.'],
        ].map(([t, d]) => (
          <Card key={t}>
            <CardHeader>
              <CardTitle as="h3">{t}</CardTitle>
              <CardDescription>{d}</CardDescription>
            </CardHeader>
            <CardContent>
              <Badge tone="accent" variant="subtle">
                Principle
              </Badge>
            </CardContent>
          </Card>
        ))}
      </div>
    </>
  );
}
