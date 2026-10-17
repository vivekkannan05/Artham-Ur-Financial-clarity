import { useFetch, useHouseholdId } from '../hooks/useFetch';
import { liabilities } from '../api/client';
import { Section, LoadingSpinner, ErrorMessage, formatCurrency } from '../components/Shared';
import type { Liability } from '../types';

/**
 * Liabilities Page — All debts with status and EMI breakdown.
 */
export default function LiabilitiesPage() {
  const householdId = useHouseholdId();
  const { data, loading, error } = useFetch<Liability[]>(
    () => householdId ? liabilities.list(householdId) : Promise.resolve([]),
    [householdId]);

  if (!householdId) return <div className="page"><h2>No household found. Create one to get started.</h2></div>;
  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={error} />;

  const totalEmi = (data ?? [])
    .filter((l) => l.status === 'ACTIVE')
    .reduce((sum, l) => sum + l.emiAmount, 0);

  return (
    <div className="page">
      <h1>Liabilities</h1>
      <p className="summary">Total Active EMI: <strong>{formatCurrency(totalEmi)}</strong>/month</p>

      <Section title="All Liabilities">
        <table className="data-table">
          <thead>
            <tr>
              <th>Type</th><th>Lender</th><th>Outstanding</th>
              <th>EMI</th><th>Rate</th><th>End Date</th><th>Status</th>
            </tr>
          </thead>
          <tbody>
            {(data ?? []).map((l) => (
              <tr key={l.id}>
                <td>{l.liabilityType}</td>
                <td>{l.lender}</td>
                <td>{formatCurrency(l.principalOutstanding)}</td>
                <td>{formatCurrency(l.emiAmount)}</td>
                <td>{l.interestRate}%</td>
                <td>{l.tenureEnd ?? '—'}</td>
                <td className={l.status === 'ACTIVE' ? 'status-green' : 'status-grey'}>{l.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Section>
    </div>
  );
}
