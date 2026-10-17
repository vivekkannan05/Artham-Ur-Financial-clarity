import { NavLink, Outlet } from 'react-router-dom';

/**
 * App layout — sidebar navigation + main content area.
 * Outlet renders the matched child route's component.
 */
export default function Layout() {
  return (
    <div className="app-layout">
      <nav className="sidebar">
        <div className="sidebar-brand">
          <h1>Artha</h1>
          <p>Financial Clarity</p>
        </div>
        <ul className="nav-links">
          <li><NavLink to="/">Dashboard</NavLink></li>
          <li><NavLink to="/goals">Goals</NavLink></li>
          <li><NavLink to="/portfolio">Portfolio</NavLink></li>
          <li><NavLink to="/cashflow">Cash Flow</NavLink></li>
          <li><NavLink to="/networth">Net Worth</NavLink></li>
          <li><NavLink to="/liabilities">Liabilities</NavLink></li>
          <li><NavLink to="/advisor">Advisor</NavLink></li>
          <li><NavLink to="/calendar">Calendar</NavLink></li>
          <li><NavLink to="/import">Import</NavLink></li>
        </ul>
      </nav>
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}
