# ADR 0013: AI components are a strategic family on a vendor-neutral model

Status: accepted (2026-11-07), wave 11 (AI foundations), under [ADR 0011](0011-strategic-scope.md)

## Context

Rook Intelligence (FastAPI, LangGraph) already streams agent replies over
server-sent events (`agent | chunk | tool | done | error`), with tools,
memory and named agent personas. The products that use it render replies as
plain text bubbles with a spinner: no Markdown, no stop button, no view of
the tool edits the assistant applies, no citations, no feedback. Other
products plan AI surfaces and would each hand-roll their own.

## Decision

1. **A new subpath, `@rookcom/monarch-react/ai`**, for every AI component, so
   products without AI never load it. Folders live under
   `components/intelligence/` (taxonomy category 17).
2. **A vendor-neutral message model** (`AiMessage` with ordered `AiPart`s:
   text, reasoning, tool-call, citation, source, file, artifact,
   edit-proposal, plan, ui, error) and **a transport interface**
   (`AiTransport.stream` → `AiStreamEvent`s). `useAiConversation` owns state:
   send, stop, regenerate and edit as branches, tool approval, feedback. It
   never imports a model vendor's SDK. Adapters ship for Rook Intelligence
   (`rookIntelligenceTransport`, byte-compatible with its SSE format) and for
   docs and tests (`mockTransport`, deterministic).
3. **An `ai` intent in the tokens**, derived like the status intents so it is
   contrast-guaranteed in all four themes: `--mn-color-ai-{solid,…,strong}`,
   plus `--mn-gradient-ai` (decorative; solid in high contrast), thinking,
   tool, citation, diff and confidence roles, and stream motion tokens.
4. **Streaming accessibility rules** ([guide](../guides/ai-accessibility.md)):
   announce at sentence boundaries through one throttled polite region, never
   per token; `aria-busy` while streaming; Stop by button and Escape; scroll
   pins only while the person is at the bottom; focus never moves.
5. **Model output is untrusted.** Markdown is parsed by Monarch into React
   elements (no HTML strings, no `dangerouslySetInnerHTML`); links pass the
   same URL check as the editor and get `rel="noopener noreferrer nofollow"`;
   images are opt-in; generative UI renders only allow-listed Monarch
   components.

### The intelligence hue

The directive asked for an AI colour at least ΔE 15 from every product
accent. A sweep of hue, saturation and lightness against all 17 product
accents and the four status colours, in all four themes, found the best
achievable minimum is about **10.6 (OKLab ×100)**, at hue 294°: the accents
already cover most of the colour wheel, and the high-contrast themes pull
every fill toward the same lightness. The token test therefore enforces 10.
AI content never relies on colour alone: it always carries a text label and
the Bot icon.

## Consequences

- Products adopt AI surfaces by choosing a transport and composing
  components; the Rook Intelligence adapter covers Dossier and Recruit today.
- A product that wants another backend writes a small adapter, not UI.
- Adding the `ai` intent adds ten variables per theme to the token output.
