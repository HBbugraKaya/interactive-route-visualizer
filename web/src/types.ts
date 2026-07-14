export type Tool =
  | 'select'
  | 'connect'
  | 'pen'
  | 'text'
  | 'arrow'
  | 'sticky';

export type CurrencyCode = 'EUR' | 'TRY' | 'USD';

export interface RouteNode {
  id: string;
  x: number;
  y: number;
  label: string;
  lat?: number | null;
  lon?: number | null;
  type?: string;
  note?: string;
  peopleCount?: number;
  stayDate?: string;
  hotelNights?: number;
  pricePerNightEur?: number;
  usePeople?: boolean;
  useStayDate?: boolean;
  useHotelNights?: boolean;
  usePrice?: boolean;
}

export interface RouteEdge {
  id: string;
  from: string;
  to: string;
}

export type AnnotationType = 'pen' | 'text' | 'arrow' | 'sticky';

export interface Annotation {
  id: string;
  type: AnnotationType;
  color: string;
  text?: string;
  x?: number;
  y?: number;
  x2?: number;
  y2?: number;
  points?: [number, number][];
}

export interface GraphMeta {
  date?: string;
  createdAt: string;
  updatedAt: string;
  currencies: CurrencyCode[];
}

export interface GraphDocument {
  meta: GraphMeta;
  nodes: RouteNode[];
  edges: RouteEdge[];
  annotations: Annotation[];
}

export interface WeatherDay {
  date: string;
  tempMax: number;
  tempMin: number;
  weatherCode: number;
  precipitation: number;
}

export interface Viewport {
  x: number;
  y: number;
  scale: number;
}

export function uid(prefix = 'id'): string {
  return `${prefix}_${Math.random().toString(36).slice(2, 9)}`;
}

export function createDemoGraph(today: string): GraphDocument {
  return {
    meta: {
      createdAt: today,
      updatedAt: today,
      currencies: ['EUR'],
    },
    nodes: [
      {
        id: 'n1',
        x: 180,
        y: 160,
        label: 'Istanbul',
        lat: 41.01,
        lon: 28.97,
        type: 'hub',
        note: 'Start',
        peopleCount: 2,
        stayDate: today,
        hotelNights: 2,
        pricePerNightEur: 120,
        usePeople: true,
        useStayDate: true,
        useHotelNights: true,
        usePrice: true,
      },
      {
        id: 'n2',
        x: 420,
        y: 220,
        label: 'Ankara',
        lat: 39.93,
        lon: 32.86,
        type: 'hub',
        note: 'Transfer',
        peopleCount: 2,
        stayDate: today,
        hotelNights: 1,
        pricePerNightEur: 120,
        usePeople: true,
        useStayDate: true,
        useHotelNights: true,
        usePrice: true,
      },
      {
        id: 'n3',
        x: 680,
        y: 160,
        label: 'Kayseri',
        lat: 38.73,
        lon: 35.48,
        type: 'stop',
        note: 'Arrival',
        peopleCount: 2,
        stayDate: today,
        hotelNights: 3,
        pricePerNightEur: 120,
        usePeople: true,
        useStayDate: true,
        useHotelNights: true,
        usePrice: true,
      },
    ],
    edges: [
      { id: 'e1', from: 'n1', to: 'n2' },
      { id: 'e2', from: 'n2', to: 'n3' },
    ],
    annotations: [
      {
        id: 'a1',
        type: 'sticky',
        color: '#fde68a',
        x: 430,
        y: 300,
        text: 'Aktarma 14:00',
      },
    ],
  };
}

const FALLBACK_RATES: Record<CurrencyCode, number> = {
  EUR: 1,
  USD: 1.14,
  TRY: 53.7,
};

export function formatMoney(eurAmount: number, currency: CurrencyCode, rates?: Record<string, number>): string {
  const rate = rates?.[currency] ?? FALLBACK_RATES[currency] ?? 1;
  const v = eurAmount * rate;
  const symbol = currency === 'TRY' ? '₺' : currency === 'USD' ? '$' : '€';
  return `${symbol}${v.toFixed(2)}`;
}

export function stopTotalEur(node: RouteNode): number | null {
  if (node.usePrice === false || node.useHotelNights === false) return null;
  const people = node.usePeople === false ? 1 : node.peopleCount ?? 1;
  const nights = node.hotelNights ?? 0;
  const price = node.pricePerNightEur ?? 120;
  return people * nights * price;
}
