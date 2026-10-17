/**
 * TypeScript interfaces matching backend Java DTOs.
 * These ensure type safety between frontend and backend.
 *
 * NAMING CONVENTION: interfaces match Java record/class names exactly.
 */

// ─── Core entities ───────────────────────────────────────
export interface Household {
  id: string;
  name: string;
  createdAt: string;
}

export interface Person {
  id: string;
  name: string;
  dateOfBirth: string;
  relationship: string;
  panEncrypted?: string;
  panMasked?: string;
}

// ─── Cash Flow ───────────────────────────────────────────
export interface CashFlowStatement {
  householdId: string;
  grossIncome: number;
  consumptionExpenses: number;
  investmentOutflows: number;
  totalExpenses: number;
  freeCashFlow: number;
  savingsRate: number;
  debtServiceRatio: number;
}

// ─── Goals ───────────────────────────────────────────────
export interface Goal {
  id: string;
  name: string;
  goalType: string;
  targetAmountToday: number;
  targetDate: string;
  priority: string;
  status: 'ACTIVE' | 'ACHIEVED' | 'ABANDONED';
  inflationRate: number;
  expectedReturnRate: number;
  sipStepUpPct?: number;
  fundingPercentage?: number;
}

export interface GoalProjection {
  targetAmountToday: number;
  inflationAdjustedTarget: number;
  yearsToGoal: number;
  currentCorpus: number;
  projectedCorpus: number;
  fundingPercentage: number;
  shortfallOrSurplus: number;
  requiredMonthlySip: number;
  requiredSipWithStepUp: number;
}

// ─── Investments ─────────────────────────────────────────
export interface Account {
  id: string;
  accountName: string;
  accountType: string;
  institution: string;
}

export interface Holding {
  id: string;
  name: string;
  accountId: string;
  ownerId: string;
  instrumentTypeCode: string;
  instrumentTypeName: string;
  assetClass: string;
  units: number;
  acquisitionCostTotal: number;
  acquisitionCostPerUnit: number;
  acquisitionDate: string;
  currentValue: number;
  valuationDate: string;
  currency: string;
  status: 'ACTIVE' | 'CLOSED';
}

// ─── Liabilities ─────────────────────────────────────────
export interface Liability {
  id: string;
  liabilityType: string;
  lender: string;
  principalOutstanding: number;
  emiAmount: number;
  interestRate: number;
  tenureEnd?: string;
  status: 'ACTIVE' | 'CLOSED' | 'DEFAULTED';
}

// ─── Net Worth ───────────────────────────────────────────
export interface NetWorthResponse {
  totalAssets: number;
  totalLiabilities: number;
  netWorth: number;
  assetsByClass: Record<string, number>;
  monthOverMonthChange?: number;
  yearOverYearChange?: number;
}

export interface EmergencyFundStatus {
  monthlyEssentialExpenses: number;
  requiredReserve: number;
  currentLiquidReserve: number;
  monthsCovered: number;
  shortfall: number;
  adequacy: 'ADEQUATE' | 'PARTIAL' | 'CRITICAL';
}

// ─── Advisor & Health Score ──────────────────────────────
export interface RuleResult {
  ruleName: string;
  priority: number;
  severity: 'CRITICAL' | 'WARNING' | 'INFO';
  category: string;
  trigger: string;
  evidence: Record<string, unknown>;
  action: string;
  expectedImpact: string;
  downsideRisk: string;
}

export interface SubScore {
  category: string;
  score: number;
  status: 'Green' | 'Yellow' | 'Red';
  explanation: string;
}

export interface HealthScore {
  overallScore: number;
  overallStatus: string;
  subScores: SubScore[];
}

// ─── Calendar ────────────────────────────────────────────
export interface CalendarEvent {
  id: string;
  eventType: string;
  eventDate: string;
  recurrence: string;
  description: string;
  linkedEntityType?: string;
  linkedEntityId?: string;
}

// ─── Import ──────────────────────────────────────────────
export interface ImportPreview {
  entityType: string;
  totalRows: number;
  validRows: number;
  errorRows: number;
  duplicateRows: number;
  validationErrors: string[];
}

export interface ImportResult {
  entityType: string;
  rowsImported: number;
  rowsSkipped: number;
  message: string;
}

// ─── Scenario ────────────────────────────────────────────
export interface ScenarioResult {
  originalNetWorth: number;
  scenarioNetWorth: number;
  netWorthImpact: number;
  caveat?: string;
}
