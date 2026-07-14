import type { WeatherDay } from '../types';
import { weatherGlyph } from '../services/weatherApi';

interface Props {
  weather?: WeatherDay;
}

export function WeatherBadge({ weather }: Props) {
  if (!weather || Number.isNaN(weather.tempMax)) return null;
  const avg = Math.round((weather.tempMax + weather.tempMin) / 2);
  return (
    <div className="weather-badge" title={`H:${weather.tempMax}° L:${weather.tempMin}° rain:${weather.precipitation}mm`}>
      <span className="weather-glyph">{weatherGlyph(weather.weatherCode)}</span>
      <span className="weather-temp">{avg}°</span>
    </div>
  );
}
