import { useFetch } from '../hooks/useFetch';
import { households, cashFlow, netWorth, advisor, goals } from '../api/client';
import { StatCard, ProgressBar, Section, SeverityBadge, LoadingSpinner, ErrorMessage, formatCurrency, formatPercent } from '../components/Shared';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import type { HealthScore, CashFlowStatement, NetWorthResponse, GoalProjection, RuleResult } from '../types';

/**
 * Dashboard — UX-1.1: Household financial overview.
 *
 * Shows: net worth, cash flow summary, goal progress bars,
 * asset allocation pie chart, emergency fund, top 3 advisor actions.
 */
const COLORS = ['#3b82f6', '#22c55e', '#eab308', '#ef4444', '#8b5cf6', '#f97316'];

// For Phase 1: use the first household found
const HOUSEHOLD_ID_KEY = 'artha_household_id';

function useHouseholdId() {
  const { data } = useFetch(() => households.list(), []);
  if (data && data.length > 0) {
    localStorage.setItem(HOUSEHOLD_ID_KEY, data[0].id);
    return data[0].id as string;
  }
  return localStorage.getItem(HOUSEHOLD_ID_KEY) ?? '';
}

export default function Dashboard() {
  const householdId = useHouseholdId();

  const cf = useFetch<CashFlowStatement>(
    () => cashFlow.get(householdId), [householdId]);
  const nw = useFetch<NetWorthResponse>(
    () => netWorth.current(householdId), [householdId]);
  const hs = useFetch<HealthScore>(
    () => advisor.healthScore(householdId), [householdId]);
  const recs = useFetch<RuleResult[]>(
    () => advisor.advise(householdId), [householdId]);
  const gl = useFetch<any[]>(
    () => goals.list(householdId), [householdId]);

  if (!householdId) {
    return <div className="page"><h2>No household found. Create one to get started.</h2></div>;
  }

  return (
    <div className="page">
      <h1>Household Dashboard</h1>

      {/* ─── Top Stats Row ─── */}
      <div className="stats-grid">
        {nw.data && (
          <>
            <StatCard label="Net Worth" value={formatCurrency(nw.data.netWorth)} status="green" />
            <StatCard label="Total Assets" value={formatCurrency(nw.data.totalAssets)} />
            <StatCard label="Total Liabilities" value={formatCurrency(nw.data.totalLiabilities)} />
          </>
        )}
        {cf.data && (
          <>
            <StatCard label="Monthly Income" value={formatCurrency(cf.data.totalIncome)} />
            <StatCard label="Free Cash Flow" value={formatCurrency(cf.data.freeCashFlow)}
              status={cf.data.freeCashFlow > 0 ? 'green' : 'red'} />
            <StatCard label="Savings Rate" value={formatPercent(cf.data.savingsRate)}
              status={cf.data.savingsRate >= 20 ? 'green' : cf.data.savingsRate >= 10 ? 'yellow' : 'red'} />
          </>
        )}
      </div>

      {/* ─── Health Score ─── */}
      {hs.data && (
        <Section title="Financial Health Score">
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

      {/* ─── Asset Allocation Pie Chart ─── */}
      {nw.data && nw.data.assetsByClass && (
        <Section title="Asset Allocation">
          <ResponsiveContainer width="100%" height={250}>
            <PieChart>
              <Pie
                data={Object.entries(nw.data.assetsByClass).map(([name, value]) => ({ name, value }))}
                cx="50%" cy="50%" outerRadius={80} dataKey="value" label={({ name, percent }) =>
                  `${name} ${(percent * 100).toFixed(0)}%`
                }
              >
                {Object.keys(nw.data.assetsByClass).map((_, i) => (
                  <Cell key={i} fill={COLORS[i % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip formatter={(v: number) => formatCurrency(v)} />
            </PieChart>
          </ResponsiveContainer>
        </Section>
      )}

      {/* ─── Goal Progress ─── */}
      {gl.data && gl.data.length > 0 && (
        <Section title="Goal Progress">
          {gl.data.map((g: any) => (
            <ProgressBar key={g.id} percent={g.fundingPercentage ?? 0}
              label={`${g.name} — ${formatCurrency(g.targetAmount)}`} />
          ))}
        </Section>
      )}

      {/* ─── Top Advisor Actions ─── */}
      {recs.data && recs.data.length > 0 && (
        <Section title="Top Actions">
          {recs.data.slice(0, 3).map((r, i) => (
            <div key={i} className="action-card">
              <SeverityBadge severity={r.severity} />
              <strong>{r.trigger}</strong>
              <p>{r.action}</p>
            </div>
          ))}
        </Section>
      )}
      {recs.data && recs.data.length === 0 && (
        <Section title="Advisor">
          <p className="on-track">Your finances are on track. No actions needed!</p>
        </Section>
      )}

      {/* ─── Loading/Error states ─── */}
      {(cf.loading || nw.loading) && <LoadingSpinner />}
      {cf.error && <ErrorMessage message={cf.error} />}
    </div>
  );
}
