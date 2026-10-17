import { useFetch, useHouseholdId } from '../hooks/useFetch';
import { holdings } from '../api/client';
import { Section, LoadingSpinner, ErrorMessage, formatCurrency } from '../components/Shared';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';

/**
 * Portfolio Page — UX-1.3: All holdings with allocation breakdown.
 */
const COLORS = ['#3b82f6', '#22c55e', '#eab308', '#ef4444', '#8b5cf6', '#f97316', '#06b6d4'];

export default function PortfolioPage() {
  const householdId = useHouseholdId();
  const { data, loading, error } = useFetch<any[]>(
    () => householdId ? holdings.list(householdId) : Promise.resolve([]),
    [householdId]);

  if (!householdId) return <div className="page"><h2>No household found. Create one to get started.</h2></div>;
  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={error} />;

  // Group by asset class for pie chart
  const byClass: Record<string, number> = {};
  (data ?? []).forEach((h: any) => {
    const cls = h.instrumentTypeCode ?? 'OTHER';
    byClass[cls] = (byClass[cls] ?? 0) + (h.currentValue ?? 0);
  });
  const pieData = Object.entries(byClass).map(([name, value]) => ({ name, value }));

  return (
    <div className="page">
      <h1>Portfolio</h1>

      {pieData.length > 0 && (
        <Section title="Allocation">
          <ResponsiveContainer width="100%" height={250}>
            <PieChart>
              <Pie data={pieData} cx="50%" cy="50%" outerRadius={80} dataKey="value"
                label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}>
                {pieData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
              </Pie>
              <Tooltip formatter={(v: number) => formatCurrency(v)} />
            </PieChart>
          </ResponsiveContainer>
        </Section>
      )}

      <Section title="Holdings">
        <table className="data-table">
          <thead>
            <tr>
              <th>Name</th><th>Type</th><th>Current Value</th><th>Invested</th><th>Status</th>
            </tr>
          </thead>
          <tbody>
            {(data ?? []).map((h: any) => (
              <tr key={h.id}>
                <td>{h.name}</td>
                <td>{h.instrumentTypeCode}</td>
                <td>{formatCurrency(h.currentValue ?? 0)}</td>
                <td>{formatCurrency(h.acquisitionCostTotal ?? 0)}</td>
                <td className={h.status === 'ACTIVE' ? 'status-green' : 'status-grey'}>{h.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Section>
    </div>
  );
}
