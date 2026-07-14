import { useCallback, useEffect, useRef, useState } from 'react';
import type { GraphStore } from '../store/graphStore';
import type { RouteNode, WeatherDay } from '../types';
import { uid } from '../types';
import { NodeView } from './NodeView';
import { DraftEdge, EdgeView } from './EdgeView';
import { AnnotationLayer } from './AnnotationLayer';

interface Props {
  store: GraphStore;
  weatherByNode: Record<string, WeatherDay>;
}

type DragMode =
  | null
  | { kind: 'pan'; ox: number; oy: number; vx: number; vy: number }
  | { kind: 'node'; id: string; ox: number; oy: number }
  | { kind: 'connect'; fromId: string }
  | { kind: 'pen'; points: [number, number][] }
  | { kind: 'arrow'; x: number; y: number; x2: number; y2: number }
  | { kind: 'anno'; id: string; ox: number; oy: number; ax: number; ay: number };

export function GraphCanvas({ store, weatherByNode }: Props) {
  const svgRef = useRef<SVGSVGElement>(null);
  const [drag, setDrag] = useState<DragMode>(null);
  const [cursor, setCursor] = useState<{ x: number; y: number } | null>(null);

  const toWorld = useCallback(
    (clientX: number, clientY: number) => {
      const svg = svgRef.current;
      if (!svg) return { x: 0, y: 0 };
      const rect = svg.getBoundingClientRect();
      const x = (clientX - rect.left - store.viewport.x) / store.viewport.scale;
      const y = (clientY - rect.top - store.viewport.y) / store.viewport.scale;
      return { x, y };
    },
    [store.viewport],
  );

  const onWheel = (e: React.WheelEvent) => {
    e.preventDefault();
    const factor = e.deltaY < 0 ? 1.08 : 0.92;
    const { x, y } = toWorld(e.clientX, e.clientY);
    store.setViewport((vp) => {
      const scale = Math.min(2.5, Math.max(0.35, vp.scale * factor));
      const rect = svgRef.current!.getBoundingClientRect();
      const sx = e.clientX - rect.left;
      const sy = e.clientY - rect.top;
      return {
        scale,
        x: sx - x * scale,
        y: sy - y * scale,
      };
    });
  };

  const onNodePointerDown = (e: React.PointerEvent, node: RouteNode) => {
    e.stopPropagation();
    (e.target as Element).setPointerCapture?.(e.pointerId);

    if (store.tool === 'connect') {
      setDrag({ kind: 'connect', fromId: node.id });
      setCursor(toWorld(e.clientX, e.clientY));
      return;
    }

    if (store.tool === 'select') {
      store.selectNode(node.id);
      store.pushHistoryBeforeDrag();
      const w = toWorld(e.clientX, e.clientY);
      setDrag({ kind: 'node', id: node.id, ox: w.x - node.x, oy: w.y - node.y });
    }
  };

  const onCanvasPointerDown = (e: React.PointerEvent) => {
    if (e.button !== 0) return;
    const w = toWorld(e.clientX, e.clientY);

    if (store.tool === 'select') {
      store.selectNode(null);
      store.selectEdge(null);
      store.selectAnnotation(null);
      setDrag({
        kind: 'pan',
        ox: e.clientX,
        oy: e.clientY,
        vx: store.viewport.x,
        vy: store.viewport.y,
      });
      return;
    }

    if (store.tool === 'pen') {
      setDrag({ kind: 'pen', points: [[w.x, w.y]] });
      return;
    }

    if (store.tool === 'arrow') {
      setDrag({ kind: 'arrow', x: w.x, y: w.y, x2: w.x, y2: w.y });
      return;
    }

    if (store.tool === 'text' || store.tool === 'sticky') {
      const text =
        store.tool === 'sticky'
          ? prompt('Sticky note text:', 'Note')
          : prompt('Text label:', 'Label');
      if (text == null) return;
      store.addAnnotation({
        id: uid('a'),
        type: store.tool,
        color: store.tool === 'sticky' ? '#fde68a' : '#e5e7eb',
        x: w.x,
        y: w.y,
        text: text || (store.tool === 'sticky' ? 'Note' : 'Text'),
      });
      return;
    }
  };

  const onPointerMove = (e: React.PointerEvent) => {
    if (!drag) return;
    const w = toWorld(e.clientX, e.clientY);

    if (drag.kind === 'pan') {
      store.setViewport({
        ...store.viewport,
        x: drag.vx + (e.clientX - drag.ox),
        y: drag.vy + (e.clientY - drag.oy),
      });
      return;
    }
    if (drag.kind === 'node') {
      store.moveNode(drag.id, w.x - drag.ox, w.y - drag.oy);
      return;
    }
    if (drag.kind === 'connect') {
      setCursor(w);
      return;
    }
    if (drag.kind === 'pen') {
      setDrag({ ...drag, points: [...drag.points, [w.x, w.y]] });
      return;
    }
    if (drag.kind === 'arrow') {
      setDrag({ ...drag, x2: w.x, y2: w.y });
      return;
    }
    if (drag.kind === 'anno') {
      store.updateAnnotation({
        ...store.doc.annotations.find((a) => a.id === drag.id)!,
        x: drag.ax + (w.x - drag.ox),
        y: drag.ay + (w.y - drag.oy),
      });
    }
  };

  const onPointerUp = (e: React.PointerEvent) => {
    if (!drag) return;

    if (drag.kind === 'connect') {
      const w = toWorld(e.clientX, e.clientY);
      const hit = store.doc.nodes.find((n) => Math.hypot(n.x - w.x, n.y - w.y) <= 28);
      if (hit) store.connectNodes(drag.fromId, hit.id);
      setCursor(null);
    }

    if (drag.kind === 'pen' && drag.points.length > 1) {
      store.addAnnotation({
        id: uid('a'),
        type: 'pen',
        color: '#f59e0b',
        points: drag.points,
      });
    }

    if (drag.kind === 'arrow') {
      store.addAnnotation({
        id: uid('a'),
        type: 'arrow',
        color: '#f59e0b',
        x: drag.x,
        y: drag.y,
        x2: drag.x2,
        y2: drag.y2,
      });
    }

    setDrag(null);
  };

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z') {
        e.preventDefault();
        store.undo();
      }
      if (e.key === 'Delete' || e.key === 'Backspace') {
        const tag = (e.target as HTMLElement)?.tagName;
        if (tag === 'INPUT' || tag === 'TEXTAREA') return;
        store.deleteSelection();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [store]);

  const nodeMap = new Map(store.doc.nodes.map((n) => [n.id, n]));
  const connectFrom = drag?.kind === 'connect' ? nodeMap.get(drag.fromId) : null;

  return (
    <svg
      ref={svgRef}
      className="graph-canvas"
      onWheel={onWheel}
      onPointerDown={onCanvasPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={onPointerUp}
      onDoubleClick={(e) => {
        if (store.tool !== 'select') return;
        const w = toWorld(e.clientX, e.clientY);
        store.addNodeAt(w.x, w.y);
      }}
    >
      <defs>
        <marker id="arrowHead" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
          <path d="M0,0 L6,3 L0,6 Z" fill="#f59e0b" />
        </marker>
        <marker id="annoArrow" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
          <path d="M0,0 L6,3 L0,6 Z" fill="#f59e0b" />
        </marker>
        <pattern id="grid" width="32" height="32" patternUnits="userSpaceOnUse">
          <path d="M 32 0 L 0 0 0 32" fill="none" stroke="rgba(255,255,255,0.04)" strokeWidth="1" />
        </pattern>
      </defs>

      <rect width="100%" height="100%" fill="url(#grid)" />

      <g transform={`translate(${store.viewport.x}, ${store.viewport.y}) scale(${store.viewport.scale})`}>
        {store.doc.edges.map((edge) => {
          const from = nodeMap.get(edge.from);
          const to = nodeMap.get(edge.to);
          if (!from || !to) return null;
          return (
            <EdgeView
              key={edge.id}
              edge={edge}
              from={from}
              to={to}
              selected={store.selectedEdgeId === edge.id}
              onSelect={store.selectEdge}
            />
          );
        })}

        {connectFrom && cursor && <DraftEdge from={connectFrom} toX={cursor.x} toY={cursor.y} />}

        <AnnotationLayer
          annotations={store.doc.annotations}
          selectedId={store.selectedAnnotationId}
          onSelect={store.selectAnnotation}
          draftPen={drag?.kind === 'pen' ? drag.points : undefined}
          draftArrow={
            drag?.kind === 'arrow'
              ? { x: drag.x, y: drag.y, x2: drag.x2, y2: drag.y2 }
              : null
          }
        />

        {store.doc.nodes.map((node) => (
          <NodeView
            key={node.id}
            node={node}
            selected={store.selectedNodeId === node.id}
            weather={weatherByNode[node.id]}
            currencies={store.doc.meta.currencies ?? ['EUR']}
            onPointerDown={onNodePointerDown}
            connectingFrom={drag?.kind === 'connect' && drag.fromId === node.id}
          />
        ))}
      </g>
    </svg>
  );
}
