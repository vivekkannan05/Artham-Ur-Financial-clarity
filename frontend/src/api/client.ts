/**
 * API Client — Centralized fetch wrappers for the Spring Boot backend.
 *
 * All API calls go through this module. This makes it easy to:
 *   1. Add auth headers later (Phase 3)
 *   2. Handle errors consistently
 *   3. Mock in tests
 *
 * In dev mode, Vite proxies /api → http://localhost:8080 (see vite.config.ts).
 * In production (Docker), Nginx proxies /api → backend:8080 (see nginx.conf).
 */

const BASE = '/api';

async function fetchJson<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    headers: { 'Content-Type': 'application/json', ...init?.headers },
    ...init,
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(`API error ${response.status}: ${text}`);
  }
  return response.json();
}

// ─── Households ──────────────────────────────────────────
export const households = {
  list: () => fetchJson<any[]>(`${BASE}/households`),
  get: (id: string) => fetchJson<any>(`${BASE}/households/${id}`),
};

// ─── Cash Flow ───────────────────────────────────────────
export const cashFlow = {
  get: (householdId: string) =>
    fetchJson<any>(`${BASE}/households/${householdId}/cash-flow`),
};

// ─── Goals ───────────────────────────────────────────────
export const goals = {
  list: (householdId: string) =>
    fetchJson<any[]>(`${BASE}/households/${householdId}/goals`),
  projection: (goalId: string) =>
    fetchJson<any>(`${BASE}/goals/${goalId}/projection`),
};

// ─── Investments ─────────────────────────────────────────
export const holdings = {
  list: (householdId: string) =>
    fetchJson<any[]>(`${BASE}/households/${householdId}/holdings`),
};

// ─── Net Worth ───────────────────────────────────────────
export const netWorth = {
  current: (householdId: string) =>
    fetchJson<any>(`${BASE}/households/${householdId}/networth`),
  emergencyFund: (householdId: string) =>
    fetchJson<any>(`${BASE}/households/${householdId}/emergency-fund`),
};

// ─── Liabilities ─────────────────────────────────────────
export const liabilities = {
  list: (householdId: string) =>
    fetchJson<any[]>(`${BASE}/households/${householdId}/liabilities`),
};

// ─── Advisor & Health Score ──────────────────────────────
export const advisor = {
  advise: (householdId: string) =>
    fetchJson<any[]>(`${BASE}/households/${householdId}/advisor`),
  healthScore: (householdId: string) =>
    fetchJson<any>(`${BASE}/households/${householdId}/health-score`),
  staleData: (householdId: string) =>
    fetchJson<any[]>(`${BASE}/households/${householdId}/stale-data`),
};

// ─── Calendar ────────────────────────────────────────────
export const calendar = {
  list: (householdId: string) =>
    fetchJson<any[]>(`${BASE}/households/${householdId}/calendar`),
  autoPopulate: (householdId: string) =>
    fetchJson<any>(`${BASE}/households/${householdId}/calendar/auto-populate`, {
      method: 'POST',
    }),
};

// ─── Import ──────────────────────────────────────────────
export const importApi = {
  previewCsv: (file: File, entityType: string, householdId: string) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('entityType', entityType);
    formData.append('householdId', householdId);
    return fetch(`${BASE}/import/csv/preview`, {
      method: 'POST',
      body: formData,
    }).then((r) => r.json());
  },
};

// ─── Scenario ────────────────────────────────────────────
export const scenario = {
  analyze: (householdId: string, overrides: Record<string, number>) =>
    fetchJson<any>(`${BASE}/households/${householdId}/scenario`, {
      method: 'POST',
      body: JSON.stringify({ overrides }),
    }),
};
