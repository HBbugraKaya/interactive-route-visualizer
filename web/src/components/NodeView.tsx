import type { PointerEvent as ReactPointerEvent } from 'react';
import type { CurrencyCode, RouteNode, WeatherDay } from '../types';
import { formatMoney, stopTotalEur } from '../types';
import { WeatherBadge } from './WeatherBadge';

const NODE_R = 22;

interface Props {
  node: RouteNode;
  selected: boolean;
  weather?: WeatherDay;
  currencies: CurrencyCode[];
  onPointerDown: (e: ReactPointerEvent, node: RouteNode) => void;
  connectingFrom: boolean;
}

export function NodeView({
  node,
  selected,
  weather,
  currencies,
  onPointerDown,
  connectingFrom,
}: Props) {
  const total = stopTotalEur(node);
  const lines: string[] = [];
  if (node.useStayDate !== false && node.stayDate) {
    lines.push(node.stayDate);
  }
  if (node.usePeople !== false && node.peopleCount) {
    lines.push(`${node.peopleCount} pax`);
  }
  if (node.useHotelNights !== false && node.hotelNights != null) {
    lines.push(`${node.hotelNights} night(s)`);
  }
  if (total != null) {
    for (const c of currencies) {
      lines.push(formatMoney(total, c));
    }
  }

  return (
    <g
      className={`route-node ${selected ? 'selected' : ''} ${connectingFrom ? 'connect-from' : ''}`}
      transform={`translate(${node.x}, ${node.y})`}
      onPointerDown={(e) => onPointerDown(e, node)}
    >
      <circle r={NODE_R + 4} className="node-halo" />
      <circle r={NODE_R} className="node-circle" />
      <text className="node-label" y={NODE_R + 16} textAnchor="middle">
        {node.label}
      </text>
      {lines.map((line, i) => (
        <text
          key={`${line}-${i}`}
          className="node-meta"
          y={NODE_R + 30 + i * 12}
          textAnchor="middle"
        >
          {line}
        </text>
      ))}
      <foreignObject x={NODE_R - 4} y={-NODE_R - 28} width={72} height={28}>
        <WeatherBadge weather={weather} />
      </foreignObject>
      <circle className="node-handle" cx={NODE_R + 2} cy={0} r={5} />
    </g>
  );
}

export { NODE_R };
