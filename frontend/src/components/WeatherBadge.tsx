import type { UniverseWeatherVO } from '../types/api';

const WEATHER_EMOJI: Record<string, string> = {
  晴: '☀️', 晴转多云: '🌤️', 多云: '⛅', 阴: '☁️', 雨: '🌧️', 大雨: '⛈️', 风暴: '🌪️', 雪: '❄️', 雾: '🌫️',
};

function wxIcon(w: string): string {
  for (const k of Object.keys(WEATHER_EMOJI)) if (w.includes(k)) return WEATHER_EMOJI[k];
  return '🌗';
}

/** 市场气象徽标（含 7/30/90 天预报占位） */
export default function WeatherBadge({ weather }: { weather: UniverseWeatherVO | null }) {
  if (!weather) return null;
  const icon = wxIcon(weather.weather);
  return (
    <div className="card">
      <h4>🌦️ 市场气象 <span className="muted-tag" style={{ fontWeight: 400 }}>—— 数据驱动的环境信号</span></h4>
      <div className="flex" style={{ gap: 12, flexWrap: 'wrap' }}>
        <span className="badge" style={{ fontSize: 13, color: 'var(--text)' }}>
          {icon} {weather.weather}
        </span>
        {weather.forecast7d && <span className="pill">7d {wxIcon(weather.forecast7d)} {weather.forecast7d}</span>}
        {weather.forecast30d && <span className="pill">30d {wxIcon(weather.forecast30d)} {weather.forecast30d}</span>}
        {weather.forecast90d && <span className="pill">90d {wxIcon(weather.forecast90d)} {weather.forecast90d}</span>}
      </div>
      <div className="mt-14">
        {[
          ['搜索热度', weather.searchSignal],
          ['评论情绪', weather.sentimentSignal],
          ['价格波动', weather.priceSignal],
          ['政策风向', weather.policySignal],
        ].map(([k, v]) => (
          <div className="weather-line" key={k}>
            <b>{k as string}</b><span>{v as string}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
