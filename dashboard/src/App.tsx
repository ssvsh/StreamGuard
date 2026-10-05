import { Link, Route, Routes } from 'react-router-dom';
import { OverviewPage } from './pages/OverviewPage';
import { ContractsPage } from './pages/ContractsPage';
import { ViolationsPage } from './pages/ViolationsPage';
import { RecommendationPage } from './pages/RecommendationPage';

export function App() {
  return (
    <>
      <nav className="nav">
        <Link to="/">Overview</Link>
        <Link to="/contracts">Contracts</Link>
        <Link to="/violations">Violations</Link>
        <Link to="/recommendation">Recommendation</Link>
      </nav>
      <div className="container">
        <Routes>
          <Route path="/" element={<OverviewPage />} />
          <Route path="/contracts" element={<ContractsPage />} />
          <Route path="/violations" element={<ViolationsPage />} />
          <Route path="/recommendation" element={<RecommendationPage />} />
        </Routes>
      </div>
    </>
  );
}