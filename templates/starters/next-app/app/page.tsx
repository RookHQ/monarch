import NextLink from 'next/link';
import {
  Badge,
  Button,
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
  Container,
  Heading,
  Stack,
  Text,
} from '@rookcom/monarch-react';
import { Greeting } from './greeting';

// A server component. Monarch components render here; event handlers live in
// client files such as greeting.tsx.
export default function HomePage() {
  return (
    <Container as="main" size="narrow" className="py-10">
      <Stack gap="6">
        <Stack gap="2">
          <Badge tone="accent">Monarch</Badge>
          <Heading level={1}>__PROJECT_NAME__</Heading>
          <Text tone="muted">
            Light, dark and high-contrast themes, accessible components and Tailwind classes that
            read the same tokens.
          </Text>
        </Stack>

        <Card>
          <CardHeader>
            <CardTitle>A form field</CardTitle>
            <CardDescription>Inputs sit in a Field with a label.</CardDescription>
          </CardHeader>
          <CardContent>
            <Greeting />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>An assistant</CardTitle>
            <CardDescription>
              A chat on a mock transport. It calls no model until you connect your own service.
            </CardDescription>
          </CardHeader>
          <CardFooter>
            <Button asChild variant="primary">
              <NextLink href="/assistant">Open the assistant</NextLink>
            </Button>
          </CardFooter>
        </Card>
      </Stack>
    </Container>
  );
}
