import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import '@rookcom/monarch-tokens/css';
import '@rookcom/monarch-tokens/css/base';
import '@rookcom/monarch-react/styles.css';
import './styles/base.css';
import './styles/gallery.css';
import './styles/shell.css';
import './styles/content.css';
import './styles/examples.css';
// Every product accent, so the playground can preview any Rook product.
import.meta.glob('../../../node_modules/@rookcom/monarch-tokens/dist/css/products/*.css', {
  eager: true,
});
import { App } from './app/app.js';

const root = document.getElementById('root');
if (!root) throw new Error('#root missing');
createRoot(root).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
