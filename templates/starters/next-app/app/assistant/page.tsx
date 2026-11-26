import NextLink from 'next/link';
import { Container, Heading, Link, Stack, Text } from '@rookcom/monarch-react';
import { AssistantChat } from './assistant-chat';

export default function AssistantPage() {
  return (
    <Container as="main" size="narrow" className="py-10">
      <Stack gap="6">
        <Stack gap="2">
          <Link asChild variant="subtle">
            <NextLink href="/">Back to home</NextLink>
          </Link>
          <Heading level={1}>Assistant</Heading>
          <Text tone="muted">
            Replies come from a mock transport in assistant-chat.tsx. No model is called.
          </Text>
        </Stack>
        <AssistantChat />
      </Stack>
    </Container>
  );
}
