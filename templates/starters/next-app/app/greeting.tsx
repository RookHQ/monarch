'use client';

import { useState } from 'react';
import { Field, FieldDescription, FieldLabel, Input, Text } from '@rookcom/monarch-react';

export function Greeting() {
  const [name, setName] = useState('');
  return (
    <div className="grid gap-4">
      <Field>
        <FieldLabel>Your name</FieldLabel>
        <Input value={name} onChange={(event) => setName(event.target.value)} autoComplete="name" />
        <FieldDescription>Stays in this browser tab.</FieldDescription>
      </Field>
      <Text aria-live="polite">
        {name.trim() ? `Hello, ${name.trim()}.` : 'Type a name above.'}
      </Text>
    </div>
  );
}
