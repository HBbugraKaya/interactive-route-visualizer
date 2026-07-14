import type { RouteEdge, RouteNode } from '../types';
import { NODE_R } from './NodeView';

interface Props {
  edge: RouteEdge;
  from: RouteNode;
  to: RouteNode;
  selected: boolean;
  onSelect: (id: string) => void;
}

function pointOnCircle(
  x1: number,
  y1: number,
  x2: number,
  y2: number,
  r: number,
): [number, number] {
  const dx = x2 - x1;
  const dy = y2 - y1;
  const len = Math.hypot(dx, dy) || 1;
  return [x1 + (dx / len) * r, y1 + (dy / len) * r];
}

export function EdgeView({ edge, from, to, selected, onSelect }: Props) {
  const [x1, y1] = pointOnCircle(from.x, from.y, to.x, to.y, NODE_R);
  const [x2, y2] = pointOnCircle(to.x, to.y, from.x, from.y, NODE_R);
  return (
    <g className={`route-edge ${selected ? 'selected' : ''}`} onPointerDown={() => onSelect(edge.id)}>
      <line x1={x1} y1={y1} x2={x2} y2={y2} className="edge-hit" />
      <line x1={x1} y1={y1} x2={x2} y2={y2} className="edge-line" markerEnd="url(#arrowHead)" />
    </g>
  );
}

interface DraftProps {
  from: RouteNode;
  toX: number;
  toY: number;
}

export function DraftEdge({ from, toX, toY }: DraftProps) {
  const [x1, y1] = pointOnCircle(from.x, from.y, toX, toY, NODE_R);
  return (
    <line
      x1={x1}
      y1={y1}
      x2={toX}
      y2={toY}
      className="edge-line draft"
      markerEnd="url(#arrowHead)"
    />
  );
}
