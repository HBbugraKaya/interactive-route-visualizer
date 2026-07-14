import type { Annotation } from '../types';

interface Props {
  annotations: Annotation[];
  selectedId: string | null;
  onSelect: (id: string) => void;
  draftPen?: [number, number][];
  draftArrow?: { x: number; y: number; x2: number; y2: number } | null;
}

export function AnnotationLayer({
  annotations,
  selectedId,
  onSelect,
  draftPen,
  draftArrow,
}: Props) {
  return (
    <g className="annotation-layer">
      {annotations.map((a) => {
        const selected = a.id === selectedId;
        if (a.type === 'pen' && a.points && a.points.length > 1) {
          const d = a.points.map((p, i) => `${i === 0 ? 'M' : 'L'}${p[0]},${p[1]}`).join(' ');
          return (
            <path
              key={a.id}
              d={d}
              className={`anno-pen ${selected ? 'selected' : ''}`}
              stroke={a.color}
              fill="none"
              onPointerDown={(e) => {
                e.stopPropagation();
                onSelect(a.id);
              }}
            />
          );
        }
        if (a.type === 'arrow' && a.x != null && a.y != null && a.x2 != null && a.y2 != null) {
          return (
            <line
              key={a.id}
              x1={a.x}
              y1={a.y}
              x2={a.x2}
              y2={a.y2}
              className={`anno-arrow ${selected ? 'selected' : ''}`}
              stroke={a.color}
              markerEnd="url(#annoArrow)"
              onPointerDown={(e) => {
                e.stopPropagation();
                onSelect(a.id);
              }}
            />
          );
        }
        if ((a.type === 'text' || a.type === 'sticky') && a.x != null && a.y != null) {
          const isSticky = a.type === 'sticky';
          return (
            <g
              key={a.id}
              transform={`translate(${a.x}, ${a.y})`}
              className={`anno-box ${isSticky ? 'sticky' : 'text'} ${selected ? 'selected' : ''}`}
              onPointerDown={(e) => {
                e.stopPropagation();
                onSelect(a.id);
              }}
            >
              <rect
                width={isSticky ? 140 : Math.max(80, (a.text?.length ?? 4) * 8)}
                height={isSticky ? 70 : 28}
                rx={6}
                fill={isSticky ? a.color : 'rgba(30,30,30,0.85)'}
                stroke={selected ? '#60a5fa' : 'transparent'}
              />
              <text x={10} y={isSticky ? 24 : 18} className="anno-text">
                {a.text || (isSticky ? 'Note' : 'Text')}
              </text>
            </g>
          );
        }
        return null;
      })}

      {draftPen && draftPen.length > 1 && (
        <path
          d={draftPen.map((p, i) => `${i === 0 ? 'M' : 'L'}${p[0]},${p[1]}`).join(' ')}
          className="anno-pen draft"
          stroke="#f59e0b"
          fill="none"
        />
      )}
      {draftArrow && (
        <line
          x1={draftArrow.x}
          y1={draftArrow.y}
          x2={draftArrow.x2}
          y2={draftArrow.y2}
          className="anno-arrow draft"
          stroke="#f59e0b"
          markerEnd="url(#annoArrow)"
        />
      )}
    </g>
  );
}
