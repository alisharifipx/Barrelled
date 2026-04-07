# Barrelled

An IntelliJ IDEA plugin that generates and maintains TypeScript/JavaScript barrel (`index.ts` or `index.js`) files for 
your project, with a UI to select exactly which exports to include.

## What is a barrel file?

A barrel file re-exports everything from a folder through a single `index.ts`, giving you one stable entry point into a 
module.

The most noticeable benefit is shorter import paths — instead of:

```ts
import { Button } from './components/Button/Button';
import { Modal } from './components/Modal/Modal';
import { type ModalProps } from './components/Modal/ModalProps';
```

You write:

```ts
import { Button, Modal, type ModalProps } from './components';
```

But the more important benefit is **encapsulation**. The barrel becomes a table of contents that defines what the
folder exposes publicly. Anything not re-exported in `index.ts` is effectively private, you can rename files, split 
components, or reorganise internals freely, and as long as the barrel still exports the same names, nothing outside the
folder breaks.
