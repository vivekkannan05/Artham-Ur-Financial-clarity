import { useFetch, useHouseholdId } from '../hooks/useFetch';
import { calendar } from '../api/client';
import { Section, LoadingSpinner, ErrorMessage } from '../components/Shared';
import type { CalendarEvent } from '../types';

/**
 * Calendar Page — UX-1.7: Financial calendar with events.
 */
export default function CalendarPage() {
  const householdId = useHouseholdId();
  const { data, loading, error } = useFetch<CalendarEvent[]>(
    () => householdId ? calendar.list(householdId) : Promise.resolve([]),
    [householdId]);

  if (!householdId) return <div className="page"><h2>No household found. Create one to get started.</h2></div>;

  const handleAutoPopulate = async () => {
    await calendar.autoPopulate(householdId);
    window.location.reload();
  };

  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={error} />;

  // Group events by month
  const byMonth: Record<string, CalendarEvent[]> = {};
  (data ?? []).forEach((e) => {
    const month = e.eventDate.substring(0, 7); // YYYY-MM
    if (!byMonth[month]) byMonth[month] = [];
    byMonth[month].push(e);
  });

  const typeColors: Record<string, string> = {
    SIP: '#3b82f6', EMI: '#ef4444', RENEWAL: '#eab308',
    MATURITY: '#22c55e', TAX_DEADLINE: '#f97316',
    GOAL_REVIEW: '#8b5cf6', CUSTOM: '#6b7280',
  };

  return (
    <div className="page">
      <h1>Financial Calendar</h1>
      <button className="btn btn-primary" onClick={handleAutoPopulate}>
        Auto-Populate Events
      </button>

      {Object.keys(byMonth).length === 0 && (
        <p>No calendar events yet. Click "Auto-Populate" to generate from your financial data.</p>
      )}

      {Object.entries(byMonth).sort().map(([month, events]) => (
        <Section key={month} title={month}>
          {events.map((e) => (
            <div key={e.id} className="calendar-event">
              <span className="event-date">{e.eventDate}</span>
              <span className="event-type" style={{
                backgroundColor: typeColors[e.eventType] ?? '#6b7280'
              }}>{e.eventType}</span>
              <span className="event-desc">{e.description}</span>
              {e.recurrence !== 'NONE' && (
                <span className="event-recurrence">({e.recurrence})</span>
              )}
            </div>
          ))}
        </Section>
      ))}
    </div>
  );
}
