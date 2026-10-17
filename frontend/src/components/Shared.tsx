import { ReactNode } from 'react';

/** Loading spinner shown while data is being fetched. */
export function LoadingSpinner() {
  return <div className="loading">Loading...</div>;
}

/** Error message display. */
export function ErrorMessage({ message }: { message: string }) {
  return <div className="error">Error: {message}</div>;
}

/** Stat card for dashboard — shows a label and value. */
export function StatCard({ label, value, status }: {
  label: string; value: string | number; status?: 'green' | 'yellow' | 'red';
}) {
  return (
    <div className={`stat-card ${status ?? ''}`}>
      <div className="stat-label">{label}</div>
      <div className="stat-value">{value}</div>
    </div>
  );
}

/** Progress bar for goal funding percentage. */
export function ProgressBar({ percent, label }: { percent: number; label?: string }) {
  const capped = Math.min(100, Math.max(0, percent));
  const color = capped >= 80 ? '#22c55e' : capped >= 50 ? '#eab308' : '#ef4444';
  return (
    <div className="progress-container">
      {label && <div className="progress-label">{label}</div>}
      <div className="progress-bar-bg">
        <div className="progress-bar-fill" style={{ width: `${capped}%`, backgroundColor: color }} />
      </div>
      <div className="progress-text">{capped.toFixed(0)}%</div>
    </div>
  );
}

/** Section wrapper with a title. */
export function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="section">
      <h2>{title}</h2>
      {children}
    </section>
  );
}

/** Severity badge for advisor recommendations. */
export function SeverityBadge({ severity }: { severity: string }) {
  const colors: Record<string, string> = {
    CRITICAL: '#ef4444', WARNING: '#eab308', INFO: '#3b82f6',
  };
  return (
    <span className="badge" style={{ backgroundColor: colors[severity] ?? '#6b7280' }}>
      {severity}
    </span>
  );
}

/** Format a number as Indian currency (₹). */
export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency', currency: 'INR', maximumFractionDigits: 0,
  }).format(value);
}

/** Format a number as percentage. */
export function formatPercent(value: number): string {
  return `${value.toFixed(1)}%`;
}
