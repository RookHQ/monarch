---
'@rookcom/monarch-react': minor
'@rookcom/monarch-tokens': minor
---

Wave 11: AI foundations (ADR 0013), in the new `@rookcom/monarch-react/ai` entry.

- Vendor-neutral message model (`AiMessage`, `AiPart`) and `useAiConversation` with `rookIntelligenceTransport` and `mockTransport`.
- Components: `AiThread`, `AiMessageView`, `AiMessageActions`, `AiPromptInput`, `AiStreamingText`, `AiMarkdown`, `AiCodeBlock`, `AiThinkingIndicator`, `AiTypingDots`, `AiStopButton`, `AiErrorMessage`, `useAiScrollAnchor`.
- Tokens: an `ai` intent (contrast-guaranteed in all four themes), `--mn-gradient-ai`, thinking, tool, citation, diff and confidence roles, and stream motion tokens.
