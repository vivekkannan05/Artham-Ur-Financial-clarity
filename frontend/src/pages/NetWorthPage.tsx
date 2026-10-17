import { useFetch } from '../hooks/useFetch';
import { netWorth } from '../api/client';
import { StatCard, Section, LoadingSpinner, ErrorMessage, formatCurrency } from '../components/Shared';
import type { NetWorthResponse, EmergencyFundStatus } from '../types';

/**
 * Net Worth Page — Assets, liabilities, and emergency fund status.
 */
export default function NetWorthPage() {
  const householdId = localStorage.getItem('artha_household_id') ?? '';
  const nw = useFetch<NetWorthResponse>(
    () => netWorth.current(householdId), [householdId]);
  const ef = useFetch<EmergencyFundStatus>(
    () => netWorth.emergencyFund(householdId), [householdId]);

  if (nw.loading) return <LoadingSpinner />;
  if (nw.error) return <ErrorMessage message={nw.error} />;

  return (
    <div className="page">
      <h1>Net Worth</h1>

      {nw.data && (
        <div className="stats-grid">
          <StatCard label="Total Assets" value={formatCurrency(nw.data.totalAssets)} status="green" />
          <StatCard label="Total Liabilities" value={formatCurrency(nw.data.totalLiabilities)} status="red" />
          <StatCard label="Net Worth" value={formatCurrency(nw.data.netWorth)}
            status={nw.data.netWorth >= 0 ? 'green' : 'red'} />
        </div>
      )}

      {nw.data?.assetsByClass && (
        <Section title="Assets by Class">
          <table className="data-table">
            <thead><tr><th>Asset Class</th><th>Value</th></tr></thead>
            <tbody>
              {Object.entries(nw.data.assetsByClass).map(([cls, val]) => (
                <tr key={cls}><td>{cls}</td><td>{formatCurrency(val as number)}</td></tr>
              ))}
            </tbody>
          </table>
        </Section>
      )}

      {ef.data && (
        <Section title="Emergency Fund">
          <div className="stats-grid">
            <StatCard label="Monthly Essentials" value={formatCurrency(ef.data.monthlyEssentialExpenses)} />
            <StatCard label="Required Reserve" value={formatCurrency(ef.data.requiredReserve)} />
            <StatCard label="Current Reserve" value={formatCurrency(ef.data.currentLiquidReserve)} />
            <StatCard label="Months Covered" value={ef.data.monthsCovered.toFixed(1)}
              status={ef.data.monthsCovered >= 6 ? 'green' : ef.data.monthsCovered >= 3 ? 'yellow' : 'red'} />
            <StatCard label="Status" value={ef.data.adequacy}
              status={ef.data.adequacy === 'ADEQUATE' ? 'green' : ef.data.adequacy === 'PARTIAL' ? 'yellow' : 'red'} />
          </div>
        </Section>
      )}
    </div>
  );
}
