import {
  Badge,
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  Container,
  Heading,
  Stack,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
  Text,
} from '@rookcom/monarch-react';
import { AssistantChat } from './AssistantChat';
import { Greeting } from './Greeting';

export function App() {
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

        <Tabs defaultValue="home">
          <TabsList aria-label="Views">
            <TabsTrigger value="home">Home</TabsTrigger>
            <TabsTrigger value="assistant">Assistant</TabsTrigger>
          </TabsList>
          <TabsContent value="home" className="pt-4">
            <Card>
              <CardHeader>
                <CardTitle>A form field</CardTitle>
                <CardDescription>Inputs sit in a Field with a label.</CardDescription>
              </CardHeader>
              <CardContent>
                <Greeting />
              </CardContent>
            </Card>
          </TabsContent>
          <TabsContent value="assistant" className="pt-4">
            <Stack gap="4">
              <Text tone="muted">
                Replies come from a mock transport in AssistantChat.tsx. No model is called.
              </Text>
              <AssistantChat />
            </Stack>
          </TabsContent>
        </Tabs>
      </Stack>
    </Container>
  );
}
