import type { Rating } from '../types/api';

const RATING_TEXT: Record<Rating, string> = {
  A: 'A', B: 'B', C: 'C', D: 'D', F: 'F', '': '—',
};

/** 评级徽标：A绿/B青/C琥珀/D橙/F红/空=灰(未推演) */
export default function RatingBadge({ rating, size = 34 }: { rating: string; size?: number }) {
  const cls = rating ? `r-${rating.toLowerCase()}` : 'r-n';
  const label = RATING_TEXT[(rating || '') as Rating] ?? rating;
  return (
    <span className={`rating-badge ${cls}`} style={{ width: size, height: size, fontSize: size * 0.47 }} title={rating ? `评级 ${rating}` : '未推演'}>
      {label}
    </span>
  );
}
