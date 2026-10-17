import { useState, useEffect } from 'react';
import { households } from '../api/client';

/**
 * Custom hook for data fetching with loading/error states.
 * Follows React conventions: returns { data, loading, error }.
 *
 * How it works:
 *   1. On mount (and when deps change), calls the fetcher function
 *   2. Sets loading=true while the request is in flight
 *   3. On success → data is set, loading becomes false
 *   4. On failure → error message is set, loading becomes false
 *   5. If the component unmounts before the request completes,
 *      the `cancelled` flag prevents state updates on an unmounted component
 */
export function useFetch<T>(fetcher: () => Promise<T>, deps: unknown[] = []) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    fetcher()
      .then((result) => {
        if (!cancelled) { setData(result); setLoading(false); }
      })
      .catch((err) => {
        if (!cancelled) { setError(err.message); setLoading(false); }
      });

    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return { data, loading, error } as const;
}

// ─── Shared localStorage key for the active household ────
const HOUSEHOLD_ID_KEY = 'artha_household_id';

/**
 * useHouseholdId — Resolves the active household ID for all pages.
 *
 * Phase 1 strategy (single-household):
 *   1. Fetch the households list from the backend
 *   2. If at least one exists, store its ID in localStorage and return it
 *   3. If none exist, return '' (empty string)
 *
 * Every page uses this hook. When householdId is '', pages show a
 * "create a household first" message instead of calling APIs that
 * would fail with a 500 error.
 *
 * Phase 3 upgrade: replace this with a proper household selector
 * (dropdown or context) that lets users switch between households.
 */
export function useHouseholdId(): string {
  const { data } = useFetch(() => households.list(), []);

  if (data && data.length > 0) {
    localStorage.setItem(HOUSEHOLD_ID_KEY, data[0].id);
    return data[0].id as string;
  }

  return localStorage.getItem(HOUSEHOLD_ID_KEY) ?? '';
}
