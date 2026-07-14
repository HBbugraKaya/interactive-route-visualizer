import type { Tool } from '../types';

const TOOLS: { id: Tool; label: string }[] = [
  { id: 'select', label: 'Select' },
  { id: 'connect', label: 'Connect' },
  { id: 'pen', label: 'Pen' },
  { id: 'text', label: 'Text' },
  { id: 'arrow', label: 'Arrow' },
  { id: 'sticky', label: 'Sticky' },
];

interface Props {
  tool: Tool;
  onChange: (tool: Tool) => void;
  onUndo: () => void;
  onClearAnnotations: () => void;
}

export function ToolPalette({ tool, onChange, onUndo, onClearAnnotations }: Props) {
  return (
    <div className="tool-palette">
      {TOOLS.map((t) => (
        <button
          key={t.id}
          type="button"
          className={tool === t.id ? 'active' : ''}
          onClick={() => onChange(t.id)}
        >
          {t.label}
        </button>
      ))}
      <span className="palette-sep" />
      <button type="button" onClick={onUndo}>
        Undo
      </button>
      <button type="button" onClick={onClearAnnotations}>
        Clear notes
      </button>
    </div>
  );
}
