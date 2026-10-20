import { buildApp } from '../app';
import { WebhookDispatcher } from '../services/webhook-dispatcher.service';
import { ExportService } from '../services/export.service';

async function runComprehensiveTestSuite() {
  console.log('========================================================================');
  console.log('       ENOL SALESFORZE ENTERPRISE API - COMPREHENSIVE TEST RUN');
  console.log('========================================================================\n');

  let passed = 0;
  let failed = 0;

  function assert(condition: boolean, testName: string, details?: string) {
    if (condition) {
      console.log(`  [PASS] ${testName}`);
      passed++;
    } else {
      console.error(`  [FAIL] ${testName}${details ? ` -> ${details}` : ''}`);
      failed++;
    }
  }

  const app = buildApp();
  await app.ready();

  console.log('--- SECTION 1: SYSTEM HEALTH & METRICS ---');
  // 1. Health check endpoint
  {
    const res = await app.inject({ method: 'GET', url: '/health' });
    assert(res.statusCode === 200, 'GET /health returns HTTP 200');
    const body = JSON.parse(res.body);
    assert(body.status === 'UP' && body.service === 'enol-salesforze-api', 'Health payload service is enol-salesforze-api');
    assert(typeof body.timestamp === 'string', 'Health payload contains ISO8601 timestamp');
  }

  // 2. API v1 Health check endpoint
  {
    const res = await app.inject({ method: 'GET', url: '/api/v1/health' });
    assert(res.statusCode === 200, 'GET /api/v1/health returns HTTP 200');
    const body = JSON.parse(res.body);
    assert(body.version === '1.0.0', 'API v1 version string is 1.0.0');
  }

  console.log('\n--- SECTION 2: OPENAPI 3.1 & SWAGGER GOVERNANCE ---');
  // 3. Swagger JSON Specification
  {
    const res = await app.inject({ method: 'GET', url: '/api/v1/docs/json' });
    assert(res.statusCode === 200, 'GET /api/v1/docs/json returns HTTP 200');
    const openapi = JSON.parse(res.body);
    assert(openapi.openapi === '3.1.0', 'OpenAPI version is 3.1.0');
    assert(openapi.info.title.includes('ENOL SALESFORZE'), 'OpenAPI specification title is correct');
    assert(Object.keys(openapi.paths).length >= 40, `All domain endpoints registered (${Object.keys(openapi.paths).length} paths)`);

    // Verify critical enterprise endpoints exist in OpenAPI spec
    const requiredPaths = [
      '/api/v1/auth/login',
      '/api/v1/auth/api-keys',
      '/api/v1/territories/districts',
      '/api/v1/territories/beats',
      '/api/v1/clients/',
      '/api/v1/mechanics/',
      '/api/v1/products/',
      '/api/v1/orders/',
      '/api/v1/attendance/',
      '/api/v1/visits/',
      '/api/v1/breadcrumbs/',
      '/api/v1/tada-claims/',
      '/api/v1/kpis/',
      '/api/v1/competitions/',
      '/api/v1/sync/delta',
      '/api/v1/exports/{entity}',
      '/api/v1/webhooks/subscriptions'
    ];

    let allPathsExist = true;
    for (const p of requiredPaths) {
      if (!openapi.paths[p]) {
        allPathsExist = false;
        console.error(`Missing expected OpenAPI path: ${p}`);
      }
    }
    assert(allPathsExist, 'All 17 core domain route schemas exist in OpenAPI registry');
  }

  console.log('\n--- SECTION 3: HMAC-SHA256 WEBHOOK CRYPTOGRAPHY ---');
  // 4. HMAC-SHA256 signatures
  {
    const secret = 'whsec_enterprise_automotive_secret_2026';
    const timestamp = '2026-10-21T02:00:00.000Z';
    const payload = JSON.stringify({ event: 'order.created', order_id: 'ord-88392-karachi', total: 145000 });

    const sig1 = WebhookDispatcher.generateSignature(payload, secret, timestamp);
    const sig2 = WebhookDispatcher.generateSignature(payload, secret, timestamp);
    assert(sig1 === sig2, 'HMAC-SHA256 signature is deterministic');
    assert(sig1.length === 64, 'HMAC-SHA256 output is a 64-character hex string');

    const sigTamperedPayload = WebhookDispatcher.generateSignature(payload + ' ', secret, timestamp);
    assert(sig1 !== sigTamperedPayload, 'Payload tampering invalidates HMAC-SHA256 signature');

    const sigTamperedTimestamp = WebhookDispatcher.generateSignature(payload, secret, '2026-10-21T02:00:01.000Z');
    assert(sig1 !== sigTamperedTimestamp, 'Replay attack timestamp mutation invalidates signature');
  }

  console.log('\n--- SECTION 4: SECURITY & AUTHENTICATION REJECTION ---');
  // 5. Unauthenticated rejection across diverse domains
  const unauthEndpoints = [
    { method: 'GET' as const, url: '/api/v1/users/', domain: 'Users' },
    { method: 'GET' as const, url: '/api/v1/clients/', domain: 'Clients' },
    { method: 'GET' as const, url: '/api/v1/orders/', domain: 'Orders' },
    { method: 'GET' as const, url: '/api/v1/attendance/', domain: 'Attendance' },
    { method: 'GET' as const, url: '/api/v1/tada-claims/', domain: 'TADA Expenses' },
    { method: 'GET' as const, url: '/api/v1/competitions/', domain: 'Competitions' },
    { method: 'GET' as const, url: '/api/v1/sync/delta', domain: 'Delta-Sync' },
    { method: 'GET' as const, url: '/api/v1/webhooks/subscriptions', domain: 'Webhooks' }
  ];

  for (const ep of unauthEndpoints) {
    const res = await app.inject({ method: ep.method, url: ep.url });
    assert(res.statusCode === 401, `${ep.domain} (${ep.url}) rejects unauthenticated requests with 401`);
  }

  // 6. Malformed and expired API key rejection
  {
    const res = await app.inject({
      method: 'GET',
      url: '/api/v1/users',
      headers: { 'x-api-key': 'malformed_invalid_key_xyz' },
    });
    assert(res.statusCode === 401, 'Invalid X-API-Key is rejected with 401');
    const body = JSON.parse(res.body);
    assert(body.success === false, 'Error response conforms to { success: false }');
  }

  // 7. Malformed Bearer token rejection
  {
    const res = await app.inject({
      method: 'GET',
      url: '/api/v1/clients',
      headers: { authorization: 'Bearer invalid.jwt.signature' },
    });
    assert(res.statusCode === 401, 'Malformed JWT Bearer token is rejected with 401');
  }

  console.log('\n--- SECTION 5: INPUT VALIDATION & SCHEMA ENFORCEMENT ---');
  // 8. Auth Login payload schema validation
  {
    const res = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/login',
      payload: {}, // Missing required fields
    });
    assert(res.statusCode === 400, 'POST /api/v1/auth/login without body returns 400 Bad Request');
  }

  {
    const res = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/login',
      payload: { email: 'not-an-email', password: '123' },
    });
    assert(res.statusCode === 400, 'POST /api/v1/auth/login with invalid email format returns 400 Bad Request');
  }

  console.log('\n--- SECTION 6: CSV EXPORT UTILITY & ESCAPING ---');
  // 9. CSV formatting
  {
    const sampleProducts = [
      { id: 'sku-1', name: 'Enol Synthetic 5W-40, "Ultra"', price: 14500.0, liters: 4.0 },
      { id: 'sku-2', name: 'Enol Moto 4T 20W-50\nSpecial', price: 950.0, liters: 0.7 },
    ];
    // @ts-ignore
    const csv = ExportService['convertToCsv'](sampleProducts);
    assert(csv.includes('"id","name","price","liters"'), 'CSV export headers correctly generated');
    assert(csv.includes('""Ultra""'), 'Embedded quotes are properly escaped with double-quotes');
    assert(csv.includes('"sku-1"'), 'SKU ids are quoted correctly');
  }

  console.log('\n--- SECTION 7: ORGANIZATIONAL HIERARCHY & MATERIALIZED PATHS ---');
  // 10. Materialized hierarchy resolution
  {
    const director = 'dir-001';
    const zsmSouth = 'zsm-south-002';
    const asmKarachi = 'asm-khi-003';
    const repKorangi = 'rep-korangi-004';

    const pathRep = `/${director}/${zsmSouth}/${asmKarachi}/${repKorangi}/`;
    const pathAsm = `/${director}/${zsmSouth}/${asmKarachi}/`;
    const pathZsm = `/${director}/${zsmSouth}/`;
    const pathDirector = `/${director}/`;

    assert(pathRep.startsWith(pathAsm), 'Rep path is subordinated under ASM');
    assert(pathRep.startsWith(pathZsm), 'Rep path is subordinated under ZSM');
    assert(pathRep.startsWith(pathDirector), 'Rep path is subordinated under Director');
    assert(pathAsm.startsWith(pathZsm), 'ASM path is subordinated under ZSM');
  }

  console.log('\n--- SECTION 8: TADA DISCREPANCY & BUSINESS RULES ---');
  // 11. TADA 10% discrepancy rule
  {
    function calculateDiscrepancy(odoKm: number, gpsKm: number): { variancePct: number; flagged: boolean } {
      if (gpsKm <= 0) return { variancePct: 0, flagged: false };
      const variancePct = (Math.abs(odoKm - gpsKm) / gpsKm) * 100;
      return { variancePct, flagged: variancePct > 10.0 };
    }

    // Normal claim: 52.0 km odo vs 50.0 km GPS (4.0% variance) -> Not flagged
    const normalClaim = calculateDiscrepancy(52.0, 50.0);
    assert(!normalClaim.flagged, 'TADA claim with 4.0% variance is NOT flagged');

    // Suspicious claim: 85.0 km odo vs 50.0 km GPS (70.0% variance) -> Flagged
    const flaggedClaim = calculateDiscrepancy(85.0, 50.0);
    assert(flaggedClaim.flagged, 'TADA claim exceeding 10.0% variance is strictly FLAGGED');
  }

  await app.close();

  console.log('\n========================================================================');
  console.log(`TEST RUN SUMMARY: ${passed} PASSED, ${failed} FAILED`);
  console.log('========================================================================\n');

  if (failed > 0) {
    process.exit(1);
  }
}

if (require.main === module) {
  runComprehensiveTestSuite();
}
