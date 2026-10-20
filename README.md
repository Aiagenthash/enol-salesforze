# ENOL SALESFORZE - ENTERPRISE FIELD FORCE AUTOMATION & TERRITORY MANAGEMENT PLATFORM
**Phase 1: Backend Database, REST API & Webhook Gateway**

An enterprise-grade, high-performance Field Force Automation (FFA), Beat Route Accounting, and Territory Management backend built for automotive lubricant sales organizations operating across Pakistan.

---

## Architecture Highlights
- **PostgreSQL 16 + PostGIS**: Multi-polygon territory boundaries, geocoded customer points, GIST spatial indexes, and automatic geometry synchronization triggers.
- **5-Tier Organizational Hierarchy**: `DIRECTOR` -> `ZSM` -> `ASM` -> `SALES_REP` -> `ADMIN` with materialized paths (`hierarchy_path`, e.g. `/<director_id>/<zsm_id>/<asm_id>/<rep_id>/`).
- **12 Domain REST API**: Complete CRUD and lifecycle business logic covering Users, Districts, Beats, Clients, Mechanics, Products, Orders, Attendance, Visits, Location Breadcrumbs, TADA Claims, KPIs, and Competitions.
- **Delta-Sync**: `GET /api/v1/sync/delta?since=ISO8601` for offline-first field mobile applications.
- **Bulk Data Exports**: `GET /api/v1/exports/:entity` producing streaming CSV or formatted JSON.
- **Outbound Webhooks Gateway**: Asynchronous dispatcher with `HMAC-SHA256` signatures (`X-Webhook-Signature`), timestamp validation, exponential backoff retries, and delivery logging.
- **OpenAPI 3.1 Documentation**: Interactive Swagger UI at `/api/v1/docs` and exported spec at `docs/openapi_v1.yaml`.
- **Dual Authentication**: Bearer JWT Tokens (`Authorization: Bearer <jwt>`) and API Keys (`X-API-Key: <key>`).

---

## Directory Structure
```
ENOL SALESFORZE/
├── db/
│   └── migrations/
│       ├── 001_initial_schema.sql         # PostGIS extensions, all 19 core tables, indexes, constraints
│       ├── 002_postgis_and_triggers.sql   # PostGIS triggers, auto updated_at, hierarchy trigger, TADA calculations
│       └── 003_seed_data.sql              # 5-tier seed users, Pakistan districts, beats, clients, SKUs, schemes
├── backend-api/
│   ├── src/
│   │   ├── config/                        # Database pool and environment configuration
│   │   ├── middleware/                    # JWT & X-API-Key auth, RBAC & hierarchy scoping
│   │   ├── routes/                        # Fastify route controllers for all 12 domains + sync + exports + webhooks
│   │   ├── services/                      # Webhook dispatcher, Delta Sync, Bulk Export services
│   │   ├── scripts/                       # Migration runner, seed runner, OpenAPI generator
│   │   ├── tests/                         # Automated test suite (Fastify injection tests)
│   │   ├── types/                         # TypeScript interfaces and domain schemas
│   │   ├── app.ts                         # Fastify application builder with Swagger OpenAPI 3.1
│   │   ├── server.ts                      # Server bootstrap and graceful shutdown
│   │   └── index.ts                       # Entrypoint
│   ├── package.json
│   └── tsconfig.json
├── docs/
│   └── openapi_v1.yaml                    # Full OpenAPI 3.1 specification (67KB+)
└── package.json                           # Root orchestration package
```

---

## 5-Tier Organizational Hierarchy
The platform organizes sales operations into five clear levels:
1. `DIRECTOR`: Country-wide visibility, national volume and revenue strategy.
2. `ZSM` (Zonal Sales Manager): Regional oversight across provincial zones (e.g. Central Punjab, South Sindh).
3. `ASM` (Area Sales Manager): District-level operations and target monitoring (e.g. Lahore, Faisalabad).
4. `SALES_REP` (Territory Sales Officer / Order Booker): Day-to-day beat visits, client ordering, cash recovery.
5. `ADMIN`: Master catalog administration, ERP connectors, and webhook integrations.

### Materialized Path & Database Trigger
The database automatically synchronizes `hierarchy_path` on row insertion and updates:
```sql
-- Manager update automatically recalculates subordinate tree
UPDATE users SET manager_id = '33333333-3333-3333-3333-333333333333' WHERE id = '44444444-4444-4444-4444-444444444444';
-- Path becomes: /11111111-1111-1111-1111-111111111111/22222222-2222-2222-2222-222222222222/33333333-3333-3333-3333-333333333333/44444444-4444-4444-4444-444444444444/
```
Subordinate queries are fast and indexable via:
```sql
SELECT * FROM users WHERE hierarchy_path LIKE '/<manager_path>/%';
```

