# SPEC-v1.md — Artha: Household Financial OS (Phase 1)

**Version:** 1.1
**Status:** Draft (Architecture Review Applied)
**Date:** 2026-10-17
**Scope:** Phase 1A (Financial Engine) + Phase 1B (Dashboard UX)

---

## 1. Overview

The Household Financial OS is a **single-user, local-first web application** — codenamed **Artha** (Sanskrit: wealth, purpose) — that gives a household one place to understand their complete financial picture: what they have, what they owe, where they are going, whether they are on track, and what they should do next.

Phase 1 delivers a **full-stack web app** (Spring Boot + React) running in Docker Compose locally. The system provides:

- **A normalized financial data model** covering household members, accounts, holdings across 12+ instrument types, liabilities, goals, and portfolios.
- **Deterministic calculation engines** for cash flow, goal projections, XIRR, CAGR, glide-path allocation, and rebalancing.
- **A layered advisor** (deterministic math → Java rule engine → AI explanation via Spring AI + Ollama) that produces explainable, traceable recommendations.
- **A financial cockpit UI** with dashboards, goal pages, portfolio pages, action center, timeline, what-if scenarios, and a financial calendar.
- **Data import** via manual entry, CSV templates, and basic CAS PDF parsing — with no dependency on live broker/bank APIs.

The core promise: deterministic, auditable financial intelligence with AI that explains but never invents.

---

## 2. Functional Requirements

### FR-1: Household & Identity Management

