import { HashRouter, Route, Routes } from 'react-router-dom';
import NavBar from './components/NavBar';
import InputPage from './pages/InputPage';
import RunPage from './pages/RunPage';
import StarMapPage from './pages/StarMapPage';
import UniverseDetailPage from './pages/UniverseDetailPage';
import DecisionPage from './pages/DecisionPage';

/** 全局应用容器：HashRouter 规避 Spring static 无 SPA fallback 的深链 404 */
export default function App() {
  return (
    <HashRouter>
      <div className="app">
        <NavBar />
        <main>
          <Routes>
            <Route path="/" element={<InputPage />} />
            <Route path="/run/:taskId" element={<RunPage />} />
            <Route path="/task/:taskId/stars" element={<StarMapPage />} />
            <Route path="/task/:taskId/stars/:universeId" element={<UniverseDetailPage />} />
            <Route path="/decision" element={<DecisionPage />} />
            <Route path="/task/:taskId/decision" element={<DecisionPage />} />
            <Route path="*" element={<InputPage />} />
          </Routes>
        </main>
      </div>
    </HashRouter>
  );
}
