import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import GoalsPage from './pages/GoalsPage';
import PortfolioPage from './pages/PortfolioPage';
import CashFlowPage from './pages/CashFlowPage';
import NetWorthPage from './pages/NetWorthPage';
import LiabilitiesPage from './pages/LiabilitiesPage';
import AdvisorPage from './pages/AdvisorPage';
import CalendarPage from './pages/CalendarPage';
import ImportPage from './pages/ImportPage';

/**
 * Root App component with React Router routes.
 * Layout wraps all routes with sidebar navigation.
 */
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="goals" element={<GoalsPage />} />
          <Route path="portfolio" element={<PortfolioPage />} />
          <Route path="cashflow" element={<CashFlowPage />} />
          <Route path="networth" element={<NetWorthPage />} />
          <Route path="liabilities" element={<LiabilitiesPage />} />
          <Route path="advisor" element={<AdvisorPage />} />
          <Route path="calendar" element={<CalendarPage />} />
          <Route path="import" element={<ImportPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