---

## 12 Domain Endpoints (`/api/v1/*`)

| Domain | Route Prefix | Key Endpoints | Description |
|---|---|---|---|
| **Auth** | `/api/v1/auth` | `POST /login`, `GET /me`, `POST /api-keys` | Dual auth (JWT / API Key) |
| **Users** | `/api/v1/users` | `GET /`, `POST /`, `GET /:id`, `GET /:id/subordinates`, `PUT /:id` | 5-Tier hierarchy management |
| **Territories** | `/api/v1/territories` | `GET /districts`, `POST /districts`, `GET /beats`, `POST /beats` | PostGIS districts & daily rep beats |
| **Clients** | `/api/v1/clients` | `GET /`, `POST /`, `GET /:id`, `GET /:id/ledger`, `POST /:id/ledger` | Retail CRM, credit limits & ledgers |
| **Mechanics** | `/api/v1/mechanics` | `GET /`, `POST /`, `GET /:id`, `POST /:id/points` | QR code influencer loyalty program |
| **Products** | `/api/v1/products` | `GET /`, `POST /`, `GET /:id`, `PUT /:id`, `DELETE /:id` | Lubricant SKUs, cartons & trade schemes |
| **Orders** | `/api/v1/orders` | `GET /`, `POST /`, `GET /:id`, `PUT /:id/status`, `GET /:id/items` | Idempotent orders & credit holds |
| **Attendance** | `/api/v1/attendance` | `GET /`, `POST /start-shift`, `POST /:id/end-shift` | Shift selfies & odometer telemetry |
| **Visits** | `/api/v1/visits` | `GET /`, `POST /check-in`, `PUT /:id/check-out` | Geofence audit, mock GPS, recoveries |
| **Breadcrumbs** | `/api/v1/breadcrumbs` | `GET /`, `POST /` (batch), `GET /user/:id/route` | GPS tracking stream & GeoJSON playback |
| **TADA Claims** | `/api/v1/tada-claims` | `GET /`, `POST /`, `GET /:id`, `PUT /:id/review` | Odometer vs GPS variance fraud audit |
| **Targets/KPIs** | `/api/v1/kpis` | `GET /`, `GET /user/:id`, `POST /`, `PUT /:id` | Target vs achieved volume/revenue scorecards |
| **Competitions**| `/api/v1/competitions`| `GET /`, `GET /:id/leaderboard`, `POST /`, `POST /:id/scores` | Sales races & live gamification boards |
| **Delta Sync** | `/api/v1/sync` | `GET /delta?since=ISO8601` | Incremental offline sync payload |
| **Exports** | `/api/v1/exports` | `GET /:entity?format=csv\|json&from=...&to=...` | High-throughput data warehouse exports |
| **Webhooks** | `/api/v1/webhooks` | `GET /subscriptions`, `POST /subscriptions`, `GET /logs`, `POST /test-dispatch` | Outbound HMAC-SHA256 event gateway |

---

## Outbound Webhook Verification
Recipient services verify webhook authenticity by inspecting `X-Webhook-Signature` and `X-Webhook-Timestamp`:
```typescript
import crypto from 'crypto';

function verifyWebhook(rawPayload: string, signatureHeader: string, timestamp: string, secret: string): boolean {
  const expectedSig = crypto
    .createHmac('sha256', secret)
    .update(`${timestamp}.${rawPayload}`)
    .digest('hex');
    
  return signatureHeader === `sha256=${expectedSig}`;
}
```

---

## Getting Started

### 1. Requirements
- Node.js >= 18 (Tested on v24.19)
- PostgreSQL 16 + PostGIS

### 2. Environment Configuration
Edit `backend-api/.env`:
```env
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/enol_salesforze
JWT_SECRET=super_secret_jwt_key_enol_salesforze_2026_enterprise
PORT=4000
```

### 3. Database Migration & Seeding
```bash
# Run schema and trigger migrations
npm run db:migrate

# Seed 5-tier organization, districts, beats, products, and mechanics
npm run db:seed
```

### 4. Running the API Server
```bash
# Development mode with hot-reload
npm run dev

# Production build and run
npm run build
npm start
```

### 5. Running Automated Tests
```bash
npm test
```
Outputs:
```
====================================================
TEST RESULTS: 16 PASSED, 0 FAILED
====================================================
```

### 6. Interactive API Documentation
Open your browser at:
`http://localhost:4000/api/v1/docs`
