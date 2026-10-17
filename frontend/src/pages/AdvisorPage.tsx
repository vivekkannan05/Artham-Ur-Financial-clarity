import { useFetch } from '../hooks/useFetch';
import { advisor } from '../api/client';
import { Section, SeverityBadge, LoadingSpinner, ErrorMessage } from '../components/Shared';
import type { HealthScore, RuleResult } from '../types';

/**
 * Advisor Page — UX-1.4: Action Center + Health Score.
 * Shows recommendations as action cards with accept/reject/defer.
 */
export default function AdvisorPage() {
  const householdId = localStorage.getItem('artha_household_id') ?? '';
  const recs = useFetch<RuleResult[]>(
    () => advisor.advise(householdId), [householdId]);
  const hs = useFetch<HealthScore>(
    () => advisor.healthScore(householdId), [householdId]);

  if (recs.loading) return <LoadingSpinner />;
  if (recs.error) return <ErrorMessage message={recs.error} />;

  return (
    <div className="page">
      <h1>Financial Advisor</h1>

      {/* Health Score */}
      {hs.data && (
        <Section title="Health Score">
          <div className="health-score-header">
            <div className={`health-overall ${hs.data.overallStatus.toLowerCase()}`}>
              {hs.data.overallScore}
            </div>
            <span className="health-status">{hs.data.overallStatus}</span>
          </div>
          <div className="subscore-grid">
            {hs.data.subScores.map((s) => (
              <div key={s.category} className={`subscore ${s.status.toLowerCase()}`}>
                <strong>{s.category}</strong>
                <div className="subscore-value">{s.score}</div>
                <small>{s.explanation}</small>
              </div>
            ))}
          </div>
        </Section>
      )}

      {/* Action Center */}
      <Section title="Action Center">
        {recs.data && recs.data.length === 0 && (
          <div className="on-track">
            <h3>No action needed — your portfolio is on track!</h3>
          </div>
        )}
        {recs.data && recs.data.map((r, i) => (
          <div key={i} className="action-card">
            <div className="action-header">
              <SeverityBadge severity={r.severity} />
              <strong>{r.category}</strong>
            </div>
            <p className="action-trigger">{r.trigger}</p>
            <div className="action-details">
              <div><strong>Action:</strong> {r.action}</div>
              <div><strong>If you act:</strong> {r.expectedImpact}</div>
              <div><strong>If you don't:</strong> {r.downsideRisk}</div>
            </div>
            <div className="action-buttons">
              <button className="btn btn-accept">Accept</button>
              <button className="btn btn-reject">Reject</button>
              <button className="btn btn-defer">Defer</button>
            </div>
          </div>
        ))}
      </Section>
    </div>
  );
}