| ID | Requirement | Source |
|---|---|---|
| FR-1.1 | The system SHALL allow creating and editing a household with one or more members. Members include: primary user, spouse, children, parents, and other dependents. Each member has a defined relationship to the primary user. | [Source: FR-001, Sec 4] |
| FR-1.2 | The system SHALL model each Person with: name, relationship to household head, date of birth, gender, income sources, tax profile (PAN, tax regime — old/new), residency status, and citizenship metadata. A spouse SHALL have an independent financial identity with separate income, assets, and tax profile. **PAN numbers SHALL be encrypted at rest** in the database (using application-level AES-256 encryption) and **displayed as masked** in the UI (e.g., `ABCD****4F`). PAN is never included in log output. | [Source: FR-001, Sec 4, Sec 5, Review: S-7] |
| FR-1.3 | The system SHALL distinguish between **legal owner**, **source-of-funds owner**, **beneficiary**, **nominee**, and **joint holder** for every holding/asset. These are five distinct concepts and SHALL NOT be conflated. The system SHALL never silently reclassify ownership — changes require explicit user action and an audit record. | [Source: Sec 6] |
| FR-1.4 | The system SHALL support the scenario where a legal owner differs from the goal beneficiary (e.g., parent owns an FD that is assigned to a child's education goal). | [Source: Sec 6, Sec 10] |
| FR-1.5 | For spouse-owned assets, the system SHALL distinguish independently earned money from assets transferred by the other spouse, so the tax engine can flag clubbing implications. | [Source: Sec 6] |

### FR-2: Income, Expense & Cash Flow

| ID | Requirement | Source |
|---|---|---|
| FR-2.1 | The system SHALL allow recording income by person and period (monthly/yearly). Income types include: salary, rental, interest, dividends, bonuses, business income, capital gains, and other. Each income entry records: person (owner), type, amount, frequency, start date, end date (if applicable), and source. | [Source: FR-002] |
| FR-2.2 | The system SHALL allow recording expenses with: category, amount, frequency (one-time / monthly / quarterly / annual), owner (person), and a flag distinguishing **consumption** (groceries, utilities) from **investment outflow** (SIP, EMI principal). Recurring expenses auto-project into future months. | [Source: FR-003] |
| FR-2.3 | The system SHALL compute a **monthly cash-flow statement** per household: gross income, essential expenses, discretionary spending, EMI payments, investment outflows (SIPs), free cash flow, savings rate (savings/gross income), and debt-service ratio (EMIs/gross income). All amounts use `BigDecimal` with 2-decimal precision for currency and 6-decimal for rates. | [Source: FR-004, Grill: Java 21] <!-- SPEC-BINDING: FR-2.3 = CASH_FLOW_PRECISION --> |

### FR-3: Accounts, Holdings & Investment Analytics

| ID | Requirement | Source |
|---|---|---|
| FR-3.1 | The system SHALL provide a **pluggable instrument type catalog** stored in the database. Phase 1 instrument types: Mutual Fund, Direct Equity (Stocks), EPF, VPF, PPF, NPS (Tier I/II), FD, RD, Sovereign Gold Bond, Physical Gold, Gold ETF, Government Bonds, Corporate Bonds, SCSS, POMIS, KVP, NSC, ULIP, ESOPs/RSUs, REITs, InvITs, Crypto. Each type defines: display name, asset class, tax treatment rules, valuation method, maturity rules, liquidity profile, and required/optional fields. New types can be added via the database without code changes. When adding a holding, the user selects from a **dropdown** of instrument types — the form adapts to show fields relevant to that type. An "Other" type SHALL exist for instruments not yet cataloged. | [Source: Grill: Instruments, NFR-6.1] |
| FR-3.2 | The system SHALL model financial **Accounts** with: owner (Person), institution name, account number (optional, masked), account type (demat, savings, PF, MF folio, etc.), currency (default INR), and status (active/closed). | [Source: FR-005, Sec 5] |
| FR-3.3 | The system SHALL model **Holdings** representing ownership of an asset: linked instrument type, account, owner (Person), units/principal, acquisition cost (total and per-unit), acquisition date, current value, valuation date, and status (active/closed/matured). A holding with zero units after full redemption SHALL be marked as closed but retained in history for XIRR calculation. | [Source: FR-005, Sec 5] |
| FR-3.4 | The system SHALL maintain an **immutable transaction ledger**. Each transaction records: date, amount, type (buy/sell/SIP/dividend/interest/redemption/switch/bonus/split/transfer), owner (Person), account, holding reference, goal reference (if SIP-to-goal mapped), source-of-funds description, and notes. Transactions SHALL NOT be modified after creation — corrections are recorded as new compensating transactions. **Immutability SHALL be enforced at the database level**: the application DB user SHALL have INSERT and SELECT privileges only on the `transaction` table (no UPDATE/DELETE). A separate `transaction_correction` table records adjustments with a reference to the original transaction. | [Source: FR-005, Sec 5, Review: R-1] |
| FR-3.5 | For **Direct Equity (Stocks)**, the system SHALL track: buy/sell transactions, quantity, purchase price, current market price (manually entered or imported), realized P&L, unrealized P&L, dividends received, and dividend yield. Phase 1 does not include corporate actions (splits/bonuses) or F&O. | [Source: Grill: Stock Depth] |
| FR-3.6 | The system SHALL calculate **investment analytics** where data supports them: XIRR (extended internal rate of return), CAGR, absolute returns, rolling returns (1Y/3Y/5Y), benchmark comparison (when benchmark data is entered), risk measures (standard deviation of returns if sufficient data points exist), and concentration metrics (% of portfolio in single holding/instrument type/asset class). All calculations are deterministic and reproducible from stored transactions. | [Source: FR-011, NFR-1.1] |
| FR-3.7 | For **Mutual Funds**, the system SHALL track fund-level analytics (manually entered or imported): TER (total expense ratio), AUM, tracking difference and tracking error (for index funds), fund manager tenure, portfolio concentration (top-10 holding %), portfolio overlap with other held funds, portfolio turnover, exit load schedule, and market-cap mix (large/mid/small/micro). | [Source: FR-012] |

### FR-4: Goals, Portfolios & Glide Path

| ID | Requirement | Source |
|---|---|---|
| FR-4.1 | The system SHALL allow creating **financial goals** with: name, type (retirement / child education / house purchase / emergency fund / custom), beneficiary (Person), owner (Person funding it), target date, target amount (in today's money), assumed inflation rate, priority (Critical / Important / Aspirational), and status (active / achieved / abandoned). | [Source: FR-007] |
| FR-4.2 | The system SHALL compute **goal funding projections**: inflation-adjusted future target amount, current allocated corpus, projected corpus at target date (using expected return), funding percentage, shortfall or surplus, required monthly SIP to close shortfall, and required SIP with annual step-up %. All projections use deterministic compound-interest math. | [Source: FR-008] <!-- SPEC-BINDING: FR-4.2 = GOAL_PROJECTION_METHOD --> |
| FR-4.3 | The system SHALL implement a **hybrid glide-path engine**: (a) **Date-based** for education and house goals — target equity/safe allocation shifts as the goal date approaches (e.g., 70% equity at 8 years out → 30% equity at 1 year out); (b) **Age-based** for retirement — allocation shifts based on the person's age (e.g., equity% = 100 − age, or a custom schedule). Glide-path schedules are configurable per goal as a table of (years-to-goal OR age → target equity% / safe%). The engine computes the current year's target allocation and recommends annual contribution routing. | [Source: FR-009, Grill: Glide Path] <!-- SPEC-BINDING: FR-4.3 = GLIDE_PATH_ALGORITHM --> |
| FR-4.4 | The system SHALL implement a **rebalancing engine** that compares actual allocation (equity/safe/other split) against the glide-path target for each goal portfolio. When deviation exceeds a configurable threshold (default: 5%), the engine SHALL recommend **redirecting new SIP contributions** to the underweight asset class before recommending sales. Sales are recommended only when contribution-based rebalancing is insufficient. | [Source: FR-010] <!-- SPEC-BINDING: FR-4.4 = REBALANCE_THRESHOLD_PCT=5 --> |
| FR-4.5 | The system SHALL support **holding-to-goal mapping** via two mechanisms: (a) **SIP-to-Goal (primary)**: when setting up a new SIP/contribution, the user assigns it to a specific goal. All future contributions from that SIP feed that goal's portfolio. (b) **Virtual % allocation (fallback)**: for existing/legacy holdings or holdings serving multiple goals, the user assigns a percentage split (e.g., Motilal Mid-cap: 80% retirement, 20% education). Default is 100% to one goal. The system SHALL ensure allocations sum to 100% per holding and prevent double-counting in net-worth calculations. | [Source: Grill: Holding Split] |
| FR-4.6 | The system SHALL support **goal prioritization and conflict resolution**. When total required SIPs across all goals exceed available free cash flow, the system SHALL: (a) flag the conflict, (b) show the shortfall by priority tier (Critical → Important → Aspirational), and (c) recommend trade-offs: reduce aspirational goals, extend timelines, or increase income/reduce expenses. | [Source: FR-021] |

### FR-5: Liabilities, Emergency Fund & Net Worth

| ID | Requirement | Source |
|---|---|---|
| FR-5.1 | The system SHALL model **liabilities** with: type (mortgage / personal loan / car loan / credit card / education loan / other), lender, principal outstanding, interest rate, EMI amount, tenure (start/end dates), prepayment history, and computed interest/principal split per EMI. For mortgages, the system links to a Property entity. | [Source: FR-006] |
| FR-5.2 | The system SHALL calculate **emergency fund adequacy**: required emergency reserve (configurable multiplier × monthly essential expenses, default: 6 months), current liquid reserve (sum of savings accounts + liquid MF + FD maturing within 3 months), months covered, shortfall, and adequacy status (Adequate / Partial / Critical). | [Source: FR-014] <!-- SPEC-BINDING: FR-5.2 = EMERGENCY_FUND_MONTHS=6, EMERGENCY_LIQUID_MATURITY_DAYS=90 --> |
| FR-5.3 | The system SHALL calculate and store **net-worth history** at monthly and yearly granularity: total assets (by asset class), total liabilities, net worth, and month-over-month / year-over-year change. History is append-only; past snapshots are never modified. | [Source: FR-017] |

### FR-6: Tax, Insurance, Property & Scenarios

| ID | Requirement | Source |
|---|---|---|
| FR-6.1 | The system SHALL provide a **tax center** that tracks: tax owner per investment, source-of-funds per contribution, interest income (from FD/RD/bonds), capital gains (short-term and long-term per instrument type using Indian tax rules), clubbing flags (when spouse receives transferred asset income), EPF taxable-interest threshold (contributions above ₹2.5L/year), and supports tax-aware withdrawal sequencing (which account to withdraw from first to minimize tax impact). Tax rules SHALL be implemented as versioned configuration, not hardcoded logic. | [Source: FR-013] <!-- SPEC-BINDING: FR-6.1 = EPF_TAX_THRESHOLD=250000 --> |
| FR-6.2 | The system SHALL track **insurance policies**: type (term life / health / property / vehicle / critical illness), insured person, policy owner, sum assured/coverage amount, annual premium, premium due date, policy start/end dates, and renewal status. The system SHALL evaluate basic **coverage gaps**: (a) term life cover < 10× annual income → flag; (b) health cover < ₹10L per family → flag. Coverage gap checks run before suggesting additional investments. | [Source: FR-015] <!-- SPEC-BINDING: FR-6.2 = TERM_LIFE_MULTIPLIER=10, HEALTH_COVER_MIN=1000000 --> |
| FR-6.3 | The system SHALL model **properties** with: purchase date, purchase cost, current estimated value, linked mortgage (liability), monthly rental income (if applicable), monthly expenses (maintenance, property tax), rental yield (annual rent / current value), net equity (current value − mortgage outstanding), and linked goals. | [Source: FR-016] |
| FR-6.4 | The system SHALL support **scenario analysis** (what-if modeling). The user can modify one or more variables — inflation rate, expected return, SIP amount, SIP step-up %, retirement age, education cost, market crash (% drop in equity), job loss (months of zero income), interest rate change — and immediately see the impact on goal funding, corpus projections, cash flow, and net worth. Scenario results are computed on-the-fly and do NOT modify stored data. | [Source: FR-020] |

### FR-7: Advisor, Recommendations & Intelligence

| ID | Requirement | Source |
|---|---|---|
| FR-7.1 | The system SHALL implement a **layered advisor engine**: **Layer 1 — Deterministic calculations** (XIRR, projections, allocations, ratios) produce numeric facts. **Layer 2 — Java rule engine** evaluates conditions against facts and produces flags/recommendations with severity (Critical / Warning / Info) and category. **Layer 3 — AI explanation** (Spring AI + Ollama) takes the flags and numeric context, produces plain-English explanations with: the evidence used, the proposed action, expected effect, downside/risk, and whether user approval is required. The advisor follows a fixed recommendation priority order: (1) goal adequacy → (2) emergency fund → (3) debt management → (4) insurance gaps → (5) asset allocation → (6) contribution adequacy → (7) investment selection → (8) tax optimization. The advisor SHALL be able to say **"do nothing"** when the portfolio is on track. | [Source: FR-009, Sec 9, Grill: Calc, Grill: Rules] |
| FR-7.2 | The system SHALL store **recommendation history**: trigger (which rule/condition), evidence (input values), recommended action, expected impact, status (pending / accepted / rejected / deferred), user decision, decision timestamp, and outcome (if tracked). | [Source: FR-023] |
| FR-7.3 | The system SHALL compute a **financial health score** with explainable sub-scores: (a) Cash-flow health (savings rate, free cash flow trend), (b) Debt health (debt-service ratio, total debt-to-asset ratio), (c) Emergency readiness (months covered vs target), (d) Goal progress (weighted funding % across goals), (e) Diversification (asset-class concentration, single-holding concentration), (f) Insurance adequacy (coverage gap flags), (g) Tax readiness (tax-loss harvesting opportunities, clubbing flags). Each sub-score has a value (0–100), a status (Green/Yellow/Red), and a one-line explanation. | [Source: FR-025] |
| FR-7.4 | The system SHALL maintain a **financial calendar** with: SIP dates, EMI due dates, insurance renewal dates, FD/RD maturity dates, tax filing deadlines, loan rate reset dates, goal review dates (annual), and custom reminders. Each entry links to the relevant account/goal/entity. | [Source: FR-022] |
| FR-7.5 | The system SHALL track **estate and succession** metadata per asset/account: nominee name, joint holder(s), beneficiary designation, and will status (included / not included / no will). | [Source: FR-018] |
| FR-7.6 | The system SHALL support **document management**: attach document references (file name, file path, document type, linked entity, date, sensitivity level) to any financial entity (account, holding, goal, liability, insurance policy). Phase 1 uses local file storage. Sensitivity levels: Normal, Sensitive (passwords/credentials), Confidential (legal documents). | [Source: FR-019] |
| FR-7.7 | The system SHALL enforce **data provenance**: every manually entered or imported financial value records its source (manual / CSV import / CAS import), last-updated timestamp, and confidence status (confirmed / estimated / stale). Values not updated in 90 days SHALL be flagged as stale. | [Source: FR-024] <!-- SPEC-BINDING: FR-7.7 = STALE_DATA_THRESHOLD_DAYS=90 --> |

### FR-8: Data Import

| ID | Requirement | Source |
|---|---|---|
| FR-8.1 | The system SHALL provide **manual entry forms** for all entity types: household member, account, holding (with instrument-type-adaptive fields), transaction, income, expense, goal, liability, insurance policy, and property. Forms SHALL validate required fields, data types, and business rules (e.g., allocation sums to 100%) before saving. | [Source: Grill: Import] |
| FR-8.2 | The system SHALL support **CSV template import** per entity type. The system provides downloadable CSV templates with required/optional column headers. On upload: (a) validate column structure, (b) parse and validate each row, (c) **detect potential duplicates** by matching on (date + amount + instrument + account) — flag matches for user review, (d) show preview with row count, validation errors, and duplicate warnings, (e) if conflicts with existing data exist → show diff per conflicting record and let user choose (keep existing / use imported / skip), (f) commit approved rows in a **single database transaction** (all-or-nothing — if any row fails, no rows are committed). | [Source: Grill: Import, Grill: Import Conflict, Review: R-3, R-7] |
| FR-8.3 | The system SHALL provide a **basic CAS PDF parser** for CAMS and KFintech Consolidated Account Statements. The parser extracts: folio numbers, fund names, transaction history (purchase/redemption/switch/dividend), units, NAV, and amount. Extracted data is presented for user review before committing. If the PDF format is unrecognizable or parsing fails, the system shows a clear error and suggests manual entry as fallback. | [Source: Grill: Import] |
| FR-8.4 | When imported data **conflicts** with existing data (same holding, same date, different values), the system SHALL: (a) flag the conflict, (b) display a side-by-side diff (existing vs imported), (c) let the user choose per record: keep existing, use imported, or skip. No silent overwrites. | [Source: Grill: Import Conflict] |

---

## 3. UX Requirements (Phase 1B)

| ID | Screen | Requirement | Source |
|---|---|---|---|
| UX-1.1 | **Household Dashboard** | Show: net worth (with trend), monthly cash flow summary, goal status (progress bars per goal), asset allocation pie chart, total liabilities, emergency fund status, insurance coverage status, and top 3 prioritized actions from the advisor. Navigation to all detail pages. | [Source: UX-001] |
| UX-1.2 | **Goal Detail Page** | Show: target amount (today + inflation-adjusted), current corpus, projected corpus at target date, funding %, shortfall/surplus, assigned investments (holdings mapped to this goal), glide-path chart (target vs actual allocation over time), risk metrics, and advisor actions specific to this goal. | [Source: UX-002] |
| UX-1.3 | **Portfolio Detail Page** | Show: all holdings in the portfolio, current allocation (equity/safe/other), contribution history chart, XIRR and absolute returns, benchmark comparison (if data available), tax view (short-term vs long-term holdings, tax liability estimate), and recommendations. | [Source: UX-003] |
| UX-1.4 | **Action Center** | Show all pending advisor recommendations as plain-English action cards: "No action needed — portfolio on track", "Redirect ₹5k SIP from Flexi Cap to Nifty 50 for education goal", "Review term insurance — coverage below 10× income", "Rebalance retirement portfolio — equity 8% above target". Each card shows: action, reason, impact of action, impact of inaction. User can accept, reject, or defer each. | [Source: UX-004] |
| UX-1.5 | **Timeline** | Show future financial milestones on a horizontal timeline: goal target dates, SIP end dates, FD/RD maturities, loan payoff dates, insurance renewals, and contribution stream releases (e.g., "Son education SIP ends 2034 → ₹32.5k/month freed for retirement"). | [Source: UX-005] |
| UX-1.6 | **What-If Experience** | Provide a panel where users select a variable to change (inflation, return, SIP amount, retirement age, etc.), adjust via slider or input, and see goal/corpus impact update in real-time (or near real-time). Changes are temporary — a "Reset" button restores actuals. Show delta: "If you increase SIP by ₹5k → shortfall reduces from ₹14L to ₹6L." | [Source: UX-006] |
| UX-1.7 | **Financial Calendar** | Show a monthly calendar view with: upcoming SIP dates, EMI due dates, insurance renewals, FD maturities, tax deadlines, and custom reminders. Each entry links to the relevant entity detail page. Overdue items highlighted in red. | [Source: UX-007] |

---

## 4. Non-Functional Requirements

### NFR-1: Correctness & Precision

| ID | Requirement | Source |
|---|---|---|
| NFR-1.1 | All financial calculations SHALL be **deterministic** — given the same inputs (transactions, assumptions, dates), the system produces identical outputs. Calculations SHALL be reproducible from stored transactions and assumption sets. No randomness in core math. | [Source: NFR Sec 11] |
| NFR-1.2 | All currency amounts SHALL use `java.math.BigDecimal` with `ROUND_HALF_UP` and 2-decimal precision. All rates (interest, return, inflation) SHALL use `BigDecimal` with 6-decimal precision. `float` and `double` SHALL NOT be used for any financial calculation. | [Source: Grill: Java 21, NFR Sec 11] <!-- SPEC-BINDING: NFR-1.2 = CURRENCY_SCALE=2, RATE_SCALE=6 --> |

### NFR-2: Auditability & Provenance

| ID | Requirement | Source |
|---|---|---|
| NFR-2.1 | Every calculation result SHALL retain: the input values used, the assumption set (return rates, inflation rates, dates), the calculation timestamp, and the formula/method applied. This enables auditing and debugging of any financial output. | [Source: NFR Sec 11] |
| NFR-2.2 | Every financial data point SHALL display its **last-updated time** and **source** (manual / CSV / CAS import). The system SHALL NOT imply live/real-time data when the value was manually entered. Stale values (>90 days since update) SHALL be visually flagged. | [Source: NFR Sec 11, FR-7.7] <!-- SPEC-BINDING: NFR-2.2 = STALE_DATA_THRESHOLD_DAYS=90 --> |

### NFR-3: Privacy & Security

| ID | Requirement | Source |
|---|---|---|
| NFR-3.1 | The system SHALL follow **privacy by design**: collect only data needed for financial functions. Financial data stays local — the Ollama LLM runs locally in Docker and financial data is never sent to external cloud APIs in Phase 1. | [Source: NFR Sec 11, Grill: LLM] |
| NFR-3.2 | Phase 1 runs as a **single-user local application** with no authentication. The Docker network is internal only (no ports exposed beyond localhost). Full authentication (OAuth2/OIDC, MFA) is deferred to Phase 3 (SaaS). | [Source: Grill: Auth] |
| NFR-3.3 | Document storage SHALL respect sensitivity levels. Sensitive and Confidential documents SHALL NOT be included in any export or log output. | [Source: FR-7.6] |

### NFR-4: Portability & Import

| ID | Requirement | Source |
|---|---|---|
| NFR-4.1 | The system SHALL NOT depend on any single broker, bank, or provider (Groww, Zerodha, INDmoney, Stable Money, MF Central). All data enters through provider-neutral interfaces (manual entry, CSV, CAS PDF). | [Source: NFR Sec 11] |
| NFR-4.2 | The system SHALL support data import via manual entry forms and CSV/CAS upload even before any live API connections exist. The **canonical data schema** is the single source of truth — all import paths convert to canonical format before database insertion. | [Source: NFR Sec 11, Grill: Import] |

### NFR-5: Explainability & AI Guardrails

| ID | Requirement | Source |
|---|---|---|
| NFR-5.1 | Every recommendation SHALL link to the numeric facts and rules that triggered it. Users can trace from recommendation → rule → calculation → input data. | [Source: NFR Sec 11] |
| NFR-5.2 | The AI layer (Spring AI + Ollama) SHALL only **explain and prioritize** — it SHALL NOT invent calculations, fabricate data, or override deterministic results. If the LLM produces output inconsistent with the calculation layer, the system SHALL prefer the deterministic result and flag the inconsistency. | [Source: Grill: Calc, Sec 2 Product Principles] |

### NFR-6: Extensibility

| ID | Requirement | Source |
|---|---|---|
| NFR-6.1 | The instrument type catalog SHALL be extensible via database configuration. Adding a new instrument type (e.g., a new government scheme) SHALL NOT require code changes — only a new database entry with the type's metadata (tax treatment, fields, valuation method). | [Source: Grill: Instruments] |
| NFR-6.2 | The database schema SHALL support **multi-currency** via a currency code field (ISO 4217) on accounts and transactions. Phase 1 defaults to INR for all entities. Currency conversion logic is deferred to Phase 2+. | [Source: Grill: Currency] |

### NFR-7: Reliability — Data Integrity (Architecture Review Additions)

| ID | Requirement | Source |
|---|---|---|
| NFR-7.1 | The following operations SHALL be **atomic database transactions** (all-or-nothing): (a) CSV/CAS import commit, (b) goal allocation percentage changes across holdings, (c) net-worth snapshot capture (must reflect a consistent point-in-time view of all assets and liabilities). If any part of these operations fails, the entire operation rolls back. | [Source: Review: R-3] |
| NFR-7.2 | The system SHALL provide a **database backup mechanism**: a `pg_dump` script (`scripts/backup.sh`) that exports the PostgreSQL database to a timestamped SQL file in a user-accessible directory (`./backups/`). The README SHALL document backup and restore procedures. Automatic daily backup via cron is optional but documented. | [Source: Review: R-4] |
| NFR-7.3 | Every calculation result SHALL store the **spec version** (e.g., `SPEC-v1.1`) and **assumption set identifier** used at computation time. This enables tracing historical results to the exact formula version that produced them, even after calculation logic is updated. | [Source: Review: R-5] |
| NFR-7.4 | Flyway SHALL be configured with `outOfOrder=false` and `validateOnMigrate=true`. If a migration fails, the backend health check SHALL report migration failure status. The README SHALL document recovery steps for failed migrations. | [Source: Review: R-6] |

### NFR-8: Security Hardening (Architecture Review Additions)

| ID | Requirement | Source |
|---|---|---|
| NFR-8.1 | All REST API endpoints SHALL enforce **server-side input validation** using Bean Validation (`@Valid`, `@NotNull`, `@Size`, etc.) on request DTOs. No business logic relies solely on frontend validation. JPA parameterized queries prevent SQL injection (inherent in JPA/Hibernate). React JSX escaping prevents XSS (inherent in React). | [Source: Review: S-3] |
| NFR-8.2 | The backend SHALL configure **CORS** to allow requests only from `http://localhost:3000` (the frontend). No wildcard (`*`) origins. CORS configuration is externalized as a config property. | [Source: Review: S-4] <!-- SPEC-BINDING: NFR-8.2 = CORS_ALLOWED_ORIGINS=http://localhost:3000 --> |
| NFR-8.3 | The `.env` file SHALL be listed in `.gitignore` and SHALL NOT be committed to version control. A `.env.example` file with placeholder values SHALL be committed instead. Docker Compose references the `.env` file via `env_file` directive. | [Source: Review: S-2] |
| NFR-8.4 | All Docker Compose port bindings SHALL use `127.0.0.1:hostPort:containerPort` syntax to prevent exposure beyond localhost. Internal-only services (postgres, ollama) SHALL NOT expose ports to the host — they communicate via the Docker internal network. Only `backend` (8080) and `frontend` (3000) are exposed to the host. | [Source: Review: S-6] |
| NFR-8.5 | Document storage SHALL enforce sensitivity levels. Sensitive and Confidential documents SHALL be excluded from list/search API responses unless explicitly requested with a `includeSensitive=true` parameter. No document content or file paths SHALL appear in log output. The document storage directory SHALL have restrictive OS permissions (700). | [Source: Review: S-5, FR-7.6] |

### NFR-9: Operational Excellence (Architecture Review Additions)

| ID | Requirement | Source |
|---|---|---|
| NFR-9.1 | The system SHALL use **structured JSON logging** (SLF4J + Logback with JSON encoder). Every API request logs: endpoint, HTTP method, duration (ms), response status code, and request ID. Every calculation logs: calculation type, input hash, duration, and output summary. Every AI/Ollama call logs: prompt token count, response time, model name. Log level is configurable via the `LOG_LEVEL` environment variable (default: `INFO`). | [Source: Review: O-1] <!-- SPEC-BINDING: NFR-9.1 = LOG_LEVEL=INFO --> |
| NFR-9.2 | The backend SHALL expose a **health check endpoint** at `GET /actuator/health` (Spring Boot Actuator) with custom health indicators for: (a) PostgreSQL connectivity, (b) Flyway migration status (all migrations applied), (c) Ollama connectivity (model loaded). Docker Compose services SHALL use `healthcheck` directives: postgres (`pg_isready`), backend (`/actuator/health`), ollama (API ping), frontend (HTTP 200 on `/`). Service startup order uses `depends_on: condition: service_healthy`. | [Source: Review: O-2, O-5] |
| NFR-9.3 | All API error responses SHALL use a **consistent JSON envelope**: `{ "status": <HTTP code>, "error": "<ERROR_CODE>", "message": "<human-readable>", "details": [...], "timestamp": "<ISO-8601>" }`. Validation errors include field-level details: `{ "field": "amount", "message": "must be positive" }`. Stack traces are never exposed in API responses; they are logged server-side only. | [Source: Review: O-3] |
| NFR-9.4 | The system SHALL maintain a **calculation audit trail**: every major calculation (XIRR, goal projection, cash flow, net worth, health score) stores its audit data as a JSONB column on the result entity or in a dedicated `calculation_audit` table. Audit data includes: calculation type, input parameters (JSON), output (JSON), formula/spec version, and timestamp. This is queryable for debugging and reproducibility verification. | [Source: Review: O-4, NFR-2.1] |

---

## 5. Edge Cases

| ID | Scenario | Expected Behavior | Source |
|---|---|---|---|
| EC-1 | XIRR calculation with zero or one transaction | Return `N/A` with explanation "Insufficient transactions for XIRR calculation." Do not throw an error. | FR-3.6, Error: Calc |
| EC-2 | Goal reaches 100% funding — new SIP contribution arrives | Route contribution to next-priority underfunded goal. If all goals funded, add to free cash flow pool. Notify user via action center. | FR-4.6 |
| EC-3 | Spouse transfers asset to other spouse | Create a transfer transaction recording source-of-funds. Flag clubbing tax implication with explanation: "Income from this asset may be taxed in [original owner]'s hands under clubbing provisions." Require explicit user acknowledgment. | Sec 6, FR-6.1 |
| EC-4 | CSV import file missing required columns | Reject the entire file before any processing. Show error: "Missing required columns: [list]. Download the template for the correct format." | FR-8.2, Error: Import |
| EC-5 | CAS PDF format is unrecognizable or corrupted | Show error: "Unable to parse this CAS statement. The format may have changed. Please try manual entry or CSV import instead." Do not partially import. | FR-8.3, Error: Import |
| EC-6 | Holding is assigned to a goal that gets deleted | Prevent goal deletion if holdings are mapped to it. Require user to reassign holdings to another goal (or remove mapping) before deletion. | Error: Data Integrity |
| EC-7 | Negative cash flow (total expenses + EMIs + SIPs > income) | Allow the data entry (it reflects reality). Display a prominent warning in the dashboard. Financial health cash-flow sub-score drops to Red. Advisor flags: "Your monthly outflow exceeds income by ₹X. Review discretionary expenses or SIP commitments." | FR-2.3, FR-7.3 |
| EC-8 | Same mutual fund held across multiple goals — NAV manually updated | Update the holding's current value once. All virtual allocations to goals recalculate automatically based on their percentage shares. Net worth counts the holding once, not per-goal. | FR-4.5 |
| EC-9 | Glide-path target date has passed (goal overdue) | Freeze allocation at the terminal (most conservative) allocation. Flag for user review: "Education goal target date was [date]. Portfolio is at terminal allocation. Please update the goal status or extend the target date." | FR-4.3 |
| EC-10 | Full redemption — holding reaches zero units | Mark holding status as "closed." Retain all transactions for historical XIRR/CAGR calculation. Do not delete. Show in portfolio history view but not in active holdings. | FR-3.3 |
| EC-11 | User selects "Other" instrument type not in catalog | Allow creation with generic fields (name, value, acquisition cost, date). Prompt: "Would you like to define this as a new instrument type for future use?" If yes, create a catalog entry. | FR-3.1 |
| EC-12 | Two persons claim the same income source | Validation error on save: "This income source is already recorded for [Person A]. Each income source must have a single owner. If this is joint income, split it into separate entries." | FR-2.1, Error: Data Integrity |
| EC-13 | Scenario analysis with extreme values (e.g., -50% market crash + 20% inflation) | Calculate and display results even if they show 0% funding or negative net worth. Add a caveat: "This is an extreme scenario. Results are for planning purposes." Do not suppress or cap results. | FR-6.4 |
| EC-14 | Import CSV with duplicate transactions (same date, amount, fund) | Flag as potential duplicates. Show to user: "These [N] transactions appear to be duplicates of existing records. Import anyway / Skip duplicates." | FR-8.4, Error: Import |

---

## 6. Technology Stack

| Component | Choice | Version | Rationale | Source |
|---|---|---|---|---|
| **Backend Framework** | Spring Boot | 3.x (latest stable) | Mature ecosystem, Spring AI integration, user's Java expertise | Grill: Tech |
| **Language** | Java | 21 LTS | Virtual threads, records, pattern matching, sealed classes; LTS for production stability | Grill: Java Version |
| **AI/LLM Framework** | Spring AI | Latest stable | Native Spring integration with Ollama, OpenAI, Anthropic; swappable providers | Grill: LLM |
| **LLM Runtime** | Ollama | Latest | Local LLM, no API costs, financial data privacy; runs in Docker | Grill: LLM |
| **Frontend Framework** | React | 19 | Latest stable, hooks, concurrent features | Grill: Frontend |
| **Frontend Language** | TypeScript | 5.x | Type safety for complex financial UIs | Grill: Frontend |
| **Frontend Tooling** | Vite | Latest | Fast dev server, HMR, optimized builds | Grill: Frontend |
| **Database** | PostgreSQL | 16 | Relational integrity for financial data, JSONB, CTEs, window functions | Grill: Data Store |
| **ORM** | JPA / Hibernate | Latest (Spring Boot managed) | Type-safe entities, relationships, lazy loading | Grill: Schema |
| **Schema Migration** | Flyway | Latest (Spring Boot managed) | Versioned, repeatable migrations from day 1 | Grill: Schema |
| **Build Tool** | Gradle (Kotlin DSL) | Latest | Modern, fast, good Spring Boot support | Implied |
| **Containerization** | Docker Compose | v2 | Orchestrate all services locally | Grill: Hosting |
| **Backend Testing** | JUnit 5 + Testcontainers | Latest | Real PostgreSQL in tests, realistic integration testing | Grill: Testing |
| **Frontend Testing** | Vitest + React Testing Library | Latest | Fast, Vite-native, component testing | Grill: Testing |
| **API Style** | REST (JSON) | — | Standard, well-tooled, sufficient for this use case | Implied |
| **API Documentation** | SpringDoc OpenAPI | Latest | Auto-generated Swagger UI from annotations | Implied |

---

## 7. Acceptance Criteria

### AC-1: Household Management
- **Given** a user creates a household with 4 members (self, spouse, son age 10, daughter age 5)
- **When** the household is saved
- **Then** all members are visible with correct relationships, and each has an independent financial profile.

### AC-2: Cash Flow Calculation
- **Given** the household has: primary income ₹1,50,000/month, spouse income ₹80,000/month, essential expenses ₹60,000, discretionary ₹20,000, EMIs ₹45,000, SIPs ₹75,500
- **When** the cash-flow engine runs
- **Then** the dashboard shows: gross income ₹2,30,000; total outflow ₹2,00,500; free cash flow ₹29,500; savings rate = (2,30,000 − 80,000) / 2,30,000 computed correctly; debt-service ratio = 45,000 / 2,30,000 = 19.6%.

### AC-3: Goal Funding Projection
- **Given** son's education goal: target ₹50L (today's money), 8 years, inflation 8%, expected return 12%, current corpus ₹0, SIP ₹32,500/month
- **When** the goal funding engine computes
- **Then** inflation-adjusted target = ₹50L × (1.08)^8 = ₹92.55L (approximately); projected corpus, shortfall/surplus, and required SIP are computed and displayed.

### AC-4: Legacy Holding Virtual Allocation
- **Given** Motilal Oswal Mid-cap fund: 500 units, current value ₹8.5L, allocated 100% to retirement goal
- **When** user changes allocation to 80% retirement / 20% son education
- **Then** retirement portfolio shows ₹6.8L mid-cap exposure; son education portfolio shows ₹1.7L additional corpus; net worth counts ₹8.5L once (not double-counted).

### AC-5: Glide-Path Annual De-risking
- **Given** son education goal, 8 years to target, glide-path schedule: 70% equity at 8yr → 50% at 4yr → 30% at 1yr
- **When** one year passes (7 years remaining)
- **Then** target equity allocation adjusts to ~67.5% (interpolated); rebalancing engine flags if actual exceeds target + threshold.

### AC-6: CSV Import with Conflict
- **Given** user has manually entered holding: HDFC Nifty 50, 100 units, ₹2,500 NAV
- **When** user imports CSV showing HDFC Nifty 50, 100 units, ₹2,520 NAV
- **Then** system shows diff: "NAV differs: existing ₹2,500 vs imported ₹2,520." User selects "use imported." Holding updates to ₹2,520.

### AC-7: Advisor "Do Nothing"
- **Given** all goals are on track (funding >90%), emergency fund adequate, insurance sufficient, allocation within threshold
- **When** advisor engine runs
- **Then** action center shows: "Your finances are on track. No action needed at this time." with supporting evidence.

### AC-8: Spouse Transfer Clubbing Flag
- **Given** primary user transfers ₹5L to spouse's investment account
- **When** transaction is recorded with source-of-funds = "Transfer from [primary user]"
- **Then** tax center flags: "Income from investments funded by [primary user] may be subject to clubbing provisions under Section 64(1)(iv)."

### AC-9: Family Reference Scenario End-to-End
- **Given** the complete family scenario data from Section 10 of requirements (retirement goal, son education, daughter education, legacy Motilal holding, parent-held FD, wife's independent income)
- **When** all data is entered and all engines run
- **Then** retirement shows combined corpus with EPF; son education shows separate portfolio with de-risking; daughter education shows longer-horizon glide path; Motilal counts as retirement mid-cap; parent FD maps to education goal with correct legal owner; wife's assets are independent; no calculation errors or double-counting.

---

## 8. Test Plan

### Unit Tests (No Infrastructure Dependency)

| ID | What It Tests | Input Fixture | Expected Output | Maps To |
|---|---|---|---|---|
| UT-01 | XIRR calculation | Series of buy transactions + current value with known XIRR | XIRR matches known value (±0.01%) | FR-3.6 |
| UT-02 | XIRR with insufficient data | Zero transactions | Returns `N/A`, no exception | EC-1 |
| UT-03 | CAGR calculation | Start value, end value, years | CAGR = (end/start)^(1/years) − 1 | FR-3.6 |
| UT-04 | SIP future value projection | Monthly SIP amount, rate, years | FV matches financial calculator | FR-4.2 |
| UT-05 | SIP with annual step-up | SIP + step-up %, rate, years | FV matches stepped calculation | FR-4.2 |
| UT-06 | Goal inflation-adjusted target | Present value, inflation rate, years | FV = PV × (1 + inflation)^years | FR-4.2 |
| UT-07 | Cash-flow engine | Income entries + expense entries + EMIs + SIPs | Correct free cash flow, savings rate, debt-service ratio | FR-2.3 |
| UT-08 | Glide-path: date-based interpolation | Years-to-goal, glide schedule table | Correct target equity% at each year | FR-4.3 |
| UT-09 | Glide-path: age-based | Person age, retirement glide schedule | Correct target equity% | FR-4.3 |
| UT-10 | Glide-path: past target date | Years-to-goal = −1 | Terminal allocation, flag raised | EC-9 |
| UT-11 | Rebalancing: within threshold | Actual 48% equity, target 50%, threshold 5% | No rebalancing needed | FR-4.4 |
| UT-12 | Rebalancing: contribution redirect | Actual 60% equity, target 50%, threshold 5% | Redirect new SIP to safe bucket | FR-4.4 |
| UT-13 | Emergency fund: adequate | 8 months covered, target 6 | Status: Adequate | FR-5.2 |
| UT-14 | Emergency fund: critical | 1 month covered, target 6 | Status: Critical, shortfall calculated | FR-5.2 |
| UT-15 | Tax: clubbing flag on spouse transfer | Transfer transaction from spouse A to spouse B account | Clubbing flag raised | FR-6.1, EC-3 |
| UT-16 | Financial health sub-scores | Fixture with known values across all 7 dimensions | Each sub-score matches expected value | FR-7.3 |
| UT-17 | Goal priority conflict | 3 goals requiring ₹80k SIPs, free cash flow ₹60k | Conflict flagged, trade-offs by priority tier | FR-4.6 |
| UT-18 | Virtual allocation sums to 100% | Holding split 60%/40% across 2 goals | Allocation math correct, net worth not double-counted | FR-4.5 |
| UT-19 | BigDecimal precision | Currency operations at edge (₹0.01 differences) | No floating-point drift | NFR-1.2 |
| UT-20 | Rule engine: each rule fires correctly | Fixture per rule: emergency low, equity high, insurance gap, etc. | Correct recommendations produced | FR-7.1 |
| UT-21 | Instrument catalog: unknown type | Holding with type "Other" | Accepts generic fields, prompts to define | EC-11 |

### Integration Tests (With Testcontainers — Real PostgreSQL)

| ID | What It Tests | Scope | Maps To |
|---|---|---|---|
| IT-01 | Family acceptance scenario end-to-end | Full data load (household, members, all accounts, holdings, goals) → run all engines → verify all outputs | AC-9 |
| IT-02 | CSV import → conflict → resolution | Upload CSV with overlapping holdings → verify conflict detection → resolve → verify final state | FR-8.2, FR-8.4, AC-6 |
| IT-03 | CAS PDF parse → holdings created | Upload sample CAS PDF → verify extracted folios, funds, transactions | FR-8.3 |
| IT-04 | Goal + portfolio + glide-path E2E | Create goal → assign holdings → advance time → verify glide-path allocation changes → rebalancing recommendation | FR-4.1–4.5, AC-5 |
| IT-05 | Advisor pipeline: calc → rules → AI | Set up scenario with known deficiencies → verify rules fire → verify AI produces explanation text | FR-7.1, AC-7 |
| IT-06 | Net-worth history accumulation | Enter data across 3 months → trigger snapshots → verify history is append-only and correct | FR-5.3 |
| IT-07 | Transaction immutability | Create transaction → attempt update → verify rejected → create correction transaction | FR-3.4 |
| IT-08 | Holding lifecycle | Create → add transactions → partial redemption → full redemption → verify closed status → XIRR still calculable | FR-3.3, EC-10 |
| IT-09 | Multi-currency schema | Create account with USD currency → add holding → verify currency stored correctly | NFR-6.2 |

### Frontend Tests

| ID | What It Tests | Scope | Maps To |
|---|---|---|---|
| FT-01 | Dashboard rendering | Mock API data → verify net worth, cash flow, goal progress bars render correctly | UX-1.1 |
| FT-02 | What-if interaction | Change inflation slider → verify projected corpus updates → reset → verify return to original | UX-1.6 |
| FT-03 | Action center cards | Mock advisor output → verify cards show action, reason, accept/reject buttons | UX-1.4 |
| FT-04 | Goal detail page | Mock goal data → verify all sections render: target, corpus, funding %, glide-path chart | UX-1.2 |
| FT-05 | CSV import flow | Upload file → verify preview → resolve conflicts → verify success message | FR-8.2, UX |
| FT-06 | Instrument type dropdown | Open add-holding form → verify all instrument types in dropdown → select MF → verify MF-specific fields appear | FR-3.1, FR-8.1 |

---

## 9. Interface Definitions

### 9.1 Core REST API Endpoints (Summary)

| Method | Endpoint | Description |
|---|---|---|
| `GET/POST/PUT/DELETE` | `/api/v1/household` | Household CRUD |
| `GET/POST/PUT/DELETE` | `/api/v1/household/{id}/members` | Person management |
| `GET/POST/PUT/DELETE` | `/api/v1/accounts` | Account management |
| `GET/POST/PUT/DELETE` | `/api/v1/holdings` | Holding management |
| `GET/POST` | `/api/v1/transactions` | Transaction ledger (POST only, no PUT/DELETE) |
| `GET/POST/PUT/DELETE` | `/api/v1/income` | Income entries |
| `GET/POST/PUT/DELETE` | `/api/v1/expenses` | Expense entries |
| `GET/POST/PUT/DELETE` | `/api/v1/goals` | Goal management |
| `GET/POST/PUT/DELETE` | `/api/v1/liabilities` | Liability management |
| `GET/POST/PUT/DELETE` | `/api/v1/insurance` | Insurance policies |
| `GET/POST/PUT/DELETE` | `/api/v1/properties` | Property management |
| `GET` | `/api/v1/cashflow/{householdId}` | Cash-flow calculation |
| `GET` | `/api/v1/networth/{householdId}` | Net-worth calculation + history |
| `GET` | `/api/v1/goals/{id}/projection` | Goal funding projection |
| `GET` | `/api/v1/goals/{id}/glidepath` | Glide-path status |
| `GET` | `/api/v1/goals/{id}/rebalance` | Rebalancing recommendation |
| `GET` | `/api/v1/advisor/{householdId}` | Run advisor engine, return recommendations |
| `GET` | `/api/v1/health-score/{householdId}` | Financial health score |
| `POST` | `/api/v1/scenario` | Run what-if scenario (body = variable overrides) |
| `GET` | `/api/v1/calendar/{householdId}` | Financial calendar events |
| `POST` | `/api/v1/import/csv` | CSV upload |
| `POST` | `/api/v1/import/cas` | CAS PDF upload |
| `GET` | `/api/v1/instrument-types` | List instrument type catalog |
| `GET` | `/api/v1/recommendations/{householdId}` | Recommendation history |
| `PUT` | `/api/v1/recommendations/{id}/decision` | Record user decision on recommendation |
| `GET` | `/actuator/health` | Health check: DB, Flyway, Ollama status |

### 9.2 Standard Error Response Format

All API errors return this consistent JSON envelope (NFR-9.3):

```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [
    { "field": "amount", "message": "must be positive" },
    { "field": "targetDate", "message": "must be a future date" }
  ],
  "timestamp": "2026-10-17T18:30:00Z"
}
```

Common error codes: `VALIDATION_ERROR`, `NOT_FOUND`, `CONFLICT` (import conflict), `IMPORT_PARSE_ERROR`, `CALCULATION_ERROR`, `OLLAMA_UNAVAILABLE`, `INTERNAL_ERROR`.

### 9.3 Key Data Schemas (JSON Examples)

#### Person
```json
{
  "id": "uuid",
  "householdId": "uuid",
  "name": "Vikram",
  "relationship": "SELF",
  "dateOfBirth": "1986-05-15",
  "gender": "MALE",
  "panNumber": "ABCD****4F (masked; encrypted at rest)",
  "taxRegime": "NEW",
  "residencyStatus": "RESIDENT",
  "createdAt": "2026-10-17T10:00:00Z",
  "updatedAt": "2026-10-17T10:00:00Z"
}
```

#### Holding
```json
{
  "id": "uuid",
  "accountId": "uuid",
  "ownerId": "uuid",
  "instrumentTypeCode": "MUTUAL_FUND",
  "name": "Bandhan Nifty 50 Index Fund",
  "units": 1500.000,
  "acquisitionCost": 225000.00,
  "currentValue": 285000.00,
  "valuationDate": "2026-10-15",
  "currency": "INR",
  "status": "ACTIVE",
  "goalAllocations": [
    { "goalId": "uuid-retirement", "percentage": 60.00 },
    { "goalId": "uuid-education-son", "percentage": 40.00 }
  ],
  "provenance": {
    "source": "MANUAL",
    "lastUpdated": "2026-10-15T18:00:00Z",
    "confidence": "CONFIRMED"
  }
}
```

#### Goal
```json
{
  "id": "uuid",
  "householdId": "uuid",
  "name": "Son Higher Education",
  "type": "CHILD_EDUCATION",
  "beneficiaryId": "uuid-son",
  "ownerId": "uuid-self",
  "targetDate": "2034-06-01",
  "targetAmountToday": 5000000.00,
  "inflationRate": 0.080000,
  "priority": "CRITICAL",
  "status": "ACTIVE",
  "glidePath": {
    "type": "DATE_BASED",
    "schedule": [
      { "yearsToGoal": 8, "equityPct": 70.00, "safePct": 30.00 },
      { "yearsToGoal": 4, "equityPct": 50.00, "safePct": 50.00 },
      { "yearsToGoal": 1, "equityPct": 30.00, "safePct": 70.00 },
      { "yearsToGoal": 0, "equityPct": 20.00, "safePct": 80.00 }
    ]
  }
}
```

#### Recommendation
```json
{
  "id": "uuid",
  "householdId": "uuid",
  "trigger": "REBALANCE_THRESHOLD_EXCEEDED",
  "ruleName": "EquityOverweightRule",
  "severity": "WARNING",
  "category": "ASSET_ALLOCATION",
  "evidence": {
    "goalName": "Retirement",
    "actualEquityPct": 72.5,
    "targetEquityPct": 60.0,
    "threshold": 5.0
  },
  "action": "Redirect next 3 months of SIP contributions to Nifty 50 Index Fund (safe bucket) instead of Flexi Cap Fund.",
  "expectedImpact": "Portfolio equity drops to ~65% within 3 months via contribution routing. No sales needed.",
  "downsideRisk": "Temporary underperformance if equity rally continues during redirect period.",
  "aiExplanation": "Your retirement portfolio has drifted to 72.5% equity, above the 60% target for your age...",
  "status": "PENDING",
  "userDecision": null,
  "decisionTimestamp": null,
  "createdAt": "2026-10-17T18:00:00Z"
}
```

---

## 10. Infrastructure

### 10.1 Docker Compose Services

| Service | Image | Purpose | Ports | Exposed to Host? |
|---|---|---|---|---|
| `backend` | Custom (Spring Boot) | Java 21 API server | `127.0.0.1:8080:8080` | ✅ Yes (API) |
| `frontend` | Custom (Vite/Nginx) | React SPA serving | `127.0.0.1:3000:80` | ✅ Yes (UI) |
| `postgres` | `postgres:16-alpine` | Financial data store | `5432` (internal only) | ❌ No — Docker network only |
| `ollama` | `ollama/ollama` | Local LLM runtime | `11434` (internal only) | ❌ No — Docker network only |

All services include `healthcheck` directives. Startup order uses `depends_on: condition: service_healthy`.

### 10.2 Infrastructure Requirements

| ID | Resource | Requirement | Source |
|---|---|---|---|
| IR-1 | `docker-compose.yml` | Orchestrates all 4 services with health checks, depends_on, and restart policies. All ports bound to `127.0.0.1` only. | Grill: Hosting |
| IR-2 | PostgreSQL volume | Named Docker volume `hfos-pgdata` for persistent data across container restarts. | Grill: Data Store |
| IR-3 | Ollama model volume | Named Docker volume `hfos-ollama-models` to persist downloaded LLM model. | Grill: LLM |
| IR-4 | Flyway migrations | `db/migration/` directory with versioned SQL files. First migration creates all Phase 1 tables. | Grill: Schema |
| IR-5 | Document storage volume | Named Docker volume `hfos-documents` for uploaded financial documents. | FR-7.6 |
| IR-6 | Environment configuration | `.env` file for: DB credentials, Ollama model name, API port, log level. No secrets in Docker image. `.env` is in `.gitignore`; `.env.example` with placeholder values is committed. | Implied, Review: S-2 |
| IR-7 | Backup script | `scripts/backup.sh` runs `pg_dump` to `./backups/artha-YYYYMMDD-HHMMSS.sql`. README documents backup/restore procedures. | Review: R-4 |
| IR-8 | `.gitignore` | Excludes: `.env`, `backups/`, `node_modules/`, `build/`, `*.class`, IDE files. | Review: S-2 |

### 10.3 Configuration Variables

| Variable | Default | Owner | Description |
|---|---|---|---|
| `POSTGRES_DB` | `hfos` | Docker | Database name |
| `POSTGRES_USER` | `hfos` | Docker | Database user |
| `POSTGRES_PASSWORD` | `hfos_local` | Docker | Database password (local only) |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/hfos` | Backend | JDBC connection URL |
| `SPRING_AI_OLLAMA_BASE_URL` | `http://ollama:11434` | Backend | Ollama API endpoint |
| `SPRING_AI_OLLAMA_CHAT_MODEL` | `llama3.1` | Backend | LLM model for AI explanations <!-- SPEC-BINDING: IR-6 = OLLAMA_MODEL --> |
| `REBALANCE_THRESHOLD_PCT` | `5` | Backend | Rebalancing trigger threshold <!-- SPEC-BINDING: FR-4.4 --> |
| `EMERGENCY_FUND_MONTHS` | `6` | Backend | Target emergency fund months <!-- SPEC-BINDING: FR-5.2 --> |
| `STALE_DATA_THRESHOLD_DAYS` | `90` | Backend | Days before data is flagged stale <!-- SPEC-BINDING: FR-7.7 --> |
| `TERM_LIFE_MULTIPLIER` | `10` | Backend | Term life coverage gap multiplier <!-- SPEC-BINDING: FR-6.2 --> |
| `HEALTH_COVER_MIN_INR` | `1000000` | Backend | Minimum health insurance coverage <!-- SPEC-BINDING: FR-6.2 --> |
| `EPF_TAX_THRESHOLD_INR` | `250000` | Backend | EPF contribution taxable threshold <!-- SPEC-BINDING: FR-6.1 --> |
| `CURRENCY_SCALE` | `2` | Backend | BigDecimal scale for currency <!-- SPEC-BINDING: NFR-1.2 --> |
| `RATE_SCALE` | `6` | Backend | BigDecimal scale for rates <!-- SPEC-BINDING: NFR-1.2 --> |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Backend | Allowed CORS origins (comma-separated) <!-- SPEC-BINDING: NFR-8.2 --> |
| `LOG_LEVEL` | `INFO` | Backend | Root log level (DEBUG/INFO/WARN/ERROR) <!-- SPEC-BINDING: NFR-9.1 --> |
| `PAN_ENCRYPTION_KEY` | *(generated locally)* | Backend | AES-256 key for PAN encryption at rest. Generated on first run if absent. |

---

## 11. Development Phases (Delivery Batches)

### Batch 1: Foundation (FR-1.x, FR-2.x)
**Focus:** Household data model, income, expenses, cash-flow engine.
**Depends on:** Nothing.
**Deliverables:**
- Database schema: household, person, income, expense tables + Flyway migrations
- REST APIs: household CRUD, person CRUD, income CRUD, expense CRUD
- Cash-flow engine service
- Unit tests: UT-07, UT-19
- Docker Compose with backend + postgres

### Batch 2: Investments (FR-3.x)
**Focus:** Accounts, holdings, transactions, instrument catalog, analytics.
**Depends on:** Batch 1 (Person, Household exist).
**Deliverables:**
- Database schema: account, instrument_type, holding, transaction tables
- Instrument type catalog seeded with all 12+ types
- REST APIs: account CRUD, holding CRUD, transaction POST/GET
- XIRR, CAGR, rolling returns calculation services
- Unit tests: UT-01 through UT-03, UT-21
- Integration test: IT-07 (transaction immutability), IT-08 (holding lifecycle)

### Batch 3: Goals (FR-4.x)
**Focus:** Goal management, funding projections, glide-path, rebalancing, holding-to-goal mapping.
**Depends on:** Batch 1, Batch 2 (holdings and persons exist).
**Deliverables:**
- Database schema: goal, portfolio, goal_allocation tables
- Goal funding projection engine
- Glide-path engine (hybrid: date-based + age-based)
- Rebalancing engine
- Holding-to-goal mapping (SIP primary + virtual allocation)
- Unit tests: UT-04 through UT-06, UT-08 through UT-12, UT-17, UT-18
- Integration test: IT-04 (goal + portfolio E2E)

### Batch 4: Liabilities & Net Worth (FR-5.x)
**Focus:** Liability tracking, emergency fund, net-worth history.
**Depends on:** Batch 1 (cash flow for emergency fund calc).
**Deliverables:**
- Database schema: liability, net_worth_snapshot tables
- Emergency fund calculator
- Net-worth engine with monthly snapshots
- Unit tests: UT-13, UT-14
- Integration test: IT-06 (net-worth history)

### Batch 5: Advanced Modules (FR-6.x)
**Focus:** Tax center, insurance, property, scenario analysis.
**Depends on:** Batch 1–4 (all core entities exist).
**Deliverables:**
- Tax calculation service with clubbing detection
- Insurance module with coverage gap analysis
- Property module
- Scenario analysis engine (what-if computation)
- Unit tests: UT-15
- Integration test: extend IT-01 family scenario with tax/insurance

### Batch 6: Intelligence (FR-7.x)
**Focus:** Advisor engine, recommendations, financial health, calendar, estate, documents, provenance.
**Depends on:** Batch 1–5 (all data and calculations exist for rules to evaluate).
**Deliverables:**
- Java rule engine (Rule interface + all rule implementations)
- Spring AI + Ollama integration for AI explanation layer
- Financial health score engine (7 sub-scores)
- Financial calendar service
- Recommendation CRUD + decision tracking
- Estate/succession metadata
- Document management (local storage)
- Data provenance tracking
- Docker Compose adds Ollama service
- Unit tests: UT-16, UT-20
- Integration test: IT-05 (advisor pipeline)

### Batch 7: Import (FR-8.x)
**Focus:** CSV import, CAS PDF parser, conflict resolution.
**Depends on:** Batch 1–3 (entities to import into).
**Deliverables:**
- CSV template definitions per entity type
- CSV upload, validate, preview, conflict-detect, commit pipeline
- Basic CAS PDF parser (CAMS + KFintech format)
- Import conflict UI (side-by-side diff)
- Integration tests: IT-02 (CSV import), IT-03 (CAS PDF)

### Batch 8: UX (UX-1.x)
**Focus:** All frontend screens.
**Depends on:** Batch 1–7 (all APIs exist).
**Deliverables:**
- React app with: Dashboard, Goal Detail, Portfolio Detail, Action Center, Timeline, What-If, Calendar
- Frontend tests: FT-01 through FT-06
- Docker Compose adds frontend service (Nginx)

---

## 12. Spec Change Log

| Date | NFR/FR | Old Value | New Value | Reason | Changed By |
|------|--------|-----------|-----------|--------|------------|
| 2026-10-17 | Multiple | — | NFR-7.x, NFR-8.x, NFR-9.x added | Architecture Review findings (13 gaps) | Agent |
| 2026-10-17 | FR-1.2 | PAN stored plaintext | PAN encrypted at rest, masked in UI | Review: S-7 (PII protection) | Agent |
| 2026-10-17 | FR-3.4 | Application-level immutability | DB-level immutability (INSERT/SELECT only) | Review: R-1 (data integrity) | Agent |
| 2026-10-17 | FR-8.2 | Basic import pipeline | Atomic transaction + duplicate detection | Review: R-3, R-7 | Agent |

*This table is populated when code-binding values change after the spec is locked.*

---

## 13. Architecture Decisions (from Phase 3 Review)

### 13.1 Coding Patterns — Adopted

| Decision | Pattern | Rationale |
|---|---|---|
| AD-1 | **`Rule` interface** for advisor rules | 15+ rules with a common evaluate pattern. Open/Closed principle justified. Spring auto-discovery. |
| AD-2 | **`BaseEntity` abstract class** (`@MappedSuperclass`) | 15+ entities share id (UUID), createdAt, updatedAt. DRY: avoids 150+ lines of boilerplate. |
| AD-3 | **`InstrumentTypeHandler` interface** | 23 instrument types, each with distinct validation, tax, and valuation logic. Strategy pattern justified. |
| AD-4 | **Repository per entity** (Spring Data JPA) | Standard Spring pattern, zero boilerplate cost. |
| AD-5 | **Domain-grouped services** (~8-10 service classes) | Group by domain, not entity. E.g., `CashFlowService` handles both income and expenses. |
| AD-6 | **Java 21 `record` types for DTOs** | Immutable, concise. Manual `toDto()`/`toEntity()` mapping. No MapStruct in Phase 1. |

### 13.2 Coding Patterns — Rejected (KISS Filter)

| Decision | Pattern | Why Rejected |
|---|---|---|
| AD-7 | ~~Interfaces for calculator classes~~ | Each calculator (XIRR, CAGR, SIP) has exactly one implementation. Interface adds indirection with no polymorphic benefit. Use plain `@Service` classes. |
| AD-8 | ~~Event sourcing for transactions~~ | Immutable append-only table achieves the same auditability without event-sourcing infrastructure. Over-engineering for Phase 1. |
| AD-9 | ~~CQRS (separate read/write models)~~ | Single-user local app with one database. No read/write scaling divergence. Standard Spring MVC suffices. |
| AD-10 | ~~Strict DDD aggregate enforcement~~ | Household is the natural root, but strict aggregate boundaries add ceremony without benefit at this scale. JPA lazy-loading handles traversal. |

### 13.3 Java 21 Feature Adoption

| Feature | Adopt? | Where Used |
|---|---|---|
| `record` types | ✅ Yes | DTOs, API request/response types, value objects |
| `sealed` classes/interfaces | ✅ Yes | `GlidePathType`, `GoalType`, `InstrumentCategory` — compiler-enforced exhaustive `switch` |
| Pattern matching (`switch`, `instanceof`) | ✅ Yes | Rule engine dispatch, instrument type handling, import format detection |
| Virtual threads | ⚠️ Enable, don't architect around | `spring.threads.virtual.enabled=true` for future-proofing |
| Text blocks (`"""`) | ✅ Yes | SQL queries, AI prompt templates, error messages |
| `Optional` for nullable returns | ✅ Yes | Repository and service return types. Never return `null`. |
| Stream API | ✅ Yes | Financial aggregations (sum, group-by, filter across holdings, goals) |

---

## 14. Reference Data

### 14.1 Instrument Type Catalog (Phase 1 Seed)

| Code | Display Name | Asset Class | Tax Treatment (Summary) | Valuation Method |
|---|---|---|---|---|
| `MUTUAL_FUND` | Mutual Fund | Equity/Debt/Hybrid (per scheme) | STCG/LTCG per holding period + scheme type | NAV × units |
| `DIRECT_EQUITY` | Stocks | Equity | STCG 20% (<1yr), LTCG 12.5% (>1yr, >₹1.25L) | Market price × quantity |
| `EPF` | Employee Provident Fund | Debt (safe) | EEE (exempt up to ₹2.5L/yr contribution) | Balance (statement) |
| `VPF` | Voluntary Provident Fund | Debt (safe) | Same as EPF | Balance (statement) |
| `PPF` | Public Provident Fund | Debt (safe) | EEE (fully exempt) | Balance (statement) |
| `NPS_TIER1` | NPS Tier I | Mixed | 80CCD(1B) ₹50k extra; partial tax on withdrawal | NAV × units |
| `NPS_TIER2` | NPS Tier II | Mixed | No special tax benefit | NAV × units |
| `FD` | Fixed Deposit | Debt | Interest taxed as income; TDS if >₹40k/yr | Principal + accrued interest |
| `RD` | Recurring Deposit | Debt | Interest taxed as income | Maturity value projection |
| `SGB` | Sovereign Gold Bond | Gold | 2.5% interest taxed; LTCG exempt on maturity (8yr) | Gold price × grams + accrued interest |
| `PHYSICAL_GOLD` | Physical Gold | Gold | LTCG 12.5% (>2yr) | Weight × market price per gram |
| `GOLD_ETF` | Gold ETF | Gold | LTCG 12.5% (>1yr) | NAV × units |
| `GOVT_BOND` | Government Bond / G-Sec | Debt | Interest taxed; LTCG on sale | Face value or market price |
| `CORP_BOND` | Corporate Bond | Debt | Interest taxed; LTCG on sale | Face value or market price |
| `SCSS` | Senior Citizens Savings Scheme | Debt | Interest taxed; 80C deduction on investment | Principal + accrued interest |
| `POMIS` | Post Office MIS | Debt | Interest taxed monthly | Principal (fixed) |
| `KVP` | Kisan Vikas Patra | Debt | Maturity amount taxed | Maturity value projection |
| `NSC` | National Savings Certificate | Debt | Interest taxed (but 80C); reinvested interest gets 80C | Face value + accrued interest |
| `ESOP` | ESOPs / RSUs | Equity | Perquisite tax on exercise; STCG/LTCG on sale | Market price × vested units |
| `REIT` | REIT | Real Estate | Dividend partially taxable; LTCG on units | NAV × units |
| `INVIT` | InvIT | Infrastructure | Similar to REIT | NAV × units |
| `CRYPTO` | Cryptocurrency | Digital Asset | 30% flat tax on gains; 1% TDS on transfer | Market price × quantity |
| `ULIP` | ULIP | Insurance + Investment | Exempt if premium <₹2.5L/yr; else taxable | Fund value (statement) |
| `OTHER` | Other | User-defined | Manual tax treatment entry | User-entered value |

### 14.2 Advisor Rule Priority Order

| Priority | Category | Example Rule |
|---|---|---|
| 1 | Goal Adequacy | "Son education goal is 62% funded — increase SIP or extend timeline" |
| 2 | Emergency Fund | "Emergency fund covers 2 months — target is 6 months" |
| 3 | Debt Management | "Debt-service ratio is 45% — consider prepaying highest-rate loan" |
| 4 | Insurance Gaps | "Term life cover is 5× income — recommended minimum is 10×" |
| 5 | Asset Allocation | "Retirement equity allocation is 72% — target is 60% for your age" |
| 6 | Contribution Adequacy | "Required SIP for all goals is ₹80k — available free cash flow is ₹60k" |
| 7 | Investment Selection | "Index fund tracking error is 1.2% — consider switching to lower-cost alternative" |
| 8 | Tax Optimization | "₹1.5L in short-term equity gains — consider tax-loss harvesting from [holding]" |

---

*End of SPEC-v1.md*
