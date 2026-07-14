import type { WeatherDay } from '../types';

function isArchiveDate(date: string): boolean {
  const d = new Date(date + 'T00:00:00');
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const max = new Date(today);
  max.setDate(max.getDate() + 15);
  return d < today || d > max;
}

export async function fetchWeather(
  lat: number,
  lon: number,
  date: string,
): Promise<WeatherDay> {
  const archive = isArchiveDate(date);
  const base = archive
    ? 'https://archive-api.open-meteo.com/v1/archive'
    : 'https://api.open-meteo.com/v1/forecast';

  const url =
    `${base}?latitude=${lat}&longitude=${lon}` +
    `&daily=temperature_2m_max,temperature_2m_min,weathercode,precipitation_sum` +
    `&timezone=auto&start_date=${date}&end_date=${date}`;

  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(`Open-Meteo HTTP ${res.status}`);
  }
  const data = await res.json();
  const daily = data.daily;
  return {
    date,
    tempMax: daily.temperature_2m_max?.[0] ?? NaN,
    tempMin: daily.temperature_2m_min?.[0] ?? NaN,
    weatherCode: daily.weathercode?.[0] ?? 0,
    precipitation: daily.precipitation_sum?.[0] ?? 0,
  };
}

export async function fetchWeatherForStops(
  stops: {
    id: string;
    lat?: number | null;
    lon?: number | null;
    stayDate?: string | null;
    useStayDate?: boolean;
  }[],
  fallbackDate: string,
): Promise<Record<string, WeatherDay>> {
  const entries = await Promise.all(
    stops
      .filter((s) => s.lat != null && s.lon != null)
      .map(async (s) => {
        try {
          const date =
            s.useStayDate !== false && s.stayDate && /^\d{4}-\d{2}-\d{2}$/.test(s.stayDate)
              ? s.stayDate
              : fallbackDate;
          const weather = await fetchWeather(s.lat as number, s.lon as number, date);
          return [s.id, weather] as const;
        } catch {
          return null;
        }
      }),
  );

  const map: Record<string, WeatherDay> = {};
  for (const e of entries) {
    if (e) map[e[0]] = e[1];
  }
  return map;
}

export function weatherGlyph(code: number): string {
  if (code === 0) return '☀';
  if (code <= 3) return '☁';
  if (code <= 48) return '霧';
  if (code <= 67) return '🌧';
  if (code <= 77) return '❄';
  if (code <= 82) return '🌧';
  if (code <= 86) return '❄';
  if (code <= 99) return '⛈';
  return '·';
}
