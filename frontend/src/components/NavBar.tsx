import { NavLink, useLocation } from 'react-router-dom';

const LINKS = [
  { to: '/', label: '新建推演', end: true },
  { to: '/decision', label: '决策', end: false },
];

/** 顶栏：品牌 + 导航；taskId 存在时展示「查看当前任务」快捷入口 */
export default function NavBar({ taskId }: { taskId?: number | string }) {
  const loc = useLocation();
  return (
    <nav className="nav">
      <div className="container nav-inner">
        <NavLink to="/" className="brand">
          <span className="dot">✦</span>
          <span>
            多元宇宙引擎
            <small>跨境策略模拟器 · 百炼复赛</small>
          </span>
        </NavLink>
        <div className="nav-links">
          {LINKS.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              end={l.end}
              className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
            >
              {l.label}
            </NavLink>
          ))}
          {taskId !== undefined && loc.pathname !== '/decision' && (
            <span className="badge" style={{ marginLeft: 6 }}>任务 #{taskId}</span>
          )}
        </div>
      </div>
    </nav>
  );
}
