import { useFetch, useHouseholdId } from '../hooks/useFetch';
import { cashFlow } from '../api/client';
import { StatCard, Section, LoadingSpinner, ErrorMessage, formatCurrency, formatPercent } from '../components/Shared';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import type { CashFlowStatement } from '../types';

/**
 * Cash Flow Page — Monthly income vs expenses breakdown.
 */
export default function CashFlowPage() {
  const householdId = useHouseholdId();
  const { data, loading, error } = useFetch<CashFlowStatement>(
    () => householdId ? cashFlow.get(householdId) : Promise.resolve({} as CashFlowStatement),
    [householdId]);

  if (!householdId) return <div className="page"><h2>No household found. Create one to get started.</h2></div>;
  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={error} />;
  if (!data) return null;

  const chartData = [
    { name: 'Income', amount: data.grossIncome },
    { name: 'Consumption', amount: data.consumptionExpenses },
    { name: 'Investments', amount: data.investmentOutflows },
    { name: 'Free Cash', amount: data.freeCashFlow },
  ];

  return (
    <div className="page">
      <h1>Cash Flow</h1>

      <div className="stats-grid">
        <StatCard label="Gross Income" value={formatCurrency(data.grossIncome)} />
        <StatCard label="Consumption" value={formatCurrency(data.consumptionExpenses)} />
        <StatCard label="Investment Outflows" value={formatCurrency(data.investmentOutflows)} />
        <StatCard label="Total Expenses" value={formatCurrency(data.totalExpenses)} />
        <StatCard label="Free Cash Flow" value={formatCurrency(data.freeCashFlow)}
          status={data.freeCashFlow > 0 ? 'green' : 'red'} />
        <StatCard label="Savings Rate" value={formatPercent(data.savingsRate)}
          status={data.savingsRate >= 20 ? 'green' : 'yellow'} />
        <StatCard label="Debt Service Ratio" value={formatPercent(data.debtServiceRatio)}
          status={data.debtServiceRatio <= 40 ? 'green' : 'red'} />
      </div>

      <Section title="Cash Flow Breakdown">
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={chartData}>
            <XAxis dataKey="name" />
            <YAxis tickFormatter={(v) => `₹${(v / 1000).toFixed(0)}k`} />
            <Tooltip formatter={(v: number) => formatCurrency(v)} />
            <Legend />
            <Bar dataKey="amount" fill="#3b82f6" />
          </BarChart>
        </ResponsiveContainer>
      </Section>
    </div>
  );
}
