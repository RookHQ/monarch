import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { DirectionProvider } from '@rookcom/monarch-react';
import './index.css'; // Tailwind, Monarch tokens (all themes) and the Tailwind theme
import '@rookcom/monarch-tokens/css/base';
import '@rookcom/monarch-tokens/css/products/neutral';
import '@rookcom/monarch-react/styles/core.css';
import '@rookcom/monarch-react/styles/ai.css'; // for components from @rookcom/monarch-react/ai
import './brand.generated.css'; // written by scripts/brand.mjs; must come after the tokens
import { App } from './App';

const root = document.getElementById('root');
if (!root) throw new Error('index.html has no #root element');

createRoot(root).render(
  <StrictMode>
    <DirectionProvider dir="ltr">
      <App />
    </DirectionProvider>
  </StrictMode>,
);
