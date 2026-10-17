import { useFetch } from '../hooks/useFetch';
import { goals } from '../api/client';
import { ProgressBar, Section, LoadingSpinner, ErrorMessage, formatCurrency } from '../components/Shared';
import type { Goal } from '../types';

/**
 * Goals Page — UX-1.2: Goal list with funding progress.
 * Clicking a goal shows projection details.
 */
export default function GoalsPage() {
  const householdId = localStorage.getItem('artha_household_id') ?? '';
  const { data, loading, error } = useFetch<Goal[]>(
    () => goals.list(householdId), [householdId]);

  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={error} />;

  return (
    <div className="page">
      <h1>Financial Goals</h1>
      {data && data.length === 0 && <p>No goals defined yet. Create one to get started.</p>}
      {data && data.map((g: any) => (
        <Section key={g.id} title={g.name}>
          <div className="goal-detail">
            <div className="goal-row">
              <span>Target: {formatCurrency(g.targetAmount)}</span>
              <span>Target Date: {g.targetDate}</span>
              <span>Priority: {g.priority}</span>
              <span className={`status-badge ${g.status.toLowerCase()}`}>{g.status}</span>
            </div>
            <ProgressBar percent={g.fundingPercentage ?? 0} />
            <GoalProjectionDetail goalId={g.id} />
          </div>
        </Section>
      ))}
    </div>
  );
}

function GoalProjectionDetail({ goalId }: { goalId: string }) {
  const { data, loading } = useFetch(
    () => goals.projection(goalId), [goalId]);

  if (loading || !data) return null;

  return (
    <div className="projection-grid">
      <div><strong>Inflation-Adjusted Target:</strong> {formatCurrency(data.inflationAdjustedTarget)}</div>
      <div><strong>Current Corpus:</strong> {formatCurrency(data.currentCorpus)}</div>
      <div><strong>Projected Corpus:</strong> {formatCurrency(data.projectedCorpus)}</div>
      <div><strong>Funding:</strong> {data.fundingPercentage?.toFixed(1)}%</div>
      <div><strong>Shortfall/Surplus:</strong> {formatCurrency(data.shortfallOrSurplus)}</div>
      <div><strong>Required SIP:</strong> {formatCurrency(data.requiredMonthlySip)}/mo</div>
    </div>
  );
}
